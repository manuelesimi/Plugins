#!/usr/bin/env bash

export JOB_DIR=%JOB_DIR%
echo "Sourcing GobyWeb plugin environment (constants.sh and auto-options.sh)"

. ${JOB_DIR}/auto-options.sh
. ${JOB_DIR}/constants.sh

if [[ ! $TMPDIR ]]; then
    echo "TMPDIR is not set or empty"
    if [[ ! $SGE_O_WORKDIR ]]; then
       # Not running inside SGE yet? Use the jobdir as TMPDIR:
        #export TMPDIR="${SGE_O_WORKDIR}"
        echo "Use JOB_DIR"
        export TMPDIR=${JOB_DIR}
    else
        # Running inside SGE? Switch to the TMPDIR:
        echo "Use SGE_O_WORKDIR"
        export TMPDIR="${SGE_O_WORKDIR}"
        mkdir -p ${TMPDIR}
        cd ${TMPDIR}
    fi
else
  mkdir -p ${TMPDIR}
  cd ${TMPDIR}
fi

if [ -d "${TMPDIR}" ]; then
    export TMP_NODE_WORK_DIR=${TMPDIR}
    if [ ! -e ${TMP_NODE_WORK_DIR}/goby.jar ]; then
             /bin/cp ${SGE_O_WORKDIR}/global_goby.jar ${TMP_NODE_WORK_DIR}/goby.jar
             /bin/cp ${SGE_O_WORKDIR}/log4j.properties ${TMP_NODE_WORK_DIR}/
             /bin/cp ${SGE_O_WORKDIR}/QueueWriter.groovy ${TMP_NODE_WORK_DIR}
             /bin/cp ${SGE_O_WORKDIR}/TsvVcfToSqlite.groovy ${TMP_NODE_WORK_DIR}
             /bin/cp ${SGE_O_WORKDIR}/icb-groovy-support.jar ${TMP_NODE_WORK_DIR}
             /bin/cp ${SGE_O_WORKDIR}/artifact-manager.jar ${TMP_NODE_WORK_DIR}
             /bin/cp ${SGE_O_WORKDIR}/serverside-dependencies.jar ${TMP_NODE_WORK_DIR}
             /bin/cp ${SGE_O_WORKDIR}/stepslogger.jar ${TMP_NODE_WORK_DIR}
    fi
fi

function initializeJobEnvironment {
    set -x
    export JOB_DIR=%JOB_DIR%
    echo "Sourcing GobyWeb plugin environment (constants.sh and auto-options.sh)"
    . ${JOB_DIR}/constants.sh
    . ${JOB_DIR}/auto-options.sh
    setup_plugin_functions
    echo "Using container technology: ${GOBYWEB_CONTAINER_TECHNOLOGY}"
    export RESULT_DIR=${JOB_DIR}/results/${TAG}

    #make sure that the dir in which files will be stored exists
    if [ -z "${FILESET_TARGET_DIR+set}" ]; then
        mkdir -p ${FILESET_TARGET_DIR}
    else
        echo "FILESET_TARGET_DIR not set (is this a resource installation?)"
    fi

    case ${GOBYWEB_CONTAINER_TECHNOLOGY} in
        singularity)
         echo "Calling legacy script with Singularity"
         info ${EXECUTION_ACTIVITY} "Calling legacy script with Singularity"
         export SINGULARITY_CACHEDIR=/scratchLocal/gobyweb/gobyweb3/SINGULARITY_CACHE
         mkdir -p ${SINGULARITY_CACHEDIR}
         function delegate_oge_job_script {
            export STATE="$1"
            singularity exec \
                    -B ${FILESET_AREA}:${FILESET_AREA} \
                    -B ${JOB_DIR}:${JOB_DIR} \
                    -B ${ARTIFACT_REPOSITORY_DIR}:${ARTIFACT_REPOSITORY_DIR} \
                    -B ~/mail:/etc/mail \
                    ${GOBYWEB_CONTAINER_NAME} %JOB_DIR%/${WRAPPER_SCRIPT_PREFIX}_legacy.sh "$1" && \
                    dieUponError 'Unable to execute with singularity container'
         }
      ;;
     none)
        echo "Calling legacy script directly"
        function delegate_oge_job_script {
            %JOB_DIR%/${WRAPPER_SCRIPT_PREFIX}_legacy.sh "$1" && \
            dieUponError 'Unable to directly delegate to legacy script'
        }
     ;;
    esac
}


