/*
 * Copyright (c) 2011  by Cornell University and the Cornell Research
 * Foundation, Inc.  All Rights Reserved.
 *
 * Permission to use, copy, modify and distribute any part of GobyWeb web
 * application for next-generation sequencing data alignment and analysis,
 * officially docketed at Cornell as D-5061 ("WORK") and its associated
 * copyrights for educational, research and non-profit purposes, without
 * fee, and without a written agreement is hereby granted, provided that
 * the above copyright notice, this paragraph and the following three
 * paragraphs appear in all copies.
 *
 * Those desiring to incorporate WORK into commercial products or use WORK
 * and its associated copyrights for commercial purposes should contact the
 * Cornell Center for Technology Enterprise and Commercialization at
 * 395 Pine Tree Road, Suite 310, Ithaca, NY 14850;
 * email:cctecconnect@cornell.edu; Tel: 607-254-4698;
 * FAX: 607-254-5454 for a commercial license.
 *
 * IN NO EVENT SHALL THE CORNELL RESEARCH FOUNDATION, INC. AND CORNELL
 * UNIVERSITY BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL,
 * OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF
 * WORK AND ITS ASSOCIATED COPYRIGHTS, EVEN IF THE CORNELL RESEARCH FOUNDATION,
 * INC. AND CORNELL UNIVERSITY MAY HAVE BEEN ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 *
 * THE WORK PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE CORNELL RESEARCH
 * FOUNDATION, INC. AND CORNELL UNIVERSITY HAVE NO OBLIGATION TO PROVIDE
 * MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.  THE CORNELL
 * RESEARCH FOUNDATION, INC. AND CORNELL UNIVERSITY MAKE NO REPRESENTATIONS AND
 * EXTEND NO WARRANTIES OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
 * PARTICULAR PURPOSE, OR THAT THE USE OF WORK AND ITS ASSOCIATED COPYRIGHTS
 * WILL NOT INFRINGE ANY PATENT, TRADEMARK OR OTHER RIGHTS.
 */

package org.campagnelab.gobyweb.plugins.xml.executables;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.campagnelab.gobyweb.plugins.PluginLoaderSettings;
import org.campagnelab.gobyweb.plugins.xml.PluginFileProvider;
import org.campagnelab.gobyweb.plugins.xml.common.PluginFile;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConsumerConfig;
import org.campagnelab.optval.OptionCallback;
import org.campagnelab.optval.OptionValidationExpression;
import org.campagnelab.optval.OptionValidationParser;
import scala.util.parsing.combinator.Parsers;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A configuration that can be executed on the grid.
 * @author Fabien Campagne
 *         Date: 10/28/11
 *         Time: 6:07 PM
 */
public abstract class ExecutableConfig extends ResourceConsumerConfig implements PluginFileProvider {


    /**
     * Configurations can contain a small number of files. Such files are transparently copied to the grid nodes before
     * executing a job.
     */
    @XmlElementWrapper(name = "files")
    @XmlElement(name = "file")
    public List<PluginFile> files = new ArrayList<PluginFile>();

    @XmlElement(name = "executableIOSchema")
    public ExecutableIOSchema executableIOSchema = new ExecutableIOSchema();

    /**
     * Plugins can be configured by the end-user by exposing options.
     */
    public Options options = new Options();

    /**
     * The runtime requirements for this executables plugin.
     */
    public RuntimeRequirements runtime = new RuntimeRequirements();


    public Execute execute;

    /**
     * Indicates if the input of the Executable IO Schema must be decorated.
     */
    private boolean isInputDecorated = false;

    /**
     * Indicates if the options must be decorated.
     */
    private boolean areOptionsDecorated = false;

    /**
     * Indicates if the output of the Executable IO Schema must be decorated.
     */
    private boolean isOutputDecorated = false;

    /**
     * Gets the options accepted by this configuration.
     * @return the options
     * @deprecated see #ExecutableConfig.getOptions()
     */
    public synchronized List<Option> options() {
        if (!this.areOptionsDecorated) {
            this.decorateOptions(this.options);
            this.areOptionsDecorated = true;
        }
        return options.option;
    }

    /**
     * Gets the option with the given id.
     * @param id
     * @return the option or null if there is no option matching the id
     */
    public Option getOption(String id) {
        for (Option option : options.option) {
            if (option.id.equalsIgnoreCase(id))
                return option;
        }
        return null;
    }

