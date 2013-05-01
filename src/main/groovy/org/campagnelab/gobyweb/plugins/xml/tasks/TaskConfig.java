package org.campagnelab.gobyweb.plugins.xml.tasks;

import org.campagnelab.gobyweb.plugins.DependencyResolver;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableInputSchema;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableOutputSchema;
import org.campagnelab.gobyweb.plugins.xml.executables.Slot;

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
    protected ExecutableInputSchema inputSchema;

    @XmlElement(name = "outputSchema")
    protected ExecutableOutputSchema outputSchema;


    public TaskConfig() {}

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

    public void setInputSchema(ExecutableInputSchema inputSchema)  {
        this.inputSchema = inputSchema;
    }

    public void setOutputSchema(ExecutableOutputSchema outputSchema)  {
        this.outputSchema = outputSchema;
    }

    @Override
    public ExecutableInputSchema getInputSchema() {
        return this.inputSchema;
    }

    @Override
    public ExecutableOutputSchema getOutputSchema() {
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
        for (Slot parameter : inputSchema.getInputSlots())
                validateFileSetReference(parameter.geType(),errors);

        //check if output fileset references are correct
        for (Slot parameter : outputSchema.getOutputSlots())
            validateFileSetReference(parameter.geType(),errors);


    }

    /**
     * Validates if the fileset exists
     * @param fileSetRef
     * @param errors
     * @return
     */
    private boolean validateFileSetReference(Slot.IOFileSetRef fileSetRef, List<String> errors) {
        if (DependencyResolver.resolveFileSet(fileSetRef.id,fileSetRef.versionAtLeast,
                fileSetRef.versionExactly,fileSetRef.versionAtMost) == null) {
            errors.add(String.format("Unable to resolve dependency for input fileset %s: failed to find a version matching the input criteria {versionExactly=%s,versionAtLeast=%s,versionAtMost=%s}",
                    fileSetRef.id, fileSetRef.versionExactly, fileSetRef.versionAtLeast, fileSetRef.versionAtMost));
            return false;
        }
        return true;
    }
}