function initializeGobyWebArtifactEnvironment {
    initializeJobEnvironment
    export ARTIFACT_REPOSITORY_DIR=%ARTIFACT_REPOSITORY_DIR%
    . ${JOB_DIR}/artifacts.sh
}

function setup_plugin_functions {
    # define no-op function to be overridden as needed by plugin script:
    plugin_alignment_combine() { echo; }
    plugin_alignment_analysis_sequential() { echo; }
    plugin_alignment_analysis_split() { echo; }
    plugin_alignment_analysis_process() { echo; }
    plugin_alignment_analysis_combine() { echo; }
    plugin_alignment_analysis_num_parts() { echo; }
    # include the plugin_align function for the appropriate aligner:
    if [ -e "${JOB_DIR}/script.sh" ]; then
        . ${JOB_DIR}/script.sh
    else
        echo "No script.sh detected in the JOB_DIR (is this a resource installation?)"
    fi
    enforce_minimum_bound_on_align_parts
}

function trace {
    echo "$*";
}
 
function pushEventFile {
    java -Dlog4j.configuration=${RESOURCES_GOBYWEB_SERVER_SIDE_LOG4J_PROPERTIES} \
    -cp ${RESOURCES_GOBYWEB_SERVER_SIDE_EVENT_TOOLS_JAR} \
    org.campagnelab.gobyweb.events.tools.PushEvents \
    -p "$1" ${QUEUE_WRITER_POSTFIX}
}

#LOG fileset activity
function LOG_JS {
    LEVEL=$1
    STATUS=$2
    FS_TAG=$3
    SLOT=$4
    EVENT_FILE=${TMPDIR}/job-slot-events-`date +%s`.proto
    java -Dlog4j.configuration=${RESOURCES_GOBYWEB_SERVER_SIDE_LOG4J_PROPERTIES} \
        -cp ${RESOURCES_GOBYWEB_SERVER_SIDE_EVENT_TOOLS_JAR} \
        org.campagnelab.gobyweb.events.tools.JobSlotEvent --new-status ${STATUS} \
        --source-tag ${TAG} --tag ${FS_TAG} -p ${EVENT_FILE} \
        --slot-name ${SLOT} --level ${LEVEL}
    pushEventFile ${EVENT_FILE}

}

function filesetRegistered {
   echo "$*";
   LOG_JS "debug" "REGISTERED" $*
}

function filesetFailed {
   echo "$*";
   LOG_JS "error" "FAILURE" $*
}

function LOG {
    LEVEL=$1
    shift
    INDEX=$1
    shift
    MESSAGE="$*";
    EVENT_FILE=${TMPDIR}/events-`date +%s`.proto
    #java -Dlog4j.configuration=${RESOURCES_GOBYWEB_SERVER_SIDE_LOG4J_PROPERTIES} \
    #    -cp ${RESOURCES_GOBYWEB_SERVER_SIDE_EVENT_TOOLS_JAR} \
    #    org.campagnelab.gobyweb.events.tools.LogEvent \
    #    --phase ${STATE} --index ${INDEX} --message ${MESSAGE} \
    #    --tag ${TAG} -p ${EVENT_FILE} --level ${LEVEL}
    #pushEventFile ${EVENT_FILE}
}

function LOG_JOB_STATUS {
    STATUS=$1
    MESSAGE =$2
    INDEX=$3
    EVENT_FILE=${TMPDIR}/events-`date +%s`.proto
    #java -Dlog4j.configuration=${RESOURCES_GOBYWEB_SERVER_SIDE_LOG4J_PROPERTIES} \
    #    -cp ${RESOURCES_GOBYWEB_SERVER_SIDE_EVENT_TOOLS_JAR} \
    #    org.campagnelab.gobyweb.events.tools.JobStatusEvent  \
    #    --phase ${STATE} --index ${INDEX} --message "${MESSAGE}" \
    #    --new-status ${STATUS} --tag ${TAG} -p ${EVENT_FILE}
    #pushEventFile ${EVENT_FILE}
}

