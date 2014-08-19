#!/bin/sh

# Execute the script from the current directory
#$ -cwd

# Combine SGE error and output files.
#$ -j y

# Cluster queue to use
#$ -q %%QUEUE_NAME%%


. constants.sh

. job_common_functions.sh


function setup_task_functions {
    # define no-op function to be overridden as needed by task script:
    plugin_task() { echo; }
    # include the plugin_task function for the appropriate task:
    . ${JOB_DIR}/script.sh

}

function run_task {

  if [ ! -z ${SGE_TASK_ID} ] && [ "${SGE_TASK_ID}" != "undefined" ] && [ "${SGE_TASK_ID}" != "unknown" ]; then
           CURRENT_PART=${SGE_TASK_ID}
  else
           CURRENT_PART=1
  fi

  plugin_task ${CURRENT_PART}

}

function combine {
    SUB_PARTS=""
    for i in 1..${NUMBER_OF_ALIGN_PARTS}
    do
        SUB_PART=`${FILESET_COMMAND} --fetch '<ID of the output fileset attached to the parallelization strategy>' --index ${SPLIT_INDEX}`
        SUB_PARTS="${SUB_PARTS} ${SUB_PART}"
    done
    COMBINED_RESULT=combined.tsv
    goby fdr `${FILESET_COMMAND} --fetch '<ID of the output fileset attached to the parallelization strategy>'` -o ${COMBINED_RESULT}

    ${FILESET_COMMAND} --push [$OUTPUT_FILE] ${COMBINED_RESULT}
    if [ $? == 0 ]; then
            echo Failed to push back the output TSV file
            return 0  #TODO: replace with dieUponError
    fi

}

case ${STATE} in
    task)
        setup_task_functions
        run_task
        ;;
    combine)
        setup_task_functions
        combine
        ;;
    *)
        cd ${JOB_DIR}
        # Determine the number of parts to parallelize with:
        NUMBER_OF_ALIGN_PARTS=`fileset_input_strategy_num_parts INPUT_FILES_IN_INPUT_FILESET `
        if [ ${NUMBER_OF_ALIGN_PARTS} -gt 1 ]; then
                ARRAY_DIRECTIVE="-t 1-${NUMBER_OF_ALIGN_PARTS}"
                fileset_input_strategy_split ${NUMBER_OF_ALIGN_PARTS} split-plan
        fi

        SUBMISSION=`qsub ${ARRAY_DIRECTIVE} -N ${TAG}.submit -terse -v STATE=task oge_task_wrapper_script.sh`
        echo ${SUBMISSION}
        if [ ${NUMBER_OF_ALIGN_PARTS} -gt 1 ]; then
          # Submit the job needed to combine results for all parts:
           COMBINE=`qsub -hold_jid ${SUBMISSION}  -N ${TAG}.combine -terse -v STATE=combine oge_task_wrapper_script.sh`
           ECHO ${COMBINE}
        fi
        ;;
esac

