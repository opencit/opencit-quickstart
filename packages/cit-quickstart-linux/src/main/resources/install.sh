#!/bin/bash

# CIT install script
# Outline:
# 1. look for ~/kms.env and source it if it's there
# 2. source the "functions.sh" file:  mtwilson-linux-util-3.0-SNAPSHOT.sh
# 3. determine if we are installing as root or non-root user; set paths
# 4. detect java
# 5. if java not installed, and we have it bundled, install it
# 6. unzip cit archive cit-zip-0.1-SNAPSHOT.zip into /opt/cit, overwrite if any files already exist
# 7. link /usr/local/bin/cit -> /opt/cit/bin/cit, if not already there
# 8. add cit to startup services
# 9. look for CIT_PASSWORD environment variable; if not present print help message and exit:
#    CIT requires a master password
#    to generate a password run "export CIT_PASSWORD=$(cit generate-password) && echo CIT_PASSWORD=$CIT_PASSWORD"
#    you must store this password in a safe place
#    losing the master password will result in data loss
# 10. cit setup
# 11. cit start

#####

# default settings
# note the layout setting is used only by this script
# and it is not saved or used by the app script
export CIT_HOME=${CIT_HOME:-/opt/cit}
CIT_LAYOUT=${CIT_LAYOUT:-home}

# the env directory is not configurable; it is defined as CIT_HOME/env and the
# administrator may use a symlink if necessary to place it anywhere else
export CIT_ENV=$CIT_HOME/env

