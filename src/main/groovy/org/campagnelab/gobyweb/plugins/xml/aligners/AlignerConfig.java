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

import org.campagnelab.gobyweb.plugins.PluginLoaderSettings;
import org.campagnelab.gobyweb.plugins.xml.executables.*;

import javax.xml.bind.annotation.*;
import java.util.List;

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

    protected ExecutableInputSchema executableInputSchema;

    protected ExecutableOutputSchema executableOutputSchema;


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

    public OutputSchema outputSchema = new OutputSchema();

    /**
     * Gets a human readable description of the configuration type
     *
     * @return the description
     */
    @Override
    public String getHumanReadableConfigType() {
        return "ALIGNER";
    }


    public OutputSchema getOriginalOutputSchema() {
        return outputSchema;
    }
    /**
     * Validates the plugins id
     *
     * @param type
     * @param id
     * @param errors the list of errors found during the validation
     */
    @Override
    public void validateId(String type, String id, List<String> errors) {
        super.validateId(type, id, errors);
        outputSchema.validate(errors);
    }

    @Override
    public String toString() {
        return String.format("%s/%s (%s) num-rules: %d, num-needs: %d", this.getHumanReadableConfigType(), this.name, this.version, this.options.rules().size(), this.runtime.needs.size());
    }

    @Override
    protected void decorateInput(ExecutableInputSchema inputSchema) {
        List<Slot> slots = inputSchema.getInputSlots();
        Slot readsSlot = new Slot();
        readsSlot.setName("INPUT_READS");
        Slot.IOFileSetRef type = new Slot.IOFileSetRef();
        type.id = PluginLoaderSettings.COMPACT_READS[0];
        type.versionAtLeast = PluginLoaderSettings.COMPACT_READS[1];
        type.versionExactly = PluginLoaderSettings.COMPACT_READS[2];
        type.versionAtMost = PluginLoaderSettings.COMPACT_READS[3];
        type.minOccurs = Integer.toString(1);
        type.maxOccurs = Integer.toString(1);
        readsSlot.seType(type);
        slots.add(readsSlot);
    }

    @Override
    public void decorateOutput(ExecutableOutputSchema outputSchema) {
        List<Slot> slots = outputSchema.getOutputSlots();
        if (supportsGobyAlignments){
            Slot gobySlot = new Slot();
            gobySlot.setName("GOBY_ALIGNMENT");
            Slot.IOFileSetRef gobyType = new Slot.IOFileSetRef();
            gobyType.id = PluginLoaderSettings.GOBY_ALIGNMENTS[0];
            gobyType.versionAtLeast = PluginLoaderSettings.GOBY_ALIGNMENTS[1];
            gobyType.versionExactly = PluginLoaderSettings.GOBY_ALIGNMENTS[2];
            gobyType.versionAtMost = PluginLoaderSettings.GOBY_ALIGNMENTS[3];
            gobyType.minOccurs = Integer.toString(1);
            gobyType.maxOccurs = Integer.toString(1);
            gobySlot.seType(gobyType);
            slots.add(gobySlot);
        }
        if (supportsBAMAlignments) {
            Slot bamSlot = new Slot();
            bamSlot.setName("BAM_ALIGNMENT");
            Slot.IOFileSetRef bamType = new Slot.IOFileSetRef();
            bamType.id = PluginLoaderSettings.BAM_ALIGNMENTS[0];
            bamType.versionAtLeast = PluginLoaderSettings.BAM_ALIGNMENTS[1];
            bamType.versionExactly = PluginLoaderSettings.BAM_ALIGNMENTS[2];
            bamType.versionAtMost = PluginLoaderSettings.BAM_ALIGNMENTS[3];
            bamType.minOccurs = Integer.toString(1);
            bamType.maxOccurs = Integer.toString(1);
            bamSlot.seType(bamType);
            slots.add(bamSlot);
        }
        Slot countsSlot = new Slot();
        countsSlot.setName("COUNTS");
        Slot.IOFileSetRef countsType = new Slot.IOFileSetRef();
        countsType.id = PluginLoaderSettings.COUNTS[0];
        countsType.versionAtLeast = PluginLoaderSettings.COUNTS[1];
        countsType.versionExactly = PluginLoaderSettings.COUNTS[2];
        countsType.versionAtMost = PluginLoaderSettings.COUNTS[3];
        countsType.minOccurs = Integer.toString(1);
        countsType.maxOccurs = Integer.toString(1);
        countsSlot.seType(countsType);
        slots.add(countsSlot);

        Slot tsvSlot = new Slot();
        tsvSlot.setName("TSV");
        Slot.IOFileSetRef tsvType = new Slot.IOFileSetRef();
        tsvType.id = PluginLoaderSettings.TSV[0];
        tsvType.versionAtLeast = PluginLoaderSettings.TSV[1];
        tsvType.versionExactly = PluginLoaderSettings.TSV[2];
        tsvType.versionAtMost = PluginLoaderSettings.TSV[3];
        tsvType.minOccurs = Integer.toString(1);
        tsvType.maxOccurs = Integer.toString(1);
        tsvSlot.seType(tsvType);
        slots.add(tsvSlot);

        Slot gzSlot = new Slot();
        gzSlot.setName("GZ");
        Slot.IOFileSetRef gzType = new Slot.IOFileSetRef();
        gzType.id = PluginLoaderSettings.GZ[0];
        gzType.versionAtLeast = PluginLoaderSettings.GZ[1];
        gzType.versionExactly = PluginLoaderSettings.GZ[2];
        gzType.versionAtMost = PluginLoaderSettings.GZ[3];
        gzType.minOccurs = Integer.toString(2);
        gzType.maxOccurs = Integer.toString(2);
        gzSlot.seType(gzType);
        slots.add(gzSlot);

        Slot statsSlot = new Slot();
        statsSlot.setName("STATS");
        Slot.IOFileSetRef statsType = new Slot.IOFileSetRef();
        statsType.id = PluginLoaderSettings.STATS[0];
        statsType.versionAtLeast = PluginLoaderSettings.STATS[1];
        statsType.versionExactly = PluginLoaderSettings.STATS[2];
        statsType.versionAtMost = PluginLoaderSettings.STATS[3];
        statsType.minOccurs = Integer.toString(0);
        statsType.maxOccurs = Integer.toString(1);
        statsSlot.seType(statsType);
        slots.add(statsSlot);
    }
}
