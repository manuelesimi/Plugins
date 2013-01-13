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

package org.campagnelab.gobyweb.plugins.xml;


import org.campagnelab.gobyweb.plugins.Plugins;
import javax.xml.bind.annotation.XmlTransient;
import java.io.File;

/**
 * Represents a file associated with a GobyWeb plugin.
 *
 * @author campagne
 *         Date: 10/13/11
 *         Time: 12:15 PM
 */

public class PluginFile {
    /**
     * The identifier for the plugin file. The file path can be retried via the variable ${PLUGINS_ plugin-id _ FILES _ file-id}
     */
    public String id;
    /**
     * The filename local to the plugin directory.
     */
    public String filename;
    /**
     * This property will be populated upon loading the plugin. It will
     * contain the full path to the local plugin file (on the web server/development machine) for this aligner. This
     * path is used to copy the plugin file to the grid servers during job submission.
     */
    @XmlTransient
    public String localFilename;
    /**
     * Indicates that this file is imported from the other plugin whose id is given by the string.
     * If the string is null, the file is local to the plugin directory. Otherwise, the string is the
     * Identifier of a plugin where the file resides. Assuming importFromPlugin=PLUGIN_ID, the file
     * will be found in
     */
    public String importFromPlugin;

    /** Indicates that the file is a directory and should be copied recursively */
    public boolean isDirectory;

    public File getLocalFile() {
        return new File(localFilename);
    }

    /**
     * Construct localFilename.
     *
     * @param pluginDirectory directory where the plugin that defines this file resides.
     * @param plugins         instance of the Plugins class, to locate other plugins by id.
     */
    public void constructLocalFilename(String pluginDirectory, Plugins plugins) {
        if (!isImport()) {
            localFilename = new File(pluginDirectory, filename).getAbsolutePath();
        } else {
            PluginConfig byId = plugins.findById(importFromPlugin);
            if (byId == null) {
                System.out.printf("Cannot locate plugin id %s %n", importFromPlugin);
            } else {
                String dir = byId.pluginDirectory;
                localFilename = new File(dir, filename).getAbsolutePath();
            }
        }
    }

    /**
     * Determine if the plugin file is imported from another plugin.
     *
     * @return True when the file is imported, false otherwise.
     */
    public boolean isImport() {
        return importFromPlugin != null;
    }
}
