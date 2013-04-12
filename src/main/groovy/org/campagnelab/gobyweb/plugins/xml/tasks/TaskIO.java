package org.campagnelab.gobyweb.plugins.xml.tasks;


import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Task input and output elements
 * @author manuele
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class TaskIO {

    String name;

    @XmlElementWrapper(name = "type")
    @XmlElement(name = "fileSetRef")
    List<IOFileSetRef> fileSetRefs = new ArrayList<IOFileSetRef>();

    public List<IOFileSetRef> getFileSetRefs() {
        return Collections.unmodifiableList(fileSetRefs);
    }

    public String getName() {
        return name;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class IOFileSetRef {

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

        protected boolean mandatory = true;


        protected IOFileSetRef() {}

    }
}
