package org.campagnelab.gobyweb.clustergateway.jobs.simulator;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * List of options added to an aligner by a {@link org.campagnelab.gobyweb.clustergateway.jobs.AlignmentAnalysisJobBuilder}
 * @author manuele
 *
 */
public class AlignmentAnalysisDefaultOptions {

    public static SortedSet<Option> get() {
        SortedSet<Option> options = new TreeSet<Option>();
        options.add(new Option("PLUGIN_ID", null, Option.OptionKind.STRING));
        options.add(new Option("ORGANISM", null, Option.OptionKind.STRING));
        options.add(new Option("GENOME_REFERENCE_ID", null, Option.OptionKind.STRING));
        options.add(new Option("DIFF_EXP_TYPE", null, Option.OptionKind.STRING));
        options.add(new Option("ENTRIES_DIRECTORY", null, Option.OptionKind.STRING));
        options.add(new Option("ENTRIES_FILES", null, Option.OptionKind.STRING));
        options.add(new Option("ALIGNMENT_FILES", null, Option.OptionKind.STRING));
        options.add(new Option("ENTRIES_EXT", null, Option.OptionKind.STRING));
        options.add(new Option("GROUPS_DEFINITION", null, Option.OptionKind.STRING));
        options.add(new Option("NUM_GROUPS", null, Option.OptionKind.NUMERIC));
        options.add(new Option("COMPARE_DEFINITION", null, Option.OptionKind.STRING));
        return options;

    }
}
