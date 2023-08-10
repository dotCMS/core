package com.dotcms.cli.command.site;

import com.dotcms.api.AuthenticationContext;
import com.dotcms.cli.command.CommandTest;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.model.config.Workspace;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@QuarkusTest
class SiteCommandTest extends CommandTest {

    @ConfigProperty(name = "com.dotcms.starter.site", defaultValue = "default")
    String siteName;

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
     * Given scenario: Simply call current site
     * Expected Result: Verify the command completes successfully
     */
    @Test
    void Test_Command_Current_Site() {
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(SiteCommand.NAME, SiteCurrent.NAME);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.startsWith("Current Site is "));
        }
    }

    /**
     * Given scenario: Simply call list all
     * Expected Result: Verify the command completes successfully
     */
    @Test
    void Test_Command_Site_List_All() {
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME, "--interactive=false");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.startsWith("name:"));
        }
    }

    /**
     * Given scenario: Simply call find by name command
     * Expected Result: Verify the command completes successfully
     */
    @Test
    void Test_Command_Site_Find_By_Name() {
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME, "--name", siteName);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.startsWith("name:"));
        }
    }


    /**
     * Given scenario: Simply call create command
     * Expected Result: Verify the command completes successfully Then test delete and verify it's gone
     */
    @Test
    void Test_Command_Site_Push_Publish_UnPublish_Then_Archive() throws IOException {

        final Workspace workspace = workspaceManager.getOrCreate();
        final String newSiteName = String.format("new.dotcms.site%d", System.currentTimeMillis());
        final CommandLine commandLine = createCommand();
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

            status = commandLine.execute(SiteCommand.NAME, SitePull.NAME, newSiteName, "--workspace", workspace.root().toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

        } finally {
            workspaceManager.destroy(workspace);
        }
    }

    /**
     * Given scenario: Simply call create command followed by copy
     * Expected Result: We simply verify the command completes successfully
     */
    @Test
    void Test_Command_Copy() {
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(SiteCommand.NAME, SiteCopy.NAME, "--idOrName", siteName);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.startsWith("New Copy Site is"));
        }
    }

    /**
     * Given scenario: Create a new site, pull it, push it, and pull it again.
     * Expected Result: The site should be created. Pulled so we can test push. At the end we delete it and verify it's gone.
     * @throws IOException
     */
    @Test
    void Test_Command_Create_Then_Pull_Then_Push() throws IOException {
        final Workspace workspace = workspaceManager.getOrCreate();
        final String newSiteName = String.format("new.dotcms.site%d", System.currentTimeMillis());
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            commandLine.setErr(out);

            int status = commandLine.execute(SiteCommand.NAME, SiteCreate.NAME, newSiteName);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            status = commandLine.execute(SiteCommand.NAME, SitePull.NAME, newSiteName);

            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            status = commandLine.execute(SiteCommand.NAME, SiteArchive.NAME, newSiteName);

            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            status = commandLine.execute(SiteCommand.NAME, SiteRemove.NAME, newSiteName, "--cli-test");

            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            status = commandLine.execute(SiteCommand.NAME, SitePull.NAME, newSiteName, "--workspace", workspace.root().toString());

            Assertions.assertEquals(ExitCode.SOFTWARE, status);

            final String output = writer.toString();

            Assertions.assertTrue(output.contains("archived successfully."));
            Assertions.assertTrue(output.contains("removed successfully."));
            Assertions.assertTrue(output.contains("404"));

        } finally {
            workspaceManager.destroy(workspace);
        }
    }

    /**
     * Given scenario: Create a new site, pull it, push it, and pull it again.
     * Expected Results: The site should be created. Pulled so we can test push.
     * @throws IOException
     */
    @Test
    void Test_Create_From_File_via_Push() throws IOException {
        final String newSiteName = String.format("new.dotcms.site%d", System.currentTimeMillis());
        String siteDescriptor = String.format("{\n"
                + "  \"siteName\" : \"%s\",\n"
                + "  \"languageId\" : 1,\n"
                + "  \"modDate\" : \"2023-05-05T00:13:25.242+00:00\",\n"
                + "  \"modUser\" : \"dotcms.org.1\",\n"
                + "  \"live\" : true,\n"
                + "  \"working\" : true\n"
                + "}",newSiteName);

        final Path path = Files.createTempFile("test", "json");
        Files.write(path, siteDescriptor.getBytes());
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            commandLine.setErr(out);
            int status = commandLine.execute(SiteCommand.NAME, SitePush.NAME, path.toFile().getAbsolutePath());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME, "--name", siteName);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        }
    }

    /**
     * Given scenario: Despite the number of times the same Site gets pulled, it should only be created once locally
     * Expected result: The WorkspaceManager should be able to create and destroy a workspace
     * @throws IOException
     */
    @Test
    void Test_Pull_Same_Site_Multiple_Times() throws IOException {
        final Workspace workspace = workspaceManager.getOrCreate();
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            for (int i=0; i<= 5; i++) {
                int status = commandLine.execute(SiteCommand.NAME, SitePull.NAME, siteName, "--workspace", workspace.root().toString());
                Assertions.assertEquals(CommandLine.ExitCode.OK, status);
                System.out.println("Site Pulled: " + i);
            }

            final String fileName = String.format("%s.json", siteName);
            final Path path = Path.of(workspace.sites().toString(), fileName);
            Assert.assertTrue(Files.exists(path));

            try (Stream<Path> walk = Files.walk(workspace.sites())) {
                long count = walk.filter(p -> Files.isRegularFile(p) && p.getFileName().toString()
                        .startsWith(siteName.toLowerCase())).count();
                Assertions.assertEquals(1, count);
            }

        } finally {
            workspaceManager.destroy(workspace);
        }
    }

}
