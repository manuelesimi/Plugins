package org.campagnelab.gobyweb.clustergateway.jobs;

import com.google.common.base.Joiner;
import org.campagnelab.gobyweb.filesets.FileSetAPI;
import org.campagnelab.gobyweb.io.AreaFactory;
import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.io.JobArea;
import org.campagnelab.gobyweb.plugins.xml.alignmentanalyses.AlignmentAnalysisConfig;
import org.campagnelab.gobyweb.plugins.xml.executables.OutputFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builder for analysis jobs
 *
 * @author manuele
 */
public class AlignmentAnalysisJobBuilder extends JobBuilder {

    private final AlignmentAnalysisConfig analysisConfig;
    private final FileSetArea fileSetArea;
    private final List<String> inputAlignmentTags;
    private String genomeID;
    private final String[] attributesFromAlignmentsMetadata = new String[]{
            "ORGANISM", "GENOME_REFERENCE_ID", "BASENAME"
    };
    private List<String> groupDefinitions;
    private List<String> comparisonPairs;

    /**
     * Creates an analysis alignment job builder.
     * @param analysisConfig the source analysis configuration
     * @param jobArea the job area where the job will be submitted
     * @param filesetAreaReference the fileset area from which alignment metadata are fetched
     * @param owner the owner of the job
     * @param inputSlots the input slots passed on the command line
     * @throws IOException
     */
    public AlignmentAnalysisJobBuilder(AlignmentAnalysisConfig analysisConfig, JobArea jobArea, String filesetAreaReference,
                                       String owner, Set<InputSlotValue> inputSlots) throws IOException {
        super(analysisConfig);
        this.analysisConfig = analysisConfig;
        // create the fileset area according to the location of the job area
        if (jobArea.isLocal()) {
            //we can use the reference name as it is because we have the same visibility
            this.fileSetArea = AreaFactory.createFileSetArea(filesetAreaReference, owner);
        } else {
            if (filesetAreaReference.startsWith("/")) {
                //the fileset area is local to the job area
                String remoteReferenceName = String.format("%s@%s:%s", jobArea.getUserName(), jobArea.getHostName(), filesetAreaReference);
                this.fileSetArea = AreaFactory.createFileSetArea(remoteReferenceName, owner);
            } else {
                //the fileset area must be remote also for the job area
                this.fileSetArea = AreaFactory.createFileSetArea(filesetAreaReference, owner);
            }
        }
        //input slots are validated elsewhere, we do not need to do it here
        InputSlotValue inputAlignments = inputSlots.iterator().next();
        this.inputAlignmentTags = inputAlignments.getValues();

    }

    /**
     * Generates the statements that copy each file produced by a plugin to the final JOB_DIR/results/TAG-filename location.
     * @return bash statements.
     */
    private String generatePluginOutputCopyStatements() {
        StringBuilder command = new StringBuilder();
        for (OutputFile file : this.analysisConfig.outputSchema.files) {
           command.append(String.format("if [ -f %s ]; then\n",file.filename));
           command.append(String.format("/bin/mv %s ${RESULT_DIR}/${TAG}-%s ; \n",
                   file.filename,file.filename));
           command.append("fi\n");
        }
        return command.toString();
    }

