package org.campagnelab.gobyweb.plugins.xml.filesets;

import org.campagnelab.gobyweb.plugins.xml.common.ResourceConsumerConfig;
import org.campagnelab.gobyweb.plugins.xml.common.Attribute;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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
public class FileSetConfig extends ResourceConsumerConfig {


    /**
     * Directories belonging to the fileset
     */
    @XmlElementWrapper(name = "fileSelectors")
    @XmlElement(name = "selector")
    protected List<ComponentSelector> dirSelectors;


    /**
     * Files belonging to the fileset
     */
    @XmlElementWrapper(name = "dirSelectors")
    @XmlElement(name = "selector")
    protected List<ComponentSelector> fileSelectors;


    /**
     * Attribute value pairs associated with this artifact.
     */
    @XmlElementWrapper(name = "attributes")
    @XmlElement(name = "attribute")
    protected List<Attribute> attributes;

    public FileSetConfig() {}

    public FileSetConfig(String id, List<ComponentSelector> files, List<ComponentSelector> dirs, List<Attribute> attributes) {
        this.id = id;
        this.fileSelectors = files;
        this.dirSelectors = dirs;
        this.attributes = attributes;
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
     *
     * @return the list of file selectors
     */
    public List<ComponentSelector> getFileSelectors() {
       if (this.fileSelectors == null)
           return Collections.EMPTY_LIST;
        else
           return Collections.unmodifiableList(this.fileSelectors);
    }


    /**
     *
     * @return the list of file selectors
     */
    public List<ComponentSelector> getDirSelectors() {
        if (this.dirSelectors == null)
            return Collections.EMPTY_LIST;
        else
            return Collections.unmodifiableList(this.dirSelectors);
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


    /**
     * A file/dir selector associated to the FileSet
     */
    static public class ComponentSelector {

        @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
        protected String id;

        protected String pattern;

        protected Boolean mandatory;

        @XmlAttribute
        protected String matchAttribute;

        protected ComponentSelector() {}

        protected ComponentSelector(String id, String pattern, Boolean mandatory, String matchAttribute) {
            this.id = id;
            this.pattern = pattern;
            this.mandatory = mandatory;
            this.matchAttribute = matchAttribute;
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
