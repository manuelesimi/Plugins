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


#select the java version to use
slchoose sun_jdk 6.0.25 dist

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

function calculate_PAD_FORMAT {
    _NUMBER=$1
    _NUMBER_TO_PAD=0
    while [ ${_NUMBER} -gt 10 ]; do
        _NUMBER_TO_PAD=$(( _NUMBER_TO_PAD + 1 ))
        _NUMBER=$(( _NUMBER / 10 ))
    done
    if [ ${_NUMBER_TO_PAD} -eq 0 ]; then
        PAD_FORMAT=%d
    else
        PAD_FORMAT=%0$(( _NUMBER_TO_PAD + 1 ))d
    fi
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


function setup_parallel_alignment_analysis {
    jobStartedEmail
    cd ${SGE_O_WORKDIR}
    ARRAY_DIRECTIVE=""
    if [ ${NUMBER_OF_ALIGN_PARTS} -gt 1 ]; then
        ARRAY_DIRECTIVE="-t 1-${NUMBER_OF_ALIGN_PARTS}"
    fi
    ALIGNMENT_ANALYSIS=`qsub ${ARRAY_DIRECTIVE} ${PART_EXCLUSIVE} -N ${TAG}.align -terse -r y -v STATE=single_alignment_analysis oge_job_script.sh`
    checkSubmission $ALIGNMENT_ANALYSIS
    ALIGNMENT_ANALYSIS=${ALIGNMENT_ANALYSIS%%.*}
    append_kill_file ${ALIGNMENT_ANALYSIS}
    POST=`qsub -N ${TAG}.post -terse -r y -hold_jid ${ALIGN} -v STATE=post,ALIGN_MODE=normal oge_job_script.sh`
    checkSubmission $POST
    append_kill_file ${POST}

}

function setup_align {
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

function setup_parallel_alignment_analysis_jobs {
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


function run_single_alignment_analysis_process {

  if [ ! -z ${SGE_TASK_ID} ] && [ "${SGE_TASK_ID}" != "undefined" ] && [ "${SGE_TASK_ID}" != "unknown" ]; then
           CURRENT_PART=${SGE_TASK_ID}
  else
           CURRENT_PART=1
  fi
  ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_DIFF_EXP_STATUS} --description "Processing run_single_alignment_analysis_process for part ${CURRENT_PART}" --index ${CURRENT_PART} --job-type job-part

  # This variable is defined on the command line: SLICING_PLAN_FILENAME

  (cd ${TMPDIR} ; plugin_alignment_analysis_process ${SLICING_PLAN_FILENAME} ${CURRENT_PART} )

  RETURN_STATUS=$?
  if [ ! $RETURN_STATUS -eq 0 ]; then
    ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_FAILED_STATUS} --description "run_single_alignment_analysis_process failed for part ${CURRENT_PART} on ${HOSTNAME}" --index ${CURRENT_PART} --job-type job-part
  fi
    # Completed, copy the results back
    if [ ! -z ${SGE_TASK_ID} ] && [ "${SGE_TASK_ID}" != "undefined" ] && [ "${SGE_TASK_ID}" != "unknown" ]; then
      echo "multiple results"
    else
      # act as if there was one part:
       SGE_TASK_ID=1
    fi
    RESULT_DIR=${SGE_O_WORKDIR}/split-results/${TAG}
    /bin/mkdir -p ${RESULT_DIR}
    /bin/cp ${TAG}-*-${SGE_TASK_ID}.${RESULT_FILE_EXTENSION}  ${RESULT_DIR}
}

function run_alignment_analysis_combine {

    ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_DIFF_EXP_STATUS} --description "Starting to combine results.." --index 1 --job-type job-part
    RESULT_DIR=${SGE_O_WORKDIR}/results/${TAG}
    (cd ${TMPDIR} ; plugin_alignment_analysis_combine ${RESULT_DIR}/${TAG}.${RESULT_FILE_EXTENSION} ${SGE_O_WORKDIR}/split-results/${TAG}/${TAG}-*.${RESULT_FILE_EXTENSION} )
    jobDieUponError "failed to combine results (${HOSTNAME})"

    %COPY_PLUGIN_OUTPUT_FILES%

    mkdir ${TMPDIR}/import-db
    cp ${RESULT_DIR}/${TAG}*.tsv ${TMPDIR}/import-db/
    cp ${RESULT_DIR}/${TAG}*.vcf.gz ${TMPDIR}/import-db/

    #Make a sqlite database into ${TMPDIR}/import-db/ of any file with ${TAG} and ${RESULT_FILE_EXTENSION} (tsv or vcf file):
    #${RESULT_FILE_EXTENSION} will be "tsv" or "vcf.gz"
    export QUEUE_WRITER
    ${RESOURCES_GROOVY_EXECUTABLE} -cp ${GOBY_DIR}:${RESOURCES_GOBYWEB_SERVER_SIDE_GLOBAL_GOBY_JAR}:${RESOURCES_GOBYWEB_SERVER_SIDE_ICB_GROOVY_SUPPORT_JAR} \
           ${RESOURCES_GOBYWEB_SERVER_SIDE_TSV_VCF_TO_SQLITE} \
           --job-start-status "${JOB_START_STATUS}" \
           --queue-writer-prefix-variable QUEUE_WRITER \
           --export-format lucene \
           ${TMPDIR}/import-db/${TAG}-*.tsv  ${TMPDIR}/import-db/${TAG}-*.vcf.gz
    jobDieUponError "failed to convert results to database"
    cp ${TMPDIR}/import-db/${TAG}*.db ${RESULT_DIR}/

    if [ ! $? -eq 0 ]; then
       # remove any previous index:
       rm -fr ${TMPDIR}/${TAG}*.lucene.index
       cp -r ${TMPDIR}/import-db/${TAG}*.lucene.index ${RESULT_DIR}/
       dieUponError "Could not copy db/lucene to ${RESULT_DIR} directory (this disk might be full)."
    fi
    ls -lrt

    ls -lrt  ${TMPDIR}/import-db

    ls -lrt  ${TMPDIR}/import-db/*

    %PUSH_PLUGIN_OUTPUT_FILES%


    jobCompletedEmail

    copy_logs diffexp 1 1
}



#pushes the results of an alignment analysis job in the fileset area
function push_analysis_results {

    file_to_push=$1
    slot=$2
    mandatory=$3
    additional_attributes=$4 #-a TABLENAME=$tablename

    #prefix with tag if needed
    if [ -e "${TMPDIR}/import-db/${file_to_push}" ]; then
       mv "${TMPDIR}/import-db/${file_to_push}" "${TMPDIR}/import-db/${TAG}-${file_to_push}"
    elif [ -e "${TMPDIR}/${file_to_push}" ]; then
       mv "${TMPDIR}/${file_to_push}" "${TMPDIR}/import-db/${TAG}-${file_to_push}"
    elif [ -e "${TMPDIR}/${TAG}-${file_to_push}" ]; then
       mv "${TMPDIR}/${TAG}-${file_to_push}" "${TMPDIR}/import-db/${TAG}-${file_to_push}"
    fi

    #push the file
    if [ -e "${TMPDIR}/import-db/${TAG}-${file_to_push}" ]; then
       local REGISTERED_TAGS=`${FILESET_COMMAND} --push -a ORGANISM=${ORGANISM} -a GENOME_REFERENCE_ID=${GENOME_REFERENCE_ID} ${additional_attributes} -a SOURCE_OUTPUT_SLOT=${slot} ${slot}: ${TMPDIR}/import-db/${TAG}-${file_to_push}`
       dieUponError "Failed to push ${file_to_push} in the FileSet area. ${REGISTERED_TAGS}"
       echo "${file_to_push} has been successfully registered with tag ${REGISTERED_TAGS}"
       ALL_REGISTERED_TAGS="${ALL_REGISTERED_TAGS} ${slot}:[${REGISTERED_TAGS}]"
    elif [ -e "${JOB_DIR}/results/${TAG}/${TAG}-${file_to_push}" ]; then
        local REGISTERED_TAGS=`${FILESET_COMMAND} --push -a ORGANISM=${ORGANISM} -a GENOME_REFERENCE_ID=${GENOME_REFERENCE_ID} ${additional_attributes} -a SOURCE_OUTPUT_SLOT=${slot} ${slot}: ${JOB_DIR}/results/${TAG}/${TAG}-${file_to_push}`
        dieUponError "Failed to push ${file_to_push} in the FileSet area. ${REGISTERED_TAGS}"
        echo "${file_to_push} has been successfully registered with tag ${REGISTERED_TAGS}"
        ALL_REGISTERED_TAGS="${ALL_REGISTERED_TAGS} ${slot}:[${REGISTERED_TAGS}]"
    else
        if [ "${mandatory}" == "true" ]; then
              echo "ERROR: Mandatory file ${file_to_push} was not produced by the job."
        fi;
    fi;
}


#pushes BAM alignments produced by an aligner job in the fileset area
function push_bam_alignments {
    echo .
    echo . Running push_bam_alignments
    echo .

    fail_when_no_results

     #push back the generated alignments
    ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_TRANSFER_STATUS} --description "Pushing results in the fileset area" --index ${CURRENT_PART} --job-type job-part

    REGISTERED_TAGS=`${FILESET_COMMAND} --push -a ORGANISM=${ORGANISM} -a GENOME_REFERENCE_ID=${GENOME_REFERENCE_ID} -a SOURCE_READS_ID=${SOURCE_READS_ID} BAM_ALIGNMENT: $RESULT_DIR/*.bam $RESULT_DIR/*.bam.bai`
    dieUponError "Failed to push the alignment files in the fileset area: ${REGISTERED_TAGS}"

    echo "The following BAM_ALIGNMENT instance has been successfully registered: ${REGISTERED_TAGS}"

    ALL_REGISTERED_TAGS="${ALL_REGISTERED_TAGS} BAM_ALIGNMENT:[${REGISTERED_TAGS}]"

}

#pushes Goby alignments produced by an aligner job in the fileset area
function push_goby_alignments {
    echo .
    echo . Running push_goby_alignments
    echo .

    fail_when_no_results

     #push back the generated alignments
     ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_TRANSFER_STATUS} --description "Pushing results in the fileset area" --index ${CURRENT_PART} --job-type job-part

     REGISTERED_TAGS=`${FILESET_COMMAND} --push -a ORGANISM=${ORGANISM} -a GENOME_REFERENCE_ID=${GENOME_REFERENCE_ID} -a SOURCE_READS_ID=${SOURCE_READS_ID} GOBY_ALIGNMENT: $RESULT_DIR/*.index $RESULT_DIR/*.entries $RESULT_DIR/*.header $RESULT_DIR/*.tmh`

     #TODO register $RESULT_DIR/*.alignment-stats.txt  and  $RESULT_DIR/*.sequence-variation-stats.tsv
     dieUponError "Failed to push the alignment files in the fileset area: ${REGISTERED_TAGS}"

     echo "The following GOBY_ALIGNMENT instance has been successfully registered: ${REGISTERED_TAGS}"
     ALL_REGISTERED_TAGS="${ALL_REGISTERED_TAGS} GOBY_ALIGNMENT:[${REGISTERED_TAGS}]"

}

#pushes any result (except the alignments) produced by an aligner job in the fileset area
function push_aligner_results {
   echo .
   echo . Running push_other_results
   echo .

   #push back ALIGNMENT_BED
   echo Pushing ALIGNMENT_BED
   REGISTERED_TAGS=`${FILESET_COMMAND} --push -a ORGANISM=${ORGANISM} -a GENOME_REFERENCE_ID=${GENOME_REFERENCE_ID} -a SOURCE_READS_ID=${SOURCE_READS_ID} ALIGNMENT_BED: $RESULT_DIR/*-all.bed.gz`
   if [ $? != 0 ]; then
        echo "Failed to push back ALIGNMENT_BED files: ${REGISTERED_TAGS}"
   fi
   echo "The following ALIGNMENT_BED instance has been successfully registered: ${REGISTERED_TAGS}"
   ALL_REGISTERED_TAGS="${ALL_REGISTERED_TAGS} ALIGNMENT_BED:[${REGISTERED_TAGS}]"

   #push back ALIGNMENT_WIG
   echo Pushing ALIGNMENT_WIG
   REGISTERED_TAGS=`${FILESET_COMMAND} --push -a ORGANISM=${ORGANISM} -a GENOME_REFERENCE_ID=${GENOME_REFERENCE_ID} -a SOURCE_READS_ID=${SOURCE_READS_ID} ALIGNMENT_WIG: $RESULT_DIR/*-all.wig.gz`
   if [ $? != 0 ]; then
        echo "Failed to push back ALIGNMENT_WIG files: ${REGISTERED_TAGS}"
   fi
   echo "The following ALIGNMENT_WIG instance has been successfully registered: ${REGISTERED_TAGS}"

   ALL_REGISTERED_TAGS="${ALL_REGISTERED_TAGS} ALIGNMENT_WIG:[${REGISTERED_TAGS}]"

   #push COUNTS back
   echo Pushing COUNTS
   REGISTERED_TAGS=`${FILESET_COMMAND} --push -a ORGANISM=${ORGANISM} -a GENOME_REFERENCE_ID=${GENOME_REFERENCE_ID} -a SOURCE_READS_ID=${SOURCE_READS_ID} COUNTS: $RESULT_DIR/*.counts`
   if [ $? != 0 ]; then
        echo "Failed to push back COUNTS files: ${REGISTERED_TAGS}"
   fi
   echo "The following COUNTS instance has been successfully registered: ${REGISTERED_TAGS}"
   ALL_REGISTERED_TAGS="${ALL_REGISTERED_TAGS} COUNTS:[${REGISTERED_TAGS}]"

   #push ALIGNMENT_ALL_FILES back
   echo Pushing ALIGNMENT_ALL_FILES
   REGISTERED_TAGS=`${FILESET_COMMAND} --push -a ORGANISM=${ORGANISM} -a GENOME_REFERENCE_ID=${GENOME_REFERENCE_ID} -a SOURCE_READS_ID=${SOURCE_READS_ID} ALIGNMENT_ALL_FILES: $RESULT_DIR/*-all-files.zip`
   if [ $? != 0 ]; then
        echo "Failed to push back ALIGNMENT_ALL_FILES files: ${REGISTERED_TAGS}"
   fi
   echo "The following ALIGNMENT_ALL_FILES instance has been successfully registered: ${REGISTERED_TAGS}"
   ALL_REGISTERED_TAGS="${ALL_REGISTERED_TAGS} ALIGNMENT_ALL_FILES:[${REGISTERED_TAGS}]"

    #push ALIGNMENT_SEQUENCE_VARIATION_STATS back
   echo Pushing ALIGNMENT_ALL_FILES
   REGISTERED_TAGS=`${FILESET_COMMAND} --push -a ORGANISM=${ORGANISM} -a GENOME_REFERENCE_ID=${GENOME_REFERENCE_ID} -a SOURCE_READS_ID=${SOURCE_READS_ID} ALIGNMENT_SEQUENCE_VARIATION_STATS: $RESULT_DIR/*.sequence-variation-stats.tsv    `
   if [ $? != 0 ]; then
        echo "Failed to push back ALIGNMENT_SEQUENCE_VARIATION_STATS files: ${REGISTERED_TAGS}"
   fi
   echo "The following ALIGNMENT_SEQUENCE_VARIATION_STATS instance has been successfully registered: ${REGISTERED_TAGS}"
   ALL_REGISTERED_TAGS="${ALL_REGISTERED_TAGS} ALIGNMENT_SEQUENCE_VARIATION_STATS:[${REGISTERED_TAGS}]"

    #push STATS back
   echo Pushing STATS
   REGISTERED_TAGS=`${FILESET_COMMAND} --push -a ORGANISM=${ORGANISM} -a GENOME_REFERENCE_ID=${GENOME_REFERENCE_ID} -a SOURCE_READS_ID=${SOURCE_READS_ID} STATS: $RESULT_DIR/*.stats`
   if [ $? != 0 ]; then
        echo "Failed to push back STATS files: ${REGISTERED_TAGS}"
   fi
   echo "The following STATS instance has been successfully registered: ${REGISTERED_TAGS}"
   ALL_REGISTERED_TAGS="${ALL_REGISTERED_TAGS} STATS:[${REGISTERED_TAGS}]"

    #push ALIGNMENT_STATS back
   echo Pushing ALIGNMENT_STATS
   REGISTERED_TAGS=`${FILESET_COMMAND} --push -a ORGANISM=${ORGANISM} -a GENOME_REFERENCE_ID=${GENOME_REFERENCE_ID} -a SOURCE_READS_ID=${SOURCE_READS_ID} ALIGNMENT_STATS: $RESULT_DIR/*.alignment-stats.txt`
   if [ $? != 0 ]; then
        echo "Failed to push back ALIGNMENT_STATS files: ${REGISTERED_TAGS}"
   fi
   echo "The following ALIGNMENT_STATS instance has been successfully registered: ${REGISTERED_TAGS}"
   ALL_REGISTERED_TAGS="${ALL_REGISTERED_TAGS} ALIGNMENT_STATS:[${REGISTERED_TAGS}]"

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

function bam_align {
    # Set CURRENT_PART because we will need it in the dieUponError function
    CURRENT_PART=1

    (cd ${TMPDIR} ; plugin_align  pre-sort-${TAG} ${BASENAME} )
    RETURN_STATUS=$?
    if [ $? -eq 0 ]; then
        # Completed, copy the results back

        RESULT_DIR=${SGE_O_WORKDIR}/results/${TAG}
        /bin/mkdir -p ${RESULT_DIR}
        /bin/cp ${BASENAME}.bam  ${RESULT_DIR}/
        /bin/cp ${BASENAME}.bam.bai  ${RESULT_DIR}/

        push_bam_alignments

        ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_COMPLETED_STATUS} --description "Job completed" --index 1 --job-type job

        jobCompletedEmail

    else
        ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_FAILED_STATUS} --description "Job failed" --index ${CURRENT_PART} --job-type job
        jobFailedEmail
        exit ${RETURN_STATUS}
    fi
}

# This function runs a Goby mode. It initializes java memory and logging parameters and can be called with any
# number of parameters. For instance goby fasta-to-compact will run the fasta-to-compact mode with no arguments.

function goby {
   set -x
   set -T
   mode_name="$1"
   shift
   echo GOBY_PROPERTIES:
   cat ${TMPDIR}/goby.properties
   java ${GRID_JVM_FLAGS} -Dlog4j.debug=true -Dlog4j.configuration=file:${GOBY_DIR}/log4j.properties \
                                             -Dgoby.configuration=file:${GOBY_DIR}/goby.properties -jar ${GOBY_DIR}/goby.jar \
                       --mode ${mode_name} $*
}

function goby_with_memory {

   memory="$1"
   mode_name="$2"
   shift
   shift
   java -Xms${memory} -Xmx${memory} -Dlog4j.debug=true -Dlog4j.configuration=file:${GOBY_DIR}/log4j.properties \
                                     -Dgoby.configuration=file:${GOBY_DIR}/goby.properties -jar ${GOBY_DIR}/goby.jar \
                       --mode ${mode_name} $*
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
            copy_logs align ${CURRENT_PART} ${NUMBER_OF_PARTS}
            ${QUEUE_WRITER} --tag ${TAG} --index ${CURRENT_PART} --job-type job-part --status ${JOB_PART_FAILED_STATUS} --description "${DESCRIPTION}"
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

function run_single_align {
    # NUMBER_OF_PARTS is the TOTAL number of sge jobs there should be, including concat and post
    # CURRENT_PART is the current part, 1-based
    echo
    if [ ! -z ${SGE_TASK_ID} ] && [ "${SGE_TASK_ID}" != "undefined" ] && [ "${SGE_TASK_ID}" != "unknown" ]; then
        CURRENT_PART=${SGE_TASK_ID}
    else
        CURRENT_PART=1
    fi

    # Here 0 and 0 indicate FULL file
    START_POSITION=0
    END_POSITION=0
    READS_FILE=${READS}
    # The reads file to process
    if [ ! -z ${SGE_TASK_ID} ] && [ "${SGE_TASK_ID}" != "undefined" ] && [ "${SGE_TASK_ID}" != "unknown" ]; then
        START_POSITION=$(( (SGE_TASK_ID - 1) * $CHUNK_SIZE ))
        END_POSITION=$(( $START_POSITION + $CHUNK_SIZE - 1 ))
    fi

    #these variables are also appended to oge-constants.sh to be visible to NYoSh-based plugins
    echo "START_POSITION=${START_POSITION}" >> ${TMPDIR}/oge-constants.sh
    echo "END_POSITION=${END_POSITION}" >> ${TMPDIR}/oge-constants.sh
    echo "READS_FILE=${READS_FILE}" >> ${TMPDIR}/oge-constants.sh


    # Run the alignment
    ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_ALIGN_STATUS} --description "Align, sub-task ${CURRENT_PART} of ${NUMBER_OF_PARTS}, starting" --index ${CURRENT_PART} --job-type job-part

      # call the aligner plugin script.sh from TMDDIR:
      ( cd ${TMPDIR} ;   plugin_align pre-sort-${TAG} ${BASENAME})

    RETURN_STATUS=$?
    if [ ! $? -eq 0 ]; then
        # Failed, no result to copy
        copy_logs align ${CURRENT_PART} ${NUMBER_OF_PARTS}
        ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_FAILED_STATUS} --description "Align, sub-task ${CURRENT_PART} of ${NUMBER_OF_PARTS}, failed" --index ${CURRENT_PART} --job-type job-part
        exit ${RETURN_STATUS}
    fi

    ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_SORT_STATUS} --description "Post-align sort, sub-task ${CURRENT_PART} of ${NUMBER_OF_PARTS}, starting" --index ${CURRENT_PART} --job-type job-part

    goby_with_memory ${PLUGIN_NEED_ALIGN_JVM} sort pre-sort-${TAG}.entries -o ${BASENAME} -f 75
    RETURN_STATUS=$?
    if [ ! $? -eq 0 ]; then
        ls -lat
        rm ${TAG}.*
        ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_FAILED_STATUS} --description "Post-align sort, sub-task ${CURRENT_PART} of ${NUMBER_OF_PARTS}, failed" --index ${CURRENT_PART} --job-type job-part
        exit ${RETURN_STATUS}
    else
        # post-align sort was successful.
        ls -lat
        rm pre-sort-*
    fi


    # Completed, copy the results back
    if [ ! -z ${SGE_TASK_ID} ] && [ "${SGE_TASK_ID}" != "undefined" ] && [ "${SGE_TASK_ID}" != "unknown" ]; then
        RESULT_DIR=${SGE_O_WORKDIR}/split-results/${TAG}-${SGE_TASK_ID}
    else
        RESULT_DIR=${SGE_O_WORKDIR}/results/${TAG}
    fi

    /bin/mkdir -p ${RESULT_DIR}
    /bin/cp ${BASENAME}*.entries ${BASENAME}*.header ${BASENAME}*.stats ${BASENAME}*.tmh ${BASENAME}*.index ${RESULT_DIR}

    copy_logs align ${CURRENT_PART} ${NUMBER_OF_PARTS}
    ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_COMPLETED_STATUS} --description "Sub-task ${CURRENT_PART} of ${NUMBER_OF_PARTS}, completed" --index ${CURRENT_PART} --job-type job-part
}

function alignment_concat {
    CURRENT_PART=$(( NUMBER_OF_ALIGN_PARTS + 1 ))

    INTERIM_RESULT_DIR=${SGE_O_WORKDIR}/split-results
    RESULT_DIR=${SGE_O_WORKDIR}/results/${TAG}

    # Run the concatenate, only if more than one align part
    ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_CONCAT_STATUS} --description "Concat, sub-task ${CURRENT_PART} of ${NUMBER_OF_PARTS}, starting" --index ${CURRENT_PART} --job-type job-part
    COMPRESS_ALIGNMENTS=" -x MessageChunksWriter:codec=hybrid-1 "
    COMPRESS_ALIGNMENTS="  "
    # Run the concatenate, only if more than one align part
     if [ ${NUMBER_OF_ALIGN_PARTS} -gt 1 ]; then
        # Each sub-concat contains at most 50 parts, the +49 helps with integer rounding
        # 100 goes to 2 sub-concats, 151 goes to 3 sub-concats.
        NUM_SUB_CONCATS=$(( ($NUMBER_OF_ALIGN_PARTS + 49) / 50))

        if [ $NUM_SUB_CONCATS -eq 1 ]; then

            goby concatenate-alignments --adjust-query-indices false --output ${BASENAME} ${COMPRESS_ALIGNMENTS} \
                ${INTERIM_RESULT_DIR}/${TAG}-*/*.entries

            RETURN_STATUS=$?
            if [ ! $RETURN_STATUS -eq 0 ]; then
                # Failed
                copy_logs concat ${CURRENT_PART} ${NUMBER_OF_PARTS}
                ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_FAILED_STATUS} --description "Concat, sub-task ${CURRENT_PART} of ${NUMBER_OF_PARTS}, failed" --index ${CURRENT_PART} --job-type job-part
                jobFailedEmail
                exit ${RETURN_STATUS}
            fi

        else
            CUR_SUB_PART=1
            for ((i = 1; i <= ${NUMBER_OF_ALIGN_PARTS}; i++));
            do
               if [ -f ${INTERIM_RESULT_DIR}/${TAG}-${i}/*.entries ]; then
                  SUB_PART_SOURCES[${CUR_SUB_PART}]="${SUB_PART_SOURCES[CUR_SUB_PART]} ${INTERIM_RESULT_DIR}/${TAG}-${i}/*.entries"
                  CUR_SUB_PART=$(( CUR_SUB_PART + 1 ))
                  if [ $CUR_SUB_PART -gt $NUM_SUB_CONCATS ]; then
                    CUR_SUB_PART=1
                  fi
               fi
            done

            # Perform concats, no more than 100 parts at a time
            # TODO parallelize this for loop
            for ((i = 1; i <= ${NUM_SUB_CONCATS}; i++));
            do
                SUB_RESULT_DIR=${INTERIM_RESULT_DIR}/${TAG}-SUB-${i}
                mkdir ${SUB_RESULT_DIR}
                SUB_RESULT_TAG=${SUB_RESULT_DIR}/${TAG}
                goby concatenate-alignments --adjust-query-indices false --output ${SUB_RESULT_TAG} \
                    ${SUB_PART_SOURCES[i]}

                RETURN_STATUS=$?
                if [ ! $RETURN_STATUS -eq 0 ]; then
                    # Failed
                    copy_logs concat ${CURRENT_PART} ${NUMBER_OF_PARTS}
                    ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_FAILED_STATUS} --description "Concat, sub-task ${CURRENT_PART} of ${NUMBER_OF_PARTS}, failed" --index ${CURRENT_PART} --job-type job-part
                    jobFailedEmail
                    exit ${RETURN_STATUS}
                fi
            done
 
            # Concat the sub-results into a single result, compressing the final alignment with the hybrid-1 codec:
            goby concatenate-alignments --adjust-query-indices false --output ${BASENAME} ${COMPRESS_ALIGNMENTS}  \
                ${INTERIM_RESULT_DIR}/${TAG}-SUB-*/*.entries

            RETURN_STATUS=$?
            if [ ! $RETURN_STATUS -eq 0 ]; then
                # Failed
                copy_logs concat ${CURRENT_PART} ${NUMBER_OF_PARTS}
                ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_FAILED_STATUS} --description "Concat, sub-task ${CURRENT_PART} of ${NUMBER_OF_PARTS}, failed" --index ${CURRENT_PART} --job-type job-part
                jobFailedEmail
                exit ${RETURN_STATUS}
            fi

        fi

        /bin/mkdir -p ${RESULT_DIR}
        /bin/cp *.entries *.header *.stats *.tmh *.index ${RESULT_DIR}
    fi
    # Call the optional combine function on the complete alignment. In most cases, this function does nothing, but can be
    # used to collect some statistics or other data from the alignment and the reads.
   ( cd ${TMPDIR}  ; plugin_alignment_combine "${TAG}" "${READS}" "${BASENAME}" )

    copy_logs concat ${CURRENT_PART} ${NUMBER_OF_PARTS}
    ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_COMPLETED_STATUS} --description "Concat, sub-task ${CURRENT_PART} of ${NUMBER_OF_PARTS} completed" --index ${CURRENT_PART} --job-type job-part
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

