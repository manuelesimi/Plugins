package org.campagnelab.gobyweb.plugins.xml.filesets;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.campagnelab.gobyweb.plugins.xml.BaseConfig;
import org.campagnelab.gobyweb.plugins.xml.SupportDependencyRange;
import org.campagnelab.gobyweb.plugins.xml.common.Attribute;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration information needed to describe a FileSet.
 * @author manuele
 * @version  1.0
 * Date: 2/7/13
 * Time: 9:59 AM
 *
 */


@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class FileSetConfig extends BaseConfig implements SupportDependencyRange, Comparable<FileSetConfig>  {

    /**
     * Files belonging to the fileset
     */
    @XmlElementWrapper(name = "files")
    @XmlElement(name = "selector")
    protected List<ComponentSelector> fileSelectors;


    /**
     * Directories belonging to the fileset
     */
    @XmlElementWrapper(name = "directories")
    @XmlElement(name = "selector")
    protected List<ComponentSelector> dirSelectors;

    /**
     * Attribute value pairs associated with this artifact.
     */
    @XmlElementWrapper(name = "attributes")
    @XmlElement(name = "attribute")
    protected List<Attribute> attributes;


    public enum SELECTOR_TYPE {
        FILE,DIR
    }

    public FileSetConfig() {}

    public FileSetConfig(String id) {
        this.id = id;
    }

    /**
     * Returns true if any of the attributes has no defined/assigned value.
     * @return True if any of the attributes has no defined/assigned value.
     */
    public boolean hasUndefinedAttributes() {
        if (attributes == null)
            return false;
        for (Attribute attribute: attributes) {
            if (attribute.value==null) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @return the list of attributes
     */
    public List<Attribute> getAttributes() {
        if (this.attributes == null)
            return Collections.EMPTY_LIST;
        else
            return Collections.unmodifiableList(this.attributes);
    }

    /**
     * Determine if this fileset is of a release equal or larger than version.
     * @param version Version string, in the format _long_ [. _long_ ]*, such as 2.3.1.2
     * @return True if this fileset config has a version number at least as large as the argument.
     */
    public boolean atLeastVersion(final String version) {
        LongList requiredVersion = makeVersionList(version);
        LongList setActualVersion = makeVersionList(this.version);
        int numRequired = requiredVersion.size();
        while (setActualVersion.size() < numRequired) {
            // Make sure we have enough pieces of version, append 0's if we don't
            setActualVersion.add(0);
        }

        for (int i=0;i<numRequired; i++) {
            if (setActualVersion.getLong(i) == requiredVersion.getLong(i)) continue;
            return setActualVersion.getLong(i) > requiredVersion.getLong(i);
        }
        return true;
    }

    /**
     * Determine if this fileset is of a release equal or lesser than version.
     * @param version Version string, in the format _long_ [. _long_ ]*, such as 2.3.1.2
     * @return True if this fileset config has a version number at least as large as the argument.
     */
    public boolean atMostVersion(final String version) {
        LongList requiredVersion = makeVersionList(version);
        LongList setActualVersion = makeVersionList(this.version);
        int numRequired = requiredVersion.size();
        while (setActualVersion.size() < numRequired) {
            // Make sure we have enough pieces of version, append 0's if we don't
            setActualVersion.add(0);
        }

        for (int i=0;i<numRequired; i++) {
            if (setActualVersion.getLong(i) == requiredVersion.getLong(i)) continue;
            return setActualVersion.getLong(i) < requiredVersion.getLong(i);
        }
        return true;
    }

    /**
     * Convert "4.3.2.1.0" into a LongList [4,3,2,1,0]
     * @param version
     * @return
     */
    private LongList makeVersionList(final String version) {
        LongList versionList = new LongArrayList();
        for (final String versionItem : version.split("[\\.]")) {
            versionList.add(Long.parseLong(versionItem));
        }
        return versionList;
    }

    public boolean exactlyVersion(final String version) {
        LongList requiredVersion = makeVersionList(version);
        LongList setActualVersion = makeVersionList(this.version);
        int numRequired = requiredVersion.size();
        int numActual = setActualVersion.size();

        if (numRequired != numActual) {
            return false;
        }

        for (int i = 0; i < numRequired; i++) {
            if (setActualVersion.getLong(i) != requiredVersion.getLong(i)) {
                return false;
            }
        }
        return true;
    }
    /**
     *
     * @return the list of file selectors
     */
    public List<ComponentSelector> getFileSelectors() {
       if (this.fileSelectors != null) {
           for (ComponentSelector selector : this.fileSelectors)
               selector.type = SELECTOR_TYPE.FILE;//cannot be done at init time
            return Collections.unmodifiableList(this.fileSelectors);
       } else
            return Collections.EMPTY_LIST;
    }

    /**
     *
     * @return the list of file selectors
     */
    public List<ComponentSelector> getDirSelectors() {
        if (this.dirSelectors != null) {
            for (ComponentSelector selector : this.dirSelectors)
                selector.type = SELECTOR_TYPE.DIR;//cannot be done at init time
            return Collections.unmodifiableList(this.dirSelectors);
        } else
            return Collections.EMPTY_LIST;
    }

    /**
     * Gets all the selectors, regardless if they refer to directories or files.
     * @return
     */
    public List<ComponentSelector> getAllSelectors() {
        List<ComponentSelector> selectors = new ArrayList<ComponentSelector>();
        if (this.fileSelectors != null)
            selectors.addAll(this.fileSelectors);
        if (this.dirSelectors != null)
            selectors.addAll(this.dirSelectors);
        return selectors;
    }

    /**
     * Sets the file selectors
     * @param selectors
     */
    protected void setFileSelectors(final List<ComponentSelector> selectors) {
        this.fileSelectors = selectors;
    }

    /**
     * Sets the dir selectors
     * @param selectors
     */
    protected void setDirSelectors(final List<ComponentSelector> selectors) {
        this.dirSelectors = selectors;
    }

    /**
     * Sets the attributes
     * @param attributes
     */
    protected void setAttributes(final List<Attribute> attributes) {
        this.attributes = attributes;
    }

    /**
     * Gets a human readable description of the configuration type
     *
     * @return the description
     */
    @Override
    public String getHumanReadableConfigType() {
        return "FILESET";
    }

    @Override
    public String toString() {
        return String.format("%s/%s (%s)", this.getHumanReadableConfigType(), this.name, this.version);
    }

    @Override
    public int compareTo(FileSetConfig fileSetConfig) {
        return (this.atLeastVersion(fileSetConfig.version)) ? -1 : 1;
    }

    /**
     * Gets the selector with the given id.
     * @param id
     * @return the selector or null if it does not exist
     */
    public ComponentSelector getSelector(String id) {
        for (ComponentSelector selector : this.fileSelectors) {
           if (selector.getId().equalsIgnoreCase(id)) {
               selector.type = SELECTOR_TYPE.FILE;
               return selector;
           }
        }
        for (ComponentSelector selector : this.dirSelectors) {
            if (selector.getId().equalsIgnoreCase(id)) {
                selector.type = SELECTOR_TYPE.DIR;
                return selector;
            }
        }
        return null;
    }


    /**
     * A file/dir selector associated to the FileSet
     */
    @XmlRootElement(name = "selector")
    static public class ComponentSelector {

        @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
        @XmlElement
        protected String id;

        @XmlElement
        protected String pattern;

        @XmlElement
        protected Boolean mandatory;

        @XmlAttribute
        protected String matchAttribute;

        protected SELECTOR_TYPE type;

        protected ComponentSelector() {}

        protected ComponentSelector(String id, String pattern, Boolean mandatory, String matchAttribute) {
            this.id = id;
            this.pattern = pattern;
            this.mandatory = mandatory;
            this.matchAttribute = matchAttribute;
        }

        public Boolean getMandatory() {
            return mandatory;
        }

        public String getPattern() {
            return pattern;
        }

        public String getId() {
            return id;
        }

        public SELECTOR_TYPE getType() {
            return type;
        }

        public String[] getMatches() {
              return matchAttribute.split(",");
        }
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

    }
}
