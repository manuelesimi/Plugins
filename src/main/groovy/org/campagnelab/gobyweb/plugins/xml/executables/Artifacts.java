package org.campagnelab.gobyweb.plugins.xml.executables;


import javax.xml.bind.annotation.XmlAttribute;

/**
 * Declaration of runtime requirement for artifacts installation. A scope can declare if it needs the support of the
 * plugins artifacts at runtime.
 *
 * @author manuele
 */
public class Artifacts {

    /**
     * A value that describes if a scope requires artifacts.
     */
    @XmlAttribute
    public boolean required;

    @XmlAttribute
    public String scope = "GLOBAL";

    public Artifacts() {/* required by JAXB */}

    public Artifacts(String scope, boolean required ) {
        this.required = required;
        this.scope = scope;
    }

}
