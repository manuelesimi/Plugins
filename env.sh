# JOB_DIR is defined by the caller.
. ${JOB_DIR}/auto-options.sh

# environment file to test genome download and index creation.
export SGE_O_WORKDIR=${JOB_DIR}
export ORGANISM="homo_sapiens"
export GENOME_REFERENCE_ID="NCBI36.54"
# Fake bash GobyWeb scripts. They don't need to contain anything because we have already set the relevant variables.
touch constants.sh

