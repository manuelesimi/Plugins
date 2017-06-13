#!/bin/bash -l

# Copyright (c) 2011  by Cornell University and the Cornell Research
# Foundation, Inc.  All Rights Reserved.
#
# Permission to use, copy, modify and distribute any part of GobyWeb web
# application for next-generation sequencing data alignment and analysis,
# officially docketed at Cornell as D-5061 ("WORK") and its associated
# copyrights for educational, research and non-profit purposes, without
# fee, and without a written agreement is hereby granted, provided that
# the above copyright notice, this paragraph and the following three
# paragraphs appear in all copies.
#
# Those desiring to incorporate WORK into commercial products or use WORK
# and its associated copyrights for commercial purposes should contact the
# Cornell Center for Technology Enterprise and Commercialization at
# 395 Pine Tree Road, Suite 310, Ithaca, NY 14850;
# email:cctecconnect@cornell.edu; Tel: 607-254-4698;
# FAX: 607-254-5454 for a commercial license.
#
# IN NO EVENT SHALL THE CORNELL RESEARCH FOUNDATION, INC. AND CORNELL
# UNIVERSITY BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL,
# OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF
# WORK AND ITS ASSOCIATED COPYRIGHTS, EVEN IF THE CORNELL RESEARCH FOUNDATION,
# INC. AND CORNELL UNIVERSITY MAY HAVE BEEN ADVISED OF THE POSSIBILITY OF SUCH
# DAMAGE.
#
# THE WORK PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE CORNELL RESEARCH
# FOUNDATION, INC. AND CORNELL UNIVERSITY HAVE NO OBLIGATION TO PROVIDE
# MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.  THE CORNELL
# RESEARCH FOUNDATION, INC. AND CORNELL UNIVERSITY MAKE NO REPRESENTATIONS AND
# EXTEND NO WARRANTIES OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT
# NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
# PARTICULAR PURPOSE, OR THAT THE USE OF WORK AND ITS ASSOCIATED COPYRIGHTS
# WILL NOT INFRINGE ANY PATENT, TRADEMARK OR OTHER RIGHTS.

# Execute the script from the current directory
#$ -cwd

# Combine SGE error and output files.
#$ -j y

# Memory resource requirements
#$ -l %PLUGIN_NEED_GLOBAL%

# Cluster queue to use
#$ -q %QUEUE_NAME%

%CPU_REQUIREMENTS%

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


function setup {

    export JOB_DIR=%JOB_DIR%
   
    # define job specific constants:
    . %JOB_DIR%/constants.sh

    # include value definitions for automatic options:
    . %JOB_DIR%/auto-options.sh

    #JAVA_OPTS is used to set the amount of memory allocated to the groovy scripts.
    export JAVA_OPTS=${PLUGIN_NEED_DEFAULT_JVM_OPTIONS}
    QUEUE_WRITER="%JOB_DIR%/groovy ${RESOURCES_GOBYWEB_SERVER_SIDE_QUEUE_WRITER} %QUEUE_WRITER_POSTFIX% "
    create_kill_file


    if [ ! -z $SGE_O_WORKDIR ]; then
        # R will be configured via the bash login
        # export R_HOME=`R RHOME | /bin/grep --invert-match WARNING`
        # export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:${R_HOME}/lib:${CLUSTER_HOME_DIR}/R/x86_64-unknown-linux-gnu-library/2.11/rJava/jri

        JAVA_LOG_DIR=${SGE_O_WORKDIR}/logs
        if [ ! -d ${JAVA_LOG_DIR} ]; then
            mkdir ${JAVA_LOG_DIR}
        fi

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

        # Show the java & goby.jar version
        echo "Java version"
        java ${PLUGIN_NEED_DEFAULT_JVM_OPTIONS} -version
        dieUponError "Could not obtain Java version number."

        echo "Goby.jar version"
        goby_with_memory 40m version
        dieUponError "Could not obtain Goby version number."

    fi
}

