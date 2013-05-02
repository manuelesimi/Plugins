package org.campagnelab.gobyweb.plugins.xml.executables;


import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Input schema for executable plugins
 *
 * @author manuele
 */
public class ExecutableInputSchema {

    @XmlElement(name = "inputSlot")
    protected List<Slot> inputSlots = new ArrayList<Slot>();

    public List<Slot> getInputSlots() {
        return inputSlots;
    }

}
