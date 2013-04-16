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

    @XmlElement(name = "parameter")
    List<TaskIO> parameters = new ArrayList<TaskIO>();

    @XmlElement(name = "criteria")
    Criteria criteria;

    public List<TaskIO> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    protected static class Criteria {

    }
}