function submit_align {
    jobStartedEmail
  cd ${SGE_O_WORKDIR}

  if [ "${SUPPORTS_BAM_ALIGNMENTS}" == "true" ]; then
    # require exclusive access to a node, where one READS file will be processed in parallel to produce a single BAM file.
    ALIGN=`qsub -N ${TAG}.bamalign -l ${PLUGIN_NEED_ALIGN} -terse -r y -v STATE=bam_align oge_job_script.sh`
    checkSubmission $ALIGN
    ALIGN=${ALIGN%%.*}
    append_kill_file ${ALIGN}
  else

    # Now setup the array job with the post-processing dependency and submit to the grid:

    ARRAY_DIRECTIVE=""
    if [ ${NUMBER_OF_ALIGN_PARTS} -gt 1 ]; then
        ARRAY_DIRECTIVE="-t 1-${NUMBER_OF_ALIGN_PARTS}"
    fi
    
    ALIGN=`qsub ${ARRAY_DIRECTIVE} -l ${PLUGIN_NEED_ALIGN} -N ${TAG}.align -terse -r y -v STATE=single_align oge_job_script.sh`
    checkSubmission $ALIGN
    ALIGN=${ALIGN%%.*}
    append_kill_file ${ALIGN}
    POST=`qsub -N ${TAG}.post -terse -r y -hold_jid ${ALIGN} -l ${PLUGIN_NEED_ALIGNMENT_POST_PROCESSING}  -v STATE=post,ALIGN_MODE=normal oge_job_script.sh`
    checkSubmission $POST
    append_kill_file ${POST}
  fi

}

function submit_parallel_alignment_analysis_jobs {
    SLICING_PLAN_FILENAME=$1
    ARRAY_DIRECTIVE=""
    if [ ${NUMBER_SEQ_VAR_SLICES} -gt 1 ]; then
        ARRAY_DIRECTIVE="-t 1-${NUMBER_SEQ_VAR_SLICES}"
    fi
    cd ${SGE_O_WORKDIR}
    # We do not require exclusive use of a server when comparing sequence variants, maximize job throughput.

    ALIGN=`qsub ${ARRAY_DIRECTIVE} -N ${TAG}.aap -l ${PLUGIN_NEED_PROCESS} -terse -r y -v STATE=single_alignment_analysis_process -v SLICING_PLAN_FILENAME=${SLICING_PLAN_FILENAME} oge_job_script.sh`
    checkSubmission ${ALIGN}
    ALIGN=${ALIGN%%.*}
    append_kill_file ${ALIGN}
    POST=`qsub -N ${TAG}.post -terse -r y -hold_jid ${ALIGN} -l ${PLUGIN_NEED_COMBINE} -v STATE=alignment_analysis_combine oge_job_script.sh`
    checkSubmission ${POST}
    append_kill_file ${POST}
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
            exit ${RETURN_STATUS}
    fi
}

# This function should be called when an empty variable requires to terminate the jon. The first argument is the variable
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

function fetch_input_alignments {

     echo "fileset command: ${FILESET_COMMAND}"

     #make sure that the dir in which reads files will be stored exists
     mkdir -p ${FILESET_TARGET_DIR}

     #INPUT_ALIGNMENTS slot is declared in AlignmentAnalysisConfig.getInput()
     ${FILESET_COMMAND} --has-fileset INPUT_ALIGNMENTS
     dieUponError "INPUT_ALIGNMENTS input entries are not available"

     ALIGNMENT_FILES=`${FILESET_COMMAND} --fetch INPUT_ALIGNMENTS`
     dieUponError "Failed to fecth INPUT_ALIGNMENTS: ${ALIGNMENT_FILES}"
     dieIfEmpty "${ALIGNMENT_FILES}" "Failed to fecth INPUT_ALIGNMENTS"

     mv ${ALIGNMENT_FILES} ${ENTRIES_DIRECTORY}

     echo "Localized ALIGNMENT_FILES ${ALIGNMENT_FILES}"

}

function fetch_input_reads {

     echo "fileset command: ${FILESET_COMMAND}"

     #make sure that the dir in which reads files will be stored exists
     mkdir -p ${FILESET_TARGET_DIR}

     #INPUT_READS slot is declated in AlignerConfig.getInput()
     ${FILESET_COMMAND} --has-fileset INPUT_READS
     dieUponError "Input compact reads are not available"

     READS=`${FILESET_COMMAND} --fetch INPUT_READS`
     dieUponError "Failed to fecth compact reads ${READS}"
     dieIfEmpty "${READS}" "Failed to fecth compact reads"
     export READS
     echo "Localized filesets ${READS}"

}

