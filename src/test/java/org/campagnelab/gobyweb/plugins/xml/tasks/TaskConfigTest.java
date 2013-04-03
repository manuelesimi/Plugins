package org.campagnelab.gobyweb.plugins.xml.tasks;

import org.campagnelab.gobyweb.plugins.xml.resources.Resource;
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

        TaskInputSchema inputSchema = new TaskInputSchema();
        TaskIO taskIOIN = new TaskIO();
        taskIOIN.name = "READS_FILE";

        TaskIO.IOFileSetRef inFileSetRef =  new TaskIO.IOFileSetRef();
        inFileSetRef.id = "COMPACT_READS";
        inFileSetRef.versionExactly = "1.0";
        inFileSetRef.maxOccurs = "1";
        inFileSetRef.minOccurs = "1";
        taskIOIN.fileSetRefs.add(inFileSetRef);
        TaskIO.IOFileSetRef inFileSetRef2 =  new TaskIO.IOFileSetRef();
        inFileSetRef2.id = "COMPACT_READS2";
        inFileSetRef2.versionAtMost = "2.0";
        inFileSetRef2.maxOccurs = "1";
        inFileSetRef2.minOccurs = "1";
        taskIOIN.fileSetRefs.add(inFileSetRef2);
        inputSchema.parameters.add(taskIOIN);
        TaskOutputSchema outputSchema = new TaskOutputSchema();
        TaskIO taskIO =  new TaskIO();
        taskIO.name = "TSV_FILE";
        TaskIO.IOFileSetRef outFileSetRef = new TaskIO.IOFileSetRef();
        outFileSetRef.id = "TSV";
        outFileSetRef.versionExactly = "1.0";
        outFileSetRef.maxOccurs = "1";
        outFileSetRef.minOccurs = "1";
        taskIO.fileSetRefs.add(outFileSetRef);
        outputSchema.returnedValues.add(taskIO);
        TaskIO taskIO2 =  new TaskIO();
        TaskIO.IOFileSetRef outFileSetRef2 = new TaskIO.IOFileSetRef();
        outFileSetRef2.id = "TSV_second_format  ";
        outFileSetRef2.versionExactly = "1.5";
        outFileSetRef2.maxOccurs = "unbounded";
        outFileSetRef2.minOccurs = "1";
        taskIO2.fileSetRefs.add(outFileSetRef2);
        outputSchema.returnedValues.add(taskIO2);

        TaskConfig task = new TaskConfig();
        task.setInputSchema(inputSchema);
        task.setOutputSchema(outputSchema);
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
        assertNotNull("deserializedTask cannot be null", deserializedTask);
        assertEquals("Task ID is not the one expected", "RNASELECT_TASK", deserializedTask.getId());
        assertNotNull("InputSchema cannot be null", deserializedTask.inputSchema);
        assertNotNull("OutputSchema cannot be null", deserializedTask.outputSchema);
        assertEquals("There must be 1 parameter", 2, deserializedTask.inputSchema.parameters.size());
        assertEquals("There must be 2 returned values", 2, deserializedTask.outputSchema.returnedValues.size());

    }
}
