#!/bin/sh

# Script to trigger the installation of artifacts for a resource plugin. Does nothing after installation.
. constants.sh


function install_resource {

   plugin_task
}

cd ${JOB_DIR}
. artifacts.sh

install_plugin_artifacts


