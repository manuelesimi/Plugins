package org.campagnelab.gobyweb.clustergateway.submission;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import org.campagnelab.gobyweb.io.CommandLineHelper;
import org.campagnelab.gobyweb.plugins.xml.aligners.AlignerConfig;

import java.util.ArrayList;
import java.util.List;


/**
 * Prepare a job request for an aligner.
 *
 * @author manuele
 */
class AlignerSubmissionRequest extends SubmissionRequest {

    AlignerConfig config;

    private static CommandLineHelper jsapHelper = new CommandLineHelper(ClusterGateway.class) {

    };

    protected AlignerSubmissionRequest(AlignerConfig config) {
       this.config = config;
    }

    protected int submitRequest(String[] args, Actions actions) throws Exception {

        List<Parameter> parameters = new ArrayList<Parameter>();
        FlaggedOption genome = new FlaggedOption("genome-reference-id")
                .setStringParser(JSAP.STRING_PARSER)
                .setRequired(true)
                .setShortFlag(JSAP.NO_SHORTFLAG)
                .setLongFlag("genome-reference-id");
        parameters.add(genome);

        FlaggedOption chunk_size = new FlaggedOption("chunk-size")
                .setStringParser(JSAP.INTEGER_PARSER)
                .setRequired(true)
                .setShortFlag(JSAP.NO_SHORTFLAG)
                .setLongFlag("chunk-size");
        parameters.add(chunk_size);

        FlaggedOption parts = new FlaggedOption("number-of-align-parts")
                .setStringParser(JSAP.INTEGER_PARSER)
                .setRequired(true)
                .setShortFlag(JSAP.NO_SHORTFLAG)
                .setLongFlag("number-of-align-parts");
        parameters.add(parts);

        //TODO: check, read and validate options from aligner config

        JSAPResult JSAPconfig = jsapHelper.configure(args, parameters);
        if (JSAPconfig == null) return 1;


        actions.submitAligner(config.getId(),
                this.getInputSlots(),
                JSAPconfig.getString("genome-reference-id"),
                JSAPconfig.getInt("chunk-size"),
                JSAPconfig.getInt("number-of-align-parts"),
                this.getUnclassifiedOptions()
        );
       return 0;
    }


}
