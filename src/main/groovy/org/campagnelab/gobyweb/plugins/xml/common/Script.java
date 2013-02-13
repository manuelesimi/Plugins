package org.campagnelab.gobyweb.plugins.xml.common;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Fabien Campagne
 *         Date: 6/21/12
 *         Time: 10:51 AM
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class Script {
    /**
     * The language in which the script is written. Must be groovy for now.
     */
    @XmlAttribute
    public String language;
    /**
     * The phase during which the script will be executed. At this time, only pne phase is supported: pre-deployment.
     * The pre-deployment phase occurs after a job has been configured with the web-app, just before the plugin is
     * copied to the execution cluster grid.
     */
    @XmlAttribute
    public String phase;
    /**
     * The filename of the script to execute.
     */
    @XmlAttribute
    public String filename;
}
