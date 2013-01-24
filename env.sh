export JOB_DIR=~/tmp/install-7082844104313396863-dir
. auto-options.sh

# environment file to test genome download and index creation.
export SGE_O_WORKDIR=.
export ORGANISM="homo_sapiens"
export GENOME_REFERENCE_ID="NCBI36.54"
# Fake bash GobyWeb scripts. They don't need to contain anything because we have already set the relevant variables.
touch constants.sh

