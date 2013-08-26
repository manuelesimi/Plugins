package org.campagnelab.gobyweb.clustergateway.jobs.simulator;

/**
 * Option for plugin submission.
 * @author mauele
 */
public class Option implements Comparable<Option> {

    public static enum OptionKind {
        FILE, DIRECTORY, NUMERIC, BOOLEAN, STRING
    }

    public final String name;
    public final String value;
    public final OptionKind kind;

    public Option(String name, String value, OptionKind kind) {
        this.name = name;
        this.value = value;
        this.kind = kind;
    }

    @Override
    public int compareTo(Option option) {
        return option.name.compareTo(this.name);
    }

}
