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

package org.campagnelab.gobyweb.plugins.xml;


import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.campagnelab.gobyweb.plugins.OptionError;
import org.campagnelab.gobyweb.plugins.OptionErrors;
import org.campagnelab.optval.OptionCallback;
import org.campagnelab.optval.OptionValidationExpression;
import org.campagnelab.optval.OptionValidationParser;
import scala.util.parsing.combinator.Parsers;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Super-class for all GobyWeb plugin configuration JAXB classes. These classes are used to unmarshall
 * configuration XML files to configure GobyWeb plugins.
 *
 * @author Fabien Campagne
 *         Date: 10/8/11
 *         Time: 7:20 PM
 */
@XmlSeeAlso({ExecutablePluginConfig.class, ResourceConfig.class})
public class PluginConfig {

    /**
     * The name of the aligner, as it will appear in the GobyWeb user interface.
     */
    public String name;
    /**
     * A unique identifier for this aligner. The identifier is used to name shell scripts to invoke
     * the aligner and should not contain any spaces.
     */
    public String id;

    /**
     * When non-null, database identifier stored by previous versions of GobyWeb. This field doesn't
     * have the requirement about containing spaces as it isn't used in scripts.
     */
    public String dbLegacyId;

    /**
     * Help text to display in the GobyWeb user-interface.
     */
    public String help;

    /**
     * If a plugin is disabled. This plugin can still have viewable objects that are made with it
     * but no NEW objects can be created using this plugin.
     */
    public boolean disabled;


    /**
     * This property will be populated upon loading the AlignerConfig instance from the plugin directory. It will
     * contain the full path to the script.sh file (on the web server/development machine) for this aligner. This
     * path is used to copy the script to the grid servers during job submission.
     */
    @XmlTransient
    // public String scriptFilename;
    /**
     * Set the plugin directory. The directory where config.xml defines  the plugin.
     */
    public void setPluginDirectory(String pluginDirectory) {
        this.pluginDirectory = pluginDirectory;
    }

    /**
     * The location of the plugin directory on the web server/development machine.
     */
    @XmlTransient
    public String pluginDirectory;

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
     * Plugins can be configured by the end-user by exposing options.
     */
    public Options options = new Options();

    public ArrayList<Option> options() {
        return options.option;
    }

    /**
     * Plugins can contain a small number of files. Such files are transparently copied to the grid nodes before
     * executing a job.
     */
    @XmlElementWrapper(name = "files")
    @XmlElement(name = "file")
    public ArrayList<PluginFile> files = new ArrayList<PluginFile>();

    /**
     * List of resource requirements. Every required resource must be available for a plugin to be installed. Missing
     * resources generate errors at plugin definition time and prevent the plugin from being shown to the end-user.
     * A plugin that has been installed is guaranteed to have access to the specified resources.
     */
    @XmlElementWrapper(name = "requires")
    @XmlElement(name = "resource")
    public ArrayList<Resource> requires = new ArrayList<Resource>();

    /**
     * Validate the plugin configuration. Call this method after unmarshalling a config to check that the configuration
     * is semantically valid. Returns null when no errors are found in the configuration, or a list of errors encountered.
     * This method also sets userDefinedValue for required options.
     *
     * @return list of error messages, or null when no errors are detected.
     */
    public ArrayList<String> validate() {
        ArrayList<String> errors = new ArrayList<String>();
        validateId("Plugin", id, errors);


        for (ValidationRule rule : options.rules) {
            rule.validWhenParsed = validateOVL(rule.validWhen, errors);
        }

        for (Option anOption : options.items()) {

            validateId("Plugin Option", anOption.id, errors);
            anOption.hiddenWhenParsed = validateOVL(anOption.hiddenWhen, errors);
            if (anOption.required) {
                if (anOption.defaultsTo == null) {
                    errors.add(String.format(
                            "Plugin Option %s is marked as required, but does not have a defaultsTo attribute. Default values are necessary for required options.",
                            anOption.id));
                } else {
                    anOption.userDefinedValue = anOption.defaultsTo;
                }
            }

        }
        for (PluginFile file : files) {
            validateId("Plugin File", file.id, errors);
        }
        validateArtifacts();
        outputSchema.validate(errors);
        if (errors.size() > 0) {
            return errors;
        } else {
            return null;
        }
    }

    protected void validateArtifacts() {

    }

    private Parsers.ParseResult<OptionValidationExpression> validateOVL(String hideWhen, final ArrayList<String> errors) {
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


    public void validateId(final String type, final String id, final ArrayList<String> errors) {
        if (id == null || id.length() == 0) {
            errors.add("Each plugin and option must have an id.");
        } else {
            for (int i = 0; i < id.length(); i++) {
                char curChar = id.charAt(i);
                if (i == 0) {
                    if (!isValidIdFirstChar(curChar)) {
                        errors.add(String.format("%s id %s is invalid. Id values must start with a letter and be" +
                                "followed by only letters, numbers, or underscores.", type, id));
                        break;
                    }
                } else {
                    if (!isValidIdNonFirstChar(curChar)) {
                        errors.add(String.format("%s id %s is invalid. Id values must start with a letter and be" +
                                "followed by only letters, numbers, or underscores.", type, id));
                        break;
                    }
                }
            }
        }
    }

    private boolean isValidIdFirstChar(final char curChar) {
        return ((curChar >= 'a' && curChar <= 'z') || (curChar >= 'A' && curChar <= 'Z'));
    }

    private boolean isValidIdNonFirstChar(final char curChar) {
        return ((curChar == '_') || (curChar >= '0' && curChar <= '9') || isValidIdFirstChar(curChar));
    }

    /**
     * Return the subset of options that have a userDefinedValue!=null.
     *
     * @return list of user-defined options. Includes required options since these must have a default value.
     */
    public ArrayList<Option> userSpecifiedOptions() {
        ArrayList<Option> filtered = new ArrayList<Option>();
        for (Option anOption : options.items()) {

            if (anOption.userDefinedValue != null) {
                filtered.add(anOption);
            }
        }
        return filtered;
    }

    public OutputSchema outputSchema = new OutputSchema();
    /**
     * Describes the plugin version. A version number associated with the files in this plugin directory.
     */
    public String version;

    /**
     * This will return the id that should be used in the database. dbLegacyId will be returned unless it is null,
     * otherwise this will return id.
     *
     * @return the id that should be used in the database
     */
    public String getDatabaseId() {
        return dbLegacyId == null ? id : dbLegacyId;
    }

    public Execute execute;

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
}
