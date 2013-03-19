
function fileset_input_strategy_num_parts {
  # args must be path of files in fileset that needs to be split

  # must return an integer that indicates how many chunks the output will be/has been split into.
}

function fileset_input_strategy_split {

  # Arg 1 is NUMBER_OF_PARTS
  # Arg 2 is filename for file representation of logical split (if produced) SPLICING_PLAN_FILENAME
  # Example:
  shift
  shift
  goby suggest-position-slices \
          --number-of-slices ${NUMBER_OF_PARTS} \
          --output ${SPLICING_PLAN_RESULT} \
          $*
}


function fileset_input_strategy_get_split_arguments {

   # Arg 1 is SLICING_PLAN_FILENAME=$1
   # Arg 2 Index of the split to process

   SLICING_PLAN_FILENAME=$1
   ARRAY_JOB_INDEX=$2

   # returns a string containing a set of arguments sufficient to obtain the indexed split of  data from
   # the complete fileset input.

   # For instance, return "*.entries -s chr1:232332 -e chr2:23023902" when splitting alignment files.
   # When splitting compact reads, with BASE_SUBSET, return: "-k <number of
}