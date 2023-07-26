//package com.dotcms.cli.command.site;
//
//import com.dotcms.api.AuthenticationContext;
//import com.dotcms.cli.command.CommandTest;
//import com.dotcms.cli.common.InputOutputFormat;
//import io.quarkus.test.junit.QuarkusTest;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//import org.eclipse.microprofile.config.inject.ConfigProperty;
//import org.junit.jupiter.api.*;
//import org.wildfly.common.Assert;
//import picocli.CommandLine;
//
//import javax.inject.Inject;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.io.StringWriter;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import picocli.CommandLine.ExitCode;
//
//@QuarkusTest
//class SiteCommandTest extends CommandTest {
//
//    @ConfigProperty(name = "com.dotcms.starter.site", defaultValue = "default")
//    String siteName;
//
//    @Inject
//    AuthenticationContext authenticationContext;
//
//    @BeforeAll
//    public static void beforeAll() {
//        disableAnsi();
//    }
//
//    @AfterAll
//    public static void afterAll() {
//        enableAnsi();
//    }
//
//    @BeforeEach
//    public void setupTest() throws IOException {
//        resetServiceProfiles();
//        final String user = "admin@dotcms.com";
//        final char[] passwd = "admin".toCharArray();
//        authenticationContext.login(user, passwd);
//    }
//
//    /**
//     * Given scenario: Simply call current site
//     * Expected Result: Verify the command completes successfully
//     */
//    @Test
//    void Test_Command_Current_Site() {
//        final CommandLine commandLine = getFactory().create();
//        final StringWriter writer = new StringWriter();
//        try (PrintWriter out = new PrintWriter(writer)) {
//            commandLine.setOut(out);
//            final int status = commandLine.execute(SiteCommand.NAME, SiteCurrent.NAME);
//            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
//            final String output = writer.toString();
//            Assertions.assertTrue(output.startsWith("Current Site is "));
//        }
//    }
//
//    /**
//     * Given scenario: Simply call list all
//     * Expected Result: Verify the command completes successfully
//     */
//    @Test
//    void Test_Command_Site_List_All() {
//        final CommandLine commandLine = getFactory().create();
//        final StringWriter writer = new StringWriter();
//        try (PrintWriter out = new PrintWriter(writer)) {
//            commandLine.setOut(out);
//            final int status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME, "--interactive=false");
//            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
//            final String output = writer.toString();
//            Assertions.assertTrue(output.startsWith("name:"));
//        }
//    }
//
//    /**
//     * Given scenario: Simply call find by name command
//     * Expected Result: Verify the command completes successfully
//     */
//    @Test
//    void Test_Command_Site_Find_By_Name() {
//        final CommandLine commandLine = getFactory().create();
//        final StringWriter writer = new StringWriter();
//        try (PrintWriter out = new PrintWriter(writer)) {
//            commandLine.setOut(out);
//            final int status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME, "--name", siteName);
//            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
//            final String output = writer.toString();
//            Assertions.assertTrue(output.startsWith("name:"));
//        }
//    }
//
//
//    /**
//     * Given scenario: Simply call create command
//     * Expected Result: Verify the command completes successfully Then test delete and verify it's gone
//     */
//    @Test
//    void Test_Command_Site_Push_Publish_UnPublish_Then_Archive() throws IOException {
//
//        final String newSiteName = String.format("new.dotcms.site%d", System.currentTimeMillis());
//        final CommandLine commandLine = getFactory().create();
//        final StringWriter writer = new StringWriter();
//        try (PrintWriter out = new PrintWriter(writer)) {
//            commandLine.setOut(out);
//            int status = commandLine.execute(SiteCommand.NAME, SiteCreate.NAME, newSiteName);
//            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
//
//            status = commandLine.execute(SiteCommand.NAME, SiteStart.NAME, newSiteName);
//            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
//
//            status = commandLine.execute(SiteCommand.NAME, SiteStop.NAME, newSiteName);
//            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
//
//            status = commandLine.execute(SiteCommand.NAME, SiteArchive.NAME, newSiteName);
//            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
//
//            status = commandLine.execute(SiteCommand.NAME, SiteUnarchive.NAME, newSiteName);
//            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
//
//            status = commandLine.execute(SiteCommand.NAME, SitePull.NAME, newSiteName);
//            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
//
//        } finally {
//            Files.deleteIfExists(Path.of(".", String.format("%s.%s", newSiteName, InputOutputFormat.defaultFormat().getExtension())));
//        }
//    }
//
//    /**
//     * Given scenario: Simply call create command followed by copy
//     * Expected Result: We simply verify the command completes successfully
//     */
//    @Test
//    void Test_Command_Copy() {
//        final CommandLine commandLine = getFactory().create();
//        final StringWriter writer = new StringWriter();
//        try (PrintWriter out = new PrintWriter(writer)) {
//            commandLine.setOut(out);
//            final int status = commandLine.execute(SiteCommand.NAME, SiteCopy.NAME, "--idOrName", siteName);
//            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
//            final String output = writer.toString();
//            Assertions.assertTrue(output.startsWith("New Copy Site is"));
//        }
//    }
//
//    /**
//     * Given scenario: Create a new site, pull it, push it, and pull it again.
//     * Expected Result: The site should be created. Pulled so we can test push. At the end we delete it and verify it's gone.
//     * @throws IOException
//     */
//    @Test
//    void Test_Command_Create_Then_Pull_Then_Push() throws IOException {
//
//        final String newSiteName = String.format("new.dotcms.site%d", System.currentTimeMillis());
//        final CommandLine commandLine = getFactory().create();
//        final StringWriter writer = new StringWriter();
//        try (PrintWriter out = new PrintWriter(writer)) {
//            commandLine.setOut(out);
//            commandLine.setErr(out);
//
//            int status = commandLine.execute(SiteCommand.NAME, SiteCreate.NAME, newSiteName);
//            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
//
//            status = commandLine.execute(SiteCommand.NAME, SitePull.NAME, newSiteName, "-saveTo="+newSiteName );
//
//            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
//
//            status = commandLine.execute(SiteCommand.NAME, SiteArchive.NAME, newSiteName);
//
//            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
//
//            status = commandLine.execute(SiteCommand.NAME, SiteRemove.NAME, newSiteName, "--cli-test");
//
//            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
//
//            status = commandLine.execute(SiteCommand.NAME, SitePull.NAME, newSiteName, "-saveTo="+newSiteName );
//
//            Assertions.assertEquals(ExitCode.SOFTWARE, status);
//
//            final String output = writer.toString();
//
//            Assertions.assertTrue(output.contains("archived successfully."));
//            Assertions.assertTrue(output.contains("removed successfully."));
//            Assertions.assertTrue(output.contains("Failed pulling Site:"));
//
//        } finally {
//            Files.deleteIfExists(Path.of(".", String.format("%s.%s", newSiteName, InputOutputFormat.defaultFormat().getExtension())));
//        }
//    }
//
//    @Test
//    void Test_Create_From_File_via_Push() throws IOException {
//        final String newSiteName = String.format("new.dotcms.site%d", System.currentTimeMillis());
//        String siteDescriptor = String.format("{\n"
//                + "  \"siteName\" : \"%s\",\n"
//                + "  \"languageId\" : 1,\n"
//                + "  \"modDate\" : \"2023-05-05T00:13:25.242+00:00\",\n"
//                + "  \"modUser\" : \"dotcms.org.1\",\n"
//                + "  \"live\" : true,\n"
//                + "  \"working\" : true\n"
//                + "}",newSiteName);
//
//        final Path path = Files.createTempFile("test", "json");
//        Files.write(path, siteDescriptor.getBytes());
//        final CommandLine commandLine = getFactory().create();
//        final StringWriter writer = new StringWriter();
//        try (PrintWriter out = new PrintWriter(writer)) {
//            commandLine.setOut(out);
//            commandLine.setErr(out);
//            int status = commandLine.execute(SiteCommand.NAME, SitePush.NAME, path.toFile().getAbsolutePath());
//            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
//
//            status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME, "--name", siteName);
//            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
//        }
//    }
//
//}
