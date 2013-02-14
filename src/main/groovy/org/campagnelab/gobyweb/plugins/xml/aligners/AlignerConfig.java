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

package org.campagnelab.gobyweb.plugins.xml.aligners;

import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;

import javax.xml.bind.annotation.*;

/**
 * Configuration information needed to describe a GobyWeb aligner plugin.
 *
 * @author Fabien Campagne
 *         Date: 10/7/11
 *         Time: 11:30 AM
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class AlignerConfig extends ExecutableConfig {

    public AlignerConfig() {
    }

    /**
     * Indicates whether the aligner can read Goby read files.
     */
    public boolean supportsGobyReads;

    /**
     * Indicates whether the aligner can write alignments in the Goby format.
     */
    public boolean supportsGobyAlignments;

    /**
     * Indicates whether the aligner can write alignments in the BAM format. When this flag is set to true, the aligner
     * script is responsible for checking the value of the PAIRED_END_ALIGNMENT environment variable. The variable
     * is true when the sample is paired-end and false otherwise. The aligner script need to appropriately handle
     * each condition. When the supportsBAMAlignments is set to false, the aligner script will only be used for single
     * end samples files, it therefore does not need to check or handle the PAIRED_END_ALIGNMENT variable, which will
     * always be false.
     */
    public boolean supportsBAMAlignments;

    /**
     * Indicates whether the aligner can read Fastq read files.
     */
    public boolean supportsFastqReads;
    /**
     * Indicates whether the aligner can read Fasta read files.
     */
    public boolean supportsFastaReads;

    /**
     * Gobyweb locates the index directory based on this pattern. Special values are
     * %ORGANISM%, %VERSION%, and %COLOR%.
     * - %ORGANISM% will be a value such as "homo_sapiens"
     * - %VERSION% specifies the version of the reference and as defined in [webserver]/conf/Config.groovy
     *   in the variable gobyweb.organismToReferencesMap, such as "NCBI36.54"
     * - %SPACE% will be the value "basespace" or "colorspace"
     */
    public String indexDirectory;

    /* -- The below determine if the aligner is suitable for a specific sample -- */

    /**
     * Indicates whether this aligner supports reads in color-space. When this flag is set to true, the aligner
     * script is responsible for checking the value of the COLOR_SPACE environment variable. The variable
     * is true when the sample is color-space encoded, and false otherwise. The aligner script need to appropriately handle
     * each condition. When the supportsColorSpace is set to false, the aligner script will only be used for base-space
     * samples files, it therefore does not need to check or handle the COLOR_SPACE variable, which will
     * always be false.
     */
    public boolean supportsColorSpace;

    /**
     * Indicates whether the aligner can perform paired-end alignment.
     */
    public boolean supportsPairedEndAlignments;
    /**
     * Indicates that the aligner can process bisulfite converted reads. If this flag is true, GobyWeb will
     * run this aligner against bisulfite converted samples.
     */
    public boolean supportsBisulfiteConvertedReads;


    /**
     * Gets a human readable description of the configuration type
     *
     * @return the description
     */
    @Override
    public String getHumanReadableConfigType() {
        return "ALIGNER";
    }


    @Override
    public String toString() {
        return String.format("%s/%s (%s) num-rules: %d, num-needs: %d", this.getHumanReadableConfigType(), this.name, this.version, this.options.rules().size(), this.runtime.needs.size());
    }
}
