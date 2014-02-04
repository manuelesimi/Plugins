package org.campagnelab.gobyweb.clustergateway.jobs;

import com.google.common.base.Joiner;
import org.campagnelab.gobyweb.clustergateway.datamodel.Alignment;
import org.campagnelab.gobyweb.clustergateway.datamodel.DiffExp;
import org.campagnelab.gobyweb.clustergateway.datamodel.Reads;
import org.campagnelab.gobyweb.filesets.FileSetAPI;
import org.campagnelab.gobyweb.io.AreaFactory;
import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.io.JobArea;
import org.campagnelab.gobyweb.plugins.PluginLoaderSettings;
import org.campagnelab.gobyweb.plugins.xml.alignmentanalyses.AlignmentAnalysisConfig;
import org.campagnelab.gobyweb.plugins.xml.executables.OutputFile;
import org.campagnelab.gobyweb.plugins.xml.executables.Slot;

import java.io.IOException;
import java.util.*;

/**
 * Builder for analysis jobs
 *
 * @author manuele
 */
public class AlignmentAnalysisJobBuilder extends JobBuilder {

    private final AlignmentAnalysisConfig analysisConfig;
    private final FileSetArea fileSetArea;
    private List<String> inputAlignmentTags = new ArrayList<String>();
    private List<String> inputReadsTags = new ArrayList<String>();
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
        this.parseInputSlots(inputSlots);
    }

    /**
     * Parses and validates the input slot values.
     * @param inputSlots
     * @throws IOException
     */
    private void parseInputSlots(Set<InputSlotValue> inputSlots) throws IOException {
        for (InputSlotValue slotValue : inputSlots) {
            if (slotValue.getName().equalsIgnoreCase("INPUT_ALIGNMENTS"))
                this.inputAlignmentTags = slotValue.getValues();

            if (slotValue.getName().equalsIgnoreCase("ALIGNMENT_SOURCE_READS"))
                this.inputReadsTags = slotValue.getValues();

        }
        //validate the tags
        assert this.inputAlignmentTags.size() != 0 : "No input alignments found, unable to build the analysis job";

        if (this.inputReadsTags.size() > 0) {
            if (this.inputAlignmentTags.size() != this.inputReadsTags.size()) {
                throw new IOException(String.format("the cardinality of the input alignments (%d) does not match the cardinality of the input reads (%d)",
                        this.inputAlignmentTags.size(), this.inputReadsTags.size()));
            }
        }
    }

    /**
     * Generates the statements that copy each file produced by a plugin to the final JOB_DIR/results/TAG-filename location.
     * @return bash statements.
     */
    private String generatePluginOutputCopyStatements() {
        StringBuilder command = new StringBuilder();
        for (OutputFile file : this.analysisConfig.output.files) {
            command.append(String.format("if [ -f %s ]; then\n",file.filename));
            command.append(String.format("/bin/mv %s ${RESULT_DIR}/${TAG}-%s ; \n",file.filename,file.filename));
            command.append("fi\n");
        }
        return command.toString();
    }
    /**
     * Generates the statements that push each file produced by a plugin to the FileSet Area.
     * @return bash statements.
     */
    private String generatePluginOutputPushStatements() {
        StringBuilder command = new StringBuilder();
        for (OutputFile file : this.analysisConfig.output.files) {
            String attributes = "";
            if ((file.tableName != null) && (file.tableName.length() > 0) )
                attributes = String.format("-a TABLENAME=%s",file.tableName);
            command.append(String.format("push_analysis_results %s %s %s \"%s\"\n", file.filename, file.id,file.required?"true":"false",attributes));
        }
        return command.toString();
    }

   /**
    * Detects the entries extension of the alignments to fetch from the fileset area.
    * @throws IOException
    */
    private String detectEntriesExt() throws IOException {
        assert (!(this.analysisConfig.supportsGobyAlignments && this.analysisConfig.supportsBAMAlignments))
                : "supportsGobyAlignments and supportsBAMAlignments cannot be both true";
        assert (this.analysisConfig.supportsGobyAlignments || this.analysisConfig.supportsBAMAlignments)
                : "supportsGobyAlignments and supportsBAMAlignments cannot be both false";
        if (this.analysisConfig.supportsBAMAlignments)
            return ".bam";
        if (this.analysisConfig.supportsGobyAlignments)
            return ".entries";
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

    /**
     * Customizes the alignment analysis job.
     * @param executableJob
     * @param commandLineOptions
     * @throws IOException
     */
    @Override
    protected void customizeJob(ExecutableJob executableJob, final Map<String, String> commandLineOptions) throws IOException {

        // the data object to pass to plugin scripts, if any
        DiffExp diffExp = new DiffExp();
        diffExp.setOptions(commandLineOptions);
        JobRuntimeEnvironment environment = executableJob.getEnvironment();

        environment.put("PLUGIN_ID", analysisConfig.getId());
        environment.put("PLUGIN_VERSION", analysisConfig.getVersion());
        environment.put("DIFF_EXP_TYPE", analysisConfig.getId());
        environment.put("PRODUCE_TAB_DELIMITED_OUTPUT", analysisConfig.producesTabDelimitedOutput);
        environment.put("PRODUCE_VARIANT_CALLING_FORMAT_OUTPUT", analysisConfig.producesVariantCallingFormatOutput);
        environment.put("INITIAL_STATE", "diffexp");
        environment.put("ENTRIES_DIRECTORY", "${JOB_DIR}/alignments");
        environment.put("ENTRIES_FILES",String.format("${ENTRIES_DIRECTORY}/*%s", this.detectEntriesExt()));
        environment.put("ALIGNMENT_FILES", "${ENTRIES_DIRECTORY}/*");
        environment.put("SUPPORTS_TRANSCRIPT_ALIGNMENTS", analysisConfig.supportsTranscriptAlignments);
        environment.put("SPLIT_PROCESS_COMBINE", analysisConfig.splitProcessCombine);
        environment.put("RESULT_FILE_EXTENSION", analysisConfig.producesTabDelimitedOutput ?
                "tsv" : analysisConfig.producesVariantCallingFormatOutput ? "vcf.gz" : "unknown");
        environment.put("PUSH_PLUGIN_OUTPUT_FILES", this.generatePluginOutputPushStatements());
        environment.put("COPY_PLUGIN_OUTPUT_FILES", this.generatePluginOutputCopyStatements());
        FileSetAPI api = FileSetAPI.getReadOnlyAPI(fileSetArea);
        List<String> errors = new ArrayList<String>();
        //map an alignment tag with the alignment basename
        Map<String,Alignment> alignmentMap = new HashMap<String, Alignment>();
        environment.put("ENTRIES_EXT",this.detectEntriesExt());
        for (String inputTag : this.inputAlignmentTags) {
            Alignment alignment = new Alignment(inputTag);
            Map<String, String> storedAttributes = api.fetchAttributes(inputTag, errors);
            if (this.inputReadsTags.size() > 0) {
                if (! this.inputReadsTags.contains(storedAttributes.get("SOURCE_READS_ID"))) {
                   throw new IOException(String.format("Input alignment %s was created from a reads file %s not indicated in the INPUT_READS slot ",
                           inputTag, storedAttributes.get("SOURCE_READS_ID")));
                }
            }
            Reads reads = new Reads(storedAttributes.get("SOURCE_READS_ID"));
            Map<String, String> readsAttributes = api.fetchAttributes(reads.getTag(), errors);
            reads.setAttributes(readsAttributes);
            reads.setBasename(readsAttributes.get("BASENAME"));
            alignment.setReads(reads);
            alignment.setAttributes(storedAttributes);
            if (errors.size() >0)
                throw new IOException(String.format("Failed to fetch attributes for tag %s: %s", inputTag,errors.get(0)));
            //check if all the input alignments refer to the genome
            if (environment.containsKey("GENOME_REFERENCE_ID") &&
                    (!(environment.getFromUndecorated("GENOME_REFERENCE_ID").equals(storedAttributes.get("GENOME_REFERENCE_ID")))))
                throw new IOException(String.format("Unable to analyse alignments with different GENOME_REFERENCE_ID (%s and %s)",
                        environment.getFromUndecorated("GENOME_REFERENCE_ID"),  storedAttributes.get("GENOME_REFERENCE_ID")));
            else {
                environment.put("GENOME_REFERENCE_ID", storedAttributes.get("GENOME_REFERENCE_ID"));
                diffExp.setReferenceId(storedAttributes.get("GENOME_REFERENCE_ID"));
            }
            //check if all the input alignments refer to the same organism
            if (environment.containsKey("ORGANISM") && (!(environment.getFromUndecorated("ORGANISM").equals(storedAttributes.get("ORGANISM")))))
                throw new IOException(String.format("Unable to analyse alignments with different ORGANISM (%s and %s)",
                        environment.getFromUndecorated("ORGANISM"),  storedAttributes.get("ORGANISM")));
            else {
                environment.put("ORGANISM", storedAttributes.get("ORGANISM"));
                diffExp.setOrganismId( storedAttributes.get("ORGANISM"));
            }
            alignment.setBasename(storedAttributes.get("BASENAME"));
            alignment.setAlignJobTag(inputTag);
            alignmentMap.put(inputTag,alignment);
            errors.clear();
        }

        //for each group, we create a filter for querying only slots belonging to the group and add the alignments to the group
        int groupNumber=0;
        Set<String> groupNames = new HashSet<String>();
        for (String groupDefinition : this.groupDefinitions) {
            groupNumber++;
            String[] tokens = groupDefinition.split("=");
            String[] tags = tokens[1].split(",");
            StringBuilder groupFilters = new StringBuilder("--filter-attribute BASENAME=");
            int i=0;
            for (String tag: tags) {
                groupFilters.append(String.format("%s,", alignmentMap.get(tag).getBasename()));
                diffExp.addAlignmentToGroup(groupNumber, i++, alignmentMap.get(tag));
            }
            //remove the last comma
            groupFilters.setLength(groupFilters.length() - 1);
            environment.put(String.format("PLUGIN_GROUP_ALIGNMENTS_FILTER_%s", tokens[0]), groupFilters.toString());
            diffExp.addGroup(groupNumber,tokens[0]);
            groupNames.add(tokens[0]);
        }

        //create a joined group definition in the form "Group_1=TAGN/Group_2=TAGX/Group_3=TAG342,TAG231"
        String joinedGroups= Joiner.on("/").join(this.groupDefinitions);
        //replace tags with their basename in the group definitions
        for (Map.Entry<String, Alignment> entry : alignmentMap.entrySet())
            joinedGroups=joinedGroups.replaceAll(entry.getKey(), entry.getValue().getBasename());
        //now the joined group definition is in the form "Group_1=basenameN/Group_2=basenameX/Group_3=basename342,basename231"
        environment.put("GROUPS_DEFINITION", joinedGroups);

        //check comparison pairs
        for (int i=1; i <= comparisonPairs.size(); i++) {
            String[] tokens = comparisonPairs.get(i-1).split("/");
            if ((groupNames.contains(tokens[0]) && groupNames.contains(tokens[1])))
            //comparison pair must be in the form "Group_2/Group_3"
                 environment.put(String.format("GROUP%d_COMPARISON_PAIR",i), comparisonPairs.get(i-1));
            else
                throw new IOException("Invalid group name specified in the comparison pair " +comparisonPairs.get(i-1));
        }
        environment.put("NUM_GROUPS",this.groupDefinitions.size());
        environment.put("COMPARE_DEFINITION", Joiner.on(",").join(this.comparisonPairs));
        executableJob.setDataForScripts(diffExp);
    }

    @Override
    protected String getVariablePrefix() {
        return String.format("PLUGINS_ALIGNMENT_ANALYSIS_%s_",this.analysisConfig.getId());
    }
}
