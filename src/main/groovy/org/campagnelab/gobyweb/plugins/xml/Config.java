package org.campagnelab.gobyweb.plugins.xml;

import java.util.List;

/**
 * Model the basic behavior of a configuration
 *
 * @author manuele
 */
public interface Config {

    /**
     * Sets the unique identifier for the configuration.
     * The identifier is used to name shell scripts to invoke
     * the plugin and should not contain any spaces.
     *
     * @param id the identifier
     */
    public void setId(String id);

    /**
     * Gets the unique identifier for this configuration.
     *
     * @return the unique identifier
     */
    public String getId();


    /**
     * Sets the configuration version.
     *
     * @param version  the version
     */
    public void setVersion(String version);

    /**
     * Gets the configuration version.
     *
     * @return  the version
     */
    public String getVersion();

    /**
     * Sets the name of the configuration, as it will appear
     * in the GobyWeb user interface.
     *
     * @param name the name
     */
    public void setName(String name);

    /**
     * Gets the name of the configuration
     *
     * @return the name
     */
    public String getName();

    /**
     * Sets the help text to display in the GobyWeb user-interface.
     *
     * @param help the help text
     */
    public void setHelp(String help);

    /**
     * Gets the help text
     *
     * @return the help text
     */
    public String getHelp();

    /**
     * When non-null, database identifier stored by previous versions of GobyWeb. This field doesn't
     * have the requirement about containing spaces as it isn't used in scripts.
     *
     * @param dbLegacyId the database identifier
     */
    public void setDbLegacyId(String dbLegacyId);

    /**
     *  Gets database identifier stored by previous versions of GobyWeb
     *
     * @return the database identifier
     */
    public String getDbLegacyId();

    /**
     * Sets the location of the configuration directory on the web server/development machine.
     */
    public void setDirectory(String pluginDirectory);

    /**
     * Gets the configuration directory
     *
     * @return  the configuration directory
     */
    public String getDirectory();

    /**
     * Checks if the configuration is disabled or not
     *
     * @return true if the configuration is disabled, false otherwise
     */
    public boolean isDisabled();

    /**
     * Disables/enables the configuration. When disabled, the configuration can still have viewable objects that are made with it
     * but no NEW objects can be created using this configuration.
     */
    public void setDisabled(boolean disabled);

    /**
     * Gets a human readable description of the configuration type
     *
     * @return the description
     */
    public String getHumanReadableConfigType();

    /**
     * Validates the configuration. Call this method after unmarshalling a config to check that the configuration
     * is semantically valid. Returns null when no errors are found in the configuration, or a list of errors encountered.
     * This method also sets userDefinedValue for required options.
     *
     * @return list of error messages, or null when no errors are detected.
     */
    public void validate(List<String> errors);

    /**
     * Validates the configuration identifier
     * @param type
     * @param id
     * @param errors
     */
    public void validateId(final String type, final String id, final List<String> errors);


    /**
     * This method called after the reading of the config.xml file
     */
    public void configFileReadEvent();

    /**
     * This method called after the configuration is loaded to allow any specific post-loading activity
     */
    public void loadCompletedEvent();

    /**
     * Sets the template to use to render the options in a
     * graphical user interface.
     *
     * @param template the template
     */
    public void setUiTemplate(String template);

    /**
     * Gets the UI template.
     *
     * @return the template
     */
    public String getUiTemplate();
}