    /**
     * Convenience method to obtain the plugin script file.
     *
     * @return File corresponding to the filename=script.sh id=SCRIPT plugin file.
     */
    public File getScriptFile() {

        PluginFile scriptPluginFile = findFileById("SCRIPT");
        assert scriptPluginFile != null : "SCRIPT plugin file must be found.";
        return scriptPluginFile.getLocalFile();

    }


    public RuntimeRequirements getRuntime() {
        return runtime;
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
        validateId("Plugin", id, errors);

        for (ValidationRule rule : options.rules) {
            rule.validWhenParsed = validateOVL(rule.validWhen, errors);
        }

        for (Option anOption : this.getOptions().items()) {
            validateId("Plugin Option", anOption.id, errors);
            anOption.hiddenWhenParsed = validateOVL(anOption.hiddenWhen, errors);
            if (anOption.required && anOption.defaultsTo != null) {
                    anOption.userDefinedValue = anOption.defaultsTo;
            }

        }
        for (PluginFile file : files) {
            validateId("Plugin File", file.id, errors);
        }

    }


    /**
     * Return the subset of options that have a userDefinedValue!=null.
     *
     * @return list of user-defined options. Includes required options since these must have a default value.
     */
    public List<Option> userSpecifiedOptions() {
        List<Option> filtered = new ArrayList<Option>();
        for (Option anOption : options.items()) {
            if (anOption.userDefinedValue != null) {
                filtered.add(anOption);
            }
        }
        return filtered;
    }

    private Parsers.ParseResult<OptionValidationExpression> validateOVL(String hideWhen, final List<String> errors) {
        if (hideWhen == null) {
            return null;
        }
        final OptionCallback callback = new OptionCallback(){

            public boolean getOptionValue(String s) {
                return false;
            }

            public String getCategoryId(String s) {
                return "";
            }

            public boolean isBoolean(String optionId) {
                // determine if option with id optionId is of correct type:
                for (Option o: options()){
                    if (o.id.equals(optionId)) {
                        switch (o.type) {
                            case BOOLEAN:
                            case SWITCH:
                            case CATEGORY:
                                return true;

                        }

                    }
                }
                return false;
            }
        };
        OptionValidationParser parser = new OptionValidationParser();

        parser.setCallback(callback);

        Parsers.ParseResult<OptionValidationExpression> ast = parser.parseItem(hideWhen);
        if (ast.successful()) {
            OptionValidationExpression validationExpression = ast.get();
            if (validationExpression.hasCompilationErrors(callback)) {
                errors.addAll(ObjectArrayList.wrap(validationExpression.errorMessages(callback)));
                return null;
            } else {
                // option hideWhen is valid.

                return ast;
                // the hide value was evaluated in the  context provided by callback.
            }
        } else {
            errors.add("option validation expression is syntactically incorrect: " + hideWhen + " " + ast);
            return null;
        }

    }

    /**
     * Find a plugin file by its identifier.
     *
     * @param identifier Identifier of a pluin file.
     * @return The plugin file, or null if none if found matching id.
     */
    protected PluginFile findFileById(String identifier) {
        for (PluginFile pf : files) {
            if (pf.id.equals(identifier)) {
                return pf;
            }
        }
        return null;
    }

    /**
     *
     */
    @Override
    public void loadCompletedEvent() {
        for (PluginFile pluginFile : this.files) {
            pluginFile.constructLocalFilename(this.pluginDirectory);
        }
    }

    /**
     *
     */
    @Override
    public void configFileReadEvent() {
        super.configFileReadEvent();
        for (PluginFile pluginFile : this.files)   {
            if (pluginFile.filename.compareTo("script.sh") ==0) {
                return;
            }
        }
        //if here, not script.sh was found. we define the default script file.
        PluginFile SCRIPT_FILE = new PluginFile();
        SCRIPT_FILE.filename = "script.sh";
        SCRIPT_FILE.id = "SCRIPT";
        this.files.add(SCRIPT_FILE);
    }

    public List<OptionError> validateOptionValues() {
        List<OptionError> errors = new ArrayList<OptionError>();
        for (Option anOption : options.items()) {
            final OptionError error = anOption.validateOptionValue();
            if (error != null) {
                errors.add(error);
            }
        }
        return errors;
    }


