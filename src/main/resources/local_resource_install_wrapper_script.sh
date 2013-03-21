#!/bin/sh


. constants.sh

function setup_task_functions {
    # define no-op function to be overridden as needed by task script:
    plugin_task() { echo; }
    # include the plugin_task function for the appropriate task:
    . ${JOB_DIR}/script.sh

}

function install_resource {

   plugin_task
}

cd ${JOB_DIR}
. artifacts.sh

install_plugin_artifacts
setup_task_functions
install_resource
