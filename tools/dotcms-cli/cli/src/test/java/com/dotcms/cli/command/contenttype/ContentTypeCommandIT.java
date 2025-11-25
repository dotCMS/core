package com.dotcms.cli.command.contenttype;

import static com.dotcms.cli.common.ContentTypesTestHelperService.SYSTEM_WORKFLOW_ID;
import static com.dotcms.cli.common.ContentTypesTestHelperService.SYSTEM_WORKFLOW_VARIABLE_NAME;
import static org.junit.jupiter.api.Assertions.fail;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.AuthenticationContext;
import com.dotcms.api.ContentTypeAPI;
import com.dotcms.api.client.MapperService;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.provider.ClientObjectMapper;
import com.dotcms.api.provider.YAMLMapperSupplier;
import com.dotcms.cli.command.CommandTest;
import com.dotcms.cli.common.ContentTypesTestHelperService;
import com.dotcms.cli.common.ContentsTestHelperService;
import com.dotcms.cli.common.InputOutputFormat;
import com.dotcms.cli.common.SitesTestHelperService;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.workflow.ImmutableWorkflow;
import com.dotcms.model.config.Workspace;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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

    @Inject
    SitesTestHelperService sitesTestHelper;

    @Inject
    ContentsTestHelperService contentsTestHelper;

    @Inject
    ContentTypesTestHelperService contentTypesTestHelper;

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
            Assertions.assertTrue(output.startsWith("variable:"));
        }
    }

    /**
     * Given scenario: We want to filter the content types by name
     * Expected result: The output should contain only the content types that match the filter
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
            Assertions.assertTrue(output.startsWith("variable: [FileAsset]"));
        }
    }

    /**
     * Given scenario: We want to filter the content types by var name
     * Expected result: The output should come back ordered by varName and direction ASC
     */
    @Test
    void Test_Command_Content_Order_By_Variable_Ascending() {
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypeFind.NAME,
                     "--page", "0", "--pageSize", "3", "--order", "variable", "--direction", "ASC");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            final List<String> strings = extractRowsByFieldName("variable",output);
            Assertions.assertEquals( 3, strings.size());
            Assertions.assertTrue(isSortedAsc(strings),()->"The strings: "+strings);
        }
    }

    /**
     * Given scenario: We want to filter the content types by var name and direction asc
     * Expected result: The output should come back ordered by varName and direction ASC
     */
    @Test
    void Test_Command_Content_Order_By_Variable_Ascending_Lower_Case() {
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypeFind.NAME,
                    "--page", "0", "--pageSize", "3", "--order", "variable", "--direction", "asc");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            final List<String> strings = extractRowsByFieldName("variable",output);
            Assertions.assertEquals( 3, strings.size());
            Assertions.assertTrue(isSortedAsc(strings),()->"The strings: "+strings);
        }
    }

    /**
     * Given scenario: We want to filter the content types by var name
     * Expected result: The output should come back ordered by varName and direction DESC
     */
    @Test
    void Test_Command_Content_Order_By_Variable_Descending() {
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypeFind.NAME,
                    "--page", "0", "--pageSize", "3", "--order", "variable", "--direction", "DESC");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            final List<String> strings = extractRowsByFieldName("variable",output);
            Assertions.assertEquals( 3, strings.size());
            Assertions.assertTrue(isSortedDesc(strings),()->"The strings: "+strings);
        }
    }

    /**
     * Given scenario: We want to filter the content types by var name and direction desc
     * Expected result: The output should come back ordered by varName and direction DESC
     */
    @Test
    void Test_Command_Content_Order_By_Variable_Descending_Lower_Case() {
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypeFind.NAME,
                    "--page", "0", "--pageSize", "3", "--order", "variable", "--direction", "desc");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            final List<String> strings = extractRowsByFieldName("variable",output);
            Assertions.assertEquals( 3, strings.size());
            Assertions.assertTrue(isSortedDesc(strings),()->"The strings: "+strings);
        }
    }

    /**
     * Given scenario: We want to order the content types by modDate
     * Expected result: The output should come back ordered by modDate and direction DESC
     */
    @Test
    void Test_Command_Content_Filter_Order_By_modDate_Descending() {
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypeFind.NAME,
                    "--page", "0", "--pageSize", "3", "--order", "modDate", "--direction", "DESC");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            final List<String> strings = extractRowsByFieldName("modDate",output);
            Assertions.assertEquals( 3, strings.size());
            Assertions.assertTrue(isSortedDesc(strings));
        }
    }


    /**
     * Given scenario: We want to order the content types by modDate
     * Expected result: The output should come back ordered by modDate and direction ASC
     */
    @Test
    void Test_Command_Content_Filter_Order_By_modDate_Ascending() {
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypeFind.NAME,
                    "--page", "0", "--pageSize", "3", "--order", "modDate", "--direction", "ASC");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            final List<String> strings = extractRowsByFieldName("modDate",output);
            Assertions.assertEquals( 3, strings.size());
            Assertions.assertTrue(isSortedAsc(strings));
        }
    }

    /**
     * Push CT from a file
     *
     * @throws IOException
     */
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
        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);
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
     * Given scenario: Pushing a content type with a detail page as URL
     *
     * @throws IOException if there is an error reading the JSON content type file
     */
    @Test
    void Test_Push_Content_Type_With_Detail_Page_With_URL() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            // ╔══════════════════════╗
            // ║  Preparing the data  ║
            // ╚══════════════════════╝

            // Creating a site
            final var newSiteResult = sitesTestHelper.createSiteOnServer();
            // Creating a couple of test pages
            final var newPageResult = contentsTestHelper.createPageOnServer(
                    newSiteResult.identifier());
            // Creating a content type file descriptor
            var detailPageURL = String.format(
                    "//%s%s",
                    newSiteResult.siteName(),
                    newPageResult.url()
            );
            final var newContentTypeResult = contentTypesTestHelper.createContentTypeDescriptorWithDetailData(
                    workspace,
                    detailPageURL, "/{name}");

            // ╔════════════════════════════════════════════════════════════╗
            // ║  Pushing the descriptor for the just created Content Type  ║
            // ╚════════════════════════════════════════════════════════════╝
            var status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ╔══════════════════════════════╗
            // ║  Validating the information  ║
            // ╚══════════════════════════════╝
            var byVarName = contentTypesTestHelper.findContentType(newContentTypeResult.variable());
            Assertions.assertTrue(byVarName.isPresent());
            Assertions.assertEquals(newContentTypeResult.variable(), byVarName.get().variable());
            Assertions.assertEquals(detailPageURL, byVarName.get().detailPage());

            // ---
            // Now validating the auto update updated the content type descriptor
            var updatedContentType = this.mapperService.map(
                    newContentTypeResult.path().toFile(),
                    ContentType.class
            );
            Assertions.assertEquals(detailPageURL, updatedContentType.detailPage());
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Pushing a content type with a detail page as ID
     *
     * @throws IOException if there is an error reading the JSON content type file
     */
    @Test
    void Test_Push_Content_Type_With_Detail_Page_With_ID() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            // ╔══════════════════════╗
            // ║  Preparing the data  ║
            // ╚══════════════════════╝

            // Creating a site
            final var newSiteResult = sitesTestHelper.createSiteOnServer();
            // Creating a couple of test pages
            final var newPageResult = contentsTestHelper.createPageOnServer(
                    newSiteResult.identifier());
            // Creating a content type file descriptor
            final var newContentTypeResult = contentTypesTestHelper.createContentTypeDescriptorWithDetailData(
                    workspace,
                    newPageResult.identifier(), "/{name}");

            // ╔════════════════════════════════════════════════════════════╗
            // ║  Pushing the descriptor for the just created Content Type  ║
            // ╚════════════════════════════════════════════════════════════╝
            var status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ╔══════════════════════════════╗
            // ║  Validating the information  ║
            // ╚══════════════════════════════╝
            var expectedDetailPageURL = String.format(
                    "//%s%s",
                    newSiteResult.siteName(),
                    newPageResult.url()
            );

            var byVarName = contentTypesTestHelper.findContentType(newContentTypeResult.variable());
            Assertions.assertTrue(byVarName.isPresent());
            Assertions.assertEquals(newContentTypeResult.variable(), byVarName.get().variable());
            Assertions.assertEquals(expectedDetailPageURL, byVarName.get().detailPage());

            // ---
            // Now validating the auto update updated the content type descriptor
            var updatedContentType = this.mapperService.map(
                    newContentTypeResult.path().toFile(),
                    ContentType.class
            );
            Assertions.assertEquals(expectedDetailPageURL, updatedContentType.detailPage());
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Testing the Content Type push command modifying the detail page
     *
     * @throws IOException if an I/O error occurs during the execution of the test
     */
    @Test
    void Test_Push_Content_Type_With_Updating_Detail_Page() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            // ╔══════════════════════╗
            // ║  Preparing the data  ║
            // ╚══════════════════════╝

            // Creating a site
            final var newSiteResult = sitesTestHelper.createSiteOnServer();
            // Creating a couple of test pages
            final var newPage1Result = contentsTestHelper.createPageOnServer(
                    newSiteResult.identifier());
            final var newPage2Result = contentsTestHelper.createPageOnServer(
                    newSiteResult.identifier());
            var detailPage1URL = String.format(
                    "//%s%s",
                    newSiteResult.siteName(),
                    newPage1Result.url()
            );
            var detailPage2URL = String.format(
                    "//%s%s",
                    newSiteResult.siteName(),
                    newPage2Result.url()
            );
            // Creating a content type file descriptor
            final var newContentTypeResult = contentTypesTestHelper.createContentTypeDescriptorWithDetailData(
                    workspace,
                    detailPage1URL, "/{name}");

            // ╔════════════════════════════════════════════════════════════╗
            // ║  Pushing the descriptor for the just created Content Type  ║
            // ╚════════════════════════════════════════════════════════════╝
            var status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ╔══════════════════════════════╗
            // ║  Validating the information  ║
            // ╚══════════════════════════════╝
            var byVarName = contentTypesTestHelper.findContentType(newContentTypeResult.variable());
            Assertions.assertTrue(byVarName.isPresent());
            Assertions.assertEquals(newContentTypeResult.variable(), byVarName.get().variable());
            Assertions.assertEquals(detailPage1URL, byVarName.get().detailPage());

            // ---
            // Now validating the auto update updated the content type descriptor
            var updatedContentType = this.mapperService.map(
                    newContentTypeResult.path().toFile(),
                    ContentType.class
            );
            Assertions.assertEquals(detailPage1URL, updatedContentType.detailPage());

            // ╔═════════════════════════════════════════════════╗
            // ║  Modifying the detail page of the content type  ║
            // ╚═════════════════════════════════════════════════╝
            updatedContentType = ImmutableSimpleContentType.builder().from(updatedContentType)
                    .detailPage(detailPage2URL).build();
            var jsonContent = this.mapperService
                    .objectMapper(newContentTypeResult.path().toFile())
                    .writeValueAsString(updatedContentType);
            Files.write(newContentTypeResult.path(), jsonContent.getBytes());

            // ╔══════════════════════════════════════════════════════════════════╗
            // ║  Pushing again the descriptor for the just created Content Type  ║
            // ╚══════════════════════════════════════════════════════════════════╝
            status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ╔══════════════════════════════╗
            // ║  Validating the information  ║
            // ╚══════════════════════════════╝
            byVarName = contentTypesTestHelper.findContentType(newContentTypeResult.variable());
            Assertions.assertTrue(byVarName.isPresent());
            Assertions.assertEquals(newContentTypeResult.variable(), byVarName.get().variable());
            Assertions.assertEquals(detailPage2URL, byVarName.get().detailPage());

            // ---
            // Now validating the auto update updated the content type descriptor
            updatedContentType = this.mapperService.map(
                    newContentTypeResult.path().toFile(),
                    ContentType.class
            );
            Assertions.assertEquals(detailPage2URL, updatedContentType.detailPage());
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Testing the Content Type push command modifying the workflows
     *
     * @throws IOException if an I/O error occurs during the execution of the test
     */
    @Test
    void Test_Push_Content_Type_With_Workflows() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            // ╔══════════════════════╗
            // ║  Preparing the data  ║
            // ╚══════════════════════╝
            // Creating a content type file descriptor
            final var newContentTypeResult = contentTypesTestHelper.createContentTypeDescriptor(
                    workspace
            );

            // ╔════════════════════════════════════════════════════════════╗
            // ║  Pushing the descriptor for the just created Content Type  ║
            // ╚════════════════════════════════════════════════════════════╝
            var status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ╔══════════════════════════════╗
            // ║  Validating the information  ║
            // ╚══════════════════════════════╝
            var byVarName = contentTypesTestHelper.findContentType(newContentTypeResult.variable());
            Assertions.assertTrue(byVarName.isPresent());
            Assertions.assertEquals(newContentTypeResult.variable(), byVarName.get().variable());

            // ---
            // Now validating the auto update updated the content type descriptor
            var updatedContentType = this.mapperService.map(
                    newContentTypeResult.path().toFile(),
                    ContentType.class
            );
            Assertions.assertEquals(
                    byVarName.get().workflows().get(0).id(),
                    updatedContentType.workflows().get(0).id()
            );
            Assertions.assertEquals(
                    byVarName.get().workflows().get(0).variableName(),
                    updatedContentType.workflows().get(0).variableName()
            );

            // ╔══════════════════════════════════════════════════════════════════════════════╗
            // ║  Modifying the workflows of the content type -> With just the variable name  ║
            // ╚══════════════════════════════════════════════════════════════════════════════╝
            updatedContentType = ImmutableSimpleContentType.builder().from(updatedContentType)
                    .workflows(
                            List.of(
                                    ImmutableWorkflow.builder()
                                            .variableName(SYSTEM_WORKFLOW_VARIABLE_NAME)
                                            .build()
                            )
                    ).build();
            var jsonContent = this.mapperService
                    .objectMapper(newContentTypeResult.path().toFile())
                    .writeValueAsString(updatedContentType);
            Files.write(newContentTypeResult.path(), jsonContent.getBytes());

            // ╔════════════════════════════════╗
            // ║  Pushing again the descriptor  ║
            // ╚════════════════════════════════╝
            status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ╔══════════════════════════════╗
            // ║  Validating the information  ║
            // ╚══════════════════════════════╝
            byVarName = contentTypesTestHelper.findContentType(newContentTypeResult.variable());
            Assertions.assertTrue(byVarName.isPresent());
            Assertions.assertEquals(newContentTypeResult.variable(), byVarName.get().variable());

            // ---
            // Now validating the auto update updated the content type descriptor
            updatedContentType = this.mapperService.map(
                    newContentTypeResult.path().toFile(),
                    ContentType.class
            );
            Assertions.assertEquals(
                    byVarName.get().workflows().get(0).id(),
                    updatedContentType.workflows().get(0).id()
            );
            Assertions.assertEquals(
                    byVarName.get().workflows().get(0).variableName(),
                    updatedContentType.workflows().get(0).variableName()
            );

            // ╔═══════════════════════════════════════════════════════════════════╗
            // ║  Modifying the workflows of the content type -> With just the id  ║
            // ╚═══════════════════════════════════════════════════════════════════╝
            updatedContentType = ImmutableSimpleContentType.builder().from(updatedContentType)
                    .workflows(
                            List.of(
                                    ImmutableWorkflow.builder()
                                            .id(SYSTEM_WORKFLOW_ID)
                                            .build()
                            )
                    ).build();
            jsonContent = this.mapperService
                    .objectMapper(newContentTypeResult.path().toFile())
                    .writeValueAsString(updatedContentType);
            Files.write(newContentTypeResult.path(), jsonContent.getBytes());

            // ╔════════════════════════════════╗
            // ║  Pushing again the descriptor  ║
            // ╚════════════════════════════════╝
            status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ╔══════════════════════════════╗
            // ║  Validating the information  ║
            // ╚══════════════════════════════╝
            byVarName = contentTypesTestHelper.findContentType(newContentTypeResult.variable());
            Assertions.assertTrue(byVarName.isPresent());
            Assertions.assertEquals(newContentTypeResult.variable(), byVarName.get().variable());

            // ---
            // Now validating the auto update updated the content type descriptor
            updatedContentType = this.mapperService.map(
                    newContentTypeResult.path().toFile(),
                    ContentType.class
            );
            Assertions.assertEquals(
                    byVarName.get().workflows().get(0).id(),
                    updatedContentType.workflows().get(0).id()
            );
            Assertions.assertEquals(
                    byVarName.get().workflows().get(0).variableName(),
                    updatedContentType.workflows().get(0).variableName()
            );
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Pushing a content type with a detail page with an invalid site
     *
     * @throws IOException if there is an error reading the JSON content type file
     */
    @Test
    void Test_Push_Content_Type_With_Detail_Page_With_Invalid_Site() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            // ╔══════════════════════╗
            // ║  Preparing the data  ║
            // ╚══════════════════════╝

            // Creating a content type file descriptor
            var detailPageURL = String.format(
                    "//%s%s",
                    "not-existing-site.com",
                    "/not-existing-page"
            );
            contentTypesTestHelper.createContentTypeDescriptorWithDetailData(
                    workspace,
                    detailPageURL, "/{name}");

            // ╔════════════════════════════════════════════════════════════╗
            // ║  Pushing the descriptor for the just created Content Type  ║
            // ╚════════════════════════════════════════════════════════════╝
            var status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(ExitCode.SOFTWARE, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Pushing a content type with a detail page with an invalid page
     *
     * @throws IOException if there is an error reading the JSON content type file
     */
    @Test
    void Test_Push_Content_Type_With_Detail_Page_With_Invalid_Page() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            // ╔══════════════════════╗
            // ║  Preparing the data  ║
            // ╚══════════════════════╝

            // Creating a site
            final var newSiteResult = sitesTestHelper.createSiteOnServer();
            // Creating a content type file descriptor
            var detailPageURL = String.format(
                    "//%s%s",
                    newSiteResult.siteName(),
                    "/not-existing-page"
            );
            final var newContentTypeResult = contentTypesTestHelper.createContentTypeDescriptorWithDetailData(
                    workspace,
                    detailPageURL, "/{name}");

            // ╔════════════════════════════════════════════════════════════╗
            // ║  Pushing the descriptor for the just created Content Type  ║
            // ╚════════════════════════════════════════════════════════════╝
            var status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(ExitCode.SOFTWARE, status);
        } finally {
            deleteTempDirectory(tempFolder);
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
            var newContentType1 = contentTypesTestHelper.createContentTypeOnServer();
            var newContentType2 = contentTypesTestHelper.createContentTypeOnServer();
            var newContentType3 = contentTypesTestHelper.createContentTypeOnServer();

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
            var newContentType4 = contentTypesTestHelper.createContentTypeDescriptor(workspace);
            var newContentType5 = contentTypesTestHelper.createContentTypeDescriptor(workspace);

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
                Assertions.assertInstanceOf(NotFoundException.class, e);
            }

            byVarName = contentTypeAPI.getContentType(newContentType4.variable(), 1L, false);
            Assertions.assertEquals(newContentType4.variable(), byVarName.entity().variable());

            byVarName = contentTypeAPI.getContentType(newContentType5.variable(), 1L, false);
            Assertions.assertEquals(newContentType5.variable(), byVarName.entity().variable());

        } finally {
            deleteTempDirectory(tempFolder);
        }
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
            contentTypesTestHelper.createContentTypeOnServer();
            contentTypesCount++;
            contentTypesTestHelper.createContentTypeOnServer();
            contentTypesCount++;
            contentTypesTestHelper.createContentTypeOnServer();
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
            contentTypesTestHelper.createContentTypeOnServer();
            contentTypesCount++;
            contentTypesTestHelper.createContentTypeOnServer();
            contentTypesCount++;
            contentTypesTestHelper.createContentTypeOnServer();
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
            contentTypesTestHelper.createContentTypeOnServer();
            contentTypesCount++;
            contentTypesTestHelper.createContentTypeOnServer();
            contentTypesCount++;
            contentTypesTestHelper.createContentTypeOnServer();
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
     * Given scenario: Testing the Content Type push command creating a content type with an ID
     *
     * @throws IOException if an I/O error occurs during the execution of the test
     */
    @Test
    void Test_Push_Content_Type_Creation_With_Id() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            // ╔══════════════════════╗
            // ║  Preparing the data  ║
            // ╚══════════════════════╝

            final var initialIdentifier = UUID.randomUUID().toString();
            final var initialVariable = "var_" + System.currentTimeMillis();
            // Creating a content type file descriptor
            final var newContentTypeResult = contentTypesTestHelper.createContentTypeDescriptorWithIdData(
                    workspace,
                    initialIdentifier, initialVariable
            );

            // ╔════════════════════════════════════════════════════════════╗
            // ║  Pushing the descriptor for the just created Content Type  ║
            // ╚════════════════════════════════════════════════════════════╝
            var status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ╔════════════════════════════════════════════╗
            // ║  Validating the information on the server  ║
            // ╚════════════════════════════════════════════╝
            var byVarName = contentTypesTestHelper.findContentType(newContentTypeResult.variable());
            Assertions.assertTrue(byVarName.isPresent());
            Assertions.assertEquals(initialVariable, byVarName.get().variable());
            Assertions.assertEquals(initialIdentifier, byVarName.get().id());

            // ---
            // Now validating the auto update updated the content type descriptor
            var updatedContentTypeDescriptor = this.mapperService.map(
                    newContentTypeResult.path().toFile(),
                    ContentType.class
            );
            Assertions.assertEquals(initialVariable, updatedContentTypeDescriptor.variable());
            Assertions.assertEquals(initialIdentifier, updatedContentTypeDescriptor.id());
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Testing the Content Type push command creating a content type with no ID
     *
     * @throws IOException if an I/O error occurs during the execution of the test
     */
    @Test
    void Test_Push_Content_Type_Creation_With_No_Id() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            // ╔══════════════════════╗
            // ║  Preparing the data  ║
            // ╚══════════════════════╝

            // Creating a content type file descriptor
            final var newContentTypeResult = contentTypesTestHelper.createContentTypeDescriptor(
                    workspace
            );

            // ╔════════════════════════════════════════════════════════════╗
            // ║  Pushing the descriptor for the just created Content Type  ║
            // ╚════════════════════════════════════════════════════════════╝
            var status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ╔════════════════════════════════════════════╗
            // ║  Validating the information on the server  ║
            // ╚════════════════════════════════════════════╝
            var byVarName = contentTypesTestHelper.findContentType(newContentTypeResult.variable());
            Assertions.assertTrue(byVarName.isPresent());
            Assertions.assertNotNull(byVarName.get().variable());
            Assertions.assertNotNull(byVarName.get().id());

            // ---
            // Now validating the auto update updated the content type descriptor
            var updatedContentTypeDescriptor = this.mapperService.map(
                    newContentTypeResult.path().toFile(),
                    ContentType.class
            );
            Assertions.assertEquals(byVarName.get().variable(),
                    updatedContentTypeDescriptor.variable());
            Assertions.assertEquals(byVarName.get().id(), updatedContentTypeDescriptor.id());
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Testing the Content Type push command modifying the ID with a non-existing
     * ID
     *
     * @throws IOException if an I/O error occurs during the execution of the test
     */
    @Test
    void Test_Push_Content_Type_Update_With_Non_Existing_Id() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            // ╔══════════════════════╗
            // ║  Preparing the data  ║
            // ╚══════════════════════╝
            // Creating a content type file descriptor
            final var newContentTypeResult = contentTypesTestHelper.createContentTypeDescriptor(
                    workspace
            );

            // ╔════════════════════════════════════════════════════════════╗
            // ║  Pushing the descriptor for the just created Content Type  ║
            // ╚════════════════════════════════════════════════════════════╝
            var status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ╔════════════════════════════════════════════╗
            // ║  Validating the information on the server  ║
            // ╚════════════════════════════════════════════╝
            var byVarName = contentTypesTestHelper.findContentType(newContentTypeResult.variable());
            Assertions.assertTrue(byVarName.isPresent());
            Assertions.assertEquals(newContentTypeResult.variable(), byVarName.get().variable());
            Assertions.assertNotNull(byVarName.get().id());

            // ---
            // Now validating the auto update updated the content type descriptor
            var updatedContentTypeDescriptor = this.mapperService.map(
                    newContentTypeResult.path().toFile(),
                    ContentType.class
            );
            Assertions.assertEquals(byVarName.get().variable(),
                    updatedContentTypeDescriptor.variable());
            Assertions.assertEquals(byVarName.get().id(), updatedContentTypeDescriptor.id());

            final var notExistingIdentifier = UUID.randomUUID().toString();
            final var previousId = updatedContentTypeDescriptor.id();

            // ╔════════════════════════════════════════╗
            // ║  Modifying the id of the content type  ║
            // ╚════════════════════════════════════════╝
            updatedContentTypeDescriptor = ImmutableSimpleContentType.builder()
                    .from(updatedContentTypeDescriptor)
                    .id(notExistingIdentifier).build();
            var jsonContent = this.mapperService
                    .objectMapper(newContentTypeResult.path().toFile())
                    .writeValueAsString(updatedContentTypeDescriptor);
            Files.write(newContentTypeResult.path(), jsonContent.getBytes());

            // ╔═════════════════════════════════════════════════════╗
            // ║  Pushing again the descriptor for the Content Type  ║
            // ╚═════════════════════════════════════════════════════╝
            status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ╔══════════════════════════════╗
            // ║  Validating the information  ║
            // ╚══════════════════════════════╝
            byVarName = contentTypesTestHelper.findContentType(newContentTypeResult.variable());
            Assertions.assertTrue(byVarName.isPresent());
            Assertions.assertEquals(newContentTypeResult.variable(), byVarName.get().variable());
            Assertions.assertNotEquals(notExistingIdentifier, byVarName.get().id());
            Assertions.assertEquals(previousId, byVarName.get().id());

            // ---
            // Now validating the auto update updated the content type descriptor
            updatedContentTypeDescriptor = this.mapperService.map(
                    newContentTypeResult.path().toFile(),
                    ContentType.class
            );
            Assertions.assertEquals(byVarName.get().variable(),
                    updatedContentTypeDescriptor.variable());
            Assertions.assertEquals(byVarName.get().id(), updatedContentTypeDescriptor.id());
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Testing the Content Type push command modifying the ID with an existing ID
     * (Normal scenario)
     *
     * @throws IOException if an I/O error occurs during the execution of the test
     */
    @Test
    void Test_Push_Content_Type_Update_With_Existing_Id() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            // ╔══════════════════════╗
            // ║  Preparing the data  ║
            // ╚══════════════════════╝
            // Creating a content type file descriptor
            final var newContentTypeResult = contentTypesTestHelper.createContentTypeDescriptor(
                    workspace
            );

            // ╔════════════════════════════════════════════════════════════╗
            // ║  Pushing the descriptor for the just created Content Type  ║
            // ╚════════════════════════════════════════════════════════════╝
            var status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ╔════════════════════════════════════════════╗
            // ║  Validating the information on the server  ║
            // ╚════════════════════════════════════════════╝
            var byVarName = contentTypesTestHelper.findContentType(newContentTypeResult.variable());
            Assertions.assertTrue(byVarName.isPresent());
            Assertions.assertEquals(newContentTypeResult.variable(), byVarName.get().variable());
            Assertions.assertNotNull(byVarName.get().id());

            // ---
            // Now validating the auto update updated the content type descriptor
            var updatedContentTypeDescriptor = this.mapperService.map(
                    newContentTypeResult.path().toFile(),
                    ContentType.class
            );
            Assertions.assertEquals(byVarName.get().variable(),
                    updatedContentTypeDescriptor.variable());
            Assertions.assertEquals(byVarName.get().id(), updatedContentTypeDescriptor.id());

            // ╔═════════════════════════════════════════════════════╗
            // ║  Pushing again the descriptor for the Content Type  ║
            // ╚═════════════════════════════════════════════════════╝
            status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ╔══════════════════════════════╗
            // ║  Validating the information  ║
            // ╚══════════════════════════════╝
            byVarName = contentTypesTestHelper.findContentType(newContentTypeResult.variable());
            Assertions.assertTrue(byVarName.isPresent());
            Assertions.assertEquals(newContentTypeResult.variable(), byVarName.get().variable());
            Assertions.assertEquals(updatedContentTypeDescriptor.id(), byVarName.get().id());

            // ---
            // Now validating the auto update updated the content type descriptor
            updatedContentTypeDescriptor = this.mapperService.map(
                    newContentTypeResult.path().toFile(),
                    ContentType.class
            );
            Assertions.assertEquals(byVarName.get().variable(),
                    updatedContentTypeDescriptor.variable());
            Assertions.assertEquals(byVarName.get().id(), updatedContentTypeDescriptor.id());
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Testing the Content Type push command modifying the ID with a null ID
     *
     * @throws IOException if an I/O error occurs during the execution of the test
     */
    @Test
    void Test_Push_Content_Type_Update_With_No_Id() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            // ╔══════════════════════╗
            // ║  Preparing the data  ║
            // ╚══════════════════════╝
            // Creating a content type file descriptor
            final var newContentTypeResult = contentTypesTestHelper.createContentTypeDescriptor(
                    workspace
            );

            // ╔════════════════════════════════════════════════════════════╗
            // ║  Pushing the descriptor for the just created Content Type  ║
            // ╚════════════════════════════════════════════════════════════╝
            var status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ╔════════════════════════════════════════════╗
            // ║  Validating the information on the server  ║
            // ╚════════════════════════════════════════════╝
            var byVarName = contentTypesTestHelper.findContentType(newContentTypeResult.variable());
            Assertions.assertTrue(byVarName.isPresent());
            Assertions.assertEquals(newContentTypeResult.variable(), byVarName.get().variable());
            Assertions.assertNotNull(byVarName.get().id());

            // ---
            // Now validating the auto update updated the content type descriptor
            var updatedContentTypeDescriptor = this.mapperService.map(
                    newContentTypeResult.path().toFile(),
                    ContentType.class
            );
            Assertions.assertEquals(byVarName.get().variable(),
                    updatedContentTypeDescriptor.variable());
            Assertions.assertEquals(byVarName.get().id(), updatedContentTypeDescriptor.id());

            final var previousId = updatedContentTypeDescriptor.id();

            // ╔════════════════════════════════════════╗
            // ║  Modifying the id of the content type  ║
            // ╚════════════════════════════════════════╝
            updatedContentTypeDescriptor = ImmutableSimpleContentType.builder()
                    .from(updatedContentTypeDescriptor)
                    .id(null).build();
            var jsonContent = this.mapperService
                    .objectMapper(newContentTypeResult.path().toFile())
                    .writeValueAsString(updatedContentTypeDescriptor);
            Files.write(newContentTypeResult.path(), jsonContent.getBytes());

            // ╔═════════════════════════════════════════════════════╗
            // ║  Pushing again the descriptor for the Content Type  ║
            // ╚═════════════════════════════════════════════════════╝
            status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ╔══════════════════════════════╗
            // ║  Validating the information  ║
            // ╚══════════════════════════════╝
            byVarName = contentTypesTestHelper.findContentType(newContentTypeResult.variable());
            Assertions.assertTrue(byVarName.isPresent());
            Assertions.assertEquals(newContentTypeResult.variable(), byVarName.get().variable());
            Assertions.assertNotNull(byVarName.get().id());
            Assertions.assertEquals(previousId, byVarName.get().id());

            // ---
            // Now validating the auto update updated the content type descriptor
            updatedContentTypeDescriptor = this.mapperService.map(
                    newContentTypeResult.path().toFile(),
                    ContentType.class
            );
            Assertions.assertEquals(byVarName.get().variable(),
                    updatedContentTypeDescriptor.variable());
            Assertions.assertEquals(byVarName.get().id(), updatedContentTypeDescriptor.id());
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Testing the Content Type push command modifying the fields ID with a null
     * ID.
     *
     * @throws IOException if an I/O error occurs during the execution of the test
     */
    @Test
    void Test_Push_Content_Type_Update_Field_With_No_Id() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            // ╔══════════════════════╗
            // ║  Preparing the data  ║
            // ╚══════════════════════╝
            // Creating a content type file descriptor
            final var newContentTypeResult = contentTypesTestHelper.createContentTypeDescriptor(
                    workspace
            );

            // ╔════════════════════════════════════════════════════════════╗
            // ║  Pushing the descriptor for the just created Content Type  ║
            // ╚════════════════════════════════════════════════════════════╝
            var status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ╔════════════════════════════════════════════╗
            // ║  Validating the information on the server  ║
            // ╚════════════════════════════════════════════╝
            var byVarName = contentTypesTestHelper.findContentType(newContentTypeResult.variable());
            Assertions.assertTrue(byVarName.isPresent());
            Assertions.assertNotNull(byVarName.get().fields());
            Assertions.assertEquals(4, byVarName.get().fields().size());

            // ---
            // Now validating the auto update updated the content type descriptor
            var updatedContentTypeDescriptor = this.mapperService.map(
                    newContentTypeResult.path().toFile(),
                    ContentType.class
            );
            Assertions.assertNotNull(updatedContentTypeDescriptor.fields());
            Assertions.assertEquals(4, updatedContentTypeDescriptor.fields().size());

            // ╔══════════════════════════════════╗
            // ║  Removing the ids on the fields  ║
            // ╚══════════════════════════════════╝
            final var descriptorFields = updatedContentTypeDescriptor.fields();
            var descriptorField1 = descriptorFields.get(0);
            final var descriptorField1Id = descriptorField1.id();
            descriptorField1 = ImmutableBinaryField.builder()
                    .from(descriptorField1)
                    .id(null).build();

            var descriptorField2 = descriptorFields.get(1);
            final var descriptorField2Id = descriptorField2.id();
            descriptorField2 = ImmutableTextField.builder()
                    .from(descriptorField2)
                    .id(null).build();

            updatedContentTypeDescriptor = ImmutableSimpleContentType.builder()
                    .from(updatedContentTypeDescriptor)
                    .fields(Arrays.asList(descriptorField1, descriptorField2)).build();
            var jsonContent = this.mapperService
                    .objectMapper(newContentTypeResult.path().toFile())
                    .writeValueAsString(updatedContentTypeDescriptor);
            Files.write(newContentTypeResult.path(), jsonContent.getBytes());

            // ╔═════════════════════════════════════════════════════╗
            // ║  Pushing again the descriptor for the Content Type  ║
            // ╚═════════════════════════════════════════════════════╝
            status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ╔══════════════════════════════╗
            // ║  Validating the information  ║
            // ╚══════════════════════════════╝
            byVarName = contentTypesTestHelper.findContentType(newContentTypeResult.variable());
            Assertions.assertTrue(byVarName.isPresent());
            Assertions.assertNotNull(byVarName.get().fields());
            Assertions.assertEquals(4, byVarName.get().fields().size());
            Assertions.assertEquals(descriptorField1Id, byVarName.get().fields().get(2).id());
            Assertions.assertEquals(descriptorField2Id, byVarName.get().fields().get(3).id());

            // ---
            // Now validating the auto update updated the content type descriptor
            updatedContentTypeDescriptor = this.mapperService.map(
                    newContentTypeResult.path().toFile(),
                    ContentType.class
            );
            Assertions.assertNotNull(updatedContentTypeDescriptor.fields());
            Assertions.assertEquals(4, updatedContentTypeDescriptor.fields().size());
            Assertions.assertEquals(
                    descriptorField1Id, updatedContentTypeDescriptor.fields().get(2).id());
            Assertions.assertEquals(
                    descriptorField2Id, updatedContentTypeDescriptor.fields().get(3).id());

        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Testing the Content Type push command modifying the fields ID with
     * non-existing IDs
     *
     * @throws IOException if an I/O error occurs during the execution of the test
     */
    @Test
    void Test_Push_Content_Type_Update_Field_With_Non_Existing_Id() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            // ╔══════════════════════╗
            // ║  Preparing the data  ║
            // ╚══════════════════════╝
            // Creating a content type file descriptor
            final var newContentTypeResult = contentTypesTestHelper.createContentTypeDescriptor(
                    workspace
            );

            // ╔════════════════════════════════════════════════════════════╗
            // ║  Pushing the descriptor for the just created Content Type  ║
            // ╚════════════════════════════════════════════════════════════╝
            var status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ╔════════════════════════════════════════════╗
            // ║  Validating the information on the server  ║
            // ╚════════════════════════════════════════════╝
            var byVarName = contentTypesTestHelper.findContentType(newContentTypeResult.variable());
            Assertions.assertTrue(byVarName.isPresent());
            Assertions.assertNotNull(byVarName.get().fields());
            Assertions.assertEquals(4, byVarName.get().fields().size());

            // ---
            // Now validating the auto update updated the content type descriptor
            var updatedContentTypeDescriptor = this.mapperService.map(
                    newContentTypeResult.path().toFile(),
                    ContentType.class
            );
            Assertions.assertNotNull(updatedContentTypeDescriptor.fields());
            Assertions.assertEquals(4, updatedContentTypeDescriptor.fields().size());

            // ╔══════════════════════════════════╗
            // ║  Changing the ids on the fields  ║
            // ╚══════════════════════════════════╝
            final var descriptorFields = updatedContentTypeDescriptor.fields();
            var descriptorField1 = descriptorFields.get(0);
            final var descriptorField1Id = descriptorField1.id();
            descriptorField1 = ImmutableBinaryField.builder()
                    .from(descriptorField1)
                    .id(UUID.randomUUID().toString()).build();

            var descriptorField2 = descriptorFields.get(1);
            final var descriptorField2Id = descriptorField2.id();
            descriptorField2 = ImmutableTextField.builder()
                    .from(descriptorField2)
                    .id(UUID.randomUUID().toString()).build();

            updatedContentTypeDescriptor = ImmutableSimpleContentType.builder()
                    .from(updatedContentTypeDescriptor)
                    .fields(Arrays.asList(descriptorField1, descriptorField2)).build();
            var jsonContent = this.mapperService
                    .objectMapper(newContentTypeResult.path().toFile())
                    .writeValueAsString(updatedContentTypeDescriptor);
            Files.write(newContentTypeResult.path(), jsonContent.getBytes());

            // ╔═════════════════════════════════════════════════════╗
            // ║  Pushing again the descriptor for the Content Type  ║
            // ╚═════════════════════════════════════════════════════╝
            status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ╔══════════════════════════════╗
            // ║  Validating the information  ║
            // ╚══════════════════════════════╝
            byVarName = contentTypesTestHelper.findContentType(newContentTypeResult.variable());
            Assertions.assertTrue(byVarName.isPresent());
            Assertions.assertNotNull(byVarName.get().fields());
            Assertions.assertEquals(4, byVarName.get().fields().size());
            Assertions.assertEquals(descriptorField1Id, byVarName.get().fields().get(2).id());
            Assertions.assertEquals(descriptorField2Id, byVarName.get().fields().get(3).id());

            // ---
            // Now validating the auto update updated the content type descriptor
            updatedContentTypeDescriptor = this.mapperService.map(
                    newContentTypeResult.path().toFile(),
                    ContentType.class
            );
            Assertions.assertNotNull(updatedContentTypeDescriptor.fields());
            Assertions.assertEquals(4, updatedContentTypeDescriptor.fields().size());
            Assertions.assertEquals(
                    descriptorField1Id, updatedContentTypeDescriptor.fields().get(2).id());
            Assertions.assertEquals(
                    descriptorField2Id, updatedContentTypeDescriptor.fields().get(3).id());

        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Testing the Content Type push command adding a new field to the content
     * type.
     *
     * @throws IOException if an I/O error occurs during the execution of the test
     */
    @Test
    void Test_Push_Content_Type_Update_Adding_Field() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            // ╔══════════════════════╗
            // ║  Preparing the data  ║
            // ╚══════════════════════╝
            // Creating a content type file descriptor
            final var newContentTypeResult = contentTypesTestHelper.createContentTypeDescriptor(
                    workspace
            );

            // ╔════════════════════════════════════════════════════════════╗
            // ║  Pushing the descriptor for the just created Content Type  ║
            // ╚════════════════════════════════════════════════════════════╝
            var status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ╔════════════════════════════════════════════╗
            // ║  Validating the information on the server  ║
            // ╚════════════════════════════════════════════╝
            var byVarName = contentTypesTestHelper.findContentType(newContentTypeResult.variable());
            Assertions.assertTrue(byVarName.isPresent());
            Assertions.assertNotNull(byVarName.get().fields());
            Assertions.assertEquals(4, byVarName.get().fields().size());

            // ---
            // Now validating the auto update updated the content type descriptor
            var updatedContentTypeDescriptor = this.mapperService.map(
                    newContentTypeResult.path().toFile(),
                    ContentType.class
            );
            Assertions.assertNotNull(updatedContentTypeDescriptor.fields());
            Assertions.assertEquals(4, updatedContentTypeDescriptor.fields().size());

            // ╔════════════════════════════════════════════════════════════════╗
            // ║  Adding new field and removing the ids on the existing fields  ║
            // ╚════════════════════════════════════════════════════════════════╝
            final var descriptorFields = updatedContentTypeDescriptor.fields();
            var descriptorField1 = descriptorFields.get(2);
            final var descriptorField1Id = descriptorField1.id();
            descriptorField1 = ImmutableBinaryField.builder()
                    .from(descriptorField1)
                    .id(null).build();

            var descriptorField2 = descriptorFields.get(3);
            final var descriptorField2Id = descriptorField2.id();
            descriptorField2 = ImmutableTextField.builder()
                    .from(descriptorField2)
                    .id(null).build();

            var newField3 = ImmutableTextField.builder()
                    .indexed(true)
                    .dataType(DataTypes.TEXT)
                    .fieldType("text")
                    .readOnly(false)
                    .required(true)
                    .searchable(true)
                    .listed(true)
                    .sortOrder(3)
                    .searchable(true)
                    .name("Field3")
                    .variable("field3")
                    .fixed(true)
                    .build();

            updatedContentTypeDescriptor = ImmutableSimpleContentType.builder()
                    .from(updatedContentTypeDescriptor)
                    .fields(Arrays.asList(descriptorField1, descriptorField2, newField3)).build();
            var jsonContent = this.mapperService
                    .objectMapper(newContentTypeResult.path().toFile())
                    .writeValueAsString(updatedContentTypeDescriptor);
            Files.write(newContentTypeResult.path(), jsonContent.getBytes());

            // ╔═════════════════════════════════════════════════════╗
            // ║  Pushing again the descriptor for the Content Type  ║
            // ╚═════════════════════════════════════════════════════╝
            status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ╔══════════════════════════════╗
            // ║  Validating the information  ║
            // ╚══════════════════════════════╝
            byVarName = contentTypesTestHelper.findContentType(newContentTypeResult.variable());
            Assertions.assertTrue(byVarName.isPresent());
            Assertions.assertNotNull(byVarName.get().fields());
            Assertions.assertEquals(5, byVarName.get().fields().size());
            Assertions.assertEquals(descriptorField1Id, byVarName.get().fields().get(2).id());
            Assertions.assertEquals(descriptorField2Id, byVarName.get().fields().get(3).id());

            // ---
            // Now validating the auto update updated the content type descriptor
            updatedContentTypeDescriptor = this.mapperService.map(
                    newContentTypeResult.path().toFile(),
                    ContentType.class
            );
            Assertions.assertNotNull(updatedContentTypeDescriptor.fields());
            Assertions.assertEquals(5, updatedContentTypeDescriptor.fields().size());
            Assertions.assertEquals(
                    descriptorField1Id, updatedContentTypeDescriptor.fields().get(2).id());
            Assertions.assertEquals(
                    descriptorField2Id, updatedContentTypeDescriptor.fields().get(3).id());

        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Testing the Content Type push command removing a field from the content
     * type.
     *
     * @throws IOException if an I/O error occurs during the execution of the test
     */
    @Test
    void Test_Push_Content_Type_Update_Removing_Field() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            // ╔══════════════════════╗
            // ║  Preparing the data  ║
            // ╚══════════════════════╝
            // Creating a content type file descriptor
            final var newContentTypeResult = contentTypesTestHelper.createContentTypeDescriptor(
                    workspace
            );

            // ╔════════════════════════════════════════════════════════════╗
            // ║  Pushing the descriptor for the just created Content Type  ║
            // ╚════════════════════════════════════════════════════════════╝
            var status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ╔════════════════════════════════════════════╗
            // ║  Validating the information on the server  ║
            // ╚════════════════════════════════════════════╝
            var byVarName = contentTypesTestHelper.findContentType(newContentTypeResult.variable());
            Assertions.assertTrue(byVarName.isPresent());
            Assertions.assertNotNull(byVarName.get().fields());
            Assertions.assertEquals(4, byVarName.get().fields().size());

            // ---
            // Now validating the auto update updated the content type descriptor
            var updatedContentTypeDescriptor = this.mapperService.map(
                    newContentTypeResult.path().toFile(),
                    ContentType.class
            );
            Assertions.assertNotNull(updatedContentTypeDescriptor.fields());
            Assertions.assertEquals(4, updatedContentTypeDescriptor.fields().size());

            // ╔═════════════════╗
            // ║  Removing field ║
            // ╚═════════════════╝
            final var descriptorFields = updatedContentTypeDescriptor.fields();
            var descriptorField1 = descriptorFields.get(0);
            final var descriptorField1Id = descriptorField1.id();
            descriptorField1 = ImmutableBinaryField.builder()
                    .from(descriptorField1)
                    .id(null).build();

            updatedContentTypeDescriptor = ImmutableSimpleContentType.builder()
                    .from(updatedContentTypeDescriptor)
                    .fields(List.of(descriptorField1)).build();
            var jsonContent = this.mapperService
                    .objectMapper(newContentTypeResult.path().toFile())
                    .writeValueAsString(updatedContentTypeDescriptor);
            Files.write(newContentTypeResult.path(), jsonContent.getBytes());

            // ╔═════════════════════════════════════════════════════╗
            // ║  Pushing again the descriptor for the Content Type  ║
            // ╚═════════════════════════════════════════════════════╝
            status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                    workspace.contentTypes().toAbsolutePath().toString(),
                    "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ╔══════════════════════════════╗
            // ║  Validating the information  ║
            // ╚══════════════════════════════╝
            byVarName = contentTypesTestHelper.findContentType(newContentTypeResult.variable());
            Assertions.assertTrue(byVarName.isPresent());
            Assertions.assertNotNull(byVarName.get().fields());
            // NOTE: byVarName.get().fields().size() is 3 because after the remove operation, and
            // not in a consistent way in relation with the add and update, the column and row fields
            // are included in the list of fields.
            Assertions.assertEquals(3, byVarName.get().fields().size());
            Assertions.assertEquals(descriptorField1Id, byVarName.get().fields().get(2).id());

            // ---
            // Now validating the auto update updated the content type descriptor
            updatedContentTypeDescriptor = this.mapperService.map(
                    newContentTypeResult.path().toFile(),
                    ContentType.class
            );
            Assertions.assertNotNull(updatedContentTypeDescriptor.fields());
            // NOTE: byVarName.get().fields().size() is 3 because after the remove operation, and
            // not in a consistent way in relation with the add and update, the column and row fields
            // are included in the list of fields.
            Assertions.assertEquals(3, updatedContentTypeDescriptor.fields().size());
            Assertions.assertEquals(
                    descriptorField1Id, updatedContentTypeDescriptor.fields().get(2).id());

        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Test Find by name authenticated with token
     * Given scenario: We want to find a content type by name using a token
     * Expected result: The content type should be found and returned
     */
    @Test
    void Test_Command_Content_Find_Authenticated_With_Token() {
        final String token = requestToken();
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypeFind.NAME, "--name", "FileAsset", "--token", token);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.startsWith("variable:"));
        }
    }

    /**
     * Function to verify if a list of strings is sorted in ascending order (case-insensitive)
     *
     * @param list the list of strings
     * @return true if the list is sorted in ascending order, false otherwise
     */
    public static boolean isSortedAsc(final List<String> list) {
        for (int i = 0; i < list.size() - 1; i++) {
            if (String.CASE_INSENSITIVE_ORDER.compare(list.get(i), list.get(i + 1)) > 0) {
                System.out.println("i: " + list.get(i) + " i+1: " + list.get(i + 1));
                return false;
            }
        }
        return true;
    }

    /**
     * Function to verify if a list of strings is sorted in descending order (case-insensitive)
     *
     * @param list the list of strings
     * @return true if the list is sorted in descending order, false otherwise
     */
    public static boolean isSortedDesc(final List<String> list) {
        for (int i = 0; i < list.size() - 1; i++) {
            if (String.CASE_INSENSITIVE_ORDER.compare(list.get(i), list.get(i + 1)) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Extracts the rows from the output text by the field name
     *
     * @param fieldName the field name to extract
     * @param inputText the input text
     * @return the list of rows
     */
    private static List<String> extractRowsByFieldName(final String fieldName,
            final String inputText) {
        List<String> varNames = new ArrayList<>();
        Pattern pattern = Pattern.compile(String.format("%s:\\s*\\[([^\\]]+)\\]", fieldName));
        Matcher matcher = pattern.matcher(inputText);
        while (matcher.find()) {
            varNames.add(matcher.group(1).replaceAll("[^a-zA-Z0-9]", ""));
        }
        return varNames;
    }

}