# load application environment variables if already defined
if [ -d $CIT_ENV ]; then
  CIT_ENV_FILES=$(ls -1 $CIT_ENV/*)
  for env_file in $CIT_ENV_FILES; do
    . $env_file
    env_file_exports=$(cat $env_file | grep -E '^[A-Z0-9_]+\s*=' | cut -d = -f 1)
    if [ -n "$env_file_exports" ]; then eval export $env_file_exports; fi
  done
fi

# load installer environment file, if present
if [ -f ~/cit.env ]; then
  echo "Loading environment variables from $(cd ~ && pwd)/cit.env"
  . ~/cit.env
  env_file_exports=$(cat ~/cit.env | grep -E '^[A-Z0-9_]+\s*=' | cut -d = -f 1)
  if [ -n "$env_file_exports" ]; then eval export $env_file_exports; fi
else
  echo "No environment file"
fi

# functions script (mtwilson-linux-util-3.0-SNAPSHOT.sh) is required
# we use the following functions:
# java_detect java_ready_report 
# echo_failure echo_warning
# register_startup_script
UTIL_SCRIPT_FILE=`ls -1 mtwilson-linux-util-*.sh | head -n 1`
if [ -n "$UTIL_SCRIPT_FILE" ] && [ -f "$UTIL_SCRIPT_FILE" ]; then
  . $UTIL_SCRIPT_FILE
fi


# determine if we are installing as root or non-root
if [ "$(whoami)" == "root" ]; then
  # create a cit user if there isn't already one created
  CIT_USERNAME=${CIT_USERNAME:-cit}
  if ! getent passwd $CIT_USERNAME 2>&1 >/dev/null; then
    useradd --comment "jbuhacoff automation project" --home $CIT_HOME --system --shell /bin/false $CIT_USERNAME
    usermod --lock $CIT_USERNAME
    # note: to assign a shell and allow login you can run "usermod --shell /bin/bash --unlock $CIT_USERNAME"
  fi
else
  # already running as cit user
  CIT_USERNAME=$(whoami)
  echo_warning "Running as $CIT_USERNAME; if installation fails try again as root"
  if [ ! -w "$CIT_HOME" ] && [ ! -w $(dirname $CIT_HOME) ]; then
    export CIT_HOME=$(cd ~ && pwd)
  fi
fi

# if cit is already installed, stop it while we upgrade/reinstall
if which cit; then
  cit stop
fi

# define application directory layout
if [ "$CIT_LAYOUT" == "linux" ]; then
  export CIT_CONFIGURATION=${CIT_CONFIGURATION:-/etc/cit}
  export CIT_REPOSITORY=${CIT_REPOSITORY:-/var/opt/cit}
  export CIT_LOGS=${CIT_LOGS:-/var/log/cit}
elif [ "$CIT_LAYOUT" == "home" ]; then
  export CIT_CONFIGURATION=${CIT_CONFIGURATION:-$CIT_HOME/configuration}
  export CIT_REPOSITORY=${CIT_REPOSITORY:-$CIT_HOME/repository}
  export CIT_LOGS=${CIT_LOGS:-$CIT_HOME/logs}
fi
export CIT_BIN=${CIT_BIN:-$CIT_HOME/bin}
export CIT_JAVA=${CIT_JAVA:-$CIT_HOME/java}

# note that the env dir is not configurable; it is defined as "env" under home
export CIT_ENV=$CIT_HOME/env


cit_backup_configuration() {
  if [ -n "$CIT_CONFIGURATION" ] && [ -d "$CIT_CONFIGURATION" ]; then
    datestr=`date +%Y%m%d.%H%M`
    backupdir=$CIT_REPOSITORY/backup/cit.configuration.$datestr
    mkdir -p $backupdir
    cp -r $CIT_CONFIGURATION/* $backupdir/
  fi
}

# backup current configuration, if they exist
cit_backup_configuration

# create application directories (chown will be repeated near end of this script, after setup)
for directory in $CIT_HOME $CIT_CONFIGURATION $CIT_ENV $CIT_REPOSITORY $CIT_LOGS; do
  mkdir -p $directory
  chown -R $CIT_USERNAME:$CIT_USERNAME $directory
  chmod 700 $directory
done


# store directory layout in env file
echo "# $(date)" > $CIT_ENV/cit-layout
echo "export CIT_HOME=$CIT_HOME" >> $CIT_ENV/cit-layout
echo "export CIT_CONFIGURATION=$CIT_CONFIGURATION" >> $CIT_ENV/cit-layout
echo "export CIT_JAVA=$CIT_JAVA" >> $CIT_ENV/cit-layout
echo "export CIT_BIN=$CIT_BIN" >> $CIT_ENV/cit-layout
echo "export CIT_REPOSITORY=$CIT_REPOSITORY" >> $CIT_ENV/cit-layout
echo "export CIT_LOGS=$CIT_LOGS" >> $CIT_ENV/cit-layout
if [ -n "$CIT_PID_FILE" ]; then echo "export CIT_PID_FILE=$CIT_PID_FILE" >> $CIT_ENV/cit-layout; fi

# store cit username in env file
echo "# $(date)" > $CIT_ENV/cit-username
echo "export CIT_USERNAME=$CIT_USERNAME" >> $CIT_ENV/cit-username

# store log level in env file, if it's set
if [ -n "$CIT_LOG_LEVEL" ]; then
  echo "# $(date)" > $CIT_ENV/cit-logging
  echo "export CIT_LOG_LEVEL=$CIT_LOG_LEVEL" >> $CIT_ENV/cit-logging
fi

# store the auto-exported environment variables in temporary env file
# to make them available after the script uses sudo to switch users;
# we delete that file later
echo "# $(date)" > $CIT_ENV/cit-setup
for env_file_var_name in $env_file_exports
do
  eval env_file_var_value="\$$env_file_var_name"
  echo "export $env_file_var_name=$env_file_var_value" >> $CIT_ENV/cit-setup
done

# cit requires java 1.7 or later
# detect or install java (jdk-1.7.0_51-linux-x64.tar.gz)
echo "Installing Java..."
JAVA_REQUIRED_VERSION=${JAVA_REQUIRED_VERSION:-1.7}
JAVA_PACKAGE=`ls -1 jdk-* jre-* java-*.bin 2>/dev/null | tail -n 1`
# check if java is readable to the non-root user
if [ -z "$JAVA_HOME" ]; then
  java_detect > /dev/null
fi
if [ -n "$JAVA_HOME" ]; then
  if [ $(whoami) == "root" ]; then
    JAVA_USER_READABLE=$(sudo -u $CIT_USERNAME /bin/bash -c "if [ -r $JAVA_HOME ]; then echo 'yes'; fi")
  else
    JAVA_USER_READABLE=$(/bin/bash -c "if [ -r $JAVA_HOME ]; then echo 'yes'; fi")
  fi
fi
if [ -z "$JAVA_HOME" ] || [ -z "$JAVA_USER_READABLE" ]; then
  JAVA_HOME=$CIT_HOME/share/jdk1.7.0_51
fi
mkdir -p $JAVA_HOME
java_install_in_home $JAVA_PACKAGE
echo "# $(date)" > $CIT_ENV/cit-java
echo "export JAVA_HOME=$JAVA_HOME" >> $CIT_ENV/cit-java
echo "export JAVA_CMD=$JAVA_HOME/bin/java" >> $CIT_ENV/cit-java
echo "export JAVA_REQUIRED_VERSION=$JAVA_REQUIRED_VERSION" >> $CIT_ENV/cit-java


# make sure unzip and authbind are installed
CIT_YUM_PACKAGES="zip unzip authbind"
CIT_APT_PACKAGES="zip unzip authbind"
CIT_YAST_PACKAGES="zip unzip authbind"
CIT_ZYPPER_PACKAGES="zip unzip authbind"
auto_install "Installer requirements" "CIT"

# setup authbind to allow non-root cit to listen on ports 80 and 443
CIT_PORT_HTTP=${CIT_PORT_HTTP:-${JETTY_PORT:-80}}
CIT_PORT_HTTPS=${CIT_PORT_HTTPS:-${JETTY_SECURE_PORT:-443}}
if [ -n "$CIT_USERNAME" ] && [ "$CIT_USERNAME" != "root" ] && [ -d /etc/authbind/byport ] && [ "$CIT_PORT_HTTP" -lt "1024" ]; then
  touch /etc/authbind/byport/$CIT_PORT_HTTP
  chmod 500 /etc/authbind/byport/$CIT_PORT_HTTP
  chown $CIT_USERNAME /etc/authbind/byport/$CIT_PORT_HTTP
fi
if [ -n "$CIT_USERNAME" ] && [ "$CIT_USERNAME" != "root" ] && [ -d /etc/authbind/byport ] && [ "$CIT_PORT_HTTPS" -lt "1024" ]; then
  touch /etc/authbind/byport/$CIT_PORT_HTTPS
  chmod 500 /etc/authbind/byport/$CIT_PORT_HTTPS
  chown $CIT_USERNAME /etc/authbind/byport/$CIT_PORT_HTTPS
fi

# delete existing java files, to prevent a situation where the installer copies
# a newer file but the older file is also there
if [ -d $CIT_HOME/java ]; then
  rm $CIT_HOME/java/*.jar
fi

# extract cit  (cit-zip-0.1-SNAPSHOT.zip)
echo "Extracting application..."
CIT_ZIPFILE=`ls -1 cit-*.zip 2>/dev/null | head -n 1`
unzip -oq $CIT_ZIPFILE -d $CIT_HOME

# if the configuration folder was specified, move the default configurations there
# that were extracted from the zip
if [ "$CIT_CONFIGURATION" != "$CIT_HOME/configuration" ]; then
  # only copy files that don't already exist in destination, to avoid overwriting
  # user's prior edits
  cp -n $CIT_HOME/configuration/* $CIT_CONFIGURATION/
  # in the future, if we have a version variable we could move the remaining
  # files into the configuration directory in a versioned subdirectory.
  # finally, remove the configuration folder so user will not be confused about
  # where to edit. 
  rm -rf $CIT_HOME/configuration
fi

# copy utilities script file to application folder
cp $UTIL_SCRIPT_FILE $CIT_HOME/bin/functions.sh

# set permissions
chown -R $CIT_USERNAME:$CIT_USERNAME $CIT_HOME
chmod 755 $CIT_HOME/bin/*

# link /usr/local/bin/cit -> /opt/cit/bin/cit
EXISTING_CIT_COMMAND=`which cit`
if [ -z "$EXISTING_CIT_COMMAND" ]; then
  ln -s $CIT_HOME/bin/cit.sh /usr/local/bin/cit
fi


# register linux startup script
if [ "$CIT_USERNAME" == "root" ]; then
  register_startup_script $CIT_HOME/bin/cit.sh cit
else
  echo "@reboot $CIT_HOME/bin/cit.sh start" > $CIT_CONFIGURATION/crontab
  crontab -u $CIT_USERNAME -l | cat - $CIT_CONFIGURATION/crontab | crontab -u $CIT_USERNAME -
fi

# add log rotation
if [ -d /etc/logrotate.d ]; then
  echo -e "$CIT_LOGS/*.log {\n  daily\n  missingok\n  compress\n  notifempty\n  copytruncate\n  rotate 7\n  size 10m\n}" > /etc/logrotate.d/cit
fi

# setup the cit, unless the NOSETUP variable is defined
if [ -z "$CIT_NOSETUP" ]; then

  # the master password is required
  if [ -z "$CIT_PASSWORD" ] && [ ! -f $CIT_CONFIGURATION/.cit_password ]; then
    touch $CIT_CONFIGURATION/.cit_password
    chown $CIT_USERNAME:$CIT_USERNAME $CIT_CONFIGURATION/.cit_password
    cit generate-password > $CIT_CONFIGURATION/.cit_password
  fi

  cit config mtwilson.extensions.fileIncludeFilter.contains "${MTWILSON_EXTENSIONS_FILEINCLUDEFILTER_CONTAINS:-mtwilson,cit,jersey-media-multipart}" >/dev/null
  cit config mtwilson.extensions.packageIncludeFilter.startsWith "${MTWILSON_EXTENSIONS_PACKAGEINCLUDEFILTER_STARTSWITH:-com.intel,org.glassfish.jersey.media.multipart}" >/dev/null
  cit config jetty.port ${JETTY_PORT:-80} >/dev/null
  cit config jetty.secure.port ${JETTY_SECURE_PORT:-443} >/dev/null

  cit config mtwilson.navbar.buttons com.intel.mtwilson.deployment.wizard,mtwilson-core-html5 >/dev/null
  cit config mtwilson.navbar.hometab com.intel.mtwilson.deployment.wizard >/dev/null

  cit setup

  # temporary fix for bug #5008
  echo >> $CIT_CONFIGURATION/extensions.cache
  echo org.glassfish.jersey.media.multipart.MultiPartFeature >> $CIT_CONFIGURATION/extensions.cache

  # create an anonymous user for open access to the cit
  # (must be after setup because password command is added via extensions)
  cit password anonymous --nopass --permissions *:*

  # extend session idle timeout to 24 hours
  cit config login.token.expires.minutes 1440 >/dev/null

fi

# delete the temporary setup environment variables file
rm -f $CIT_ENV/cit-setup

# if the installation package includes the component installers, copy them
# now to the package repository
#director-0.1-SNAPSHOT.bin -> cit3-director.bin
DIRECTOR_BIN=`ls -1 director-*.bin | head -n 1`
if [ -n "$DIRECTOR_BIN" ]; then
  echo "Copying Trust Director installer..."
  mkdir -p $CIT_HOME/repository/packages/director
  cp $DIRECTOR_BIN $CIT_HOME/repository/packages/director/cit3-director.bin
fi
#kms-linux-makeself-0.1-SNAPSHOT.bin -> cit3-keybroker.bin
KMS_BIN=`ls -1 kms-*.bin | head -n 1`
if [ -n "$KMS_BIN" ]; then
  echo "Copying Key Broker installer..."
  mkdir -p $CIT_HOME/repository/packages/key_broker
  cp $KMS_BIN $CIT_HOME/repository/packages/key_broker/cit3-keybroker.bin
fi
#kmsproxy-linux-makeself-0.1-SNAPSHOT.bin -> cit3-keybrokerproxy.bin
KMSPROXY_BIN=`ls -1 kmsproxy-*.bin | head -n 1`
if [ -n "$KMSPROXY_BIN" ]; then
  echo "Copying Key Broker Proxy installer..."
  mkdir -p $CIT_HOME/repository/packages/key_broker_proxy
  cp $KMSPROXY_BIN $CIT_HOME/repository/packages/key_broker_proxy/cit3-keybrokerproxy.bin
fi
#mtwilson-openstack-controller-0.1-20160113.122000-74.bin -> cit3-openstack-extensions.bin
OPENSTACK_EXTENSIONS_BIN=`ls -1 mtwilson-openstack-controller-*.bin | head -n 1`
if [ -n "$OPENSTACK_EXTENSIONS_BIN" ]; then
  echo "Copying OpenStack Extensions installer..."
  mkdir -p $CIT_HOME/repository/packages/openstack_extensions
  cp $OPENSTACK_EXTENSIONS_BIN $CIT_HOME/repository/packages/openstack_extensions/cit3-openstack-extensions.bin
fi
#mtwilson-server-3.0-20160113.120305-66.bin -> cit3-attestation.bin
ATTESTATION_SERVICE_BIN=`ls -1 mtwilson-server-*.bin | head -n 1`
if [ -n "$ATTESTATION_SERVICE_BIN" ]; then
  echo "Copying Attestation Service installer..."
  mkdir -p $CIT_HOME/repository/packages/attestation_service
  cp $ATTESTATION_SERVICE_BIN $CIT_HOME/repository/packages/attestation_service/cit3-attestation.bin
fi
#mtwilson-openstack-trusted-node-ubuntu-0.1-SNAPSHOT.bin -> cit3-openstack-trusted-node-ubuntu.bin
TRUSTAGENT_UBUNTU_BIN=`ls -1 mtwilson-openstack-trusted-node-ubuntu-*.bin | head -n 1`
if [ -n "$TRUSTAGENT_UBUNTU_BIN" ]; then
  echo "Copying Trust Agent Ubuntu installer..."
  mkdir -p $CIT_HOME/repository/packages/trustagent_ubuntu
  cp $TRUSTAGENT_UBUNTU_BIN $CIT_HOME/repository/packages/trustagent_ubuntu/cit3-openstack-trusted-node-ubuntu.bin
fi

TRUSTAGENT_RHEL_BIN=`ls -1 mtwilson-openstack-trusted-node-rhel-*.bin | head -n 1`
if [ -n "$TRUSTAGENT_UBUNTU_BIN" ]; then
  echo "Copying Trust Agent RHEL installer..."
  mkdir -p $CIT_HOME/repository/packages/trustagent_ubuntu
  cp $TRUSTAGENT_RHEL_BIN $CIT_HOME/repository/packages/trustagent_ubuntu/cit3-openstack-trusted-node-rhel.bin
fi

# ensure the cit owns all the content created during setup
for directory in $CIT_HOME $CIT_CONFIGURATION $CIT_JAVA $CIT_BIN $CIT_ENV $CIT_REPOSITORY $CIT_LOGS; do
  chown -R $CIT_USERNAME:$CIT_USERNAME $directory
done

# start the server, unless the NOSETUP variable is defined
if [ -z "$CIT_NOSETUP" ]; then cit start; fi
