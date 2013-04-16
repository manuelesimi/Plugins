#!/bin/sh

#
. constants.sh

READ_FILES_LIST=""

function plugin_task {

     ${FILESET_COMMAND} --has-fileset INPUT_READS
     #${FILESET_COMMAND} --has-fileset INPUT_READS.*
     #${FILESET_COMMAND} --has-fileset INPUT_READS.READS_FILE
     if [ $? == 0 ]; then
       echo Input compact reads are not available
       return 0
     fi

     READ_FILES_LIST=`${FILESET_COMMAND} --fetch INPUT_READS.READS_FILE`
     #READ_FILES_LIST=`${FILESET_COMMAND} --fetch INPUT_READS.*`
     #READ_FILES_LIST=`${FILESET_COMMAND} --fetch INPUT_READ`

     if [ $? == 0 ]; then
        echo Failed to fecth compact reads
        echo ${READ_FILES_LIST}
        return 0
     fi
     echo "Localized filesets ${READ_FILES_LIST}"

     java -cp rnaselect-1.0.0-tool.jar:$CLASSPATH org.campagnelab.rnaselect.App2 --output out.tsv ${READ_FILES_LIST}

     #push back the generated tsv
     ${FILESET_COMMAND} --push STATS: **/*.tsv
     #${FILESET_COMMAND} --push STATS: out.tsv
     #${FILESET_COMMAND} --push **/*.tsv
     #${FILESET_COMMAND} --push out.tsv

      if [ $? == 0 ]; then
        echo Failed to push back the output TSV file
        return 0
     fi
}



#TODO: replace return 0 with dieUponError