#!/bin/bash

# Script to trigger the installation of artifacts for a resource plugin. Does nothing after installation.

function install_resource {
   echo ""
}

#in case the script is re-run from the command line, we need to set here the JOB dir
if [ -z "$JOB_DIR" ]; then
    export JOB_DIR=="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
fi


    cd ${JOB_DIR}

    . constants.sh
    . auto-options.sh
    . artifacts.sh

LOG_FILE="resource-install-`date "+%Y-%m-%d-%H:%M:%S"`.log"
install_plugin_artifacts 2>&1 |tee ${LOG_FILE}
if [ $?==0 ]; then
  echo "Installation completed successfully." >>${LOG_FILE}
else
  echo "An error occured"
fi
