package org.campagnelab.gobyweb.clustergateway.submission;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import org.campagnelab.gobyweb.plugins.xml.alignmentanalyses.AlignmentAnalysisConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Prepare a job request for an alignment analysis job.
 *
 * @author manuele
 */
class AlignmentAnalysisSubmissionRequest extends SubmissionRequest {

    private final AlignmentAnalysisConfig alignmentAnalysisConfig;

    AlignmentAnalysisSubmissionRequest(AlignmentAnalysisConfig alignmentAnalysisConfig) {
        this.alignmentAnalysisConfig = alignmentAnalysisConfig;
        this.executableConfig = alignmentAnalysisConfig;
    }

    @Override
    protected List<Parameter> getAdditionalParameters() {
        List<Parameter> parameters = new ArrayList<Parameter>();
        FlaggedOption groups = new FlaggedOption("GROUP_DEFINITION")
                .setStringParser(JSAP.STRING_PARSER)
                .setRequired(true)
                .setAllowMultipleDeclarations(true)
                .setShortFlag(JSAP.NO_SHORTFLAG)
                .setLongFlag("GROUP_DEFINITION");
        groups.setHelp("The group definition list. Each definition must be in the form: Group_N=TAG,TAG342,TAG231,etc. TAGs must match the ones declared in the SLOTS");
        parameters.add(groups);

        FlaggedOption pairs = new FlaggedOption("COMPARISON_PAIR")
                .setStringParser(JSAP.STRING_PARSER)
                .setRequired(true)
                .setAllowMultipleDeclarations(true)
                .setShortFlag(JSAP.NO_SHORTFLAG)
                .setLongFlag("COMPARISON_PAIR");
        pairs.setHelp("The comparison pair list. Each pair must be in the form Group_Name1/Group_Name2. Group names must match the ones declared in the GROUP_DEFINITION");
        parameters.add(pairs);

        return parameters;
    }
    @Override
    protected int submit(JSAPResult config, Actions actions) throws Exception {
        if (alignmentAnalysisConfig.isDisabled())
            throw new Exception(String.format("Alignment analysis %s is currently disabled", alignmentAnalysisConfig.getId()));
        actions.submitAnalysis(alignmentAnalysisConfig,
                this.getInputSlots(),
                config.getStringArray("GROUP_DEFINITION"),
                config.getStringArray("COMPARISON_PAIR"),
                this.getUnclassifiedOptions());
        return 0;
    }
}
