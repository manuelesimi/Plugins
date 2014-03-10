package org.campagnelab.gobyweb.plugins.xml.executables;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

/**
 * Describe a SGE/OGE resource needed by a plugin at runtime.
 * @author Fabien Campagne
 *         Date: 11/17/11
 *         Time: 6:46 PM
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class RuntimeRequirements {

    @XmlElement(name = "need")
    public ArrayList<Need> needs=new ArrayList<Need>();

    @XmlElement(name = "artifacts")
    public ArrayList<Artifacts> artifacts=new ArrayList<Artifacts>();

    public ArrayList<Need> needs() {
        return needs;
    }

    public ArrayList<Artifacts> artifacts() {
        return artifacts;
    }
}
