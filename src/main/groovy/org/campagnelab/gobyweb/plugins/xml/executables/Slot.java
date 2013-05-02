package org.campagnelab.gobyweb.plugins.xml.executables;

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

    IOFileSetRef type;

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
        public  String minOccurs;

        @XmlAttribute
        public String maxOccurs;

        public IOFileSetRef() {}

    }

}
