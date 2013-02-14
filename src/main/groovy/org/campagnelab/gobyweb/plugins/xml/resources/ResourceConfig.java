/*
 * Copyright (c) 2011  by Cornell University and the Cornell Research
 * Foundation, Inc.  All Rights Reserved.
 *
 * Permission to use, copy, modify and distribute any part of GobyWeb web
 * application for next-generation sequencing data alignment and analysis,
 * officially docketed at Cornell as D-5061 ("WORK") and its associated
 * copyrights for educational, research and non-profit purposes, without
 * fee, and without a written agreement is hereby granted, provided that
 * the above copyright notice, this paragraph and the following three
 * paragraphs appear in all copies.
 *
 * Those desiring to incorporate WORK into commercial products or use WORK
 * and its associated copyrights for commercial purposes should contact the
 * Cornell Center for Technology Enterprise and Commercialization at
 * 395 Pine Tree Road, Suite 310, Ithaca, NY 14850;
 * email:cctecconnect@cornell.edu; Tel: 607-254-4698;
 * FAX: 607-254-5454 for a commercial license.
 *
 * IN NO EVENT SHALL THE CORNELL RESEARCH FOUNDATION, INC. AND CORNELL
 * UNIVERSITY BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL,
 * OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF
 * WORK AND ITS ASSOCIATED COPYRIGHTS, EVEN IF THE CORNELL RESEARCH FOUNDATION,
 * INC. AND CORNELL UNIVERSITY MAY HAVE BEEN ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 *
 * THE WORK PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE CORNELL RESEARCH
 * FOUNDATION, INC. AND CORNELL UNIVERSITY HAVE NO OBLIGATION TO PROVIDE
 * MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.  THE CORNELL
 * RESEARCH FOUNDATION, INC. AND CORNELL UNIVERSITY MAKE NO REPRESENTATIONS AND
 * EXTEND NO WARRANTIES OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
 * PARTICULAR PURPOSE, OR THAT THE USE OF WORK AND ITS ASSOCIATED COPYRIGHTS
 * WILL NOT INFRINGE ANY PATENT, TRADEMARK OR OTHER RIGHTS.
 */

package org.campagnelab.gobyweb.plugins.xml.resources;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.campagnelab.gobyweb.plugins.xml.common.PluginFile;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Describes a resource installed in GobyWeb. Resources typically represent third-party software. At this time,
 * GobyWeb resources must be compiled to execute on the Linux server where jobs will be run. Resource executables
 * must be described as plugin files and associated with an identifier unique within the resource.
 *
 * @author Fabien Campagne
 *         Date: 10/18/11
 *         Time: 11:05 AM
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class ResourceConfig extends ResourceConsumerConfig {

    /**
     * List of resource requirements. Every required resource must be available for a plugin to be installed. Missing
     * resources generate errors at plugin definition time and prevent the plugin from being shown to the end-user.
     * A plugin that has been installed is guaranteed to have access to the specified resources.
     */
    @XmlElementWrapper(name = "artifacts")
    @XmlElement(name = "artifact")
    public ArrayList<Artifact> artifacts = new ArrayList<Artifact>();


    /**
     * Files contained by the resource
     */
    @XmlElementWrapper(name = "files")
    @XmlElement(name = "file")
    public List<PluginFile> files = new ArrayList<PluginFile>();
    /**
     * Determine if this resource is of a release equal or larger than version.
     * @param version Version string, in the format _long_ [. _long_ ]*, such as 2.3.1.2
     * @return True if this resource config has a version number at least as large as the argument.
     */
    public boolean atLeastVersion(final String version) {

        LongList requiredVersion = makeVersionList(version);
        LongList resourceActualVersion = makeVersionList(this.version);
        int numRequired = requiredVersion.size();
        while (resourceActualVersion.size() < numRequired) {
            // Make sure we have enough pieces of version, append 0's if we don't
            resourceActualVersion.add(0);
        }

        for (int i=0;i<numRequired; i++) {
            if (resourceActualVersion.getLong(i) == requiredVersion.getLong(i)) continue;
            return resourceActualVersion.getLong(i) > requiredVersion.getLong(i);
        }
        return true;
    }

    /**
     * Convert "4.3.2.1.0" into a LongList [4,3,2,1,0]
     * @param version
     * @return
     */
    private LongList makeVersionList(final String version) {
        LongList versionList = new LongArrayList();
        for (final String versionItem : version.split("[\\.]")) {
            versionList.add(Long.parseLong(versionItem));
        }
        return versionList;
    }

    public boolean exactlyVersion(final String version) {
        LongList requiredVersion = makeVersionList(version);
        LongList resourceActualVersion = makeVersionList(this.version);
        int numRequired = requiredVersion.size();
        int numActual = resourceActualVersion.size();

        if (numRequired != numActual) {
            return false;
        }

        for (int i = 0; i < numRequired; i++) {
            if (resourceActualVersion.getLong(i) != requiredVersion.getLong(i)) {
                return false;
            }
        }
        return true;
    }


    /**
     * Validates the configuration. Call this method after unmarshalling a config to check that the configuration
     * is semantically valid. Returns null when no errors are found in the configuration, or a list of errors encountered.
     * This method also sets userDefinedValue for required options.
     *
     * @return list of error messages, or null when no errors are detected.
     */
    @Override
    public void validate(List<String> errors) {
        this.validateArtifacts();
    }

    private void validateArtifacts() {
        if (!artifacts.isEmpty())  {
            // the configuration has at least one artifact, it must provide an install.sh BASH file.
        }

    }

    /**
     * Post load activities
     */
    @Override
    public void loadCompletedEvent() {
        super.loadCompletedEvent();
        for (PluginFile pluginFile : this.files) {
            pluginFile.constructLocalFilename(this.pluginDirectory);
        }
    }

    /**
     * Post config file activities
     */
    @Override
    public void configFileReadEvent() {
        super.configFileReadEvent();
        if (this.artifacts.isEmpty())
            return;
        for (PluginFile file : this.files) {

            if (file.filename.compareTo("install.sh") == 0) {
                return;
            }
        }
        //if here, not script.sh was found. we define the default script file.
        PluginFile SCRIPT_FILE = new PluginFile();
        SCRIPT_FILE.filename = "install.sh";
        SCRIPT_FILE.id = "INSTALL";
        this.files.add(SCRIPT_FILE);
    }

    /**
     * Gets a human readable description of the configuration type
     *
     * @return the description
     */
    @Override
    public String getHumanReadableConfigType() {
        return "RESOURCE";
    }


    @Override
    public String toString() {
        return String.format("%s/%s (%s)",this.getHumanReadableConfigType(), this.name, this.version);
    }

    public List<PluginFile> getFiles() {
        return files;
    }
}
