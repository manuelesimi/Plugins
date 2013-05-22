package org.campagnelab.gobyweb.plugins.xml.executables;


import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Output schema for executable plugins.
 *
 * @author manuele
 */
public class ExecutableOutputSchema {

    @XmlElement(name = "outputSlot")
    protected List<Slot> outputSlots = new ArrayList<Slot>();

    /**
     * Gets the output slots for this schema.
     * @return the list of output slots
     */
    public List<Slot> getOutputSlots() {
        return outputSlots;
    }
}
