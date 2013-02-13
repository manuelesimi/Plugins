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

package org.campagnelab.gobyweb.plugins.xml.common;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
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
public class Attribute {
    /**
     * The attribute name.
     */
    @XmlAttribute
    public String name;
    /**
     * The value of the attribute, or null if the value has not yet been assigned.
     */
    @XmlAttribute
    public String value;

}
