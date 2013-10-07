package org.campagnelab.gobyweb.clustergateway.jobs.simulator;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * List of options added to an aligner by a {@link org.campagnelab.gobyweb.clustergateway.jobs.TaskJobBuilder}
 * @author manuele
 *
 */
public class TaskDefaultOptions {
    public static SortedSet<Option> get() {
        SortedSet<Option> options = new TreeSet<Option>();
        options.add(new Option("PLUGIN_ID", null, Option.OptionKind.STRING));
        options.add(new Option("PLUGIN_VERSION", null, Option.OptionKind.STRING));
        return options;
    }
}