    public OptionErrors validateOptionValues(final String prefix, final Map<String, String> attributes) {
        OptionErrors oe=new OptionErrors();
        for (Option anOption : options.items()) {
            String attributeName = ((prefix == null) ? "" : prefix) + anOption.id;
            final OptionError error = anOption.validateOptionValue(attributes.get(attributeName));
            if (error != null) {
                error.setFieldId(attributeName);
                oe.optionsToReset.add(error);
            }
        }
        return oe;
    }

    public List<PluginFile> getFiles() {
        return files;
    }

    public Options getOptions() {
        if (!this.areOptionsDecorated) {
            this.decorateOptions(this.options);
            this.areOptionsDecorated = true;
        }
        return options;
    }

    /**
     * This method is a hook for subclasses that want to decorate
     * the plugin options
     * @param inputSchema
     */
    protected void decorateOptions(Options options) {}

    public Execute getExecute() {
        return execute;
    }

    /**
     * Sets the input of the Executable I/O Schema.
     * @param inputSchema
     */
    public void setInput(ExecutableInputSchema inputSchema)  {
        this.executableIOSchema.inputSchema = inputSchema;
    }

    /**
     * Gets the output of the Executable I/O Schema.
     * @return
     */
    public synchronized ExecutableInputSchema getInput() {
        if (this.executableIOSchema.inputSchema == null)
            this.executableIOSchema.inputSchema = new ExecutableInputSchema();
        if (!this.isInputDecorated) {
            this.decorateInput(this.executableIOSchema.inputSchema);
            this.isInputDecorated = true;
        }
        return this.executableIOSchema.inputSchema;
    }

    /**
     * This method is a hook for subclasses that want to decorate
     * the Executable I/O Schema's input
     * @param inputSchema
     */
    protected synchronized void decorateInput(ExecutableInputSchema inputSchema) {}

    /**
     * Sets the output of the Executable I/O Schema.
     * @param outputSchema
     */
    public synchronized void setOutput(ExecutableOutputSchema outputSchema)  {
        this.executableIOSchema.outputSchema = outputSchema;
    }

    /**
     * Gets the output of the Executable I/O Schema.
     * @return
     */
    public synchronized ExecutableOutputSchema getOutput() {
        if (this.executableIOSchema.outputSchema == null)
            this.executableIOSchema.outputSchema = new ExecutableOutputSchema();
        this.decorateOutput(this.executableIOSchema.outputSchema);
        return  this.executableIOSchema.outputSchema;
    }

    /**
     * This method is a hook for subclasses that want to decorate
     * the Executable I/O Schema's input.
     * @param outputSchema
     */
    protected synchronized void decorateOutput(ExecutableOutputSchema outputSchema) {
        if (!this.isOutputDecorated) {
            this.addJobMetadataSlot(outputSchema);
            this.isOutputDecorated = true;
        }
    }


    private void addJobMetadataSlot(ExecutableOutputSchema outputSchema) {
        List<Slot> slots = outputSchema.getOutputSlots();
        boolean found = false;
        for (Slot slot : slots) {
            if (slot.getName().equals("JOB_METADATA"))
                   found = true;
        }
        if (!found) {
            Slot metadataSlot = new Slot();
            metadataSlot.setName("JOB_METADATA");
            Slot.IOFileSetRef metadataType = new Slot.IOFileSetRef();
            metadataType.id = PluginLoaderSettings.JOB_METADATA[0];
            metadataType.versionAtLeast = PluginLoaderSettings.JOB_METADATA[1];
            metadataType.versionExactly = PluginLoaderSettings.JOB_METADATA[2];
            metadataType.versionAtMost = PluginLoaderSettings.JOB_METADATA[3];
            metadataType.minOccurs = Integer.toString(1);
            metadataType.maxOccurs = Integer.toString(1);
            metadataSlot.seType(metadataType);
            slots.add(metadataSlot);
        }
    }

    /**
     * The I/O Schema defining the filesets consumed and produced by
     * the plugin when executed as job.
     */
    public static class ExecutableIOSchema {

        @XmlElement(name = "input")
        public ExecutableInputSchema inputSchema;

        @XmlElement(name = "output")
        public ExecutableOutputSchema outputSchema;
    }
}
