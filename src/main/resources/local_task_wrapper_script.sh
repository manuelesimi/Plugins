#!/bin/sh


. constants.sh

function setup_task_functions {
    # define no-op function to be overridden as needed by task script:
    plugin_task() { echo; }
    # include the plugin_task function for the appropriate task:
    . ${JOB_DIR}/script.sh

}

function run_task {
   plugin_task
}

cd ${JOB_DIR}
setup_task_functions
run_task
