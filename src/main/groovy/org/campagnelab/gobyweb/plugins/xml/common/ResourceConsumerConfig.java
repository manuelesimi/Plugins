package org.campagnelab.gobyweb.plugins.xml.common;

import org.campagnelab.gobyweb.plugins.xml.BaseConfig;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.ArrayList;
import java.util.List;


/**
 * A configuration that consumes resources
 */
public abstract class ResourceConsumerConfig extends BaseConfig {



    /**
     * List of resource requirements. Every required resource must be available for a plugin to be installed. Missing
     * resources generate errors at plugin definition time and prevent the plugin from being shown to the end-user.
     * A plugin that has been installed is guaranteed to have access to the specified resources.
     */
    @XmlElementWrapper(name = "requires")
    @XmlElement(name = "resource")
    public List<Resource> requires = new ArrayList<Resource>();

    public List<Resource> getRequiredResources() {
        return this.requires;
    }
}
