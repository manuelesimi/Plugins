package org.campagnelab.gobyweb.plugins.xml.tasks;

import org.campagnelab.gobyweb.plugins.DependencyResolver;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Describes a task that can be submitted on the cluster.
 * @author manuele
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class TaskConfig extends ExecutableConfig {

    @XmlElement(name = "inputSchema")
    protected TaskInputSchema inputSchema;

    @XmlElement(name = "outputSchema")
    protected TaskOutputSchema outputSchema;


    protected TaskConfig() {}

    protected TaskConfig(String id) { this.setId(id);}




    @Override
    public String toString() {
        return String.format("%s/%s (%s)",this.getHumanReadableConfigType(), this.name, this.version);
    }

    /**
     * Gets a human readable description of the configuration type
     *
     * @return the description
     */
    @Override
    public String getHumanReadableConfigType() {
        return "TASK";
    }

    public void setInputSchema(TaskInputSchema inputSchema)  {
        this.inputSchema = inputSchema;
    }

    public void setOutputSchema(TaskOutputSchema outputSchema)  {
        this.outputSchema = outputSchema;
    }

    public TaskInputSchema getInputSchema() {
        return this.inputSchema;
    }

    public TaskOutputSchema getOutputSchema() {
        return  this.outputSchema;
    }
    /**
     * Validates the configuration. Call this method after unmarshalling a config to check that the configuration
     * is semantically valid. Returns null when no errors are found in the configuration, or a list of errors encountered.
     * This method also sets userDefinedValue for required options.
     *
     * @return list of error messages, or null when no errors are detected.
     */
    @Override
    public void validate(List<String> errors) {
        super.validate(errors);
        //check if input fileset references are correct
        for (TaskIO parameter : inputSchema.parameters) {
            for (TaskIO.IOFileSetRef fileSetRef : parameter.fileSetRefs)
                validateFileSetReference(fileSetRef,errors);
        }

        //check if output fileset references are correct
        for (TaskIO parameter : outputSchema.returnedValues) {
            if (parameter.fileSetRefs.size() > 1) {
                errors.add(String.format("Invalid returned value %s defined in the output schema: there must be only one fileset reference in the returned values",
                    parameter.name));
            }
            validateFileSetReference(parameter.fileSetRefs.get(0),errors);
        }

    }

    /**
     * Validates if the fileset exists
     * @param fileSetRef
     * @param errors
     * @return
     */
    private boolean validateFileSetReference(TaskIO.IOFileSetRef fileSetRef, List<String> errors) {
        if (DependencyResolver.resolveFileSet(fileSetRef.id,fileSetRef.versionAtLeast,
                fileSetRef.versionExactly,fileSetRef.versionAtMost) == null) {
            errors.add(String.format("Unable to resolve dependency for input fileset %s: failed to find a version matching the input criteria {versionExactly=%s,versionAtLeast=%s,versionAtMost=%s}",
                    fileSetRef.id, fileSetRef.versionExactly, fileSetRef.versionAtLeast, fileSetRef.versionAtMost));
            return false;
        }
        return true;
    }
}
