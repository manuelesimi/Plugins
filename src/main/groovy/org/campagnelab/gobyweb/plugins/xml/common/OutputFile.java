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

package org.campagnelab.gobyweb.plugins.xml.common;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Describes potential output file of a GobyWeb plugin.
 * @author Fabien Campagne
 *         Date: 10/13/11
 *         Time: 2:48 PM
 */

@XmlAccessorType(XmlAccessType.FIELD)
public class OutputFile {

    public enum OutputFileType {
        IMAGE,
        BROWSE,
        DOWNLOAD
    }

    /**
     * Identifier of the output file.
     */
    public String id;

    /**
     * Indicate whether the output file must be produced for the plugin analysis to be considered successful.
     */
    public boolean required;
    /**
     * Describe the type of data stored in the file. We use a mime type when available. GobyWeb will use this
     * mime-type when sending data from this file back to the browser.
     */
    public String mimeType;

    /**
     * Filename of the output in the job directory. The plugin interprets the filename as relative to the job directory.
     * When the plugin returns, the oge_job_submit.sh script renames the result file to ${JOB_DIR}/results/${TAG}-filename
     */
    public String filename;

    /**
     * Short name describing the output file.  This will be shown to end-users in the GobyWeb interface.
     * The name should be short ot fit in the user interface. Use help for longer descriptions.
     */
    public String name;

    /**
     * Help text describing the content of the file and how it was generated. This will be shown to end-users in the GobyWeb interface.
     */
    public String help;

    /**
     * If applicable, the table name associated with this data.
     */
    public String tableName;

    /**
     * This is generally null but may be populated with a transient value to contain the full path to a
     * specific file.
     */
    @XmlTransient
    public String filenameFullPath;

    /**
     * Determines the more general type of a file given it's mime type, if the file
     * will be displayed to the user inline (IMAGE), browsed (sqlite or Lucene-index), otherwise
     * a file that the user can download.
     *
     * @return
     */
    public OutputFileType getFileType() {
        if (mimeType.startsWith("image/")) {
            return OutputFileType.IMAGE;
        } else if (
                mimeType.equals("application/x-sqlite3") ||
                        mimeType.equals("application/lucene-index")) {
            return OutputFileType.BROWSE;
        } else {
            return OutputFileType.DOWNLOAD;
        }
    }

    public static OutputFile copyFrom(final OutputFile from) {
        final OutputFile copy = new OutputFile();
        copy.id = from.id;
        copy.required = from.required;
        copy.mimeType = from.mimeType;
        copy.filename = from.filename;
        copy.name = from.name;
        copy.help = from.help;
        copy.filenameFullPath = from.filenameFullPath;
        copy.tableName = from.tableName;
        return copy;
    }

    public void afterUnmarshal(final Unmarshaller u, final Object parent) {
    }

}
