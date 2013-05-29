package org.campagnelab.gobyweb.clustergateway.jobs;

import org.campagnelab.gobyweb.io.AreaFactory;
import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.io.JobArea;
import org.campagnelab.gobyweb.plugins.xml.alignmentanalyses.AlignmentAnalysisConfig;

import java.io.IOException;
import java.util.Set;

/**
 * Builder for analysis jobs
 *
 * @author manuele
 */
public class AnalysisJobBuilder extends JobBuilder {

    private final AlignmentAnalysisConfig analysisConfig;
    private final FileSetArea fileSetArea;
    private final String inputReadsTag;

    /**
     * Creates an analysis alignment job builder.
     * @param analysisConfig the source analysis configuration
     * @param jobArea the job area where the job will be submitted
     * @param filesetAreaReference the fileset area from which reads metadata are fetched
     * @param owner the owner of the job
     * @param inputSlots the input slots passed on the command line
     * @throws IOException
     */
    public AnalysisJobBuilder(AlignmentAnalysisConfig analysisConfig, JobArea jobArea, String filesetAreaReference,
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
        InputSlotValue inputReads = inputSlots.iterator().next();
        this.inputReadsTag = inputReads.getValues().get(0);

    }

    @Override
    protected void customizeJob(ExecutableJob executableJob) throws IOException {

    }
}
