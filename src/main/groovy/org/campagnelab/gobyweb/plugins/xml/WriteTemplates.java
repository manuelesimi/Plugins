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

import org.campagnelab.gobyweb.plugins.xml.aligners.AlignerConfig;
import org.campagnelab.gobyweb.plugins.xml.alignmentanalyses.AlignmentAnalysisConfig;
import org.campagnelab.gobyweb.plugins.xml.common.PluginFile;
import org.campagnelab.gobyweb.plugins.xml.executables.*;
import static org.campagnelab.gobyweb.plugins.PluginLoaderSettings.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;

/**
 * @author campagne
 *         Date: 10/7/11
 *         Time: 11:58 AM
 */
public class WriteTemplates {
    public static void main(String args[]) {

        try {
            Class<?>[] classes = new Class<?>[CONFIGS_TO_CLASSES.values().length];
            int j=0;
            for (CONFIGS_TO_CLASSES value : CONFIGS_TO_CLASSES.values())  {
                classes[j++] = value.register();
            }
            final JAXBContext jc = JAXBContext.newInstance(classes);

            {
                int i;
                final Marshaller m = jc.createMarshaller();
                AlignerConfig config = new AlignerConfig();
                config.id = "aligner_1";
                config.name = "aligner 1 goby output";
                config.help = "Some help text.";
                config.supportsBAMAlignments = false;
                config.supportsColorSpace = true;
                config.supportsFastaReads = true;
                config.supportsFastqReads = true;
                config.supportsGobyReads = true;
                config.supportsGobyAlignments = true;
                config.supportsBisulfiteConvertedReads = false;

                Option option = new Option();
                option.autoFormat = true;
                option.required=true;
                option.defaultsTo="DEFAULT";
                option.flagFormat = " --align-type=%s "; //GSNAP type format
                option.type = Option.OptionType.CATEGORY;
                option.categories.add(new Category("cmet", "bisulfite alignment", "Instructs GSNAP to align bisulfite converted reads."));
                option.categories.add(new Category("id", "name", "hep."));
                option.help = "Help text";
                option.id = "align_type";
                option.name = "Alignment type";

                config.options.items().add(option);
                config.options.add(new ValidationRule("value(OPTION_ID)==true","OPTION_ID must be true"));
                option = new Option();
                option.autoFormat = true;
                option.flagFormat = " --flag ";
                option.type = Option.OptionType.BOOLEAN;

                option.help = "Help text";
                option.id = "some_flag";
                option.name = "Some flag";

                config.options.items().add(option);
                PluginFile pluginFile = new PluginFile();
                pluginFile.id = "pf1";
                pluginFile.filename = "somerthing.R";
                config.files.add(pluginFile);
                StringWriter writer = new StringWriter();
                m.marshal(config, writer);
                System.out.println("alignConfig: " + writer.getBuffer().toString());
            }
            {
                final Marshaller m = jc.createMarshaller();
                StringWriter writer = new StringWriter();

                AlignmentAnalysisConfig aaConfig = new AlignmentAnalysisConfig();
                aaConfig.id = "analysis_1";
                aaConfig.name = "analysis 1 ";
                aaConfig.help = "Some help text.";
                OutputFile outputFile = new OutputFile();
                outputFile.id = "ID";
                outputFile.mimeType = "plain/tsv";
                outputFile.name = "statistics";
                outputFile.help = "help";
                outputFile.filename = "image.png";
                aaConfig.output.files.add(outputFile);
                OutputFile outputFile2 = new OutputFile();
                outputFile2.id = "ID";
                outputFile2.mimeType = "plain/tsv";
                outputFile2.help = "help";
                outputFile2.name = "statistics";
                outputFile2.filename = "image.png";
                aaConfig.output.files.add(outputFile);

                m.marshal(aaConfig, writer);
                System.out.println("AlignmentAnalysisConfig: " + writer.getBuffer().toString());

            }
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

}