function LOG_JOB_EVENT {
   NAME=$1
   TYPE=$2
   INDEXES=$3
   MAX_INDEX_PARAM=
   if [[ $INDEXES ]]; then
    MAX_INDEX_PARAM="--max-index ${INDEXES}"
   else
    MAX_INDEX_PARAM="--max-index 1"
   fi
   EVENT_FILE=${TMPDIR}/events-`date +%s`.proto
   #java -Dlog4j.configuration=${RESOURCES_GOBYWEB_SERVER_SIDE_LOG4J_PROPERTIES} \
   #     -cp ${RESOURCES_GOBYWEB_SERVER_SIDE_EVENT_TOOLS_JAR} \
   #     org.campagnelab.gobyweb.events.tools.JobEvent \
   #     --name ${NAME} --type ${TYPE} \
   #     ${MAX_INDEX_PARAM} --tag ${TAG} -p ${EVENT_FILE}
   # pushEventFile ${EVENT_FILE}
}

function trace {
    trace_index 1 "$*";
}

function trace_index {
    INDEX=$1
    shift
    LOG "trace" ${INDEX} "$*";
}

function debug {
    debug_index 1 "$*";
}

function debug_index {
    INDEX=$1
    shift
    LOG "debug" ${INDEX} "$*";
}

function info {
    info_index 1 "$*";
}

function info_index {
    INDEX=$1
    shift
    LOG "info" ${INDEX} "$*";
}

function error {
    error_index 1 "$*";
}

function error_index {
    INDEX=$1
    shift
    LOG "error" ${INDEX} "$*";
}

function newActivity {
    ACTIVITY=$1
    LOG_JOB_EVENT ${ACTIVITY} "NEW_ACTIVITY";
    CURRENT_ACTIVITY=${ACTIVITY}
}

function activityCompleted {
    ACTIVITY=$1
    LOG_JOB_EVENT ${ACTIVITY} "ACTIVITY_COMPLETED";
    CURRENT_ACTIVITY=""
}

function newPhase {
    NUM_OF_INDEXES=$1
    LOG_JOB_EVENT ${STATE} "NEW_PHASE" ${NUM_OF_INDEXES};
}

function phaseCompleted {
    PHASE_INDEX=$1
    LOG_JOB_EVENT ${STATE} "PHASE_COMPLETED" ${PHASE_INDEX};
}

if [ -z "${GOBYWEB_CONTAINER_TECHNOLOGY+set}" ]; then
    export GOBYWEB_CONTAINER_TECHNOLOGY="none"
else
    if [ -z "${GOBYWEB_CONTAINER_NAME+set}" ]; then
    export GOBYWEB_CONTAINER_NAME="shub://CampagneLaboratory/GobyWeb-Singularity"
    fi
fi

if [ -z "${TMPDIR+set}" ]; then
    if [ -z  "${SGE_O_WORKDIR}+set" ]; then
       # Not running inside SGE yet? Use the jobdir as TMPDIR:
     #   export TMPDIR="${SGE_O_WORKDIR}"
        export TMPDIR=${JOB_DIR}
    else
        # Running inside SGE? Switch to the TMPDIR:
        mkdir -p ${TMPDIR}
        cd ${TMPDIR}
    fi
fi

function goby {
   set -T
   mode_name="$1"
   shift
   echo GOBY_PROPERTIES:
   cat ${TMPDIR}/goby.properties
   java ${GRID_JVM_FLAGS} -Dlog4j.debug=false -Dlog4j.configuration=file:${GOBY_DIR}/log4j.properties \
                                             -Dgoby.configuration=file:${GOBY_DIR}/goby.properties -jar ${GOBY_DIR}/goby.jar \
                       --mode ${mode_name} $*
}

function goby_with_memory {

   memory="$1"
   mode_name="$2"
   shift
   shift
   # note that we defined MaxHeapSize and CompressedClassSpaceSize to workaround bug http://bugs.java.com/view_bug.do?bug_id=8043516
   java -XX:MaxHeapSize=512m -XX:CompressedClassSpaceSize=64m -Xms${memory} -Xmx${memory} -Dlog4j.debug=false -Dlog4j.configuration=file:${GOBY_DIR}/log4j.properties \
                                     -Dgoby.configuration=file:${GOBY_DIR}/goby.properties -jar ${GOBY_DIR}/goby.jar \
                       --mode ${mode_name} $*
}

