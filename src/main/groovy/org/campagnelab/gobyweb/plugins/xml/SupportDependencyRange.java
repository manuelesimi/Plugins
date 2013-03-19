package org.campagnelab.gobyweb.plugins.xml;

/**
 * A configuration that allows its consumers to specify a dependency with "AtLeast" constraint
 */
public interface SupportDependencyRange {

    /**
     * Determine if this configuration is of a release equal to version.
     * @param version Version string, in the format _long_ [. _long_ ]*, such as 2.3.1.2
     * @return True if this configuration config has a version number equals to the argument.
     */
    public boolean exactlyVersion(final String version);

    /**
     * Determine if this configuration is of a release equal or larger than version.
     * @param version Version string, in the format _long_ [. _long_ ]*, such as 2.3.1.2
     * @return True if this configuration config has a version number at least as large as the argument.
     */
    public boolean atLeastVersion(final String version);

    /**
     * Determine if this configuration is of a release equal or lesser than version.
     * @param version Version string, in the format _long_ [. _long_ ]*, such as 2.3.1.2
     * @return True if this configuration has a version number at most as large as the argument.
     */
    public boolean atMostVersion(final String version);

 }
