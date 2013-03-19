#!/bin/sh

#
. constants.sh

READ_FILES_LIST=""
OUTPUT_FILE=${JOB_DIR}/output-data/out.tsv

function plugin_task {

     # ${FILESET_COMMAND}
     # options:  (-i|--pb-file) <pbfile> [(-d|--download-dir) <downloadDir>] [-q|--has-fileset] [-f|--fetch] [-p|--push] [-h|--help] [-l|--info] fileset1 fileset2 ... filesetN

     ${FILESET_COMMAND} --has-fileset COMPACT_READS
     if [ $? == 0 ]; then
       echo Input compact reads are not available
       return 0
     fi

     READ_FILES_LIST=`${FILESET_COMMAND} --fetch COMPACT_READS.READS_FILE`
     if [ $? == 0 ]; then
        echo Failed to fecth compact reads
        echo ${READ_FILES_LIST}
        return 0
     fi
     echo "Localized filesets ${READ_FILES_LIST}"

     #TODO: query filsets before to know where to store files
     mkdir "${JOB_DIR}/output-data"
     java -cp rnaselect-1.0.0-tool.jar:$CLASSPATH org.campagnelab.rnaselect.App2  --output ${OUTPUT_FILE} ${READ_FILES_LIST}

     #push back the generated tsv
     ${FILESET_COMMAND} --push [$OUTPUT_FILE] TSV.TSV
      if [ $? == 0 ]; then
        echo Failed to push back the output TSV file
        return 0
     fi
}