    private void populateJobEnvironment(JobRuntimeEnvironment environment)
            throws IOException {
        environment.put("PLUGIN_ID", analysisConfig.getId());
        environment.put("DIFF_EXP_TYPE", analysisConfig.getId());
        environment.put("INITIAL_STATE", "diffexp");
        environment.put("SUPPORTS_TRANSCRIPT_ALIGNMENTS", analysisConfig.supportsTranscriptAlignments);
        environment.put("SPLIT_PROCESS_COMBINE", analysisConfig.splitProcessCombine);
        environment.put("RESULT_FILE_EXTENSION", analysisConfig.producesTabDelimitedOutput ?
                "tsv" : analysisConfig.producesVariantCallingFormatOutput ? "vcf.gz" : "unknown");

        environment.put("COPY_PLUGIN_OUTPUT_FILES", this.generatePluginOutputCopyStatements());
        FileSetAPI api = FileSetAPI.getReadOnlyAPI(fileSetArea);
        List<String> errors = new ArrayList<String>();
        environment.put("ENTRIES_NAME", this.detectEntriesName());
        for (String inputTag : this.inputAlignmentTags) {
            Map<String, String> storedAttributes = api.fetchAttributes(inputTag, errors);
            if (errors.size() >0)
                throw new IOException(String.format("Failed to fetch attributes for tag %s: %s", inputTag,errors.get(0)));
            //check if all the input alignments refer to the genome
            if (environment.containsKey("GENOME_REFERENCE_ID") &&
                    (!(environment.getFromUndecorated("GENOME_REFERENCE_ID").equals(storedAttributes.get("GENOME_REFERENCE_ID")))))
                throw new IOException(String.format("Unable to analyse alignments with different GENOME_REFERENCE_ID (%s and %s)",
                        environment.getFromUndecorated("GENOME_REFERENCE_ID"),  storedAttributes.get("GENOME_REFERENCE_ID")));
            else
                environment.put("GENOME_REFERENCE_ID", storedAttributes.get("GENOME_REFERENCE_ID"));
            //check if all the input alignments refer to the same organism
            if (environment.containsKey("ORGANISM") && (!(environment.getFromUndecorated("ORGANISM").equals(storedAttributes.get("ORGANISM")))))
                throw new IOException(String.format("Unable to analyse alignments with different ORGANISM (%s and %s)",
                        environment.getFromUndecorated("ORGANISM"),  storedAttributes.get("ORGANISM")));
            else
                environment.put("ORGANISM", storedAttributes.get("ORGANISM"));
            errors.clear();
            //replace tags with their basename in the group definitions
            for (String definition : this.groupDefinitions)
                definition.replaceAll(inputTag,storedAttributes.get("BASENAME"));
            errors.clear();
        }

        //GROUPS_DEFINITION="Group_1=TAGN/Group_2=TAGX/Group_3=TAG342,TAG231"
        environment.put("GROUPS_DEFINITION", Joiner.on("/").join(this.groupDefinitions));
        for (int i=1; i <= comparisonPairs.size(); i++) {
            //comparison pair must be in the form "Group_2/Group_3"
            environment.put(String.format("GROUP%d_COMPARISON_PAIR",i), comparisonPairs.get(i-1));
        }
        environment.put("NUM_GROUPS",this.groupDefinitions.size());

        //TODO: ENTRIES_DIRECTORY is used by 2 plugins and they expect to have all the entries files in a single folder.
        //TODO we may need to change them in order to check if ENTRIES_DIRECTORY is available and, if not, fetch
        //TODO the entries files from the fileset area.

    }

    /**
     * Detects the entries name of the alignments to fetch from the fileset area.
     * @return the entries name
     * @throws IOException
     */
    private String detectEntriesName() throws IOException {
        assert (!(this.analysisConfig.supportsGobyAlignments && this.analysisConfig.supportsBAMAlignments))
                : "supportsGobyAlignments and supportsBAMAlignments cannot be both true";
        assert (this.analysisConfig.supportsGobyAlignments || this.analysisConfig.supportsBAMAlignments)
                : "supportsGobyAlignments and supportsBAMAlignments cannot be both false";
        if (this.analysisConfig.supportsBAMAlignments)
            return "BAM";
        if (this.analysisConfig.supportsGobyAlignments)
            return "ENTRIES";
        throw new IOException("Unable to detect the entries name for the analysis (both goby and bam are not supported");
    }

    /**
     * Sets the comparison pairs for the analysis.
     * Each pair must be in the form "Group_1/Group_2"
     * @param comparisonPairs the list of comparison pairs
     */
    public void setComparisonPairs(List<String> comparisonPairs) {
        this.comparisonPairs = comparisonPairs;
    }

    /**
     * Sets the group definitions for the analysis.
     * Each group definition must be in the form "Group_1=TAG1,TAG2,...,TAGN.
     * Tags must match the input slots
     * @param groupDefinitions
     */
    public void setGroupDefinition(List<String> groupDefinitions) {
        this.groupDefinitions = groupDefinitions;
    }

    @Override
    protected void customizeJob(ExecutableJob executableJob) throws IOException {
        this.populateJobEnvironment(executableJob.getEnvironment());
    }
}
