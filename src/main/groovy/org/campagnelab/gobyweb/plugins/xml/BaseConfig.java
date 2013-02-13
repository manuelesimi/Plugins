package org.campagnelab.gobyweb.plugins.xml;

import javax.xml.bind.annotation.XmlTransient;
import java.util.List;

/**
 * Super-class for all GobyWeb plugin configuration JAXB classes. These classes are used to unmarshall
 * configuration XML files to configure GobyWeb plugins.ngs
 */
public abstract class BaseConfig implements Config {

    /**
     * The name, as it will appear in the GobyWeb user interface.
     */
    protected String name;
    /**
     * A unique identifier for this aligner. The identifier is used to name shell scripts to invoke
     * the aligner and should not contain any spaces.
     */
    protected String id;

    /**
     * Describes the configuration version. A version number associated with the files in the configuration directory.
     */
    protected String version;

    /**
     * When non-null, database identifier stored by previous versions of GobyWeb. This field doesn't
     * have the requirement about containing spaces as it isn't used in scripts.
     */
    protected String dbLegacyId;

    /**
     * Help text to display in the GobyWeb user-interface.
     */
    protected String help;

    /**
     * If a plugin is disabled. This plugin can still have viewable objects that are made with it
     * but no NEW objects can be created using this plugin.
     */
    protected boolean disabled = false;


    /**
     * The location of the plugin directory on the web server/development machine.
     */
    @XmlTransient
    protected String pluginDirectory;

    /**
     * Set the plugin directory. The directory where config.xml defines  the plugin.
     */
    public void setPluginDirectory(String pluginDirectory) {
        this.pluginDirectory = pluginDirectory;
    }

    /**
     * Sets the unique identifier for the configuration.
     * The identifier is used to name shell scripts to invoke
     * the plugin and should not contain any spaces.
     *
     * @param id the identifier
     */
    @Override
    public void setId(String id) {
       this.id = id;
    }

    /**
     * Gets the unique identifier for this configuration.
     *
     * @return the unique identifier
     */
    @Override
    public String getId() {
        return this.id;
    }

    /**
     * Sets the name of the configuration, as it will appear
     * in the GobyWeb user interface.
     *
     * @param name the name
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the name of the configuration
     *
     * @return the name
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Gets the unique identifier for this configuration.
     *
     * @return the unique identifier
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the configuration version.
     *
     * @param version  the version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Sets the help text to display in the GobyWeb user-interface.
     *
     * @param help the help text
     */
    @Override
    public void setHelp(String help) {
        this.help = help;
    }

    /**
     * Gets the help text
     *
     * @return the help text
     */
    @Override
    public String getHelp() {
        return this.help;
    }

    /**
     * When non-null, database identifier stored by previous versions of GobyWeb. This field doesn't
     * have the requirement about containing spaces as it isn't used in scripts.
     *
     * @param dbLegacyId the database identifier
     */
    @Override
    public void setDbLegacyId(String dbLegacyId) {
        this.dbLegacyId = dbLegacyId;
    }

    /**
     * This will return the id that should be used in the database. dbLegacyId will be returned unless it is null,
     * otherwise this will return id.
     *
     * @return the id that should be used in the database
     */
    @Override
    public String getDbLegacyId() {
        return (this.dbLegacyId == null) ? id : dbLegacyId;
    }

    /**
     * Sets the location of the configuration directory on the web server/development machine.
     */
    @Override
    public void setDirectory(String pluginDirectory) {
        this.pluginDirectory = pluginDirectory;
    }


    /**
     * Gets the configuration directory
     *
     * @return the configuration directory
     */
    @Override
    public String getDirectory() {
        return this.pluginDirectory;
    }

    /**
     * Checks if the configuration is disabled or not
     *
     * @return true if the configuration is disabled, false otherwise
     */
    @Override
    public boolean isDisabled() {
        return this.disabled;
    }

    /**
     * Disables the configuration. When disabled, the configuration can still have viewable objects that are made with it
     * but no NEW objects can be created using this configuration.
     */
    @Override
    public void disable() {
         this.disabled = true;
    }

    /**
     * Validates the plugins id
     * @param type
     * @param id
     * @param errors the list of errors found during the validation
     */
    @Override
    public void validateId(final String type, final String id, final List<String> errors) {
        if (id == null || id.length() == 0) {
            errors.add("Each plugin and option must have an id." );
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
     * This definition makes optional the loadCompletedEvent() method in sub-classes
     */
    @Override
    public void loadCompletedEvent() {}

    /**
     * This definition makes optional the configFileReadEvent() method in sub-classes
     */
    @Override
    public void configFileReadEvent() {}


    /**
     * Overrides standard {@link Object#equals(Object)} for using plugins inside collections
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseConfig that = (BaseConfig) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    /*
     * Overrides standard {@link Object#hashCode()} for using plugins inside collections
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /*
     * Overrides standard {@link Object#toString()} to force to print basic info of the configuration
     */
    @Override
    public abstract String toString();

}
