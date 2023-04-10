package com.dotcms.cli.command.site;

import com.dotcms.api.AuthenticationContext;
import com.dotcms.cli.command.CommandTest;
import com.dotcms.cli.common.InputOutputFormat;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.*;
import picocli.CommandLine;

import javax.inject.Inject;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

@QuarkusTest
class SiteCommandTest extends CommandTest {

    @ConfigProperty(name = "com.dotcms.starter.site", defaultValue = "default")
    String siteName;

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

    @Test
    void Test_Command_Current_Site() {
        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(SiteCommand.NAME, SiteCurrent.NAME);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.startsWith("Current Site is "));
        }
    }

    @Test
    void Test_Command_Site_List_All() {
        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME, "--interactive=false");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.startsWith("name:"));
        }
    }

    @Test
    void Test_Command_Site_Find_By_Name() {
        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME, "--name", siteName);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.startsWith("name:"));
        }
    }


    @Test
    void Test_Command_Site_Push_Publish_UnPublish_Then_Archive() throws IOException {

        final String newSiteName = String.format("new.dotcms.site%d", System.currentTimeMillis());
        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            int status = commandLine.execute(SiteCommand.NAME, SiteCreate.NAME, newSiteName);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            status = commandLine.execute(SiteCommand.NAME, SiteStart.NAME, newSiteName);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            status = commandLine.execute(SiteCommand.NAME, SiteStop.NAME, newSiteName);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            status = commandLine.execute(SiteCommand.NAME, SiteArchive.NAME, newSiteName);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            status = commandLine.execute(SiteCommand.NAME, SiteUnarchive.NAME, newSiteName);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            status = commandLine.execute(SiteCommand.NAME, SitePull.NAME, newSiteName);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

        } finally {
            Files.deleteIfExists(Path.of(".", String.format("%s.%s", newSiteName, InputOutputFormat.defaultFormat().getExtension())));
        }
    }

    @Test
    void Test_Command_Copy() {
        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(SiteCommand.NAME, SiteCopy.NAME, "--idOrName", siteName);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.startsWith("New Copy Site is"));
        }
    }

}
