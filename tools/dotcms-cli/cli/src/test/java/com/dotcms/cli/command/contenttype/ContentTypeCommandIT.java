package com.dotcms.cli.command.contenttype;

import static org.junit.jupiter.api.Assertions.fail;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.AuthenticationContext;
import com.dotcms.api.ContentTypeAPI;
import com.dotcms.api.client.MapperService;
import com.dotcms.api.client.model.RestClientFactory;
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
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.config.Workspace;
import com.dotcms.model.contenttype.AbstractSaveContentTypeRequest;
import com.dotcms.model.contenttype.SaveContentTypeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class ContentTypeCommandIT extends CommandTest {

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    WorkspaceManager workspaceManager;

    @Inject
    RestClientFactory clientFactory;

    @Inject
    MapperService mapperService;

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

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePull.NAME,
                    "fileAsset", "--verbose", "--workspace", workspace.root().toString());
            Assertions.assertEquals(ExitCode.OK, status);

            // Reading the resulting JSON file
            final var contentTypeFilePath = Path.of(workspace.contentTypes().toString(),
                    "FileAsset.json");
            Assertions.assertTrue(Files.exists(contentTypeFilePath));

            // Validating it is a valid content type descriptor
            var json = Files.readString(contentTypeFilePath);
            final ObjectMapper objectMapper = new ClientObjectMapper().getContext(null);
            final ContentType contentType = objectMapper.readValue(json, ContentType.class);
            Assertions.assertNotNull(contentType.variable());
        } finally {
            workspaceManager.destroy(workspace);
        }
    }

    @Test
    void Test_Command_Content_Type_Pull_Then_Push_YML() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

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
                final String content = new String(bytes);
                //Verify that the pulled CT does not come with the generated workflow array
                //Because this array is only intended to be consumed by the endpoint that updates the CT
                Pattern pattern = Pattern.compile("\"workflow\"\\s*:\\s*(\\[.*?\\])");
                Matcher matcher = pattern.matcher(content);
                Assert.assertFalse(matcher.find());
                final ObjectMapper objectMapper = new YAMLMapperSupplier().get();
                final ContentType contentType = objectMapper.readValue(bytes, ContentType.class);
                Assertions.assertNotNull(contentType.variable());
                status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                        path.toAbsolutePath().toString(), "--fail-fast", "-e");
                Assertions.assertEquals(ExitCode.OK, status);
            } finally {
                workspaceManager.destroy(workspace);
            }
        }
    }

    /**
     * here we're validating that when updating a CT that has associated Workflows
     * It still retains that information after an update op is performed
     * Given scenario: We pull FileAsset save it locally as a file then we push it back in via Push command
     * Expected Result: When the FileAsset is pulled down for the first time it
     * @throws IOException
     */
    @Test
    void Test_Command_Content_Type_Pull_Then_Push_Json() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String contentTypeVarName = "FileAsset";
            int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePull.NAME,
                    contentTypeVarName, "--format", "JSON", "--workspace",
                    workspace.root().toString()
            );
            Assertions.assertEquals(ExitCode.OK, status);
            try {
                final String fileName = String.format("%s.json", contentTypeVarName);
                final Path path = Path.of(workspace.contentTypes().toString(), fileName);
                Assert.assertTrue(Files.exists(path));

                byte[] bytes = Files.readAllBytes(path);
                final String content = new String(bytes);
                //Verify that the pulled CT does not come with the generated workflow array
                //Because this array is only intended to be consumed by the endpoint that updates the CT
                Pattern pattern = Pattern.compile("\"workflow\"\\s*:\\s*(\\[.*?\\])");
                Matcher matcher = pattern.matcher(content);
                Assert.assertFalse(matcher.find());

                final ObjectMapper objectMapper = new ClientObjectMapper().getContext(null);
                final ContentType contentType = objectMapper.readValue(bytes, ContentType.class);
                Assertions.assertNotNull(contentType.variable());
                Assertions.assertFalse(
                        Objects.requireNonNull(contentType.workflows(),
                                "We're expecting to comeback with workflows" ).isEmpty(),
                        "We're expecting the CT to have a workflow already assigned."
                );

                writer.getBuffer().setLength(0); //Clear output so we can get access to the new results only

                status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                        path.toAbsolutePath().toString(), "--fail-fast", "-e"
                );
                Assertions.assertEquals(ExitCode.OK, status);

                Files.delete(path);

                //Now request the CT once again to verify we still get consistent data in the workflow department
                status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePull.NAME,
                        contentTypeVarName, "--format", "JSON", "--workspace",
                        workspace.root().toString());
                Assertions.assertEquals(ExitCode.OK, status);

                //Re verify the contentlet
                Assert.assertTrue(Files.exists(path));

                byte[]  updatedBytes = Files.readAllBytes(path);
                final ContentType updatedContentType = objectMapper.readValue(updatedBytes, ContentType.class);
                Assertions.assertNotNull(updatedContentType.variable());

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
                    contentTypeFilePath.toAbsolutePath().toString(), "--fail-fast", "-e");
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
                    contentTypeFilePath.toAbsolutePath().toString(), "--fail-fast", "-e");
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
                    "--non-interactive");
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
    @Disabled("This test is index dependent therefore there's a chance to see it fail from time to time")
    @Test
    void Test_Push_New_Content_Type_From_File_Then_Remove() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

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

        final Path path = Path.of(workspace.contentTypes().toString(), "temp.json");
        Files.writeString(path, asString);
        try {
            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(ContentTypeCommand.NAME,
                        ContentTypePush.NAME, path.toAbsolutePath().toString(), "--fail-fast",
                        "-e");
                Assertions.assertEquals(ExitCode.OK, status);
                final String output = writer.toString();
                System.out.println(output);
            }

            final int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypeRemove.NAME,
                    varName, "--cli-test");
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
     * This tests will test the functionality of the content type push command when pushing a
     * folder, checking the content types are properly add, updated and removed on the remote
     * server.
     */
    @Test
    void Test_Command_Content_Type_Folder_Push() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);

            final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);

            // ╔══════════════════════╗
            // ║  Preparing the data  ║
            // ╚══════════════════════╝

            // --
            // Pulling all the existing content types created in other tests to avoid unwanted deletes
            var contentTypesResponse = contentTypeAPI.getContentTypes(
                    null,
                    1,
                    1000,
                    "variable",
                    null,
                    null,
                    null
            );
            var pullCount = 0;
            if (contentTypesResponse != null && contentTypesResponse.entity() != null) {
                for (ContentType contentType : contentTypesResponse.entity()) {
                    var status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePull.NAME,
                            contentType.variable(), "--workspace", workspace.root().toString());
                    Assertions.assertEquals(CommandLine.ExitCode.OK, status);
                    pullCount++;
                }
            }

            // ---
            // Creating a some test content types in the server
            var newContentType1 = createContentType(workspace, false);
            var newContentType2 = createContentType(workspace, false);
            var newContentType3 = createContentType(workspace, false);

            // ---
            // Pulling the just created content types - We need the files in the content types folder
            // - Ignoring 1 content type, in that way we can force a remove
            int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePull.NAME,
                    newContentType1, "--workspace", workspace.root().toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePull.NAME,
                    newContentType2, "--workspace", workspace.root().toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ---
            // Renaming the content type in order to force an update in the push
            final var newNewContentType2Path = Path.of(workspace.contentTypes().toString(),
                    newContentType2 + ".json");
            var mappedContentType2 = this.mapperService.map(
                    newNewContentType2Path.toFile(),
                    ContentType.class
            );
            var jsonContent = this.mapperService.objectMapper(newNewContentType2Path.toFile())
                    .writeValueAsString(mappedContentType2);
            jsonContent = jsonContent.replace("name-" + newContentType2,
                    "name-" + newContentType2 + "-renamed");
            Files.write(newNewContentType2Path, jsonContent.getBytes());

            // ---
            // Creating some content types to fire some additions
            var newContentType4 = createContentType(workspace, true);
            var newContentType5 = createContentType(workspace, true);

            // Make sure we have the proper amount of files in the content types folder
            try (Stream<Path> walk = Files.walk(workspace.contentTypes())) {
                long count = walk.filter(Files::isRegularFile).count();
                Assertions.assertEquals(4 + pullCount, count);
            }

            // ╔═══════════════════════╗
            // ║  Pushing the changes  ║
            // ╚═══════════════════════╝
            status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--removeContentTypes", "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(
                    output.contains(
                            "Push Data: [4] ContentTypes to push: (2 New - 1 Modified) - 1 to Delete"
                    )
            );

            // ╔══════════════════════════════╗
            // ║  Validating the information  ║
            // ╚══════════════════════════════╝
            var byVarName = contentTypeAPI.getContentType(newContentType1, 1L, false);
            Assertions.assertEquals(newContentType1, byVarName.entity().variable());

            byVarName = contentTypeAPI.getContentType(newContentType2, 1L, false);
            Assertions.assertEquals(newContentType2, byVarName.entity().variable());
            Assertions.assertEquals("name-" + newContentType2 + "-renamed",
                    byVarName.entity().name());

            try {
                contentTypeAPI.getContentType(newContentType3, 1L, false);
                Assertions.fail(" 404 Exception should have been thrown here.");
            } catch (Exception e) {
                Assertions.assertTrue(e instanceof NotFoundException);
            }

            byVarName = contentTypeAPI.getContentType(newContentType4, 1L, false);
            Assertions.assertEquals(newContentType4, byVarName.entity().variable());

            byVarName = contentTypeAPI.getContentType(newContentType5, 1L, false);
            Assertions.assertEquals(newContentType5, byVarName.entity().variable());

        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * This method creates a content type in the workspace either as a file or by making an API
     * call
     *
     * @param workspace The workspace where the content type should be created
     * @param asFile    Determines whether the content type should be created as a file or API call
     * @return The variable name of the created content type
     * @throws IOException If an I/O error occurs while creating the content type file
     */
    private String createContentType(Workspace workspace, boolean asFile) throws IOException {

        final long identifier = System.currentTimeMillis();
        final String varName = "var_" + identifier;

        final ImmutableSimpleContentType contentType = ImmutableSimpleContentType.builder()
                .baseType(BaseContentType.CONTENT)
                .description("ct for testing.")
                .name("name-" + varName)
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

        if (!asFile) {

            final SaveContentTypeRequest saveRequest = AbstractSaveContentTypeRequest.builder()
                    .of(contentType).build();

            final ContentTypeAPI contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);
            final ResponseEntityView<List<ContentType>> response = contentTypeAPI.createContentTypes(
                    List.of(saveRequest));
        } else {

            final ObjectMapper objectMapper = new ClientObjectMapper().getContext(null);
            final String asString = objectMapper.writeValueAsString(contentType);

            final Path path = Path.of(workspace.contentTypes().toString(),
                    String.format("%s.json", varName));
            Files.writeString(path, asString);
        }

        return varName;
    }

    /**
     * <b>Command to test:</b> content type pull <br>
     * <b>Given Scenario:</b> Test the content type pull command. This test pulls all the content
     * types in the default format (JSON). <br>
     * <b>Expected Result:</b> All the existing content types should be pulled and saved as JSON
     * files.
     *
     * @throws IOException if there is an error pulling the content types
     */
    @Test
    void Test_Command_Content_Type_Pull_Pull_All_Default_Format() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();

        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        // First we need to see if we already have content types to pull
        final var contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);

        // --
        // Pulling all the existing content types to have a proper count
        var contentTypesResponse = contentTypeAPI.getContentTypes(
                null,
                1,
                1000,
                "variable",
                null,
                null,
                null
        );
        var contentTypesCount = 0;
        if (contentTypesResponse != null && contentTypesResponse.entity() != null) {
            contentTypesCount = contentTypesResponse.entity().size();
        }

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            // ---
            // ---
            // Creating a some test content types in the server
            createContentType(workspace, false);
            contentTypesCount++;
            createContentType(workspace, false);
            contentTypesCount++;
            createContentType(workspace, false);
            contentTypesCount++;

            // Pulling all content types
            var status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePull.NAME,
                    "--workspace", workspace.root().toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // Make sure we have the proper amount of JSON files in the content types folder
            try (Stream<Path> walk = Files.walk(workspace.contentTypes())) {

                var jsonFiles = walk.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".json"))
                        .collect(Collectors.toList());

                // Check the count first
                Assertions.assertEquals(contentTypesCount, jsonFiles.size(),
                        "The number of JSON files does not match the expected content types count.");

                // Now check that none of the JSON files are empty
                for (Path jsonFile : jsonFiles) {
                    long fileSize = Files.size(jsonFile);
                    Assertions.assertTrue(fileSize > 0,
                            "JSON file " + jsonFile + " is empty.");
                }
            }

        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * <b>Command to test:</b> content type pull <br>
     * <b>Given Scenario:</b> Test the content type pull command. This test pulls all the content
     * types in the YAML format. <br>
     * <b>Expected Result:</b> All the existing content types should be pulled and saved as YAML
     * files.
     *
     * @throws IOException if there is an error pulling the content types
     */
    @Test
    @Order(13)
    void Test_Command_Content_Type_Pull_Pull_All_YAML_Format() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();

        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        // First we need to see if we already have contentTypes to pull
        final var contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);

        // --
        // Pulling all the existing content types to have a proper count
        var contentTypesResponse = contentTypeAPI.getContentTypes(
                null,
                1,
                1000,
                "variable",
                null,
                null,
                null
        );
        var contentTypesCount = 0;
        if (contentTypesResponse != null && contentTypesResponse.entity() != null) {
            contentTypesCount = contentTypesResponse.entity().size();
        }

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            // ---
            // Creating a some test content types in the server
            createContentType(workspace, false);
            contentTypesCount++;
            createContentType(workspace, false);
            contentTypesCount++;
            createContentType(workspace, false);
            contentTypesCount++;

            // Pulling all content types
            var status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePull.NAME,
                    "--workspace", workspace.root().toString(),
                    "-fmt", InputOutputFormat.YAML.toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // Make sure we have the proper amount of JSON files in the content types folder
            try (Stream<Path> walk = Files.walk(workspace.contentTypes())) {

                var files = walk.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".yml"))
                        .collect(Collectors.toList());

                // Check the count first
                Assertions.assertEquals(contentTypesCount, files.size(),
                        "The number of YAML files does not match the expected content types count.");

                // Now check that none of the JSON files are empty
                for (Path file : files) {
                    long fileSize = Files.size(file);
                    Assertions.assertTrue(fileSize > 0,
                            "YAML file " + file + " is empty.");
                }
            }

        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * <b>Command to test:</b> content type pull <br>
     * <b>Given Scenario:</b> Test the content type pull command. This test pulls all the content
     * types twice, testing the override works properly.<br>
     * <b>Expected Result:</b> All the existing content types should be pulled and saved as YAML
     * files.
     *
     * @throws IOException if there is an error pulling the content types
     */
    @Test
    @Order(14)
    void Test_Command_Content_Type_Pull_Pull_All_Twice() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();

        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        // First we need to see if we already have content types to pull
        final var contentTypeAPI = clientFactory.getClient(ContentTypeAPI.class);

        // --
        // Pulling all the existing content types to have a proper count
        var contentTypesResponse = contentTypeAPI.getContentTypes(
                null,
                1,
                1000,
                "variable",
                null,
                null,
                null
        );
        var contentTypesCount = 0;
        if (contentTypesResponse != null && contentTypesResponse.entity() != null) {
            contentTypesCount = contentTypesResponse.entity().size();
        }

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            // ---
            // Creating a some test content types in the server
            createContentType(workspace, false);
            contentTypesCount++;
            createContentType(workspace, false);
            contentTypesCount++;
            createContentType(workspace, false);
            contentTypesCount++;

            // Pulling all content types
            var status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePull.NAME,
                    "--workspace", workspace.root().toString(),
                    "-fmt", InputOutputFormat.YAML.toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // Executing a second pull of all the content types
            status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePull.NAME,
                    "--workspace", workspace.root().toString(),
                    "-fmt", InputOutputFormat.YAML.toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // Make sure we have the proper amount of JSON files in the content types folder
            try (Stream<Path> walk = Files.walk(workspace.contentTypes())) {

                var files = walk.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".yml"))
                        .collect(Collectors.toList());

                // Check the count first
                Assertions.assertEquals(contentTypesCount, files.size(),
                        "The number of YAML files does not match the expected content types count.");

                // Now check that none of the JSON files are empty
                for (Path file : files) {
                    long fileSize = Files.size(file);
                    Assertions.assertTrue(fileSize > 0,
                            "YAML file " + file + " is empty.");
                }
            }

        } finally {
            deleteTempDirectory(tempFolder);
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

}
