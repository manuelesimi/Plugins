package org.campagnelab.gobyweb.plugins.xml.tasks;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Task input schema
 * @author manuele
 */
public class TaskInputSchema {

    @XmlElement(name = "inputSlot")
    List<TaskIO> inputSlots = new ArrayList<TaskIO>();

    @XmlElement(name = "criteria")
    Criteria criteria;

    public List<TaskIO> getInputSlots() {
        return Collections.unmodifiableList(inputSlots);
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    protected static class Criteria {

    }
}
