package org.campagnelab.gobyweb.plugins.xml.executables;

import com.google.api.client.repackaged.com.google.common.base.Strings;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * A slot in the I/O Schema
 *
 * @author manuele
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Slot {

    String name;

    String description;

    IOFileSetRef type;

    public static final String UNBOUNDED_SLOT = "unbounded";

    public IOFileSetRef geType() {
        return this.type;
    }

    public String getName() {
        return name;
    }

    public void seType(IOFileSetRef type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name=name;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class IOFileSetRef {

        public String id;
        /**
         * Minimum version number of the fileset required. Any version is acceptable when version==null.
         */
        public String versionAtLeast;

        /**
         * Maximum version number of the fileset required. Any version is acceptable when version==null.
         */
        public String versionAtMost;

        /**
         * Exact version number of the fileset required. Any version is acceptable when version==null.
         */
        public String versionExactly;

        @XmlAttribute
        public  String minOccurs = "1";

        @XmlAttribute
        public String maxOccurs = "1";

        public IOFileSetRef() {}

        public String getMinOccurs() {
            return Strings.isNullOrEmpty(minOccurs)? "1" : minOccurs;
        }
        public String getMaxOccurs() {
            return Strings.isNullOrEmpty(maxOccurs)? "1" : maxOccurs;
        }


    }

}
