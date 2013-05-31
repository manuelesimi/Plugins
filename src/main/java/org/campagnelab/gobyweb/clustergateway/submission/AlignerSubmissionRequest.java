package org.campagnelab.gobyweb.clustergateway.submission;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import org.campagnelab.gobyweb.plugins.xml.aligners.AlignerConfig;

import java.util.ArrayList;
import java.util.List;


/**
 * Prepare a job request for an aligner.
 *
 * @author manuele
 */
class AlignerSubmissionRequest extends SubmissionRequest {

    AlignerConfig alignerConfig;

    protected AlignerSubmissionRequest(AlignerConfig alignerConfig) {
       this.alignerConfig = alignerConfig;
    }

    @Override
    protected List<Parameter> getAdditionalParameters() {
        List<Parameter> parameters = new ArrayList<Parameter>();

        //add mandatory parameters common to all aligners
        FlaggedOption genome = new FlaggedOption("genome-reference-id")
                .setStringParser(JSAP.STRING_PARSER)
                .setRequired(true)
                .setShortFlag(JSAP.NO_SHORTFLAG)
                .setLongFlag("genome-reference-id");
        genome.setHelp("The reference genome.");
        parameters.add(genome);

        FlaggedOption chunk_size = new FlaggedOption("chunk-size")
                .setStringParser(JSAP.STRING_PARSER)
                .setRequired(true)
                .setShortFlag(JSAP.NO_SHORTFLAG)
                .setLongFlag("chunk-size");
        chunk_size.setHelp("The size of each chunk.");
        parameters.add(chunk_size);

        FlaggedOption parts = new FlaggedOption("number-of-align-parts")
                .setStringParser(JSAP.STRING_PARSER)
                .setRequired(true)
                .setShortFlag(JSAP.NO_SHORTFLAG)
                .setLongFlag("number-of-align-parts");
        parts.setHelp("The number of parts in which the job will be splitted.");
        parameters.add(parts);

        //add parameters from aligner configuration
        for (org.campagnelab.gobyweb.plugins.xml.executables.Option option : alignerConfig.getOptions().option){
            FlaggedOption jsapOption = new FlaggedOption(option.id)
                    .setStringParser(JSAP.STRING_PARSER)
                    .setRequired(option.required)
                    .setShortFlag(JSAP.NO_SHORTFLAG)
                    .setLongFlag(option.id)
                    .setDefault(option.defaultsTo);
            jsapOption.setHelp(option.help);
            parameters.add(jsapOption);
        }

        return parameters;
    }

    /**
     * Submits the aligner request.
     * @param config the parsed interface configuration
     * @param actions
     * @return  0 if the aligner was successfully submitted, anything else if failed
     * @throws Exception
     */
    protected int submit(JSAPResult config, Actions actions) throws Exception {

        actions.submitAligner(alignerConfig.getId(),
                this.getInputSlots(),
                config.getString("genome-reference-id"),
                Integer.valueOf(config.getString("chunk-size")),
                Integer.valueOf(config.getString("number-of-align-parts")),
                this.getUnclassifiedOptions());
       return 0;
    }


}
