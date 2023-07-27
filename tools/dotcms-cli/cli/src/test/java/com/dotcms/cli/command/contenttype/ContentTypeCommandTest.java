package com.dotcms.cli.command.contenttype;

import static org.junit.jupiter.api.Assertions.fail;

import com.dotcms.api.AuthenticationContext;
import com.dotcms.api.provider.ClientObjectMapper;
import com.dotcms.api.provider.YAMLMapperSupplier;
import com.dotcms.cli.command.CommandTest;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.model.config.Workspace;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.wildfly.common.Assert;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

@QuarkusTest
public class ContentTypeCommandTest extends CommandTest {

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    WorkspaceManager workspaceManager;

    @BeforeAll
    public static void beforeAll() {
        disableAnsi();
    }

    @AfterAll
    public static void afterAll() {
        enableAnsi();
    }

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
        final CommandLine commandLine = getFactory().create();
                final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePull.NAME, "fileAsset", "--workspace", workspace.root().toString());
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
        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String contentTypeVarName = "FileAsset";
            int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePull.NAME, contentTypeVarName, "--format", "YML", "--workspace", workspace.root().toString());
            Assertions.assertEquals(ExitCode.OK, status);
            final String output = writer.toString();
            System.out.println(output);
            try{
                final String fileName = String.format("%s.yml", contentTypeVarName);
                final Path path = Path.of(workspace.contentTypes().toString(), fileName);
                Assert.assertTrue(Files.exists(path));
                byte[] bytes = Files.readAllBytes(path);
                final ObjectMapper objectMapper = new YAMLMapperSupplier().get();
                final ContentType contentType = objectMapper.readValue(bytes, ContentType.class);
                Assertions.assertNotNull(contentType.variable());
                status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME, fileName, "--format", "YML", "--workspace", workspace.root().toString());
                Assertions.assertEquals(ExitCode.OK, status);
            } finally {
                workspaceManager.destroy(workspace);
            }
        }
    }

    /**
     * List all CT
     */
    @Test
    void Test_Command_Content_List_Option() {
        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypeFind.NAME,  "--interactive=false");
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
        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypeFind.NAME, "--name", "FileAsset", "--page", "0", "--pageSize", "10");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.startsWith("varName:"));
        }
    }

    /**
     * Push CT from a file
     * @throws IOException
     */
    @Test
    void Test_Push_New_Content_Type_From_File_Then_Remove() throws IOException {
        final long identifier =  System.currentTimeMillis();

        final String varName = "__var__"+identifier;

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
                                .name("__bin_var__"+identifier)
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
            final CommandLine commandLine = getFactory().create();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypePush.NAME,
                        jsonFile.getAbsolutePath(), "--workspace", workspace.root().toString());
                Assertions.assertEquals(ExitCode.OK, status);
                final String output = writer.toString();
                System.out.println(output);
            }

            final int status = commandLine.execute(ContentTypeCommand.NAME, ContentTypeRemove.NAME, varName,
                    "--cli-test", "--workspace", workspace.root().toString());
            Assertions.assertEquals(ExitCode.OK, status);

            //A simple Thread.sleep() could do it too but Sonar strongly recommends we stay away from doing that.
            int count = 0;
            while (ExitCode.SOFTWARE != commandLine.execute(ContentTypeCommand.NAME, ContentTypePull.NAME,
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
     * Given scenario: Despite the number of times the same content type is pulled, it should only be created once
     * Expected result: The WorkspaceManager should be able to create and destroy a workspace
     * @throws IOException
     */
    @Test
    void Test_Pull_Same_Content_Type_Multiple_Times() throws IOException {
        final Workspace workspace = workspaceManager.getOrCreate();
        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String contentTypeVarName = "Image";
            for (int i=0; i<= 5; i++) {
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
}
