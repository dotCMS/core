package com.dotmarketing.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Map;
import org.junit.Test;

/**
 * Unit tests for DockerSecretUtil class
 * @author nollymar
 */
public class DockerSecretsUtilTest {

    /**
     * Method to test: {@link DockerSecretsUtil#loadFromFile(File)}
     * Test case: The file does not exist
     * Expected result: It should fail with a DotRuntimeException
     */
    @Test(expected = DotRuntimeException.class)
    public void testLoadFromFileWhenFileDoesNotExist(){

        final File fakeFile = new File("fakeFile");
        DockerSecretsUtil.loadFromFile(fakeFile);
    }

    /**
     * Method to test: {@link DockerSecretsUtil#load(File)}
     * Test case: The file does not exist
     * Expected result: It should fail with a DotRuntimeException
     */
    @Test(expected = DotRuntimeException.class)
    public void testLoadWhenFileDoesNotExist(){

        final File fakeFile = new File("fakeFile");
        DockerSecretsUtil.load(fakeFile);
    }

    /**
     * Method to test: {@link DockerSecretsUtil#loadFromFile(File)}
     * Test case: The file path does not exist
     * Expected result: It should fail with a DotRuntimeException
     */
    @Test(expected = DotRuntimeException.class)
    public void testLoadFromFileWhenFilePathDoesNotExist(){
        DockerSecretsUtil.loadFromFile("fakeFile");
    }

    /**
     * Method to test: {@link DockerSecretsUtil#loadFromFile(File)}
     * Test case: The file with secrets is malformed
     * Expected result: It should fail with a DotRuntimeException
     */
    @Test(expected = DotRuntimeException.class)
    public void testLoadFromFileWithInvalidSecrets() throws IOException {
        final File file = createTempFile(
                "MyKey1=MyValue1\n" +
                        "MyKey2MyValue2\n", null);

        DockerSecretsUtil.loadFromFile(file);
    }

    /**
     * Method to test: {@link DockerSecretsUtil#loadFromFile(File)}
     * Test case: The method is called using a file with valid secrets
     * Expected result: Success
     */
    @Test
    public void testLoadFromFileWithValidSecrets() throws IOException {
        final File file = createTempFile(
                "MyKey1=MyValue1\n" +
                        "MyKey2=MyValue2\n", null);

        final Map<String, String> result = DockerSecretsUtil.loadFromFile(file);

        assertTrue(UtilMethods.isSet(result));

        assertEquals(2, result.size());
        assertEquals("MyValue1", result.get("MyKey1"));
        assertEquals("MyValue2", result.get("MyKey2"));
    }

    /**
     * Method to test: {@link DockerSecretsUtil#load(File)}
     * Test case: The directory is empty
     * Expected result: It should fail with a DotRuntimeException
     */
    @Test(expected = DotRuntimeException.class)
    public void testLoadWithAnEmptyDir() throws IOException {
        final Path emptyDir = createTempDir();

        DockerSecretsUtil.load(emptyDir.toFile());
    }

    /**
     * Method to test: {@link DockerSecretsUtil#load(File)}
     * Test case: The method is called using a directory with valid secrets
     * Expected result: Success
     */
    @Test
    public void testLoadWithValidSecrets() throws IOException {
        final File secretsDir = createTempDir().toFile();

        final File secrets1 = createTempFile("MyValue1", secretsDir);

        final File secrets2 = createTempFile("MyValue2", secretsDir);

        final Map<String, String> result = DockerSecretsUtil.load(secretsDir);

        assertEquals(2, result.size());
        assertEquals("MyValue1", result.get(secrets1.getName()));
        assertEquals("MyValue2", result.get(secrets2.getName()));
    }

    private Path createTempDir() throws IOException {
        return Files.createTempDirectory("TestDockerSecretsDir_");
    }

    /**
     * Creates a temporal file using a given content and an optional directory
     * @param content
     * @param directory
     * @return
     * @throws IOException
     */
    private File createTempFile(final String content, final File directory) throws IOException {

        final File tempTestFile = File
                .createTempFile("TestDockerSecrets_" + new Date().getTime(), ".txt", directory);
        FileUtils.writeStringToFile(tempTestFile, content);

        return tempTestFile;
    }

}
