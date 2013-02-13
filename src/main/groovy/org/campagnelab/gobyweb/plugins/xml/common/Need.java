package org.campagnelab.gobyweb.plugins.xml.common;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Declaration of runtime requirement. A need has a key and a value, where key describe the type
 * of resource a tool needs. The value indicates how much resource will be consumed by the tool
 * during its execution (quantity) or whether the ype of resource is needed at all (true/false).
 * Key/value pairs are assembled and given to the SGE/OGE -l command line argument. For instance,
 * key=excl, value=true will yield -l excl=true which will request exclusive use of a node on the grid
 * for this plugin script.
 *
 * @author Fabien Campagne
 *         Date: 11/17/11
 *         Time: 6:47 PM
 */
public class Need {
    /**
     * A key that identifies an SGE resource.
     */
    @XmlAttribute
    public String key;
    /**
     * A value that describes how much of the resource is needed at runtime by the plugin.
     */
    @XmlAttribute
    public String value;
    /**
     * Scope for this resource need. Typically GLOBAL (default, used for submitting new jobs, running alignmentpost  processing), ALIGN (used to run the aligner plugins align function),
     * PROCESS (used for alignment analysis process function), or COMBINE (used for alignment analysis combine function).
     */
    @XmlAttribute
    public String scope = "GLOBAL";

    public Need() {
    }

    public Need(String scope, String key, String value ) {
        this.key = key;
        this.value = value;
        this.scope = scope;
    }
}
