#!/bin/sh

#
. constants.sh

DIRS_LIST=""

OUTPUT_FILE=${JOB_DIR}/output-data/out.tar

function plugin_task {

     # ${FILESET_COMMAND}
     # options:  (-i|--pb-file) <pbfile> [(-d|--download-dir) <downloadDir>] [-q|--has-fileset] [-f|--fetch] [-p|--push] [-h|--help] [-l|--info] fileset1 fileset2 ... filesetN

     ${FILESET_COMMAND} --has-fileset GENERIC_DIRECTORY
     if [ $? == 0 ]; then
       echo Input TEXTs are not available
     else
        DIRS_LIST=`${FILESET_COMMAND} --fetch GENERIC_DIRECTORY`
        if [ $? == 0 ]; then
            echo Failed to fecth TEXT entries
            echo ${DIRS_LIST}
            return 0
         fi
     fi


     echo "Localized filesets ${DIRS_LIST}"

     mkdir "${JOB_DIR}/output-data"
     tar -cvf ${OUTPUT_FILE} ${DIRS_LIST}

     #will be replaced by a request to file-manager
     #scp ${OUTPUT_FILE} ${WEB_SERVER_SSH_PREFIX}:${RESULTS_WEB_DIR}
}
