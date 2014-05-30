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

import org.campagnelab.optval.OptionValidationExpression;
import scala.util.parsing.combinator.Parsers;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Describes a GobyWeb option.
 *
 * @author campagne
 *         Date: 10/7/11
 *         Time: 11:45 AM
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Option {

    // TODO: Put the errors in the messages file for ease of transformation ?

    private static final Pattern FLOAT_PATTERN = Pattern.compile("[\\-\\+]?\\d+(\\.\\d*)?(e-?\\d+)?");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("-?\\d+?");

    /**
     * Identifier to use in shell scripts and config files to refer to the option.
     */
    public String id;
    /**
     * Name to display in the GobyWeb user interface.
     */
    public String name;
    /**
     * Text that describes the option to end-users.
     */
    public String help;
    /**
     * Indicates if the option is required (true), or optional (false).
     */
    public boolean required;
    /**
     * The value the option defaults to if it is required, but not provided.
     */
    public String defaultsTo;

    /**
     * Allows multiple declaration of this option from the command line.
     */
    public boolean allowMultiple = false;

    public enum OptionType {
        /**
         * A BOOLEAN is either true or false. It can be shown as a checkbox. The userDefinedValue field will be true or
         * false. Autoformat always writes the value of the boolean to ALL_OTHER_OPTIONS.
         */
        BOOLEAN,
        /**
         * A switch is either active or inactive. It can be shown as a checkbox. Like BOOLEAN, userDefinedValue will be true of false
         * for switches, but autoformat will only write to ALL_OTHER_OPTIONS when userDefinedValue == "true.
         */
        SWITCH,
        CATEGORY,
        STRING,
        INTEGER,
        DOUBLE
    }

    /**
     * Expression that evaluates to a boolean and determines when this option is hidden from the user interface.
     * The expression must be expressed in the Option Validation Language.
     */
    @XmlAttribute
    public String hiddenWhen;
    /**
     * Field where we store the parsed AST of the hiddenWhen expression.
     */
    @XmlTransient
    public Parsers.ParseResult<OptionValidationExpression> hiddenWhenParsed;

    public Option() {
        categories = new ArrayList<Category>();
    }

    /**
     * Type of the value for this option.
     */
    public OptionType type;
    /**
     * List of category values. Only populated if type == Category.
     */
    @XmlElementWrapper(name = "categories")
    @XmlElement(name = "category")
    public ArrayList<Category> categories;
    /**
     * Indicates if the option can be automatically formatted. When auto-format is true, the option is
     * appended in an arbitrary order, with other auto-format options to the environment variable
     * PLUGINS_ plygin-type _ plugin-id _ ALL_OTHER_OPTIONS. GobyWeb plugin scripts are responsible
     * for inserting auto-format options in the appropriate location on the tool command line.
     */

    public boolean autoFormat;
    /**
     * Flag format string, suitable to call String.format(flagFormat, value) and produce an option suitably formatted.
     * Multiple calls to format should be interleaved with space characters when includeSpaces=true.
     */
    public String flagFormat;
    /**
     * When includeSpaces is true, autoformat surrounds each option with a space character.
     */
    public boolean includeSpaces = true;
    /**
     * When non-null, database identifier stored by previous versions of GobyWeb. This is useful to keep
     * the application compatible with the state stored by previous versions. New database ids are always
     * constructed as aligner-id.option-id when dbLegacyId is null.
     */
    public String dbLegacyId;
    /**
     * When the end-user interacts with the GobyWeb UI and sets the value of an option, the UI sets the chosen value
     * in this field as a string.
     */
    public String userDefinedValue;

    /**
     * This will return the id that should be used in the database. dbLegacyId will be returned unless it is null,
     * otherwise this will return id.
     *
     * @return the id that should be used in the database
     */
    public String getDatabaseId() {
        return dbLegacyId == null ? id : dbLegacyId;
    }

    /**
     * Validate the value passed in.
     *
     * @return null means the value validated correctly. A non-null string describes the error.
     */
    public OptionError validateOptionValue() {
        return validateOptionValue(userDefinedValue);
    }

    /**
     * Validate the value passed in.
     *
     * @param value the value to validate
     * @return null means the value validated correctly. A non-null string describes the error.
     */
    public OptionError validateOptionValue(final String value) {
        //System.out.println("Validating value=" + value);
        if (value == null || value.length() == 0) {
            if (required || type == OptionType.SWITCH || type == OptionType.BOOLEAN || type == OptionType.CATEGORY) {
                // These types are implicitly required
                return new OptionError(this, String.format("Must not be undefined."));
            } else {
                // Empty but not required. No problem.
                return null;
            }
        }
        switch (type) {
            case STRING:
                // Required is good enough
                return null;
            case BOOLEAN:
            case SWITCH:
                if (value.toLowerCase().equals("true") || value.toLowerCase().equals("false")) {
                    return null;
                } else {
                    return new OptionError(this, String.format("Must be set to 'true' or 'false'."));
                }
            case CATEGORY:
                for (Category category : categories) {
                    //check if the value is the ID or the Value of a category
                    if ((category.id.equals(value)) || (category.value.toLowerCase().equals(value.toLowerCase()))) {
                        return null;
                    }
                }
                return new OptionError(this, String.format("The category %s/%s does not exist.", value, id));
            case INTEGER:
                if (validateInteger(value)) {
                    return null;
                } else {
                    return new OptionError(this, String.format("Must be an integer, but isn't."));
                }
            case DOUBLE:
                if (validateDouble(value)) {
                    return null;
                } else {
                    return new OptionError(this, String.format("Must be a floating point number, but isn't."));
                }
        }
        return new OptionError(this, "validation failed for an unknown reason");
    }

    private boolean validateDouble(final String v) {
        if (FLOAT_PATTERN.matcher(v).matches()) {
            try {
                // We SHOULD have a valid Double, but let's make sure.
                final Double d = Double.valueOf(v);
                return true;
            } catch (java.lang.NumberFormatException e) {
                // It isn't a double. Move on to the next case.
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean validateInteger(final String v) {
        if (INTEGER_PATTERN.matcher(v).matches()) {
            try {
                // We SHOULD have a valid Double, but let's make sure.
                final Integer d = Integer.valueOf(v);
                return true;
            } catch (java.lang.NumberFormatException e) {
                // It isn't a double. Move on to the next case.
                return false;
            }
        } else {
            return false;
        }
    }

    public String convertLegacyValue(final String legacyValue) {
        if (legacyValue != null && legacyValue.length() != 0) {
            if (type == OptionType.CATEGORY) {
                for (Category category : categories) {
                    if (category.dbLegacyValue != null && category.dbLegacyValue.length() > 0) {
                        if (category.dbLegacyValue.equals(legacyValue)) {
                            return category.id;
                        }
                    }
                }
            }
        }
        return legacyValue;
    }

    public String categoryIdToValue(final String categoryId) {
        if (categoryId != null && categoryId.length() != 0) {
            for (Category category : categories) {
                if (category.id.equals(categoryId)) {
                    return category.value;
                }
            }
        }
        return categoryId;
    }

    /**
     * Gets the allowed values for all the categories.
     * @return
     */
    public List<String> categoryValues() {
        List<String> values = new ArrayList<String>();
        for (Category category : categories)
            values.add(category.value);
        return values;
    }
}
