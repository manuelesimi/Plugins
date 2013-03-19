#!/bin/sh

#
. constants.sh

READ_FILES_LIST=""

function plugin_task {

     # Usage: ${FILESET_COMMAND}
     #           (-i|--pb-file) <pbfile> [(-d|--download-dir) <downloadDir>] [-g|--get-push-destination] [-q|--has-fileset] [-f|--fetch] [(-p|--push) <push>] [-h|--help] [-l|--info] fileset1 fileset2 ... filesetN
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

     java -cp rnaselect-1.0.0-tool.jar:$CLASSPATH org.campagnelab.rnaselect.App2 --output out.tsv ${READ_FILES_LIST}

     #push back the generated tsv
     ${FILESET_COMMAND} --push:out.tsv TSV.TSV
      if [ $? == 0 ]; then
        echo Failed to push back the output TSV file
        return 0
     fi
}



#TODO: replace return 0 with dieUponError