function dieUponError {
    RETURN_STATUS=$?
    DESCRIPTION=$1

    if [[ "${CURRENT_PART}" == "" ]]; then
        CURRENT_PART=1
    fi
    if [[ "${NUMBER_OF_PARTS}" == "" ]]; then
        NUMBER_OF_PARTS=1
    fi

    if [ ! ${RETURN_STATUS} -eq 0 ]; then
            # Failed, no result to copy
            copy_logs align ${CURRENT_PART} ${NUMBER_OF_PARTS}
            ${QUEUE_WRITER} --tag ${TAG} --index ${CURRENT_PART} --job-type job-part --status ${JOB_PART_FAILED_STATUS} --description "${DESCRIPTION}"
            error_index ${CURRENT_PART} "${DESCRIPTION}"
            LOG_JOB_STATUS FAILURE "${DESCRIPTION}" ${CURRENT_PART}
            exit ${RETURN_STATUS}
    fi
}

# This function should be called when an empty variable requires to terminate the job. The first argument is the variable
# to check, the second is the error message to report to the end-user.
function dieIfEmpty {
    VAR=$1
    DESCRIPTION=$2

    if [[ "${CURRENT_PART}" == "" ]]; then
        CURRENT_PART=1
    fi
    if [[ "${NUMBER_OF_PARTS}" == "" ]]; then
        NUMBER_OF_PARTS=1
    fi

    if [[ -z "${VAR// }" ]]; then
       #publish_exceptions
       fatal "Job failed. Error description: ${DESCRIPTION}" "done" "${CURRENT_PART}" "${NUMBER_OF_PARTS}"
       copy_logs align ${CURRENT_PART} ${NUMBER_OF_PARTS}
       exit ${RETURN_STATUS}
    fi
}

