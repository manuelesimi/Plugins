
#functions common to all types of job.

function push_filesets {
     slot_name=$1
     shift
     patterns="$@"
     REGISTERED_TAGS=`${FILESET_COMMAND} --push ${slot_name}: ${patterns} `
     dieUponError "Failed to push back the ${slot_name} Result"
     ALL_REGISTERED_TAGS="${ALL_REGISTERED_TAGS:-} ${REGISTERED_TAGS} "
     info "${slot_name}:[${REGISTERED_TAGS}]" "${JOB_REGISTERED_FILESETS_STATUS}"
     echo "${REGISTERED_TAGS}"
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
   info "JOB_METADATA: ${REGISTERED_TAGS}" "${JOB_REGISTERED_FILESETS_STATUS}"
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
    mkdir -p ${JAVA_LOG_DIR}/${STEP_NAME}
    /bin/cp ${TMPDIR}/java-log-output.log ${JAVA_LOG_DIR}/${STEP_NAME}/java-log-output-${START_PART}-of-${END_PART}.log
    /bin/cp ${TMPDIR}/steplogs/*.slog ${JAVA_LOG_DIR}/${STEP_NAME}/
    /bin/cp ${TMPDIR}/*.slog ${JAVA_LOG_DIR}/${STEP_NAME}/
}

# This function should be called when an error condition requires to terminate the job. The first argument is a description
# of the error that will be communicated to the end-user (will be displayed in the GobyWeb job status interface).

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
       fatal "Job failed. Error description: ${DESCRIPTION}" "done" "${CURRENT_PART}" "${NUMBER_OF_PARTS}"
       copy_logs align ${CURRENT_PART} ${NUMBER_OF_PARTS}
       exit ${RETURN_STATUS}
    fi
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

function jobCompleted {
   jobCompletedEmail
   info "Job completed" "done"
}

function jobFailed {
   jobFailedEmail
   fatal "Job failed" "done"
}

function jobStarted {
    jobStartedEmail
   info "Task submitted: ${SUBMISSION}" "submitted"
}