function jobDieUponError {
    RETURN_STATUS=$?
    DESCRIPTION=$1
    if [ "${CURRENT_PART}" == "" ]; then
        CURRENT_PART=1
    fi
    if [ ! ${RETURN_STATUS} -eq 0 ]; then
            # Failed, no result to copy
            copy_logs job ${CURRENT_PART} ${NUMBER_OF_PARTS}
            ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_FAILED_STATUS} --description "Job failed" --index ${CURRENT_PART} --job-type job
            jobFailedEmail
            exit ${RETURN_STATUS}
    fi
}



function fail_when_no_results {
  RESULT_DIR=${SGE_O_WORKDIR}/results/${TAG}

    if [ ! -d "$RESULT_DIR" ]; then
        # Output dir doesn't exist but it should
        copy_logs counts ${CURRENT_PART} ${NUMBER_OF_PARTS}
        ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_FAILED_STATUS} --description "-" --index ${CURRENT_PART} --job-type job-part
        ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_FAILED_STATUS} --description "Job failed" --index ${CURRENT_PART} --job-type job
        jobFailedEmail
        exit 1
    fi
}





function cleanup {
    CURRENT_PART=${NUMBER_OF_PARTS}
    # We have concat'd. Remove the interim results.
    if [ ! -z ${SGE_O_WORKDIR} ]; then

        if [ -d ${SGE_O_WORKDIR}/results/ ]; then
            rm -rf ${SGE_O_WORKDIR}/results/
        fi

    fi
    #
    # Keep this in case we need to run again with the same version?
    #
    # rm -rf ${GOBY_DIR}
}

#######################################################################################
## Script logic starts here
#######################################################################################

setup

ARTIFACT_REPOSITORY_DIR=%ARTIFACT_REPOSITORY_DIR%

if [ -z "${GOBYWEB_CONTAINER_TECHNOLOGY+set}" ]; then
    export GOBYWEB_CONTAINER_TECHNOLOGY="none"
else
    if [ -z "${GOBYWEB_CONTAINER_NAME+set}" ]; then
    export GOBYWEB_CONTAINER_NAME="gobyweb_oge_execution_environment"
    fi
fi

case ${GOBYWEB_CONTAINER_TECHNOLOGY} in
 singularity)
     DELEGATE_OGE_JOB_SCRIPT="singularity exec ${GOBYWEB_CONTAINER_NAME} %JOB_DIR%/oge_job_script_legacy.sh"
 ;;
 none)
    DELEGATE_OGE_JOB_SCRIPT="%JOB_DIR%/oge_job_script_legacy.sh"
 ;;
esac

if [ -z "${STATE+set}" ]; then
 // When state is not defined, assume the user wants to submit the job to OGE.
 export STATE="submit"
fi

case ${STATE} in
    submit)
        cd ${JOB_DIR}
        SUBMISSION=`qsub -N ${TAG}.submit -r y -terse -v STATE=${INITIAL_STATE} oge_job_script.sh`
        checkSubmission $SUBMISSION
        append_kill_file ${SUBMISSION}
        echo ${SUBMISSION}
        ;;

    pre_align)
        STATE="pre_align"
        ${DELEGATE_OGE_JOB_SCRIPT} "$*"
        submit_align
        ;;

    diffexp)
        if [ "${SPLIT_PROCESS_COMBINE}" == "false" ]; then
           ${DELEGATE_OGE_JOB_SCRIPT} "diffexp_sequential"
         else
            ${DELEGATE_OGE_JOB_SCRIPT} "diffexp_parallel"
            # Next, start SGE array jobs with NUMBER_SEQ_VAR_SLICES pieces:
            submit_parallel_alignment_analysis_jobs $RESULT_DIR/${TAG}-slicing-plan.txt
            RETURN_STATUS=$?
            # we exit ${RETURN_STATUS} here because the job has been submitted to SGE. Other parts will execute and
            # finish or fail the job
            exit ${RETURN_STATUS}
        fi
        cleanup
        ;;
    *)
        # delegate everything else either inside container or execute directly legacy script:
        ${DELEGATE_OGE_JOB_SCRIPT} ${STATE}
        ;;
esac
