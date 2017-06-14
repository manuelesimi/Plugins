#!/bin/sh
WRAPPER_SCRIPT_PREFIX="local_task_wrapper_script"
. %JOB_DIR%/common.sh


if [ -z "${STATE+set}" ]; then
 // When state is not defined, assume the user wants to submit the job to OGE.
 export STATE="submit"
fi


case ${GOBYWEB_CONTAINER_TECHNOLOGY} in
 singularity)
     DELEGATE_OGE_JOB_SCRIPT="singularity exec ${GOBYWEB_CONTAINER_NAME} %JOB_DIR%/local_task_wrapper_script_legacy.sh"
 ;;
 none)
    DELEGATE_OGE_JOB_SCRIPT="%JOB_DIR%/local_task_wrapper_script_legacy.sh"
 ;;
esac


case ${STATE} in

    run_in_container)
        STATE="run_task"
        ${DELEGATE_OGE_JOB_SCRIPT} "$*"
        ;;
    *)
        # delegate everything else either inside container or execute directly legacy script:
        export STATE="run_task"
        ${DELEGATE_OGE_JOB_SCRIPT} ${STATE}
        ;;
esac
