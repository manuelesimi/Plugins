# Plugins SDK - Aligner script template

# This is the only function that aligners need to implement.
# Parameters:
#   $1: a temporary filename
#   $2: the basename that should be used to store the sorted alignment

function plugin_align {
  #sample parameters reading
  OUTPUT=$1
  BASENAME=$2

  #aligner's logic goes here

}

# This function may be used by a plugin to push additional results in the FileSet area.
# Goby/BAM alignments, counts, tmhs, alignment statistics, bedgraph and wiggles files are automatically pushed by the plugin SDK.
# Here a plugin can push further results that depend on the plugin's logic and according to the OutputSlots declared in
# the plugin configuration.
#
# The function is executed in the JOB_DIR
#
function plugin_push_results {
   echo
   #sample pushing
   #local OUTPUT=`${FILESET_COMMAND} --push OUTPUT_SLOT_NAME: pattern`
   #dieUponError "Failed to push OUTPUT_SLOT_NAME in the fileset area. ${OUTPUT}"
   #echo "The following OUTPUT_SLOT_NAME instances have been successfully registered: ${OUTPUT}"
}