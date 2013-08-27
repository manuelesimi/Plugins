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
        return options;
    }
}
