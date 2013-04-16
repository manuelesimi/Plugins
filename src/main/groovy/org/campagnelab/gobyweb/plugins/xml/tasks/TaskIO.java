package org.campagnelab.gobyweb.plugins.xml.tasks;


import javax.xml.bind.annotation.*;

/**
 * Task input and output elements
 * @author manuele
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class TaskIO {

    String name;

    IOFileSetRef type;

    public IOFileSetRef geType() {
        return this.type;
    }

    public String getName() {
        return name;
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

        public boolean mandatory = true;


        protected IOFileSetRef() {}

    }
}
