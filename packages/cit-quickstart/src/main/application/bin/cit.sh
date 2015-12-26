#!/bin/bash

# chkconfig: 2345 80 30
# description: QUICK START FOR CLOUD INTEGRITY TECHNOLOGY

### BEGIN INIT INFO
# Provides:          cit
# Required-Start:    $remote_fs $syslog
# Required-Stop:     $remote_fs $syslog
# Should-Start:      $portmap
# Should-Stop:       $portmap
# X-Start-Before:    nis
# X-Stop-After:      nis
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# X-Interactive:     true
# Short-Description: cit
# Description:       Main script to run cit commands
### END INIT INFO
DESC="CIT"
NAME=cit

# the home directory must be defined before we load any environment or
# configuration files; it is explicitly passed through the sudo command
export CIT_HOME=${CIT_HOME:-/opt/cit}

# the env directory is not configurable; it is defined as CIT_HOME/env and the
# administrator may use a symlink if necessary to place it anywhere else
export CIT_ENV=$CIT_HOME/env

cit_load_env() {
  local env_files="$@"
  local env_file_exports
  for env_file in $env_files; do
    if [ -n "$env_file" ] && [ -f "$env_file" ]; then
      . $env_file
      env_file_exports=$(cat $env_file | grep -E '^[A-Z0-9_]+\s*=' | cut -d = -f 1)
      if [ -n "$env_file_exports" ]; then eval export $env_file_exports; fi
    fi
  done  
}

