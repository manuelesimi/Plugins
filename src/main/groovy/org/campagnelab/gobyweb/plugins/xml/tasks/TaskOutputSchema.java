package org.campagnelab.gobyweb.plugins.xml.tasks;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Task output schema
 * @author manuele
 */
public class TaskOutputSchema {

    @XmlElement(name = "fileSetRef")
    List<OutputFileSetRef> fileSetRefs = new ArrayList<OutputFileSetRef>();

    @XmlAccessorType(XmlAccessType.FIELD)
    protected static class OutputFileSetRef {

        protected String id;

        protected String version;

        protected boolean failIfNotProduced = true;

        @XmlAttribute
        String minOccurs;

        @XmlAttribute
        String maxOccurs;

        protected OutputFileSetRef() {}
    }

}
