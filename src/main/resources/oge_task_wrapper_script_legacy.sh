#!/bin/bash -l

# Execute the script from the current directory
#$ -cwd

# Combine SGE error and output files.
#$ -j y

# Cluster queue to use
#$ -q %QUEUE_NAME%

. %JOB_DIR%/common.sh
oge_task_parallel_wrapper_script.sh

function setup_task_functions {

    # define no-op function to be overridden as needed by task script:
    plugin_task() { echo; }
    # include the plugin_task function for the appropriate task:
    . ${JOB_DIR}/script.sh

}

function install_resources {
    #include needed function for resources installation
    ARTIFACT_REPOSITORY_DIR=%ARTIFACT_REPOSITORY_DIR%
    . %JOB_DIR%/artifacts.sh
     install_plugin_mandatory_artifacts
     if [ "${PLUGIN_ARTIFACTS_TASK}" != "false" ]; then
           install_plugin_artifacts
     fi
}


# Pushes some job metadata to the fileset area
# param $1: space-separated list of fileset tags registered by the job
function push_job_metadata {
   tags="$@"
   rm -rf ${JOB_DIR}/${TAG}.properties
   rm -rf %FILESET_AREA%/${TAG:0:1}/${TAG}
   echo "JOB=${TAG}" >> ${JOB_DIR}/${TAG}.properties
   echo "OWNER=${OWNER}" >> ${JOB_DIR}/${TAG}.properties
   echo "PLUGIN=${PLUGIN_ID}" >> ${JOB_DIR}/${stats_file}
   echo "COMPLETED=`date +"%Y-%m-%d %T%z"`" >> ${JOB_DIR}/${TAG}.properties
   echo "TAGS=${tags}" >> ${JOB_DIR}/${TAG}.properties
   echo "SHAREDWITH=" >> ${JOB_DIR}/${TAG}.properties
   REGISTERED_TAGS=`${FILESET_COMMAND} --push --fileset-tag ${TAG} JOB_METADATA: ${JOB_DIR}/${TAG}.properties`
   echo "The following JOB_METADATA instance has been successfully registered: ${REGISTERED_TAGS}"
}


function run_task {
   ALL_REGISTERED_TAGS=""
   plugin_task
   push_job_metadata ${ALL_REGISTERED_TAGS}
   ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_COMPLETED_STATUS} --description "Task completed" --index 0 --job-type job
}

setup


case ${STATE} in
    task)
        initializeGobyWebArtifactEnvironment
        setup_task_functions
        install_resources
        LOG_FILE="run-task-`date "+%Y-%m-%d-%H:%M:%S"`.log"

        #Aggregate metadata attributes to reduce the disk accesses
        ${FILESET_COMMAND} --aggregate-attributes *
        dieUponError "Unable to aggregate FileSet metadata before the task execution."

        run_task 2>&1 |tee ${LOG_FILE}
        STATUS=$?
        if [ ${STATUS}==0 ]; then
         echo "Task execution completed successfully." >>${LOG_FILE}
        else
         echo "An error occured"
         exit ${STATUS}
        fi
        ;;

    *)

esac

