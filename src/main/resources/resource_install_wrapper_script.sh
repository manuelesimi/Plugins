#!/bin/sh

# Script to trigger the installation of artifacts for a resource plugin. Does nothing after installation.

#in case the script is re-run from the command line, we need to set here the JOB dir
if [ -z "$JOB_DIR" ]; then
    export JOB_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
fi


function install_resource {

   echo ""
}

cd $JOB_DIR

. constants.sh

. artifacts.sh

install_plugin_artifacts


