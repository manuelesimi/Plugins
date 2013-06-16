package org.campagnelab.gobyweb.plugins.xml.executables;


import org.campagnelab.gobyweb.plugins.xml.resources.Resource;
import org.campagnelab.gobyweb.plugins.xml.tasks.TaskConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Tester for {@link org.campagnelab.gobyweb.plugins.xml.tasks.TaskConfig}
 * @author manuele
 *
 */
@RunWith(JUnit4.class)
public class TaskConfigTest {

    private static TaskConfig taskConfig;
    private static String serializedTask;
    private static TaskConfig deserializedTask;
    private static java.io.File schemaFile = new File("./schemas/plugins.xsd");

    @Test
    public void marshall() {

        Marshaller marshaller=null;
        try {
            JAXBContext context = JAXBContext.newInstance(TaskConfig.class);
            marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        } catch (Exception e) {
            e.printStackTrace();
            fail("failed at creating JAXB mashaller");
        }

        ExecutableInputSchema inputSchema = new ExecutableInputSchema();

        Slot taskIOIN = new Slot();
        taskIOIN.name = "READS_FILE";
        Slot.IOFileSetRef inFileSetRef =  new Slot.IOFileSetRef();
        inFileSetRef.id = "COMPACT_READS";
        inFileSetRef.versionExactly = "1.0";
        inFileSetRef.maxOccurs = "1";
        inFileSetRef.minOccurs = "1";
        taskIOIN.type = inFileSetRef;
        inputSchema.inputSlots.add(taskIOIN);
        ExecutableOutputSchema outputSchema = new ExecutableOutputSchema();

        Slot taskIOOUT =  new Slot();
        taskIOOUT.name = "TSV_FILE";
        Slot.IOFileSetRef outFileSetRef = new Slot.IOFileSetRef();
        outFileSetRef.id = "TSV";
        outFileSetRef.versionExactly = "1.0";
        outFileSetRef.maxOccurs = "1";
        outFileSetRef.minOccurs = "1";
        taskIOOUT.type = outFileSetRef;
        outputSchema.outputSlots.add(taskIOOUT);

        Slot taskIOOUT2 =  new Slot();
        Slot.IOFileSetRef outFileSetRef2 = new Slot.IOFileSetRef();
        outFileSetRef2.id = "TSV_second_format  ";
        outFileSetRef2.versionExactly = "1.5";
        outFileSetRef2.maxOccurs = "unbounded";
        outFileSetRef2.minOccurs = "1";
        taskIOOUT2.type = outFileSetRef2;
        outputSchema.outputSlots.add(taskIOOUT2);

        TaskConfig task = new TaskConfig();
        task.setInput(inputSchema);
        task.setOutput(outputSchema);
        task.setId("RNASELECT_TASK");
        task.setName("RNASELECT Task_Name");
        task.setHelp("Execute RNASELECT resource as Task to process multiple reads files and produce a TSV");
        task.requires = new ArrayList<Resource>();
        Resource resource = new Resource();
        resource.id = "RNASELECT";
        resource.versionAtLeast = "1.0";
        task.requires.add(resource);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            marshaller.marshal(task, outputStream);
            serializedTask = new String(outputStream.toByteArray(), "UTF-8");
            System.out.println(serializedTask);
        } catch (Exception e) {
            e.printStackTrace();
            fail("failed at marshalling FileSetConfig");
        }

    }

    @Test
    public void unmarshall() {
        Unmarshaller unmarshaller = null;
        try {
            JAXBContext context = JAXBContext.newInstance(TaskConfig.class);
            unmarshaller = context.createUnmarshaller();

        } catch (Exception e) {
            fail("failed at creating JAXB unmarshaller");
        }
        assertNotNull("Unmarshaller cannot be null", unmarshaller);
        try {
            deserializedTask = (TaskConfig) unmarshaller.unmarshal(new StreamSource(new StringReader(serializedTask)));
        } catch (JAXBException e) {
            e.printStackTrace();
            fail("Failed at unmarshalling FileSetConfig");
        }
        assertNotNull("DeserializedTask cannot be null", deserializedTask);
        assertEquals("Task ID is not the one expected", "RNASELECT_TASK", deserializedTask.getId());
        assertNotNull("InputSchema cannot be null", deserializedTask.getInput());
        assertNotNull("OutputSchema cannot be null", deserializedTask.getOutput());
        assertEquals("There must be only 1 input slot", 1, deserializedTask.getInput().getInputSlots().size());
        assertEquals("There must be 2 output slots", 2, deserializedTask.getOutput().getOutputSlots().size());

    }
}
