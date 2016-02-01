#!/bin/sh

# This script uses monitor.sh to show a progress bar while running install.sh
mkdir -p /tmp/cit/monitor/install-quickstart

echo "Installing Cloud Integrity Technology (R)..."
chmod +x monitor.sh install.sh
./monitor.sh install.sh install.sh.mark /tmp/cit/monitor/install-quickstart

# after installation is complete, display the endpoint URL for user
if [ $? -eq 0 ]; then
  cit config endpoint.url
else
  echo "Installation failed; log file is at /tmp/cit/monitor/install-quickstart/stdout"
fi
