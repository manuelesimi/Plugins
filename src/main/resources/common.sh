#!/usr/bin/env bash

function debug {
    echo "$*";
}
function error {
    echo "$*";
}
if [ -z "${GOBYWEB_CONTAINER_TECHNOLOGY+set}" ]; then
    export GOBYWEB_CONTAINER_TECHNOLOGY="none"
else
    if [ -z "${GOBYWEB_CONTAINER_NAME+set}" ]; then
    export GOBYWEB_CONTAINER_NAME="artifacts/base"
    fi
fi

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