function copy_logs {
    STEP_NAME=$1
    if [[ $2 == *\.* ]]; then
        START_PART=`printf "%07.3f" $2`
    else
        START_PART=`printf "%03d" $2`
    fi
    if [[ $3 == *\.* ]]; then
        END_PART=`printf "%07.3f" $3`
    else
        END_PART=`printf "%03d" $3`
    fi
    JAVA_LOG_DIR=${SGE_O_WORKDIR}/logs
    mkdir -p ${JAVA_LOG_DIR}/${STEP_NAME}
    /bin/cp ${TMPDIR}/java-log-output.log ${JAVA_LOG_DIR}/${STEP_NAME}/java-log-output-${START_PART}-of-${END_PART}.log
    /bin/cp ${TMPDIR}/steplogs/*.slog ${JAVA_LOG_DIR}/${STEP_NAME}/
    /bin/cp ${TMPDIR}/*.slog ${JAVA_LOG_DIR}/${STEP_NAME}/
}



function checkSubmission {
    if [ -z $1 ]; then
        # Kill any already submitted jobs, inform the web server the job has been killed. Quit the script.
        %KILL_FILE%
        exit 0
    fi
}

function create_kill_file {
    if [ ! -f %KILL_FILE% ]; then
        . %JOB_DIR%/constants.sh

        echo '#!/bin/bash -l' >> %KILL_FILE%
        echo 'export JOB_DIR=%JOB_DIR%' >> %KILL_FILE%
        echo 'export TMPDIR=%JOB_DIR%' >> %KILL_FILE%
        echo 'if [[ ! "--no-queue-message" == $1 ]]; then' >> %KILL_FILE%
        echo "${QUEUE_WRITER} --tag %TAG% --status %JOB_KILLED_STATUS% --description \"Job killed\" --index -1 --job-type job" >> %KILL_FILE%
        echo 'fi' >> %KILL_FILE%
        chmod 700 %KILL_FILE%
    fi
}

function append_kill_file {
    echo "qdel $1" >> %KILL_FILE%
    chmod 700 %KILL_FILE%
}

function jobStartedEmail {
    %JOB_STARTED_EMAIL% > /dev/null
    # Wait for the mail to be sent, otherwise it will just disappear
    sleep 10
}

function jobFailedEmail {
    %JOB_FAILED_EMAIL% > /dev/null
    # Wait for the mail to be sent, otherwise it will just disappear
    sleep 10
}

function jobCompletedEmail {
    %JOB_COMPLETED_EMAIL% > /dev/null
    # Wait for the mail to be sent, otherwise it will just disappear
    sleep 10
}

function setup {

    initializeJobEnvironment
    #JAVA_OPTS is used to set the amount of memory allocated to the groovy scripts.
    export JAVA_OPTS=${PLUGIN_NEED_DEFAULT_JVM_OPTIONS}
    QUEUE_WRITER="%JOB_DIR%/groovy ${RESOURCES_GOBYWEB_SERVER_SIDE_QUEUE_WRITER} %QUEUE_WRITER_POSTFIX% "
    create_kill_file


    if [ ! -z $SGE_O_WORKDIR ]; then
        # R will be configured via the bash login
        # export R_HOME=`R RHOME | /bin/grep --invert-match WARNING`
        # export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:${R_HOME}/lib:${CLUSTER_HOME_DIR}/R/x86_64-unknown-linux-gnu-library/2.11/rJava/jri

        export JAVA_LOG_DIR=${SGE_O_WORKDIR}/logs
        if [ ! -d ${JAVA_LOG_DIR} ]; then
            mkdir ${JAVA_LOG_DIR}
        fi

        GOBY_DIR=${SGE_O_WORKDIR}/goby
        if [ ! -d "${GOBY_DIR}" ]; then
            echo Creating goby dir...
            mkdir -p ${GOBY_DIR}
             /bin/cp ${SGE_O_WORKDIR}/global_goby.jar ${GOBY_DIR}/goby.jar
             /bin/cp ${SGE_O_WORKDIR}/log4j.properties ${GOBY_DIR}/
             /bin/cp ${SGE_O_WORKDIR}/QueueWriter.groovy ${GOBY_DIR}
             /bin/cp ${SGE_O_WORKDIR}/TsvVcfToSqlite.groovy ${GOBY_DIR}
             /bin/cp ${SGE_O_WORKDIR}/icb-groovy-support.jar ${GOBY_DIR}
             /bin/cp ${SGE_O_WORKDIR}/artifact-manager.jar ${GOBY_DIR}
             /bin/cp ${SGE_O_WORKDIR}/serverside-dependencies.jar ${GOBY_DIR}
             /bin/cp ${SGE_O_WORKDIR}/stepslogger.jar ${GOBY_DIR}


        fi

        # Copy the goby and support tools to the local node
        if [ -d "${TMPDIR}" ]; then
            echo Copying goby dir to the local node ...
            /bin/cp ${GOBY_DIR}/* ${TMPDIR}
            cd ${TMPDIR}
            export TMPDIR
        fi

        print_OGE_env
    fi
}

function enforce_minimum_bound_on_align_parts() {
    # minimum bound on NUMBER_OF_ALIGN_PARTS: the number of splits already done (in case we reached the concat step):
    if [ ! -z "${NUMBER_OF_ALIGN_PARTS+set}" ]; then
        NUMBER_OF_SPLITS_COMPLETED=`ls -1 ${JOB_DIR}/split-results/|wc -l`
        if [ ${NUMBER_OF_ALIGN_PARTS} -lt ${NUMBER_OF_SPLITS_COMPLETED} ]; then
            export NUMBER_OF_ALIGN_PARTS=${NUMBER_OF_SPLITS_COMPLETED}
        fi
    fi

}

function print_OGE_env {
      if [ ! -z $SGE_O_WORKDIR ]; then


        echo ------------------------------------------------------
        echo This machines hostname: `hostname`
        echo ------------------------------------------------------
        echo SGE: qsub is running on ${SGE_O_HOST}
        echo SGE: originating queue is ${QUEUE}
        echo SGE: executing cell is ${SGE_CELL}
        echo SGE: working directory is ${SGE_O_WORKDIR}
        echo SGE: execution mode is ${ENVIRONMENT}
        echo SGE: execution host is ${HOSTNAME}
        echo SGE: job identifier is ${JOB_ID}
        echo SGE: job name is ${JOB_NAME}
        echo SGE: job current state = ${STATE}
        echo SGE: task number is ${SGE_TASK_ID}
        echo SGE: current home directory is ${SGE_O_HOME}
        echo SGE: scratch directory is ${TMPDIR}
        echo SGE: PATH = ${SGE_O_PATH}
        echo ------------------------------------------------------


        # Show the java & goby.jar version
        echo "Java version"
        java ${PLUGIN_NEED_DEFAULT_JVM_OPTIONS} -version
        dieUponError "Could not obtain Java version number."

        #echo "Goby.jar version"
        #goby_with_memory 40m version
        #dieUponError "Could not obtain Goby version number."

    fi
}

if [ -z "${STATE+set}" ]; then
 # When state is not defined, assume the user wants to submit the job to OGE.
 export STATE="submit"
 echo "Defined STATE=${STATE}"
fi