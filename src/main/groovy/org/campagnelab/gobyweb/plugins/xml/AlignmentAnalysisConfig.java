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

import javax.xml.bind.annotation.*;
import java.util.ArrayList;

/**
 * Configuration information needed to describe a GobyWeb alignment analysis plugin.
 *
 * @author Fabien Campagne
 *         Date: 10/9/11
 *         Time: 11:05 AM
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class AlignmentAnalysisConfig extends ExecutablePluginConfig {
    /**
     * Indicates if the plugin supports transcript alignments (i.e., built against cDNA references).
     * When this flag is set to true, the analysis plugin
     * script is responsible for checking the value of the IS_TRANSCRIPT environment variable. The variable
     * is true when the alignments are transcript alignments, and false otherwise. The plugin script needs
     * to appropriately handle each condition. When the supportsTranscriptAnalysis is set to false, the
     * analysis script will only be used for alignments built against full genome references. In these cases,
     * the analysis script therefore does not need to check or handle the IS_TRANSCRIPT variable, which will
     * always be false.
     */
    public boolean supportsTranscriptAlignments;

    /**
     * Indicates whether the alignment analysis can read goby alignments.
     */
    public boolean supportsGobyAlignments;

    /**
     * Indicates whether the alignment analysis can read bam alignments.
     */
    public boolean supportsBAMAlignments;
    /**
     * When this flag is true (default), copy the alignment files to the source directory before executing the
     * plugin script.
     */
    public boolean copyAlignments=true;
    /**
     * When this flag is true (default), copy the weight files to the source directory before executing the
     * plugin script.
     */
    public boolean copyWeightFiles=true;
    /**
     * Indicate that this plugin must only be provided with alignments from reads treated with bisulfite.
     */
    public boolean requiresBisulfiteAlignments;
    /**
     * Indicates that this analysis yields tab delimited output. When this attribute is true,
     * producesVariantCallingFormatOutput must be false.
     */
    public boolean producesTabDelimitedOutput;
    /**
     * Indicates that this analysis yields output in the VCF format.  When this attribute is true,
     * producesTabDelimitedOutput must be false.
     */
   public boolean producesVariantCallingFormatOutput;
    /**
     * Indicate that the analysis script supports splitProcessCombine. When true, the plugin script implements
     * three functions:
     * <ol>
     * <LI>plugin_alignment_analysis_split. This function determines how to split the alignment files for parallel
     * processing</LI>
     * <LI>plugin_alignment_analysis_process. This function processes one chunk of the parallel job.</LI>
     * <LI>plugin_alignment_analysis_combine. This function combines results from independent jobs into a final analysis result.</LI>
     * </ol>
     * <p/>
     * When false, the plugin script only needs to define the function plugin_alignment_analysis_sequential, which is
     * responsible for completing the analysis and writing results.
     */
    public boolean splitProcessCombine;
    /**
     * Minimum number of groups supported by this plugin. When a user indicates a k group comparison, only plugins
     * with  minimumNumberOfGroups <= k are selectable.
     */
    public int minimumNumberOfGroups;
     /**
     * Maximum number of groups supported by this plugin.  When a user indicates a k group comparison, only plugins
     * with k<=maximumNumberOfGroups are selectable.
     */
    public int maximumNumberOfGroups;

    public enum AnalysisType {
        DiffExp,
        SequenceVariants,
        Methylation
    }

    /**
     * The type of analysis that the plugin produces. Valid values are
     */
    public AnalysisType analysisType;

    @Override

    public void validate(ArrayList<String> errors) {

        super.validate(errors);

        if (!producesTabDelimitedOutput && !producesVariantCallingFormatOutput) {
            errors.add("At least one output (TSV or VCF) must be specified.");
        }
        if (producesTabDelimitedOutput && producesVariantCallingFormatOutput) {
            errors.add("TSV and VCF are mutually exclusive. An alignment analysis plugin cannot support both.");
        }

    }


}
