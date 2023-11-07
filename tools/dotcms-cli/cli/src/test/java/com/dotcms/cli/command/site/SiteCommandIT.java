package com.dotcms.cli.command.site;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.AuthenticationContext;
import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.MapperService;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.cli.command.CommandTest;
import com.dotcms.cli.common.InputOutputFormat;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.model.config.Workspace;
import com.dotcms.model.site.GetSiteByNameRequest;
import com.dotcms.model.site.Site;
import com.dotcms.model.site.SiteView;
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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class SiteCommandIT extends CommandTest {

    @ConfigProperty(name = "com.dotcms.starter.site", defaultValue = "default")
    String siteName;

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
     * Given scenario: Simply call current site Expected Result: Verify the command completes
     * successfully
     */
    @Test
    @Order(1)
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
     * Given scenario: Simply call find by name command Expected Result: Verify the command
     * completes successfully
     */
    @Test
    @Order(2)
    void Test_Command_Site_Find_By_Name() {
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME, "--name",
                    siteName);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.startsWith("name:"));
        }
    }


    /**
     * Given scenario: Simply call create command Expected Result: Verify the command completes
     * successfully Then test delete and verify it's gone
     */
    @Test
    @Order(3)
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

            status = commandLine.execute(SiteCommand.NAME, SitePull.NAME, newSiteName,
                    "--workspace", workspace.root().toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

        } finally {
            workspaceManager.destroy(workspace);
        }
    }

    /**
     * Given scenario: Simply call create command followed by copy Expected Result: We simply verify
     * the command completes successfully
     */
    @Test
    @Order(4)
    void Test_Command_Copy() {
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(SiteCommand.NAME, SiteCopy.NAME, "--idOrName",
                    siteName);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.startsWith("New Copy Site is"));
        }
    }

    /**
     * Given scenario: Create a new site, pull it, push it, and pull it again. Expected Result: The
     * site should be created. Pulled so we can test push. At the end we delete it and verify it's
     * gone.
     *
     * @throws IOException
     */
    @Test
    @Order(5)
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

            status = commandLine.execute(SiteCommand.NAME, SiteRemove.NAME, newSiteName,
                    "--cli-test");

            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            status = commandLine.execute(SiteCommand.NAME, SitePull.NAME, newSiteName,
                    "--workspace", workspace.root().toString());

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
     * Given scenario: Create a new site, pull it, push it, and pull it again. Expected Results: The
     * site should be created. Pulled so we can test push.
     *
     * @throws IOException
     */
    @Test
    @Order(6)
    void Test_Create_From_File_via_Push() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();

        try {
            final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

            final String newSiteName = String.format("new.dotcms.site%d",
                    System.currentTimeMillis());
            String siteDescriptor = String.format("{\n"
                    + "  \"siteName\" : \"%s\",\n"
                    + "  \"languageId\" : 1,\n"
                    + "  \"modDate\" : \"2023-05-05T00:13:25.242+00:00\",\n"
                    + "  \"modUser\" : \"dotcms.org.1\",\n"
                    + "  \"live\" : true,\n"
                    + "  \"working\" : true\n"
                    + "}", newSiteName);

            final var path = Path.of(workspace.sites().toString(), "test.json");
            Files.write(path, siteDescriptor.getBytes());
            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                commandLine.setErr(out);
                int status = commandLine.execute(SiteCommand.NAME, SitePush.NAME,
                        path.toFile().getAbsolutePath(), "--fail-fast", "-e");
                Assertions.assertEquals(ExitCode.OK, status);

                status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME, "--name", siteName);
                Assertions.assertEquals(ExitCode.OK, status);
            }
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Despite the number of times the same Site gets pulled, it should only be
     * created once locally Expected result: The WorkspaceManager should be able to create and
     * destroy a workspace
     *
     * @throws IOException
     */
    @Test
    @Order(7)
    void Test_Pull_Same_Site_Multiple_Times() throws IOException {
        final Workspace workspace = workspaceManager.getOrCreate();
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            for (int i = 0; i <= 5; i++) {
                int status = commandLine.execute(SiteCommand.NAME, SitePull.NAME, siteName,
                        "--workspace", workspace.root().toString());
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

    /**
     * <b>Command to test:</b> site pull <br>
     * <b>Given Scenario:</b> Test the site pull command. This test checks if the JSON site
     * file has a "dotCMSObjectType" field with the value "Site". <br>
     * <b>Expected Result:</b> The JSON site file should have a
     * "dotCMSObjectType" field with the value "Site".
     *
     * @throws IOException if there is an error reading the JSON site file
     */
    @Test
    @Order(8)
    void Test_Command_Site_Pull_Checking_JSON_DotCMS_Type() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();

        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final String newSiteName = String.format("new.dotcms.site%d", System.currentTimeMillis());

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            // Creating a new site
            int status = commandLine.execute(SiteCommand.NAME, SiteCreate.NAME, newSiteName);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // Pulling the site
            status = commandLine.execute(SiteCommand.NAME, SitePull.NAME, newSiteName,
                    "--workspace", workspace.root().toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // Reading the JSON site file to check if the json has a: "dotCMSObjectType" : "Site"
            final var siteFilePath = Path.of(workspace.sites().toString(), newSiteName + ".json");
            var json = Files.readString(siteFilePath);
            Assertions.assertTrue(json.contains("\"dotCMSObjectType\" : \"Site\""));

            // And now pushing the site back to the server to make sure the structure is still correct
            status = commandLine.execute(SiteCommand.NAME, SitePush.NAME,
                    siteFilePath.toAbsolutePath().toString(), "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * <b>Command to test:</b> site pull <br>
     * <b>Given Scenario:</b> Test the site pull command. This test checks if the YAML site
     * file has a "dotCMSObjectType" field with the value "Site". <br>
     * <b>Expected Result:</b> The YAML site file should have a
     * "dotCMSObjectType" field with the value "Site".
     *
     * @throws IOException if there is an error reading the YAML site file
     */
    @Test
    @Order(9)
    void Test_Command_Site_Pull_Checking_YAML_DotCMS_Type() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();

        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final String newSiteName = String.format("new.dotcms.site%d", System.currentTimeMillis());

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            // Creating a new site
            int status = commandLine.execute(SiteCommand.NAME, SiteCreate.NAME, newSiteName);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // Pulling the site
            status = commandLine.execute(SiteCommand.NAME, SitePull.NAME, newSiteName,
                    "-fmt", InputOutputFormat.YAML.toString(), "--workspace",
                    workspace.root().toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // Reading the YAML site file to check if the yaml has a: "dotCMSObjectType" : "Site"
            final var siteFilePath = Path.of(workspace.sites().toString(), newSiteName + ".yml");
            var json = Files.readString(siteFilePath);
            Assertions.assertTrue(json.contains("dotCMSObjectType: \"Site\""));

            // And now pushing the site back to the server to make sure the structure is still correct
            status = commandLine.execute(SiteCommand.NAME, SitePush.NAME,
                    siteFilePath.toAbsolutePath().toString(), "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Simply call list all Expected Result: Verify the command completes
     * successfully
     */
    @Test
    @Order(10)
    void Test_Command_Site_List_All() {
        final Set<String> uniqueSiteTest = new HashSet<>();
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME,
                    "--non-interactive");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            final String[] lines = output.split(System.lineSeparator());
            for (String line : lines) {
                Assert.assertFalse(uniqueSiteTest.contains(line));
                uniqueSiteTest.add(line);
            }
        }
    }

    /**
     * This tests will test the functionality of the site push command when pushing a folder,
     * checking the sites are properly add, updated and removed on the remote server.
     */
    @Test
    @Order(11)
    void Test_Command_Site_Folder_Push() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);

            final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

            // ╔══════════════════════╗
            // ║  Preparing the data  ║
            // ╚══════════════════════╝

            // --
            // Pulling all the existing sites created in other tests to avoid unwanted deletes
            var sitesResponse = siteAPI.getSites(
                    null,
                    null,
                    false,
                    false,
                    1,
                    1000
            );
            var pullCount = 0;
            if (sitesResponse != null && sitesResponse.entity() != null) {
                for (Site site : sitesResponse.entity()) {
                    var status = commandLine.execute(SiteCommand.NAME, SitePull.NAME,
                            site.hostName(),
                            "--workspace", workspace.root().toString());
                    Assertions.assertEquals(CommandLine.ExitCode.OK, status);
                    pullCount++;
                }
            }

            // ---
            // Creating a some test sites
            final String newSiteName1 = String.format("new.dotcms.site1-%d",
                    System.currentTimeMillis());
            var status = commandLine.execute(SiteCommand.NAME, SiteCreate.NAME, newSiteName1);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            final String newSiteName2 = String.format("new.dotcms.site2-%d",
                    System.currentTimeMillis());
            status = commandLine.execute(SiteCommand.NAME, SiteCreate.NAME, newSiteName2);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            final String newSiteName3 = String.format("new.dotcms.site3-%d",
                    System.currentTimeMillis());
            status = commandLine.execute(SiteCommand.NAME, SiteCreate.NAME, newSiteName3);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ---
            // Pulling the just created sites - We need the files in the sites folder
            // - Ignoring 1 site, in that way we can force a remove
            status = commandLine.execute(SiteCommand.NAME, SitePull.NAME, newSiteName1,
                    "--workspace", workspace.root().toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            status = commandLine.execute(SiteCommand.NAME, SitePull.NAME, newSiteName2,
                    "--workspace", workspace.root().toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ---
            // Renaming the site in order to force an update in the push
            final var newSiteName2Path = Path.of(workspace.sites().toString(),
                    newSiteName2 + ".json");
            var mappedSite2 = this.mapperService.map(
                    newSiteName2Path.toFile(),
                    SiteView.class
            );
            mappedSite2 = mappedSite2.withHostName(newSiteName2 + "-updated");
            var jsonContent = this.mapperService.objectMapper(newSiteName2Path.toFile())
                    .writeValueAsString(mappedSite2);
            Files.write(newSiteName2Path, jsonContent.getBytes());

            // ---
            // Creating a new site file
            final String newSiteName4 = String.format("new.dotcms.site4-%d",
                    System.currentTimeMillis());
            String siteDescriptor = String.format("{\n"
                    + "  \"siteName\" : \"%s\",\n"
                    + "  \"languageId\" : 1,\n"
                    + "  \"modDate\" : \"2023-05-05T00:13:25.242+00:00\",\n"
                    + "  \"modUser\" : \"dotcms.org.1\",\n"
                    + "  \"live\" : true,\n"
                    + "  \"working\" : true\n"
                    + "}", newSiteName4);

            final var path = Path.of(workspace.sites().toString(), "test.site4.json");
            Files.write(path, siteDescriptor.getBytes());

            // Make sure we have the proper amount of files in the sites folder
            try (Stream<Path> walk = Files.walk(workspace.sites())) {
                long count = walk.filter(Files::isRegularFile).count();
                Assertions.assertEquals(3 + pullCount, count);
            }

            // ╔═══════════════════════╗
            // ║  Pushing the changes  ║
            // ╚═══════════════════════╝
            status = commandLine.execute(SiteCommand.NAME, SitePush.NAME,
                    workspace.sites().toAbsolutePath().toString(),
                    "--removeSites", "--forceSiteExecution", "--fail-fast", "-e");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(
                    output.contains(
                            "Push Data: [3] Sites to push: (1 New - 1 Modified) - 1 to Delete"
                    )
            );

            // ╔══════════════════════════════╗
            // ║  Validating the information  ║
            // ╚══════════════════════════════╝
            var byName = siteAPI.findByName(
                    GetSiteByNameRequest.builder().siteName("default").build()
            );
            Assertions.assertEquals("default", byName.entity().siteName());

            byName = siteAPI.findByName(
                    GetSiteByNameRequest.builder().siteName(newSiteName1).build()
            );
            Assertions.assertEquals(newSiteName1, byName.entity().siteName());

            try {
                siteAPI.findByName(
                        GetSiteByNameRequest.builder().siteName(newSiteName2).build()
                );
                Assertions.fail(" 404 Exception should have been thrown here.");
            } catch (Exception e) {
                Assertions.assertTrue(e instanceof NotFoundException);
            }

            byName = siteAPI.findByName(
                    GetSiteByNameRequest.builder().siteName(newSiteName2 + "-updated").build()
            );
            Assertions.assertEquals(newSiteName2 + "-updated", byName.entity().siteName());

            try {
                siteAPI.findByName(
                        GetSiteByNameRequest.builder().siteName(newSiteName3).build()
                );
                Assertions.fail(" 404 Exception should have been thrown here.");
            } catch (Exception e) {
                Assertions.assertTrue(e instanceof NotFoundException);
            }

            byName = siteAPI.findByName(
                    GetSiteByNameRequest.builder().siteName(newSiteName4).build()
            );
            Assertions.assertEquals(newSiteName4, byName.entity().siteName());

        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * <b>Command to test:</b> site pull <br>
     * <b>Given Scenario:</b> Test the site pull command. This test pulls all the sites in the
     * default format (JSON). <br>
     * <b>Expected Result:</b> All the existing sites should be pulled and saved as JSON files.
     *
     * @throws IOException if there is an error pulling the sites
     */
    @Test
    @Order(12)
    void Test_Command_Site_Pull_Pull_All_Default_Format() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();

        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        // First we need to see if we already have sites to pull
        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        // --
        // Pulling all the existing sites to have a proper count
        var sitesResponse = siteAPI.getSites(
                null,
                null,
                false,
                false,
                1,
                1000
        );
        var sitesCount = 0;
        if (sitesResponse != null && sitesResponse.entity() != null) {
            sitesCount = sitesResponse.entity().size();
        }

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            // ---
            // Creating a some test sites
            final String newSiteName1 = String.format("new.dotcms.site1-%d",
                    System.currentTimeMillis());
            var status = commandLine.execute(SiteCommand.NAME, SiteCreate.NAME, newSiteName1);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            sitesCount++;

            final String newSiteName2 = String.format("new.dotcms.site2-%d",
                    System.currentTimeMillis());
            status = commandLine.execute(SiteCommand.NAME, SiteCreate.NAME, newSiteName2);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            sitesCount++;

            final String newSiteName3 = String.format("new.dotcms.site3-%d",
                    System.currentTimeMillis());
            status = commandLine.execute(SiteCommand.NAME, SiteCreate.NAME, newSiteName3);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            sitesCount++;

            // Pulling all sites
            status = commandLine.execute(SiteCommand.NAME, SitePull.NAME,
                    "--workspace", workspace.root().toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // Make sure we have the proper amount of JSON files in the sites folder
            try (Stream<Path> walk = Files.walk(workspace.sites())) {

                var jsonFiles = walk.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".json"))
                        .collect(Collectors.toList());

                // Check the count first
                Assertions.assertEquals(sitesCount, jsonFiles.size(),
                        "The number of JSON files does not match the expected sites count.");

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
     * <b>Command to test:</b> site pull <br>
     * <b>Given Scenario:</b> Test the site pull command. This test pulls all the sites in the
     * YAML format. <br>
     * <b>Expected Result:</b> All the existing sites should be pulled and saved as YAML files.
     *
     * @throws IOException if there is an error pulling the sites
     */
    @Test
    @Order(13)
    void Test_Command_Site_Pull_Pull_All_YAML_Format() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();

        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        // First we need to see if we already have sites to pull
        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        // --
        // Pulling all the existing sites to have a proper count
        var sitesResponse = siteAPI.getSites(
                null,
                null,
                false,
                false,
                1,
                1000
        );
        var sitesCount = 0;
        if (sitesResponse != null && sitesResponse.entity() != null) {
            sitesCount = sitesResponse.entity().size();
        }

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            // ---
            // Creating a some test sites
            final String newSiteName1 = String.format("new.dotcms.site1-%d",
                    System.currentTimeMillis());
            var status = commandLine.execute(SiteCommand.NAME, SiteCreate.NAME, newSiteName1);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            sitesCount++;

            final String newSiteName2 = String.format("new.dotcms.site2-%d",
                    System.currentTimeMillis());
            status = commandLine.execute(SiteCommand.NAME, SiteCreate.NAME, newSiteName2);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            sitesCount++;

            final String newSiteName3 = String.format("new.dotcms.site3-%d",
                    System.currentTimeMillis());
            status = commandLine.execute(SiteCommand.NAME, SiteCreate.NAME, newSiteName3);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            sitesCount++;

            // Pulling all sites
            status = commandLine.execute(SiteCommand.NAME, SitePull.NAME,
                    "--workspace", workspace.root().toString(),
                    "-fmt", InputOutputFormat.YAML.toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // Make sure we have the proper amount of JSON files in the sites folder
            try (Stream<Path> walk = Files.walk(workspace.sites())) {

                var files = walk.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".yml"))
                        .collect(Collectors.toList());

                // Check the count first
                Assertions.assertEquals(sitesCount, files.size(),
                        "The number of YAML files does not match the expected sites count.");

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
     * <b>Command to test:</b> site pull <br>
     * <b>Given Scenario:</b> Test the site pull command. This test pulls all the sites twice,
     * testing the override works properly.<br>
     * <b>Expected Result:</b> All the existing sites should be pulled and saved as YAML files.
     *
     * @throws IOException if there is an error pulling the sites
     */
    @Test
    @Order(14)
    void Test_Command_Site_Pull_Pull_All_Twice() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();

        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        // First we need to see if we already have sites to pull
        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        // --
        // Pulling all the existing sites to have a proper count
        var sitesResponse = siteAPI.getSites(
                null,
                null,
                false,
                false,
                1,
                1000
        );
        var sitesCount = 0;
        if (sitesResponse != null && sitesResponse.entity() != null) {
            sitesCount = sitesResponse.entity().size();
        }

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            // ---
            // Creating a some test sites
            final String newSiteName1 = String.format("new.dotcms.site1-%d",
                    System.currentTimeMillis());
            var status = commandLine.execute(SiteCommand.NAME, SiteCreate.NAME, newSiteName1);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            sitesCount++;

            final String newSiteName2 = String.format("new.dotcms.site2-%d",
                    System.currentTimeMillis());
            status = commandLine.execute(SiteCommand.NAME, SiteCreate.NAME, newSiteName2);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            sitesCount++;

            final String newSiteName3 = String.format("new.dotcms.site3-%d",
                    System.currentTimeMillis());
            status = commandLine.execute(SiteCommand.NAME, SiteCreate.NAME, newSiteName3);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            sitesCount++;

            // Pulling all sites
            status = commandLine.execute(SiteCommand.NAME, SitePull.NAME,
                    "--workspace", workspace.root().toString(),
                    "-fmt", InputOutputFormat.YAML.toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // Executing a second pull of all the sites
            status = commandLine.execute(SiteCommand.NAME, SitePull.NAME,
                    "--workspace", workspace.root().toString(),
                    "-fmt", InputOutputFormat.YAML.toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // Make sure we have the proper amount of JSON files in the sites folder
            try (Stream<Path> walk = Files.walk(workspace.sites())) {

                var files = walk.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".yml"))
                        .collect(Collectors.toList());

                // Check the count first
                Assertions.assertEquals(sitesCount, files.size(),
                        "The number of YAML files does not match the expected sites count.");

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
