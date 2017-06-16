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
                .setDefault("50000000")
                .setShortFlag(JSAP.NO_SHORTFLAG)
                .setLongFlag("CHUNK_SIZE");
        chunk_size.setHelp("The number of bytes of compressed reads file to give to a single align part.");
        parameters.add(chunk_size);

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
        int memory = config.userSpecified("container_memory")? config.getInt("container_memory"):0;

        actions.submitAligner(alignerConfig,
                this.getInputSlots(),
                config.getString("GENOME_REFERENCE_ID"),
                Long.valueOf(config.getString("CHUNK_SIZE")),
                this.getUnclassifiedOptions(), memory);
       return 0;
    }

}
