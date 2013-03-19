package org.campagnelab.gobyweb.plugins.xml.filesets;

import org.campagnelab.gobyweb.plugins.xml.common.Attribute;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.Assert.*;


import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

/**
 * Tester for {@link org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig}
 * @author manuele
 * @version  1.0
 * Date: 2/7/13
 * Time: 12:00 AM
 *
 */
@RunWith(JUnit4.class)
public class FileSetConfigTest {

    private static FileSetConfig fileSetConfig;
    private static String serializedSet;
    private static FileSetConfig deserializedSet;

    @Test
    public void marshall() {

        Marshaller marshaller=null;
        try {
            JAXBContext context = JAXBContext.newInstance(FileSetConfig.class);
            marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        } catch (Exception e) {
            e.printStackTrace();
            fail("failed at creating JAXB mashaller");
        }
        assertNotNull("Marshaller cannot be null", marshaller);
        List<FileSetConfig.ComponentSelector> files = new ArrayList<FileSetConfig.ComponentSelector>();
        files.add(new FileSetConfig.ComponentSelector("READS", "*.compacts-reads", true, "basename"));
        List<FileSetConfig.ComponentSelector> dirs = new ArrayList<FileSetConfig.ComponentSelector>();
        dirs.add(new FileSetConfig.ComponentSelector("Test-DIR", "*/**", true, "basename"));
        dirs.add(new FileSetConfig.ComponentSelector("Test-Another-DIR", "*/**", false, ""));
        List<Attribute> attributes = new ArrayList<Attribute>();
        Attribute attribute = new Attribute();
        attribute.name = "attr1";
        attribute.value = "value1";
        attributes.add(attribute);
        Attribute attribute2 = new Attribute();
        attribute2.name = "attr2";
        attribute2.value = "value2";
        attributes.add(attribute2);
        fileSetConfig = new FileSetConfig("COMPACT_READS");
        fileSetConfig.setName("NAME");
        fileSetConfig.setVersion("1.21");
        fileSetConfig.setHelp("help string");
        fileSetConfig.setAttributes(attributes);
        fileSetConfig.setFileSelectors(files);
        fileSetConfig.setDirSelectors(dirs);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            marshaller.marshal(fileSetConfig, outputStream);
            serializedSet = new String(outputStream.toByteArray(), "UTF-8");
            System.out.println(serializedSet);
        } catch (Exception e) {
            e.printStackTrace();
            fail("failed at marshalling FileSetConfig");
        }
    }


    @Test
    public void unmarshall() {
        Unmarshaller unmarshaller = null;
        try {
            JAXBContext context = JAXBContext.newInstance(FileSetConfig.class);
            unmarshaller = context.createUnmarshaller();
        } catch (Exception e) {
            fail("failed at creating JAXB unmarshaller");
        }
        assertNotNull("Unmarshaller cannot be null", unmarshaller);
        System.out.println(serializedSet);
        try {
            deserializedSet = (FileSetConfig) unmarshaller.unmarshal(new StreamSource(new StringReader(serializedSet)));
        } catch (JAXBException e) {
            e.printStackTrace();
            fail("Failed at unmarshalling FileSetConfig");
        }
        System.out.println("Unmarshalled ID " + deserializedSet.getId());
        System.out.println("Unmarshalled values ");
        for (FileSetConfig.ComponentSelector component : deserializedSet.getFileSelectors())
            System.out.println("\tFile: Id " + component.id + ", pattern " + component.pattern +", mandatory " + component.mandatory);

        for (FileSetConfig.ComponentSelector component : deserializedSet.getDirSelectors())
            System.out.println("\tDir: Id " + component.id + ", pattern " + component.pattern +", mandatory " + component.mandatory);

        for (Attribute attribute : deserializedSet.getAttributes()) {
            System.out.println("\tAttribute: name " + attribute.name + ", value " + attribute.value);

        }
    }

}
