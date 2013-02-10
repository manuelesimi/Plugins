# JOB_DIR is defined by the caller.
. ${JOB_DIR}/auto-options.sh

# environment file to test genome download and index creation.
export SGE_O_WORKDIR=${JOB_DIR}

export ORGANISM="homo_sapiens"
export GENOME_REFERENCE_ID="NCBI36.54"
#export ORGANISM="Caenorhabditis_elegans"
#export GENOME_REFERENCE_ID="WBcel215.69"
# Fake bash GobyWeb scripts. They don't need to contain anything because we have already set the relevant variables.
touch constants.sh

# This function must exist in an environment script:

function plugin_install_artifact {
    return 0
}