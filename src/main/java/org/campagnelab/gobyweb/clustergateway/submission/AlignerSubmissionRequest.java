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
        FlaggedOption genome = new FlaggedOption("genome-reference-id")
                .setStringParser(JSAP.STRING_PARSER)
                .setRequired(true)
                .setShortFlag(JSAP.NO_SHORTFLAG)
                .setLongFlag("genome-reference-id");
        genome.setHelp("The reference genome.");
        parameters.add(genome);

        FlaggedOption chunk_size = new FlaggedOption("chunk-size")
                .setStringParser(JSAP.INTEGER_PARSER)
                .setRequired(true)
                .setShortFlag(JSAP.NO_SHORTFLAG)
                .setLongFlag("chunk-size");
        chunk_size.setHelp("The size of each chunk.");
        parameters.add(chunk_size);

        FlaggedOption parts = new FlaggedOption("number-of-align-parts")
                .setStringParser(JSAP.INTEGER_PARSER)
                .setRequired(true)
                .setShortFlag(JSAP.NO_SHORTFLAG)
                .setLongFlag("number-of-align-parts");
        parts.setHelp("The number of parts in which the job will be splitted.");
        parameters.add(parts);

        //TODO: check, read and validate options from aligner config

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
                config.getInt("chunk-size"),
                config.getInt("number-of-align-parts"),
                this.getUnclassifiedOptions());
       return 0;
    }


}
