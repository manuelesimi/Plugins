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

    private final AlignerConfig alignerConfig;

    protected AlignerSubmissionRequest(AlignerConfig alignerConfig) {
       this.alignerConfig = alignerConfig;
       this.executableConfig = alignerConfig;
    }

    @Override
    protected List<Parameter> getAdditionalParameters() {
        List<Parameter> parameters = new ArrayList<Parameter>();

        //add mandatory parameters common to all aligners
        FlaggedOption genome = new FlaggedOption("GENOME_REFERENCE_ID")
                .setStringParser(JSAP.STRING_PARSER)
                .setRequired(true)
                .setShortFlag(JSAP.NO_SHORTFLAG)
                .setLongFlag("GENOME_REFERENCE_ID");
        genome.setHelp("The reference genome.");
        parameters.add(genome);

        FlaggedOption chunk_size = new FlaggedOption("CHUNK_SIZE")
                .setStringParser(JSAP.STRING_PARSER)
                .setRequired(true)
                .setShortFlag(JSAP.NO_SHORTFLAG)
                .setLongFlag("CHUNK_SIZE");
        chunk_size.setHelp("The size of each chunk.");
        parameters.add(chunk_size);

        FlaggedOption parts = new FlaggedOption("NUMBER_OF_ALIGN_PARTS")
                .setStringParser(JSAP.STRING_PARSER)
                .setRequired(true)
                .setShortFlag(JSAP.NO_SHORTFLAG)
                .setLongFlag("NUMBER_OF_ALIGN_PARTS");
        parts.setHelp("The number of parts in which the job will be split.");
        parameters.add(parts);

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
        if (alignerConfig.isDisabled())
            throw new Exception(String.format("Aligner %s is currently disabled", alignerConfig.getId()));
        actions.submitAligner(alignerConfig,
                this.getInputSlots(),
                config.getString("GENOME_REFERENCE_ID"),
                Integer.valueOf(config.getString("CHUNK_SIZE")),
                Integer.valueOf(config.getString("NUMBER_OF_ALIGN_PARTS")),
                this.getUnclassifiedOptions());
       return 0;
    }

}
