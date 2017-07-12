#!/bin/sh
WRAPPER_SCRIPT_PREFIX="local_task_wrapper_script"
. %JOB_DIR%/common.sh


if [ -z "${STATE+set}" ]; then
 // When state is not defined, assume the user wants to submit the job to OGE.
 export STATE="submit"
fi

initializeJobEnvironment
case ${STATE} in

    run_in_container)
        STATE="run_task"
        delegate_oge_job_script "$*"
        ;;
    *)
        # delegate everything else either inside container or execute directly legacy script:
        export STATE="run_task"
        delegate_oge_job_script ${STATE}
        ;;
esac
