package org.campagnelab.gobyweb.plugins.xml.tasks;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Task output schema
 * @author manuele
 */
public class TaskOutputSchema {

    @XmlElement(name = "outputSlot")
    List<TaskIO> outputSlots = new ArrayList<TaskIO>();

    public List<TaskIO> getOutputSlots() {
        return Collections.unmodifiableList(outputSlots);
    }
}
