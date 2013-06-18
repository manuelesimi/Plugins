# Plugins SDK - Task script template

# This is the only function that tasks need to implement.
# It is expected to use FILESET_COMMAND for fetching the input filesets and pushing the produced filesets.
# No input parameters are passed to the function in the current implementation.
function plugin_task {

  echo  "the plugin logic goes here"

}

# This function may be used by a task to push results in the FileSet area according to the OutputSlots declared in
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