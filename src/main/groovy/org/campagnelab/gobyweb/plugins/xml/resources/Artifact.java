/*
 * Copyright (c) 2011-2012  by Cornell University  and the  Cornell Research Foundation,
 * Inc. All Rights Reserved.
 *
 * GobyWeb plugins  are released  under the LGPL3 license,  unless  otherwise specified
 * by the license of a specific plugin. See the file LGPL3.license in this distribution
 * for a copy of the LGPL license.
 *
 * When a plugin is not released under the LGPL3 license,  the comments  at the top  of
 * the plugin's config.xml will indicate how that specific plugin is released/licensed.
 */

package org.campagnelab.gobyweb.plugins.xml.resources;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.campagnelab.gobyweb.plugins.xml.common.Attribute;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * Describes an artifact. Artifacts are parts of a resource plugin that must be obtained and installed at runtime.
 * Each artifact is represented by an ID, which the plugin script knows how to fetch and install on the cluster node.
 * If a resource defines at least one artifact, it must implement a plugin_install_artifact function with two arguments:
 * artifact id and installation path (to a directory where the artifact will be installed).
 *
 * @author Fabien Campagne
 *         Date: 12/15/12
 *         Time: 1:36 PM
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Artifact {
    /**
     * Identifier of the artifact.
     */
    @XmlAttribute
    public String id;
    /**
     * The installation order, an integer between 0 and Integer.MAX_VALUE. Value 0 indicates that the
     * artifact should be installed first, 1 second, and so on.
     */
    @XmlAttribute
    public int order;
    /**
     * Attribute value pairs associated with this artifact.
     */
    @XmlElementWrapper(name = "attributes")
    @XmlElement(name = "attribute")
    public List<Attribute> attributes;

    public Artifact() {
        this.attributes = new ObjectArrayList<Attribute>();
    }

    /**
     * Returns true if any of the attributes has no defined/assigned value.
     * @return True if any of the attributes has no defined/assigned value.
     */
    public boolean hasUndefinedAttributes() {
        for (Attribute attribute: attributes) {
            if (attribute.value==null) {
                return true;
            }
        }
        return false;
    }
}
