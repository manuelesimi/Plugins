# Installation script for STAR version 2.2.0g
function plugin_install_artifact {

    id=$1
    installation_path=$2

    case ${id} in

        'EXECUTABLE' )
            wget ftp://ftp2.cshl.edu/gingeraslab/tracks/STARrelease/Alpha/STAR_2.2.0g.tgz
            gzip -c -d  STAR_2.2.0g.tgz |tar -xvf -
            (cd STAR_2.2.0g; make)
            cp STAR_2.2.0g/STAR ${installation_path}/
            return 0
            ;;

        'INDEX' )
            ORGANISM=$3
            BUILD_NUMBER=$4
            ENSEMBL_RELEASE=$5
            echo "Organism=${ORGANISM} Reference-build=${GENOME_REFERENCE_ID}"

            STAR_DIR=${RESOURCES_ARTIFACTS_STAR_EXECUTABLE}
            INDEX_DIR=index
            mkdir -p ${INDEX_DIR}
            GENOME_DIR=$(eval echo \${RESOURCES_ARTIFACTS_ENSEMBL_GENOMES_TOPLEVEL_${ORGANISM}_${BUILD_NUMBER}_${ENSEMBL_RELEASE}})
            FAI_INDEXED_GENOME_DIR=$(eval echo \${RESOURCES_ARTIFACTS_FAI_INDEXED_GENOMES_TOPLEVEL_${ORGANISM}_${BUILD_NUMBER}_${ENSEMBL_RELEASE}})


            NUM_THREADS=`grep physical  /proc/cpuinfo |grep id|wc -l`

            if [ "$ORGANISM" = "HOMO_SAPIENS" ]; then

                rm -f SJ.Gencode11.tab
                wget ftp://ftp2.cshl.edu/gingeraslab/tracks/STARrelease/STARgenomes/SpliceJunctionDatabases/SJ.Gencode11.tab
                sed -e 's/^chr//' SJ.Gencode11.tab >SJ.Gencode11.tab.fixed
                SPLICE_SITES_OPTION=" --sjdbFileChrStartEnd SJ.Gencode11.tab.fixed --sjdbOverhang 49"
            fi
            INPUT_FASTA_NO_GZ=genome.fasta
            gzip -c -d  ${FAI_INDEXED_GENOME_DIR}/genome-toplevel.fasta.gz >${INPUT_FASTA_NO_GZ}

            nice ${STAR_DIR}/STAR --runMode genomeGenerate --genomeDir ${INDEX_DIR} \
            --genomeFastaFiles ${INPUT_FASTA_NO_GZ}  --runThreadN ${NUM_THREADS} ${SPLICE_SITES_OPTION}
            STATUS=$?
            if [ ${STATUS} != 0 ]; then
             return ${STATUS}
            fi
            # Keep only the first ID to make short chromosome names:
            cut -f 1 -d " " ${INDEX_DIR}/chrName.txt > tmp-chrName
            mv tmp-chrName  ${INDEX_DIR}/chrName.txt
            cp -r ${INDEX_DIR} ${installation_path}/
            echo "Finished indexing Organism=${ORGANISM} Reference-build=${GENOME_REFERENCE_ID}"
            return 0
            ;;

        *)  echo "Resource artifact id not recognized: "+$id
            return 99
            ;;

    esac

    return 1

}


function get_attribute_values() {

    id=$1
    out=$2

    echo get_attribute_values for ID=${id}

    echo # get environment variables for GobyWeb job from SGE work directory:
    . ${SGE_O_WORKDIR}/constants.sh

    BUILD_NUMBER=`echo ${GENOME_REFERENCE_ID} | awk -F\. '{print $1}'`
    ENSEMBL_VERSION_NUMBER=`echo ${GENOME_REFERENCE_ID} | awk -F\. '{print $(NF)}'`
    echo >>${out} "organism=${ORGANISM}"
    echo >>${out} "reference-build=${BUILD_NUMBER}"
    echo >>${out} "ensembl-version-number=${ENSEMBL_VERSION_NUMBER}"

    echo "Printing result from ${out}:"
    cat ${out}
    return 0
}