function alignment_counts {
    echo .
    echo . Running counts
    echo .
    CURRENT_PART=$(( NUMBER_OF_ALIGN_PARTS + 2 ))

   fail_when_no_results

    #
    # Counts
    #
    ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_COUNTS_STATUS} --description "Counts, sub-task ${CURRENT_PART} of ${NUMBER_OF_PARTS}, starting" --index ${CURRENT_PART} --job-type job-part
    goby alignment-to-counts \
        --full-genome true $RESULT_DIR/*.entries
    RETURN_STATUS=$?
    if [ ! $RETURN_STATUS -eq 0 ]; then
        # Failed
        ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_FAILED_STATUS} --description "Counts, sub-task ${CURRENT_PART} of ${NUMBER_OF_PARTS}, failed" --index ${CURRENT_PART} --job-type job-part
        # Don't exit.
    fi
}

function alignment_stats {
    echo .
    echo . Running alignment_stats
    echo .
    CURRENT_PART=$(( NUMBER_OF_ALIGN_PARTS + 2 ))

    RESULT_DIR=${SGE_O_WORKDIR}/results/${TAG}
    OUTPUT_FILENAME=${RESULT_DIR}/${BASENAME}.alignment-stats.txt

    if [ ! -d "$RESULT_DIR" ]; then
        # Output dir doesn't exist but it should
        copy_logs counts ${CURRENT_PART} ${NUMBER_OF_PARTS}
        ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_FAILED_STATUS} --description "-" --index ${CURRENT_PART} --job-type job-part
        ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_FAILED_STATUS} --description "Job failed" --index ${CURRENT_PART} --job-type job
        jobFailedEmail
        exit 1
    fi

    #
    # Create alignment stats
    #
    ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_ALIGNMENT_STATS_STATUS} --description "Alignment Stats, sub-task ${CURRENT_PART} of ${NUMBER_OF_PARTS}, starting" --index ${CURRENT_PART} --job-type job-part
    goby compact-file-stats \
        -o ${OUTPUT_FILENAME} \
        $RESULT_DIR/*.entries
    RETURN_STATUS=$?
    if [ ! $RETURN_STATUS -eq 0 ]; then
        # Failed
        ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_FAILED_STATUS} --description "Alignment Stats, sub-task ${CURRENT_PART} of ${NUMBER_OF_PARTS}, failed" --index ${CURRENT_PART} --job-type job-part
        # Don't exit
    fi
}

function alignment_sequence_variation_stats {
    echo .
    echo . Running sequence_variation_stats
    echo .
    CURRENT_PART=$(( NUMBER_OF_ALIGN_PARTS + 2 ))

    RESULT_DIR=${SGE_O_WORKDIR}/results/${TAG}
    OUTPUT_FILENAME=${RESULT_DIR}/${BASENAME}.sequence-variation-stats.tsv

    if [ ! -d "$RESULT_DIR" ]; then
        # Output dir doesn't exist but it should
        copy_logs counts ${CURRENT_PART} ${NUMBER_OF_PARTS}
        ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_FAILED_STATUS} --description "-" --index ${CURRENT_PART} --job-type job-part
        ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_FAILED_STATUS} --description "Job failed" --index ${CURRENT_PART} --job-type job
        jobFailedEmail
        exit 1
    fi

    #
    # Create sequence-variation-stats
    #
    ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_ALIGNMENT_SEQ_VARIATION_STATS_STATUS} --description "Alignment Sequence Variation Stats, sub-task ${CURRENT_PART} of ${NUMBER_OF_PARTS}, starting" --index ${CURRENT_PART} --job-type job-part
    goby sequence-variation-stats2 \
        -o ${OUTPUT_FILENAME} \
        $RESULT_DIR/*.entries
    RETURN_STATUS=$?
    if [ ! $RETURN_STATUS -eq 0 ]; then
        # Failed
        ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_FAILED_STATUS} --description "Alignment Sequence Variation Stats, sub-task ${CURRENT_PART} of ${NUMBER_OF_PARTS}, failed" --index ${CURRENT_PART} --job-type job-part
        # Don't exit ${RETURN_STATUS}
    fi
}

function wiggles {
    echo .
    echo . Running wiggles
    echo .
    CURRENT_PART=$(( NUMBER_OF_ALIGN_PARTS + 2 ))
    #
    # Wiggles
    #
    ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_WIGGLES_STATUS} --description "Wiggles, sub-task ${CURRENT_PART} of ${NUMBER_OF_PARTS}, starting" --index ${CURRENT_PART} --job-type job-part
    goby counts-to-wiggle --label ${READS_LABEL} --resolution 20 $RESULT_DIR/*.counts
    RETURN_STATUS=$?
    if [ ! $RETURN_STATUS -eq 0 ]; then
        # Failed
        ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_FAILED_STATUS} --description "Wiggles, sub-task ${CURRENT_PART} of ${NUMBER_OF_PARTS}, failed" --index ${CURRENT_PART} --job-type job-part
        # Don't exit
    fi
}

function bedgraph {
    echo .
    echo . Running bedgraph
    echo .
    CURRENT_PART=$(( NUMBER_OF_ALIGN_PARTS + 2 ))
    #
    # bedgraph
    #
    ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_WIGGLES_STATUS} --description "Bedgraph, sub-task ${CURRENT_PART} of ${NUMBER_OF_PARTS}, starting" --index ${CURRENT_PART} --job-type job-part
    goby counts-to-bedgraph --label ${READS_LABEL} $RESULT_DIR/*.counts
    RETURN_STATUS=$?
    if [ ! $RETURN_STATUS -eq 0 ]; then
        # Failed
        ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_FAILED_STATUS} --description "Bedgraph, sub-task ${CURRENT_PART} of ${NUMBER_OF_PARTS}, failed" --index ${CURRENT_PART} --job-type job-part
        # Don't exit
    fi
}


function compress {
    echo .
    echo . Running compress
    echo .
    CURRENT_PART=$(( NUMBER_OF_ALIGN_PARTS + 2 ))
    #
    # Compress to a single file
    #
    ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_COMPRESS_STATUS} --description "Compressing files" --index ${CURRENT_PART} --job-type job-part
    cd $RESULT_DIR
    zip ${BASENAME}-all-files.zip ${BASENAME}*
    cd ${SGE_O_WORKDIR}
}

function cleanup {
    CURRENT_PART=${NUMBER_OF_PARTS}
    # We have concat'd. Remove the interim results.
    if [ ! -z ${SGE_O_WORKDIR} ]; then
        if [ -d ${SGE_O_WORKDIR}/split-results/ ]; then
          echo Not removing split-results
          #  rm -rf ${INTERIM_RESULT_DIR}
        fi

        if [ -d ${SGE_O_WORKDIR}/results/ ]; then
            rm -rf ${SGE_O_WORKDIR}/results/
        fi

        if [ -d ${SGE_O_WORKDIR}/source/ ]; then
            rm -rf ${SGE_O_WORKDIR}/source/
        fi
    fi
    #
    # Keep this in case we need to run again with the same version?
    #
    # rm -rf ${GOBY_DIR}
}

function job_complete {
    echo .
    echo . Running job_complete
    echo .
    CURRENT_PART=$(( NUMBER_OF_ALIGN_PARTS + 2 ))
    #
    # Job completely done
    #
    copy_logs complete ${CURRENT_PART} ${NUMBER_OF_PARTS}
    ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_COMPLETED_STATUS} --description "-" --index ${CURRENT_PART} --job-type job-part
    ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_COMPLETED_STATUS} --description "Job completed" --index ${CURRENT_PART} --job-type job

    jobCompletedEmail
}


function diffexp_job_complete {
    echo .
    echo . diffexp_job_complete
    echo .
    #
    # Job completely done
    #
    copy_logs diffexp 1 1

    ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_COMPLETED_STATUS} --description "-" --index ${CURRENT_PART} --job-type job-part
    ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_COMPLETED_STATUS} --description "Job completed" --index ${CURRENT_PART} --job-type job

    jobCompletedEmail
}

function diffexp {
    jobStartedEmail

    CURRENT_PART=1

    RESULT_DIR=${SGE_O_WORKDIR}/results/${TAG}
    /bin/mkdir -p ${RESULT_DIR}

    #create the directory where alignments will be downloaded
    /bin/mkdir -p  "${ENTRIES_DIRECTORY}"


    #fetch the input entries from the fileset area
    fetch_input_alignments

    #
    # Differential Expression Analysis
    #
    ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_START_STATUS} --description "-" --index ${CURRENT_PART} --job-type job

    RETURN_STATUS=0


    if [ "${SPLIT_PROCESS_COMBINE}" == "false" ]; then
        ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_START_STATUS} --description "Starting alignment analysis plugin." --index ${CURRENT_PART} --job-type job

        (cd ${TMPDIR} ; plugin_alignment_analysis_sequential )
        dieUponError "Alignment analysis plugin failed."

        ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_START_STATUS} --description "Alignment analysis plugin has returned." --index ${CURRENT_PART} --job-type job

        /bin/mkdir -p ${RESULT_DIR}
        %COPY_PLUGIN_OUTPUT_FILES%

    else
      # define how to slice the input files for parallelization:
      ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_DIFF_EXP_STATUS} --description "Starting to define parallelization plan." --index ${CURRENT_PART} --job-type job-part

      (cd ${TMPDIR} ; plugin_alignment_analysis_split 25 ${TAG}-slicing-plan.txt ${ENTRIES_FILES} )
      dieUponError "Unable to split alignment into parts for parallel processing."
      cp ${TMPDIR}/${TAG}-slicing-plan.txt ${RESULT_DIR}/${TAG}-slicing-plan.txt

      NUMBER_SEQ_VAR_SLICES=`(cd ${TMPDIR} ; plugin_alignment_analysis_num_parts ${RESULT_DIR}/${TAG}-slicing-plan.txt)`

      # Introduce a synonym since some part of this script also use NUMBER_OF_ALIGN_PARTS
      NUMBER_OF_ALIGN_PARTS=${NUMBER_SEQ_VAR_SLICES}

      ${QUEUE_WRITER} --tag ${TAG} --status ${JOB_PART_DIFF_EXP_STATUS} --description "Parallelization plan generated. " --index ${CURRENT_PART} --job-type job-part

      # start SGE array jobs with NUMBER_SEQ_VAR_SLICES pieces:

      setup_parallel_alignment_analysis_jobs $RESULT_DIR/${TAG}-slicing-plan.txt
      RETURN_STATUS=$?
      # we exit ${RETURN_STATUS} here because the job has been submitted to SGE. Other parts will execute and
      # finish or fail the job
      exit ${RETURN_STATUS}
    fi

    #
    # Push alignment default results
    #
    push_alignment_analysis_results
    dieUponError "Cannot push alignment Results"
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

function setup_plugin_functions {
    # define no-op function to be overridden as needed by plugin script:
    plugin_alignment_combine() { echo; }
    plugin_alignment_analysis_sequential() { echo; }
    plugin_alignment_analysis_split() { echo; }
    plugin_alignment_analysis_process() { echo; }
    plugin_alignment_analysis_combine() { echo; }
    # include the plugin_align function for the appropriate aligner:
    . ${JOB_DIR}/script.sh

}

#######################################################################################
## Script logic starts here
#######################################################################################

setup

ARTIFACT_REPOSITORY_DIR=%ARTIFACT_REPOSITORY_DIR%
. ${JOB_DIR}/artifacts.sh


case ${STATE} in
    install_plugin_artifacts)
        install_plugin_mandatory_artifacts
        install_plugin_artifacts
        setup_plugin_functions
        ;;
    pre_align)
        install_plugin_mandatory_artifacts
        if [ "${PLUGIN_ARTIFACTS_SUBMIT}" == "true" ]; then
            install_plugin_artifacts
        fi
        #fetch the input reads from the fileset area
        fetch_input_reads
        echo "export READS=${READS}" >> "${JOB_DIR}/constants.sh"
        setup_align
        ;;
    bam_align)
        install_plugin_mandatory_artifacts
        if [ "${PLUGIN_ARTIFACTS_ALIGN}" != "false" ]; then
            install_plugin_artifacts
        fi
        setup_plugin_functions
        fetch_input_reads
        echo "export READS=${READS}" >> "${JOB_DIR}/constants.sh"
        bam_align
        ;;
    single_align)
        install_plugin_mandatory_artifacts
        if [ "${PLUGIN_ARTIFACTS_ALIGN}" != "false" ]; then
            install_plugin_artifacts
        fi
        setup_plugin_functions
        run_single_align
        ;;
    single_alignment_analysis_process)
        install_plugin_mandatory_artifacts
        if [ "${PLUGIN_ARTIFACTS_PROCESS}" != "false" ]; then
            install_plugin_artifacts
        fi
        setup_plugin_functions
        run_single_alignment_analysis_process
        ;;
    alignment_analysis_combine)
        install_plugin_mandatory_artifacts
        if [ "${PLUGIN_ARTIFACTS_COMBINE}" != "false" ]; then
            install_plugin_artifacts
        fi
        ALL_REGISTERED_TAGS=""
        setup_plugin_functions
        run_alignment_analysis_combine
        push_job_metadata ${ALL_REGISTERED_TAGS}
        diffexp_job_complete
        ;;
    diffexp)
        install_plugin_mandatory_artifacts
        if [ "${PLUGIN_ARTIFACTS_SUBMIT}" == "true" ]; then
            install_plugin_artifacts
        fi
        setup_plugin_functions
        diffexp
        cleanup
        ;;
    post)
        install_plugin_mandatory_artifacts
        if [ "${PLUGIN_ARTIFACTS_POST}" == "true" ]; then
            install_plugin_artifacts
        fi
        ALL_REGISTERED_TAGS=""
        setup_plugin_functions
        alignment_concat
        alignment_counts
        alignment_stats
        alignment_sequence_variation_stats
        wiggles
        bedgraph
        compress
        if [ "${SUPPORTS_GOBY_ALIGNMENTS}" == "true" ]; then
            push_goby_alignments
        fi
        if [ "${SUPPORTS_BAM_ALIGNMENTS}" == "true" ]; then
            push_bam_alignments
        fi
        push_aligner_results
        push_job_metadata ${ALL_REGISTERED_TAGS}
        cleanup
        job_complete
        ;;
    *)
        cd ${JOB_DIR}
        #install_plugin_mandatory_artifacts
        #if [ "${PLUGIN_ARTIFACTS_SUBMIT}" == "true" ]; then
        #    install_plugin_artifacts
        #fi

        SUBMISSION=`qsub -N ${TAG}.submit -r y -terse -v STATE=${INITIAL_STATE} oge_job_script.sh`
        checkSubmission $SUBMISSION
        append_kill_file ${SUBMISSION}
        echo ${SUBMISSION}
        ;;
esac
