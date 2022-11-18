package com.dotcms.cli.command;

import com.dotcms.api.AuthenticationContext;
import com.dotcms.api.provider.ClientObjectMapper;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.Date;
import javax.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@QuarkusTest
public class ContentTypeCommandTest extends CommandTest{

    @Inject
    AuthenticationContext authenticationContext;

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
     * @throws JsonProcessingException
     */
    @Test
    public void Test_Command_Content_List_Option() throws JsonProcessingException {

        final CommandLine commandLine = factory.create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(ContentTypeCommand.NAME, "--list", "--interactive=false");
            Assertions.assertEquals(ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.startsWith("varName:"));
        }
    }

    /**
     * Pull single CT by varName
     * @throws JsonProcessingException
     */
    @Test
    public void Test_Command_Content_Type_Pass_Pull_Option() throws JsonProcessingException {

        final CommandLine commandLine = factory.create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(ContentTypeCommand.NAME, "--pull", "fileAsset", "--saveTo", "./lol.text");
            Assertions.assertEquals(ExitCode.OK, status);
            final String output = writer.toString();
            //System.out.println(output);
            final ObjectMapper objectMapper = new ClientObjectMapper().getContext(null);
            final ContentType contentType = objectMapper.readValue(output, ContentType.class);
            Assertions.assertNotNull(contentType.variable());

        }
    }

    /**
     * Simple filter test
     */
    @Test
    public void Test_Command_Content_Type_Pass_Filter_Short_View_Option() {

        final CommandLine commandLine = factory.create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(ContentTypeCommand.NAME, "--filter", "FileAsset");
            Assertions.assertEquals(ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.startsWith("varName: [FileAsset]"));
        }
    }

    /**
     * Test invalid combination to verify group of options are mutually exclusive
     */
    @Test
    public void Test_Command_Content_Type_Pass_Filter_Invalid_Options() {
        final CommandLine commandLine = factory.create();
        final int status = commandLine.execute(ContentTypeCommand.NAME, "--filter", "-ls");
        Assertions.assertEquals(ExitCode.USAGE, status);
    }

    /**
     * Push CT from a file
     * @throws IOException
     */
    @Test
    public void Test_Push_New_Content_Type_From_File_Then_Remove() throws IOException {

        long identifier =  System.currentTimeMillis();

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

        final CommandLine commandLine = factory.create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(ContentTypeCommand.NAME, "--push", jsonFile.getAbsolutePath() );
            Assertions.assertEquals(ExitCode.OK, status);
            final String output = writer.toString();
            System.out.println(output);
        }

        final int status = commandLine.execute(ContentTypeCommand.NAME, "--remove", varName );
        Assertions.assertEquals(ExitCode.OK, status);

        final int status2 = commandLine.execute(ContentTypeCommand.NAME, "--pull", varName );
        Assertions.assertEquals(ExitCode.SOFTWARE, status2);
    }

}