# load environment variables; these override any existing environment variables.
# the idea is that if someone wants to override these, they must have write
# access to the environment files that we load here. 
if [ -d $CIT_ENV ]; then
  cit_load_env $(ls -1 $CIT_ENV/*)
fi

###################################################################################################

# if non-root execution is specified, and we are currently root, start over; the CIT_SUDO variable limits this to one attempt
# we make an exception for the uninstall command, which may require root access to delete users and certain directories
if [ -n "$CIT_USERNAME" ] && [ "$CIT_USERNAME" != "root" ] && [ $(whoami) == "root" ] && [ -z "$CIT_SUDO" ] && [ "$1" != "uninstall" ]; then
  export CIT_SUDO=true
  sudo -u $CIT_USERNAME -H -E cit $*
  exit $?
fi

###################################################################################################

# default directory layout follows the 'home' style
export CIT_CONFIGURATION=${CIT_CONFIGURATION:-${CIT_CONF:-$CIT_HOME/configuration}}
export CIT_JAVA=${CIT_JAVA:-$CIT_HOME/java}
export CIT_BIN=${CIT_BIN:-$CIT_HOME/bin}
export CIT_REPOSITORY=${CIT_REPOSITORY:-$CIT_HOME/repository}
export CIT_LOGS=${CIT_LOGS:-$CIT_HOME/logs}

###################################################################################################


# load linux utility
if [ -f "$CIT_HOME/bin/functions.sh" ]; then
  . $CIT_HOME/bin/functions.sh
fi

###################################################################################################

# stored master password
if [ -z "$CIT_PASSWORD" ] && [ -f $CIT_CONFIGURATION/.cit_password ]; then
  export CIT_PASSWORD=$(cat $CIT_CONFIGURATION/.cit_password)
fi

# all other variables with defaults
CIT_HTTP_LOG_FILE=${CIT_HTTP_LOG_FILE:-$CIT_LOGS/http.log}
JAVA_REQUIRED_VERSION=${JAVA_REQUIRED_VERSION:-1.7}
JAVA_OPTS=${JAVA_OPTS:-"-Dlogback.configurationFile=$CIT_CONFIGURATION/logback.xml"}

CIT_SETUP_FIRST_TASKS=${CIT_SETUP_FIRST_TASKS:-"update-extensions-cache-file"}
CIT_SETUP_TASKS=${CIT_SETUP_TASKS:-"password-vault jetty-ports jetty-tls-keystore shiro-ssl-port"}

# the standard PID file location /var/run is typically owned by root;
# if we are running as non-root and the standard location isn't writable 
# then we need a different place
CIT_PID_FILE=${CIT_PID_FILE:-/var/run/cit.pid}
touch $CIT_PID_FILE >/dev/null 2>&1
if [ $? == 1 ]; then CIT_PID_FILE=$CIT_LOGS/cit.pid; fi

###################################################################################################

# java command
if [ -z "$JAVA_CMD" ]; then
  if [ -n "$JAVA_HOME" ]; then
    JAVA_CMD=$JAVA_HOME/bin/java
  else
    JAVA_CMD=`which java`
  fi
fi

# generated variables; look for common jars and feature-specific jars
JARS=$(ls -1 $CIT_JAVA/*.jar $CIT_HOME/features/*/java/*.jar)
CLASSPATH=$(echo $JARS | tr ' ' ':')

# the classpath is long and if we use the java -cp option we will not be
# able to see the full command line in ps because the output is normally
# truncated at 4096 characters. so we export the classpath to the environment
export CLASSPATH

###################################################################################################

# run a cit command
cit_run() {
  local args="$*"
  $JAVA_CMD $JAVA_OPTS com.intel.mtwilson.launcher.console.Main $args
  return $?
}

# run default set of setup tasks and check if admin user needs to be created
cit_complete_setup() {
  # run all setup tasks, don't use the force option to avoid clobbering existing
  # useful configuration files
  cit_run setup $CIT_SETUP_FIRST_TASKS
  cit_run setup $CIT_SETUP_TASKS
}

# arguments are optional, if provided they are the names of the tasks to run, in order
cit_setup() {
  local args="$*"
  $JAVA_CMD $JAVA_OPTS com.intel.mtwilson.launcher.console.Main setup $args
  return $?
}

cit_start() {
    if [ -z "$CIT_PASSWORD" ]; then
      echo_failure "Master password is required; export CIT_PASSWORD"
      return 1
    fi

    # check if we're already running - don't start a second instance
    if cit_is_running; then
        echo "CIT is running"
        return 0
    fi

    # check if we need to use authbind or if we can start java directly
    prog="$JAVA_CMD"
    if [ -n "$CIT_USERNAME" ] && [ "$CIT_USERNAME" != "root" ] && [ $(whoami) != "root" ] && [ -n "$(which authbind 2>/dev/null)" ]; then
      prog="authbind $JAVA_CMD"
      JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"
    fi

    # the subshell allows the java process to have a reasonable current working
    # directory without affecting the user's working directory. 
    # the last background process pid $! must be stored from the subshell.
    (
      cd $CIT_HOME
      $prog $JAVA_OPTS com.intel.mtwilson.launcher.console.Main jetty-start >>$CIT_HTTP_LOG_FILE 2>&1 &
      echo $! > $CIT_PID_FILE
    )
    if cit_is_running; then
      echo_success "Started CIT"
    else
      echo_failure "Failed to start CIT"
    fi
}

# returns 0 if CIT is running, 1 if not running
# side effects: sets CIT_PID if CIT is running, or to empty otherwise
cit_is_running() {
  CIT_PID=
  if [ -f $CIT_PID_FILE ]; then
    CIT_PID=$(cat $CIT_PID_FILE)
    local is_running=`ps -A -o pid | grep "^\s*${CIT_PID}$"`
    if [ -z "$is_running" ]; then
      # stale PID file
      CIT_PID=
    fi
  fi
  if [ -z "$CIT_PID" ]; then
    # check the process list just in case the pid file is stale
    CIT_PID=$(ps -A ww | grep -v grep | grep java | grep "com.intel.mtwilson.launcher.console.Main jetty-start" | grep "$CIT_CONFIGURATION" | awk '{ print $1 }')
  fi
  if [ -z "$CIT_PID" ]; then
    # CIT is not running
    return 1
  fi
  # CIT is running and CIT_PID is set
  return 0
}


cit_stop() {
  if cit_is_running; then
    kill -9 $CIT_PID
    if [ $? ]; then
      echo "Stopped CIT"
      # truncate pid file instead of erasing,
      # because we may not have permission to create it
      # if we're running as a non-root user
      echo > $CIT_PID_FILE
    else
      echo "Failed to stop CIT"
    fi
  fi
}

# removes CIT home directory (including configuration and data if they are there).
# if you need to keep those, back them up before calling uninstall,
# or if the configuration and data are outside the home directory
# they will not be removed, so you could configure CIT_CONFIGURATION=/etc/cit
# and CIT_REPOSITORY=/var/opt/cit and then they would not be deleted by this.
cit_uninstall() {
    remove_startup_script cit
	rm -f /usr/local/bin/cit
    if [ -z "$CIT_HOME" ]; then
      echo_failure "Cannot uninstall because CIT_HOME is not set"
      return 1
    fi
    if [ "$1" == "--purge" ]; then
      rm -rf $CIT_HOME $CIT_CONFIGURATION $CIT_DATA $CIT_LOGS
    else
      rm -rf $CIT_HOME/bin $CIT_HOME/java $CIT_HOME/features
    fi
    groupdel $CIT_USERNAME > /dev/null 2>&1
    userdel $CIT_USERNAME > /dev/null 2>&1
}

print_help() {
    echo "Usage: $0 start|stop|uninstall|version"
    echo "Usage: $0 setup [--force|--noexec] [task1 task2 ...]"
    echo "Available setup tasks:"
    echo $CIT_SETUP_TASKS | tr ' ' '\n'
}

###################################################################################################

# here we look for specific commands first that we will handle in the
# script, and anything else we send to the java application

case "$1" in
  help)
    print_help
    ;;
  start)
    cit_start
    ;;
  stop)
    cit_stop
    ;;
  restart)
    cit_stop
    cit_start
    ;;
  status)
    if cit_is_running; then
      echo "CIT is running"
      exit 0
    else
      echo "CIT is not running"
      exit 1
    fi
    ;;
  setup)
    shift
    if [ -n "$1" ]; then
      cit_setup $*
    else
      cit_complete_setup
    fi
    ;;
  uninstall)
    shift
    cit_stop
    cit_uninstall $*
    ;;
  *)
    if [ -z "$*" ]; then
      print_help
    else
      #echo "args: $*"
      $JAVA_CMD $JAVA_OPTS com.intel.mtwilson.launcher.console.Main $*
    fi
    ;;
esac


exit $?
