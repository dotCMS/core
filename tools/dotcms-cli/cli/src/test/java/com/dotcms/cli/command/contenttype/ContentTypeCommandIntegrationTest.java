package com.dotcms.cli.command.contenttype;

import static org.junit.jupiter.api.Assertions.fail;

import com.dotcms.api.AuthenticationContext;
import com.dotcms.api.provider.ClientObjectMapper;
import com.dotcms.api.provider.YAMLMapperSupplier;
import com.dotcms.cli.command.CommandTest;
import com.dotcms.cli.common.InputOutputFormat;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.model.config.Workspace;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@QuarkusTest
class ContentTypeCommandIntegrationTest extends CommandTest {

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    WorkspaceManager workspaceManager;

    @BeforeEach
    public void setupTest() throws IOException {
        resetServiceProfiles();
        final String user = "admin@dotcms.com";
        final char[] passwd = "admin".toCharArray();
        authenticationContext.login(user, passwd);
    }

    /**
     * Pull single CT by varName
     */
    @Test
    void Test_Command_Content_Type_Pull_Option() throws IOException {
        final Workspace workspace = workspaceManager.getOrCreate();
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePull.NAME,
                    "fileAsset", "--verbose", "--workspace", workspace.root().toString());
            Assertions.assertEquals(ExitCode.OK, status);
            final String output = writer.toString();
            //System.out.println(output);
            final ObjectMapper objectMapper = new ClientObjectMapper().getContext(null);
            final ContentType contentType = objectMapper.readValue(output, ContentType.class);
            Assertions.assertNotNull(contentType.variable());
            //  System.out.println(workspace);
        } finally {
            workspaceManager.destroy(workspace);
        }
    }

    @Test
    void Test_Command_Content_Type_Pull_Then_Push_YML() throws IOException {
        final Workspace workspace = workspaceManager.getOrCreate();
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String contentTypeVarName = "FileAsset";
            int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePull.NAME,
                    contentTypeVarName, "--format", "YML", "--workspace",
                    workspace.root().toString());
            Assertions.assertEquals(ExitCode.OK, status);
            final String output = writer.toString();
            System.out.println(output);
            try {
                final String fileName = String.format("%s.yml", contentTypeVarName);
                final Path path = Path.of(workspace.contentTypes().toString(), fileName);
                Assert.assertTrue(Files.exists(path));
                byte[] bytes = Files.readAllBytes(path);
                final ObjectMapper objectMapper = new YAMLMapperSupplier().get();
                final ContentType contentType = objectMapper.readValue(bytes, ContentType.class);
                Assertions.assertNotNull(contentType.variable());
                status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                        fileName, "--format", "YML", "--workspace", workspace.root().toString());
                Assertions.assertEquals(ExitCode.OK, status);
            } finally {
                workspaceManager.destroy(workspace);
            }
        }
    }

    /**
     * <b>Command to test:</b> content-type pull <br>
     * <b>Given Scenario:</b> Checks if the JSON content type
     * file has a "dotCMSObjectType" field with the value "ContentType". <br>
     * <b>Expected Result:</b> The JSON content type file should have a
     * "dotCMSObjectType" field with the value "ContentType".
     *
     * @throws IOException if there is an error reading the JSON content type file
     */
    @Test
    void Test_Command_Content_Type_Pull_Checking_JSON_DotCMS_Type() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();

        final String contentTypeVarName = "FileAsset";

        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);

            int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePull.NAME,
                    contentTypeVarName,
                    "--workspace", workspace.root().toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // Reading the JSON content type file to check if the json has a: "dotCMSObjectType" : "ContentType"
            final var contentTypeFilePath = Path.of(workspace.contentTypes().toString(),
                    contentTypeVarName + ".json");
            var json = Files.readString(contentTypeFilePath);
            Assertions.assertTrue(json.contains("\"dotCMSObjectType\" : \"ContentType\""));

            // And now pushing the content type back to the server to make sure the structure is still correct
            status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    contentTypeFilePath.toAbsolutePath().toString());
            Assertions.assertEquals(ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * <b>Command to test:</b> content-type pull <br>
     * <b>Given Scenario:</b> Checks if the YAML content type
     * file has a "dotCMSObjectType" field with the value "ContentType". <br>
     * <b>Expected Result:</b> The YAML content type file should have a
     * "dotCMSObjectType" field with the value "ContentType".
     *
     * @throws IOException if there is an error reading the YAML content type file
     */
    @Test
    void Test_Command_Content_Type_Pull_Checking_YAML_DotCMS_Type() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();

        final String contentTypeVarName = "FileAsset";

        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);

            int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePull.NAME,
                    contentTypeVarName,
                    "-fmt", InputOutputFormat.YAML.toString(), "--workspace",
                    workspace.root().toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // Reading the YAML content type file to check if the yaml has a: "dotCMSObjectType" : "ContentType"
            final var contentTypeFilePath = Path.of(workspace.contentTypes().toString(),
                    contentTypeVarName + ".yml");
            var json = Files.readString(contentTypeFilePath);
            Assertions.assertTrue(json.contains("dotCMSObjectType: \"ContentType\""));

            // And now pushing the content type back to the server to make sure the structure is still correct
            status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    contentTypeFilePath.toAbsolutePath().toString(), "-fmt",
                    InputOutputFormat.YAML.toString());
            Assertions.assertEquals(ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * List all CT
     */
    @Test
    void Test_Command_Content_List_Option() {
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypeFind.NAME,
                    "--interactive=false");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.startsWith("varName:"));
        }
    }

    /**
     * Test Filter options
     */
    @Test
    void Test_Command_Content_Filter_Option() {
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypeFind.NAME,
                    "--name", "FileAsset", "--page", "0", "--pageSize", "10");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.startsWith("varName:"));
        }
    }

    /**
     * Push CT from a file
     *
     * @throws IOException
     */
    @Test
    void Test_Push_New_Content_Type_From_File_Then_Remove() throws IOException {
        final long identifier = System.currentTimeMillis();

        final String varName = "__var__" + identifier;

        final ImmutableSimpleContentType contentType = ImmutableSimpleContentType.builder()
                .baseType(BaseContentType.CONTENT)
                .description("ct for testing.")
                .name("name")
                .variable(varName)
                .modDate(new Date())
                .fixed(true)
                .iDate(new Date())
                .host("SYSTEM_HOST")
                .folder("SYSTEM_FOLDER")
                .addFields(
                        ImmutableBinaryField.builder()
                                .name("__bin_var__" + identifier)
                                .fixed(false)
                                .listed(true)
                                .searchable(true)
                                .unique(false)
                                .indexed(true)
                                .readOnly(false)
                                .forceIncludeInApi(false)
                                .modDate(new Date())
                                .required(false)
                                .variable("lol")
                                .sortOrder(1)
                                .dataType(DataTypes.SYSTEM).build()
                ).build();
        final ObjectMapper objectMapper = new ClientObjectMapper().getContext(null);
        final String asString = objectMapper.writeValueAsString(contentType);
        System.out.println(asString);
        final File jsonFile = File.createTempFile("temp", ".json");
        Files.writeString(jsonFile.toPath(), asString);
        final Workspace workspace = workspaceManager.getOrCreate();
        try {
            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(ContentTypeCommand.NAME,
                        ContentTypePush.NAME,
                        jsonFile.getAbsolutePath(), "--workspace", workspace.root().toString());
                Assertions.assertEquals(ExitCode.OK, status);
                final String output = writer.toString();
                System.out.println(output);
            }

            final int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypeRemove.NAME,
                    varName,
                    "--cli-test", "--workspace", workspace.root().toString());
            Assertions.assertEquals(ExitCode.OK, status);

            //A simple Thread.sleep() could do it too but Sonar strongly recommends we stay away from doing that.
            int count = 0;
            while (ExitCode.SOFTWARE != commandLine.execute(ContentTypeCommand.NAME,
                    ContentTypePull.NAME,
                    varName, "--workspace", workspace.root().toString())) {
                System.out.println("Waiting for content type to be removed");
                count++;
                if (count > 10) {
                    fail("Content type was not removed");
                }
            }
        } finally {
            workspaceManager.destroy(workspace);
        }
    }

    /**
     * Given scenario: Despite the number of times the same content type is pulled, it should only
     * be created once Expected result: The WorkspaceManager should be able to create and destroy a
     * workspace
     *
     * @throws IOException
     */
    @Test
    void Test_Pull_Same_Content_Type_Multiple_Times() throws IOException {
        final Workspace workspace = workspaceManager.getOrCreate();
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String contentTypeVarName = "Image";
            for (int i = 0; i <= 5; i++) {
                int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePull.NAME,
                        contentTypeVarName, "--workspace", workspace.root().toString());
                Assertions.assertEquals(ExitCode.OK, status);
                System.out.println("CT Pulled: " + i);
            }

            final String fileName = String.format("%s.json", contentTypeVarName);
            final Path path = Path.of(workspace.contentTypes().toString(), fileName);
            Assert.assertTrue(Files.exists(path));

            try (Stream<Path> walk = Files.walk(workspace.contentTypes())) {
                long count = walk.filter(p -> Files.isRegularFile(p) && p.getFileName().toString()
                        .startsWith(contentTypeVarName)).count();
                Assertions.assertEquals(1, count);
            }

        } finally {
            workspaceManager.destroy(workspace);
        }
    }

    /**
     * Creates a temporary folder with a random name.
     *
     * @return The path to the created temporary folder.
     * @throws IOException If an I/O error occurs while creating the temporary folder.
     */
    private Path createTempFolder() throws IOException {

        String randomFolderName = "folder-" + UUID.randomUUID();
        return Files.createTempDirectory(randomFolderName);
    }

    /**
     * Deletes a temporary directory and all its contents.
     *
     * @param folderPath The path to the temporary directory to be deleted.
     * @throws IOException If an I/O error occurs while deleting the directory or its contents.
     */
    private void deleteTempDirectory(Path folderPath) throws IOException {
        Files.walkFileTree(folderPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file); // Deletes the file
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                Files.delete(dir); // Deletes the directory after its content has been deleted
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Test
    void TestFailureOnPurpose() throws IOException {
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypeFind.NAME,
                    "--interactive=false");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.startsWith("LOL:"));
        }
    }

}
