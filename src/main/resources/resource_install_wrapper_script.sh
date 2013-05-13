#!/bin/bash

# Script to trigger the installation of artifacts for a resource plugin. Does nothing after installation.
function install_resource {

   echo ""
}

#in case the script is re-run from the command line, we need to set here the JOB dir
if [ -z "$JOB_DIR" ]; then
    export JOB_DIR=="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
fi

. constants.sh

cd ${JOB_DIR}
. artifacts.sh

install_plugin_artifacts 2>&1 |tee resource-install-`date "+%Y-%m-%d-%H:%M:%S"`.log



