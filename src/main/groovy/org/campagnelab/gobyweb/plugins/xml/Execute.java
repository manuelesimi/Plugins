package org.campagnelab.gobyweb.plugins.xml;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;

/**
 * @author Fabien Campagne
 *         Date: 6/21/12
 *         Time: 10:51 AM
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class Execute {
    public ArrayList<Script> scripts() {
        return scripts;
    }

    @XmlElement(name = "script")
    public ArrayList<Script> scripts = new ArrayList<Script>();
}
