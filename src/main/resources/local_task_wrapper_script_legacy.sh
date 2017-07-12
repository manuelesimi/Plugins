#!/bin/sh
WRAPPER_SCRIPT_PREFIX="local_task_wrapper_script"
. %JOB_DIR%/common.sh


function setup_task_functions {
    # define no-op function to be overridden as needed by task script:
    plugin_task() { echo; }
    # include the plugin_task function for the appropriate task:
    . ${JOB_DIR}/script.sh
    enforce_minimum_bound_on_align_parts
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
}

#in case the script is re-run from the command line, we need to set here the JOB dir
if [ -z "$JOB_DIR" ]; then
    export JOB_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
fi

export TMPDIR=$JOB_DIR

#JAVA_OPTS is used to set the amount of memory allocated to the groovy scripts.
export JAVA_OPTS=${PLUGIN_NEED_DEFAULT_JVM_OPTIONS}

cd ${JOB_DIR}

GOBY_DIR=${JOB_DIR}/goby
if [ ! -d ${GOBY_DIR} ]; then
    mkdir -p ${GOBY_DIR}
    /bin/cp ${JOB_DIR}/global_goby.jar ${GOBY_DIR}/goby.jar
    /bin/cp ${JOB_DIR}/log4j.properties ${GOBY_DIR}/
    /bin/cp ${JOB_DIR}/QueueWriter.groovy ${GOBY_DIR}/
    /bin/cp ${JOB_DIR}/TsvVcfToSqlite.groovy ${GOBY_DIR}/
    /bin/cp ${JOB_DIR}/icb-groovy-support.jar ${GOBY_DIR}/
    /bin/cp ${JOB_DIR}/artifact-manager.jar ${GOBY_DIR}/
    /bin/cp ${JOB_DIR}/serverside-dependencies.jar ${GOBY_DIR}/
    /bin/cp ${JOB_DIR}/stepslogger.jar ${GOBY_DIR}/

fi

 #make sure that the dir in which reads files will be stored exists
 mkdir -p ${FILESET_TARGET_DIR}
tail wuioweiwo
dieUponError "Failure"

setup_task_functions
initializeGobyWebArtifactEnvironment

install_resources


LOG_FILE="run-task-`date "+%Y-%m-%d-%H:%M:%S"`.log"

run_task 2>&1 |tee ${LOG_FILE}
if [ $?==0 ]; then
  echo "Task execution completed successfully." >>${LOG_FILE}

else
  echo "An error occurred"
fi