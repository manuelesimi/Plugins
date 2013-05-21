#!/bin/sh

#
. constants.sh

READ_FILES_LIST=""

function plugin_task {

     echo "fileset command: ${FILESET_COMMAND}"

     ${FILESET_COMMAND} --has-fileset INPUT_READS
     if [ $? != 0 ]; then
       echo Input compact reads are not available
       return 0
     fi

     READ_FILES_LIST=`${FILESET_COMMAND} --fetch INPUT_READS`
     if [ $? != 0 ]; then
        echo Failed to fecth compact reads
        echo ${READ_FILES_LIST}
        return 0
     fi
     echo "Localized filesets ${READ_FILES_LIST}"

     java -cp ${RESOURCES_RNASELECT_RNASELECT_TOOL}:$CLASSPATH org.campagnelab.rnaselect.App2 --output out.tsv ${READ_FILES_LIST}

     #push back the generated tsv
     REGISTERED_TAGS=`${FILESET_COMMAND} --push STATS: *.tsv`
     if [ $? != 0 ]; then
        echo Failed to push back the output TSV file
        return 0
     fi
     echo "RNA-select registered the following FileSet instances: ${REGISTERED_TAGS}"
}



#TODO: replace return 0 with dieUponError