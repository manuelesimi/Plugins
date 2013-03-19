package org.campagnelab.gobyweb.plugins.xml.tasks;


import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Task input schema
 * @author manuele
 */
public class TaskInputSchema {

    @XmlElement(name = "fileSetRef")
    List<InputFileSetRef> fileSetRefs = new ArrayList<InputFileSetRef>();

    @XmlElement(name = "criteria")
    Criteria criteria;

    @XmlAccessorType(XmlAccessType.FIELD)
    protected static class InputFileSetRef {

        protected String id;
        /**
         * Minimum version number of the fileset required. Any version is acceptable when version==null.
         */
        protected String versionAtLeast;

        /**
         * Maximum version number of the fileset required. Any version is acceptable when version==null.
         */
        protected String versionAtMost;

        /**
         * Exact version number of the fileset required. Any version is acceptable when version==null.
         */
        protected String versionExactly;

        @XmlAttribute
        protected  String minOccurs;

        @XmlAttribute
        protected String maxOccurs;

        protected InputFileSetRef() {}

    }

    @XmlAccessorType(XmlAccessType.FIELD)
    protected static class Criteria {

    }
}
