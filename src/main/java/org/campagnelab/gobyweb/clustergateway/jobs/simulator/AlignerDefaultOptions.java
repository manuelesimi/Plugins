package org.campagnelab.gobyweb.clustergateway.jobs.simulator;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * List of options added to an aligner by a {@link org.campagnelab.gobyweb.clustergateway.jobs.AlignerJobBuilder}
 * @author manuele
 */
class AlignerDefaultOptions {

   protected static SortedSet<Option> get() {
       SortedSet<Option> options = new TreeSet<Option>();
       //added by OGE script
       options.add(new Option("START_POSITION",null, Option.OptionKind.NUMERIC));
       options.add(new Option("END_POSITION",null, Option.OptionKind.NUMERIC));
       options.add(new Option("READS_FILE", null, Option.OptionKind.STRING));
       //added by the SDK
       options.add(new Option("PAIRED_END_ALIGNMENT",null, Option.OptionKind.BOOLEAN));
       options.add(new Option("BISULFITE_SAMPLE",null, Option.OptionKind.BOOLEAN));
       options.add(new Option("COLOR_SPACE",null, Option.OptionKind.BOOLEAN));
       options.add(new Option("ORGANISM", null, Option.OptionKind.STRING));
       options.add(new Option("ALIGNER", null, Option.OptionKind.STRING));
       options.add(new Option("PLUGIN_ID", null, Option.OptionKind.STRING));
       options.add(new Option("READS_PLATFORM", null, Option.OptionKind.STRING));
       options.add(new Option("PAIRED_END_DIRECTIONS",null, Option.OptionKind.STRING));
       options.add(new Option("PAIRED_END_ALIGNMENT",null, Option.OptionKind.BOOLEAN));
       options.add(new Option("LIB_PROTOCOL_PRESERVE_STRAND",null, Option.OptionKind.BOOLEAN));
       options.add(new Option("READS_LABEL",null, Option.OptionKind.STRING));
       options.add(new Option("BASENAME",null, Option.OptionKind.STRING));
       options.add(new Option("INPUT_READ_LENGTH",null, Option.OptionKind.NUMERIC));
       options.add(new Option("GENOME_REFERENCE_ID", null, Option.OptionKind.STRING));
       options.add(new Option("SOURCE_READS_ID", null, Option.OptionKind.STRING));
       options.add(new Option("CHUNK_SIZE",null, Option.OptionKind.NUMERIC));
       options.add(new Option("NUMBER_OF_ALIGN_PARTS",null, Option.OptionKind.NUMERIC));
       options.add(new Option("NUMBER_OF_PARTS",null, Option.OptionKind.NUMERIC));

       return options;
   }
}
