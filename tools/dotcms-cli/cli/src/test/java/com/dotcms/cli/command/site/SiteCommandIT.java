package com.dotcms.cli.command.site;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.AuthenticationContext;
import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.MapperService;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.cli.command.CommandTest;
import com.dotcms.cli.common.InputOutputFormat;
import com.dotcms.cli.common.SitesTestHelperService;
import com.dotcms.cli.common.SitesTestHelperService.SiteDescriptorCreationResult;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.config.Workspace;
import com.dotcms.model.site.GetSiteByNameRequest;
import com.dotcms.model.site.Site;
import com.dotcms.model.site.SiteVariableView;
import com.dotcms.model.site.SiteView;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

    @Inject
    SitesTestHelperService sitesTestHelper;

    @BeforeEach
    public void setupTest() throws IOException {
        resetServiceProfiles();
        final String user = "admin@dotcms.com";
        final char[] passwd = "admin".toCharArray();
        authenticationContext.login(user, passwd);
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
        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);
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

        final var siteName = sitesTestHelper.createSiteOnServer().siteName();

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            final int status = commandLine.execute(SiteCommand.NAME, SiteCopy.NAME,
                    "--idOrName", siteName);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
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
        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);
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

            // Creating a test site
            var result = sitesTestHelper.createSiteDescriptor(workspace, false);

            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                commandLine.setErr(out);
                int status = commandLine.execute(SiteCommand.NAME, SitePush.NAME,
                        result.path().toFile().getAbsolutePath(), "--fail-fast", "-e");
                Assertions.assertEquals(ExitCode.OK, status);

                status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME,
                        "--name", result.siteName());
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
        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);
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
            // Creating some test sites
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
            // Creating some test sites
            sitesTestHelper.createSiteOnServer().siteName();
            sitesCount++;

            sitesTestHelper.createSiteOnServer().siteName();
            sitesCount++;

            sitesTestHelper.createSiteOnServer().siteName();
            sitesCount++;

            // Pulling all sites
            final var status = commandLine.execute(SiteCommand.NAME, SitePull.NAME,
                    "--workspace", workspace.root().toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // Make sure we have the proper number of JSON files in the sites folder
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
            // Creating some test sites
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
            // Creating some test sites
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
     * Given scenario: Create a new site using a file and the push command, then verify the site
     * descriptor was updated with the proper identifier.
     */
    @Test
    @Order(15)
    void Test_Create_From_File_via_Push_Checking_Auto_Update() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();

        try {
            final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

            // Creating a test site
            var result = sitesTestHelper.createSiteDescriptor(workspace, false);

            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                commandLine.setErr(out);

                // Pushing the sites
                int status = commandLine.execute(SiteCommand.NAME, SitePush.NAME,
                        result.path().toFile().getAbsolutePath(), "--fail-fast", "-e");
                Assertions.assertEquals(ExitCode.OK, status);

                // Validating the site was created
                status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME,
                        "--name", result.siteName());
                Assertions.assertEquals(ExitCode.OK, status);

                // ---
                // Now validating the auto update updated the site descriptor
                var updatedSite = this.mapperService.map(
                        result.path().toFile(),
                        SiteView.class
                );
                Assertions.assertEquals(result.siteName(), updatedSite.siteName());
                Assertions.assertEquals(1, updatedSite.languageId());
                Assertions.assertNotNull(updatedSite.identifier());
                Assertions.assertFalse(updatedSite.identifier().isBlank());
            }
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Create a new site using a file and the push command disabling the auto
     * update, then verify the site descriptor was not updated.
     */
    @Test
    @Order(16)
    void Test_Create_From_File_via_Push_With_Auto_Update_Disabled() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();

        try {
            final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

            // Creating a test site
            var result = sitesTestHelper.createSiteDescriptor(workspace, false);

            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                commandLine.setErr(out);

                // Pushing the sites
                int status = commandLine.execute(SiteCommand.NAME, SitePush.NAME,
                        result.path().toFile().getAbsolutePath(), "--fail-fast", "-e",
                        "--disable-auto-update");
                Assertions.assertEquals(ExitCode.OK, status);

                // Validating the site was created
                status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME,
                        "--name", result.siteName());
                Assertions.assertEquals(ExitCode.OK, status);

                // ---
                // Now validating the auto update did not update the site descriptor
                var updatedSite = this.mapperService.map(
                        result.path().toFile(),
                        SiteView.class
                );
                Assertions.assertEquals(result.siteName(), updatedSite.siteName());
                Assertions.assertEquals(1, updatedSite.languageId());
                Assertions.assertNull(updatedSite.identifier());
            }
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Create a couple of sites using files, one of them is marked as the default
     * site. Then change the default site directly in the server and push the changes again without
     * changing the site descriptors.
     * <p>
     * Expected Result: The default site should change according to what we have in the workspace
     * sites folder.
     */
    @Test
    @Order(17)
    void Test_Default_Site_Change() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();

        try {
            final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

            // ╔══════════════════════╗
            // ║  Preparing the data  ║
            // ╚══════════════════════╝
            var result = sitesTestHelper.createSiteDescriptor(workspace, false);
            var result1 = sitesTestHelper.createSiteDescriptor(workspace, true);

            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                commandLine.setErr(out);

                // ╔═══════════════════════╗
                // ║  Pushing the changes  ║
                // ╚═══════════════════════╝
                int status = commandLine.execute(SiteCommand.NAME, SitePush.NAME,
                        workspace.sites().toAbsolutePath().toString(), "--fail-fast", "-e");
                Assertions.assertEquals(ExitCode.OK, status);

                // Validating the sites were created
                status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME,
                        "--name", result.siteName());
                Assertions.assertEquals(ExitCode.OK, status);
                status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME,
                        "--name", result1.siteName());
                Assertions.assertEquals(ExitCode.OK, status);

                // ---
                // Now validating the updated the site descriptors
                var site1 = this.mapperService.map(
                        result.path().toFile(),
                        SiteView.class
                );
                Assertions.assertEquals(result.siteName(), site1.siteName());
                Assertions.assertEquals(1, site1.languageId());
                Assertions.assertNotNull(site1.identifier());
                Assertions.assertFalse(site1.identifier().isBlank());
                Assertions.assertNotEquals(Boolean.TRUE, site1.isDefault());

                var site2 = this.mapperService.map(
                        result1.path().toFile(),
                        SiteView.class
                );
                Assertions.assertEquals(result1.siteName(), site2.siteName());
                Assertions.assertEquals(1, site2.languageId());
                Assertions.assertNotNull(site2.identifier());
                Assertions.assertFalse(site2.identifier().isBlank());
                Assertions.assertEquals(Boolean.TRUE, site2.isDefault());

                // ╔═════════════════════════════╗
                // ║  Changing the default site  ║
                // ╚═════════════════════════════╝
                final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);
                var changeResult = siteAPI.makeDefault(site1.identifier());
                Assertions.assertTrue(changeResult.entity());

                // ╔═════════════════╗
                // ║  Pushing again  ║
                // ╚═════════════════╝
                status = commandLine.execute(SiteCommand.NAME, SitePush.NAME,
                        workspace.sites().toAbsolutePath().toString(), "--fail-fast", "-e");
                Assertions.assertEquals(ExitCode.OK, status);

                // ╔════════════════════════════════════════════════════════════╗
                // ║  Validating the default site is back to the initial state  ║
                // ╚════════════════════════════════════════════════════════════╝
                var defaultSiteResult = siteAPI.defaultSite();
                Assertions.assertEquals(
                        site2.identifier(),
                        defaultSiteResult.entity().identifier()
                );
            }
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * This method tests the functionality of archiving a site. It creates a temporary folder for
     * the workspace and performs the following steps: 1. Creates two sites in the workspace. 2.
     * Pushes the changes to create the sites. 3. Validates that the sites were created
     * successfully. 4. Validates the site descriptors for both sites. 5. Marks one of the sites as
     * archived updating its site descriptor. 6. Pushes the changes to update the site. 7. Validates
     * that the site descriptor reflects the archived status. 8. Requests the site from the server
     * to ensure the data matches between the server and the local file.
     *
     * @throws IOException if there is an error creating the temporary folder or writing to files
     */
    @Test
    @Order(18)
    void Test_Archive_Site() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();

        try {
            final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

            // ╔══════════════════════╗
            // ║  Preparing the data  ║
            // ╚══════════════════════╝
            var result = sitesTestHelper.createSiteDescriptor(workspace, false);
            var result1 = sitesTestHelper.createSiteDescriptor(workspace, false);

            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                commandLine.setErr(out);

                // ╔═══════════════════════════════════════════════════╗
                // ║  Pushing the changes in order to crete the sites  ║
                // ╚═══════════════════════════════════════════════════╝
                int status = commandLine.execute(SiteCommand.NAME, SitePush.NAME,
                        workspace.sites().toAbsolutePath().toString(), "--fail-fast", "-e");
                Assertions.assertEquals(ExitCode.OK, status);

                // Validating the sites were created
                status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME,
                        "--name", result.siteName());
                Assertions.assertEquals(ExitCode.OK, status);
                status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME,
                        "--name", result1.siteName());
                Assertions.assertEquals(ExitCode.OK, status);

                // ---
                // Now validating the updated the site descriptors
                var site1 = this.mapperService.map(
                        result.path().toFile(),
                        SiteView.class
                );
                validateSiteDescriptor(result, site1, false);

                var site2 = this.mapperService.map(
                        result1.path().toFile(),
                        SiteView.class
                );
                validateSiteDescriptor(result1, site2, false);

                // ╔══════════════════════════════╗
                // ║  Marking a site as archived  ║
                // ╚══════════════════════════════╝
                site2 = this.mapperService.map(
                        result1.path().toFile(),
                        SiteView.class
                );
                site2 = site2.withIsArchived(true);
                var jsonContent = this.mapperService
                        .objectMapper(result1.path().toFile())
                        .writeValueAsString(site2);
                Files.write(result1.path(), jsonContent.getBytes());

                // ╔═════════════════╗
                // ║  Pushing again  ║
                // ╚═════════════════╝
                status = commandLine.execute(SiteCommand.NAME, SitePush.NAME,
                        workspace.sites().toAbsolutePath().toString(), "--fail-fast", "-e");
                Assertions.assertEquals(ExitCode.OK, status);

                // ╔═════════════════════════════════════════════════════════════╗
                // ║  Validating we have the proper data in the site descriptor  ║
                // ╚═════════════════════════════════════════════════════════════╝
                site2 = this.mapperService.map(
                        result1.path().toFile(),
                        SiteView.class
                );
                validateSiteDescriptor(result1, site2, true);

                // Requesting the site from the server to make sure the data matches between the
                // server and the local file
                final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);
                final ResponseEntityView<SiteView> serverSite = siteAPI.findById(
                        site2.identifier()
                );
                Assertions.assertNotNull(serverSite);
                Assertions.assertNotNull(serverSite.entity());
                Assertions.assertNotNull(serverSite.entity().identifier());
                Assertions.assertEquals(site2.siteName(), serverSite.entity().siteName());
                Assertions.assertEquals(site2.identifier(), serverSite.entity().identifier());
                Assertions.assertEquals(site2.isArchived(), serverSite.entity().isArchived());
            }
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Create a site with variables, push the changes to the server.
     * <p>
     * Expected result: The site should be created with the variables.
     *
     * @throws IOException if there is an error creating the temporary folder or writing to files
     */
    @Test
    @Order(19)
    void Test_Command_Create_Site_With_Variables() throws IOException {

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

            var result = sitesTestHelper.createSiteDescriptor(workspace, false);

            // Pushing the changes to create the site
            int status = commandLine.execute(SiteCommand.NAME, SitePush.NAME,
                    workspace.sites().toAbsolutePath().toString(), "--fail-fast", "-e");
            Assertions.assertEquals(ExitCode.OK, status);

            // Validating the site was created
            status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME,
                    "--name", result.siteName());
            Assertions.assertEquals(ExitCode.OK, status);

            // ╔══════════════════════════════╗
            // ║  Validating the information  ║
            // ╚══════════════════════════════╝

            // Now validating the updated site descriptors
            var mappedSiteWithVariables = this.mapperService.map(
                    result.path().toFile(),
                    SiteView.class
            );
            Assertions.assertEquals(result.siteName(), mappedSiteWithVariables.siteName());
            Assertions.assertEquals(1, mappedSiteWithVariables.languageId());
            Assertions.assertNotNull(mappedSiteWithVariables.identifier());
            Assertions.assertFalse(mappedSiteWithVariables.identifier().isBlank());
            Assertions.assertNotNull(mappedSiteWithVariables.variables());
            Assertions.assertEquals(5, mappedSiteWithVariables.variables().size());

            // Now check the server
            var byName = siteAPI.findByName(
                    GetSiteByNameRequest.builder().siteName(result.siteName()).build()
            );
            Assertions.assertEquals(result.siteName(), byName.entity().siteName());
            Assertions.assertNotNull(byName.entity().variables());
            Assertions.assertEquals(5, byName.entity().variables().size());

            // Validating everything matches between the local site and the server
            var localSiteVariables = mappedSiteWithVariables.variables();
            var serverSiteVariables = byName.entity().variables();
            for (int i = 0; i < localSiteVariables.size(); i++) {
                var localVar = localSiteVariables.get(i);
                var serverVar = serverSiteVariables.get(i);
                Assertions.assertEquals(localVar.name(), serverVar.name());
                Assertions.assertEquals(localVar.key(), serverVar.key());
                Assertions.assertEquals(localVar.value(), serverVar.value());
            }

        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Create a site without variables, push the changes to the server.
     * <p>
     * Expected result: The site should be created without variables.
     *
     * @throws IOException if there is an error creating the temporary folder or writing to files
     */
    @Test
    @Order(20)
    void Test_Command_Create_Site_Without_Variables() throws IOException {

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

            var result = sitesTestHelper.createSiteDescriptor(workspace, false, false);

            // Pushing the changes to create the site
            int status = commandLine.execute(SiteCommand.NAME, SitePush.NAME,
                    workspace.sites().toAbsolutePath().toString(), "--fail-fast", "-e");
            Assertions.assertEquals(ExitCode.OK, status);

            // Validating the site was created
            status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME,
                    "--name", result.siteName());
            Assertions.assertEquals(ExitCode.OK, status);

            // ╔══════════════════════════════╗
            // ║  Validating the information  ║
            // ╚══════════════════════════════╝

            // Now validating the updated site descriptors
            var mappedSiteWithVariables = this.mapperService.map(
                    result.path().toFile(),
                    SiteView.class
            );
            Assertions.assertEquals(result.siteName(), mappedSiteWithVariables.siteName());
            Assertions.assertEquals(1, mappedSiteWithVariables.languageId());
            Assertions.assertNotNull(mappedSiteWithVariables.identifier());
            Assertions.assertFalse(mappedSiteWithVariables.identifier().isBlank());
            Assertions.assertNotNull(mappedSiteWithVariables.variables());
            Assertions.assertEquals(0, mappedSiteWithVariables.variables().size());

            // Now check the server
            var byName = siteAPI.findByName(
                    GetSiteByNameRequest.builder().siteName(result.siteName()).build()
            );
            Assertions.assertEquals(result.siteName(), byName.entity().siteName());
            Assertions.assertNotNull(byName.entity().variables());
            Assertions.assertEquals(0, byName.entity().variables().size());

        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Create a site with variables, push the changes to the server, update the site
     * variables a couple of times and push those changes.
     * <br>
     * Expected result: The site should be created with the variables, and the variables should be
     * updated for each modification in the server.
     *
     * @throws IOException if there is an error creating the temporary folder or writing to files
     */
    @Test
    @Order(21)
    void Test_Command_Updating_Site_Variables() throws IOException {

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

            var result = sitesTestHelper.createSiteDescriptor(workspace, false, false);

            // Pushing the changes to create the site
            int status = commandLine.execute(SiteCommand.NAME, SitePush.NAME,
                    workspace.sites().toAbsolutePath().toString(), "--fail-fast", "-e");
            Assertions.assertEquals(ExitCode.OK, status);

            // Validating the site was created
            status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME,
                    "--name", result.siteName());
            Assertions.assertEquals(ExitCode.OK, status);

            // ---
            // Now validating the updated the site descriptors
            var mappedSiteWithVariables = this.mapperService.map(
                    result.path().toFile(),
                    SiteView.class
            );
            validateLocalDescriptor(result.siteName(), mappedSiteWithVariables, 0);

            // ╔════════════════════╗
            // ║  Adding variables  ║
            // ╚════════════════════╝
            var siteWithVariables = this.mapperService.map(
                    result.path().toFile(),
                    SiteView.class
            );
            siteWithVariables = siteWithVariables.withVariables(
                    SiteVariableView.builder()
                            .name("var1Name")
                            .key("var1Key")
                            .value("var1Value")
                            .build(),
                    SiteVariableView.builder()
                            .name("var2Name")
                            .key("var2Key")
                            .value("var2Value")
                            .build(),
                    SiteVariableView.builder()
                            .name("var3Name")
                            .key("var3Key")
                            .value("var3Value")
                            .build(),
                    SiteVariableView.builder()
                            .name("var4Name")
                            .key("var4Key")
                            .value("var4Value")
                            .build(),
                    SiteVariableView.builder()
                            .name("var5Name")
                            .key("var5Key")
                            .value("var5Value")
                            .build()
            );
            var jsonContent = this.mapperService
                    .objectMapper(result.path().toFile())
                    .writeValueAsString(siteWithVariables);
            Files.write(result.path(), jsonContent.getBytes());

            // Pushing again
            status = commandLine.execute(SiteCommand.NAME, SitePush.NAME,
                    workspace.sites().toAbsolutePath().toString(), "--fail-fast", "-e");
            Assertions.assertEquals(ExitCode.OK, status);

            // ╔══════════════════════════════╗
            // ║  Validating the information  ║
            // ╚══════════════════════════════╝
            // Now validating the updated the site descriptors
            mappedSiteWithVariables = this.mapperService.map(
                    result.path().toFile(),
                    SiteView.class
            );
            validateLocalDescriptor(result.siteName(), mappedSiteWithVariables, 5);

            // Now check the server
            var byName = siteAPI.findByName(
                    GetSiteByNameRequest.builder().siteName(result.siteName()).build()
            );
            validateServerVariables(result.siteName(), byName.entity(), 5);

            // Validating everything matches between the local site and the server
            validateVariablesMatches(siteWithVariables, byName.entity());

            // ╔══════════════════════════╗
            // ║  Updating the variables  ║
            // ╚══════════════════════════╝
            siteWithVariables = this.mapperService.map(
                    result.path().toFile(),
                    SiteView.class
            );
            siteWithVariables = siteWithVariables.withVariables(
                    SiteVariableView.builder()
                            .name("var5Name")
                            .key("var5KeyUpdated")
                            .value("var5Value")
                            .build(),
                    SiteVariableView.builder()
                            .name("var3NameUpdated")
                            .key("var3Key")
                            .value("var3ValueUpdated")
                            .build(),
                    SiteVariableView.builder()
                            .name("var2Name")
                            .key("var2Key")
                            .value("var2Value")
                            .build()
            );
            jsonContent = this.mapperService
                    .objectMapper(result.path().toFile())
                    .writeValueAsString(siteWithVariables);
            Files.write(result.path(), jsonContent.getBytes());

            // Pushing again
            status = commandLine.execute(SiteCommand.NAME, SitePush.NAME,
                    workspace.sites().toAbsolutePath().toString(), "--fail-fast", "-e");
            Assertions.assertEquals(ExitCode.OK, status);

            // ╔══════════════════════════════════════════╗
            // ║  Validating the information was updated  ║
            // ╚══════════════════════════════════════════╝

            // Now validating the updated the site descriptors
            mappedSiteWithVariables = this.mapperService.map(
                    result.path().toFile(),
                    SiteView.class
            );
            validateLocalDescriptor(result.siteName(), mappedSiteWithVariables, 3);

            // Now check the server
            byName = siteAPI.findByName(
                    GetSiteByNameRequest.builder().siteName(result.siteName()).build()
            );
            validateServerVariables(result.siteName(), byName.entity(), 3);

            // Validating everything matches between the local site and the server
            validateVariablesMatches(siteWithVariables, byName.entity());

            // ╔════════════════════════════════╗
            // ║  Updating again the variables  ║
            // ╚════════════════════════════════╝
            siteWithVariables = this.mapperService.map(
                    result.path().toFile(),
                    SiteView.class
            );
            siteWithVariables = siteWithVariables.withVariables(
                    SiteVariableView.builder()
                            .name("var4Name")
                            .key("var4Key")
                            .value("var4Value")
                            .build(),
                    SiteVariableView.builder()
                            .name("var1Name")
                            .key("var1Key")
                            .value("var1Value")
                            .build(),
                    SiteVariableView.builder()
                            .name("var2Name")
                            .key("var2Key")
                            .value("var2Value")
                            .build(),
                    SiteVariableView.builder()
                            .name("var5Name")
                            .key("var5Key")
                            .value("var5Value")
                            .build(),
                    SiteVariableView.builder()
                            .name("var3Name")
                            .key("var3Key")
                            .value("var3Value")
                            .build()
            );
            jsonContent = this.mapperService
                    .objectMapper(result.path().toFile())
                    .writeValueAsString(siteWithVariables);
            Files.write(result.path(), jsonContent.getBytes());

            // Pushing again
            status = commandLine.execute(SiteCommand.NAME, SitePush.NAME,
                    workspace.sites().toAbsolutePath().toString(), "--fail-fast", "-e");
            Assertions.assertEquals(ExitCode.OK, status);

            // ╔══════════════════════════════════════════╗
            // ║  Validating the information was updated  ║
            // ╚══════════════════════════════════════════╝

            // Now validating the updated the site descriptors
            mappedSiteWithVariables = this.mapperService.map(
                    result.path().toFile(),
                    SiteView.class
            );
            validateLocalDescriptor(result.siteName(), mappedSiteWithVariables, 5);

            // Now check the server
            byName = siteAPI.findByName(
                    GetSiteByNameRequest.builder().siteName(result.siteName()).build()
            );
            validateServerVariables(result.siteName(), byName.entity(), 5);

            // Validating everything matches between the local site and the server
            validateVariablesMatches(siteWithVariables, byName.entity());

        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Create a site with variables, push the changes to the server, update the site
     * variables including duplicated variables and push those changes.
     * <br>
     * Expected result: The site should be created with the variables, and the variables should be
     * updated handling properly the duplicated variables, no duplications should return from the
     * server.
     *
     * @throws IOException if there is an error creating the temporary folder or writing to files
     */
    @Test
    @Order(22)
    void Test_Command_Duplicated_Variables() throws IOException {

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

            var result = sitesTestHelper.createSiteDescriptor(workspace, false, false);

            // Pushing the changes to create the site
            int status = commandLine.execute(SiteCommand.NAME, SitePush.NAME,
                    workspace.sites().toAbsolutePath().toString(), "--fail-fast", "-e");
            Assertions.assertEquals(ExitCode.OK, status);

            // Validating the site was created
            status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME,
                    "--name", result.siteName());
            Assertions.assertEquals(ExitCode.OK, status);

            // ---
            // Now validating the updated the site descriptors
            var mappedSiteWithVariables = this.mapperService.map(
                    result.path().toFile(),
                    SiteView.class
            );
            validateLocalDescriptor(result.siteName(), mappedSiteWithVariables, 0);

            // ╔═══════════════════════════════╗
            // ║  Adding duplicated variables  ║
            // ╚═══════════════════════════════╝
            var siteWithVariables = this.mapperService.map(
                    result.path().toFile(),
                    SiteView.class
            );
            siteWithVariables = siteWithVariables.withVariables(
                    SiteVariableView.builder()
                            .name("var1Name")
                            .key("var1Key")
                            .value("var1Value")
                            .build(),
                    SiteVariableView.builder()
                            .name("var2Name")
                            .key("var2Key")
                            .value("var2Value")
                            .build(),
                    SiteVariableView.builder()
                            .name("var3Name")
                            .key("var3Key")
                            .value("var3Value")
                            .build(),
                    SiteVariableView.builder()
                            .name("var3Name")
                            .key("var3Key")
                            .value("var3Value")
                            .build(),
                    SiteVariableView.builder()
                            .name("var4Name")
                            .key("var4Key")
                            .value("var4Value")
                            .build(),
                    SiteVariableView.builder()
                            .name("var5Name")
                            .key("var5Key")
                            .value("var5Value")
                            .build(),
                    SiteVariableView.builder()
                            .name("var5Name")
                            .key("var5Key")
                            .value("var5Value")
                            .build(),
                    SiteVariableView.builder()
                            .name("var5Name")
                            .key("var5Key")
                            .value("var5Value")
                            .build()
            );
            var jsonContent = this.mapperService
                    .objectMapper(result.path().toFile())
                    .writeValueAsString(siteWithVariables);
            Files.write(result.path(), jsonContent.getBytes());

            // Pushing again
            status = commandLine.execute(SiteCommand.NAME, SitePush.NAME,
                    workspace.sites().toAbsolutePath().toString(), "--fail-fast", "-e");
            Assertions.assertEquals(ExitCode.OK, status);

            // ╔══════════════════════════════╗
            // ║  Validating the information  ║
            // ╚══════════════════════════════╝
            // Now validating the updated the site descriptors
            mappedSiteWithVariables = this.mapperService.map(
                    result.path().toFile(),
                    SiteView.class
            );
            validateLocalDescriptor(result.siteName(), mappedSiteWithVariables, 5);

            // Now check the server
            var byName = siteAPI.findByName(
                    GetSiteByNameRequest.builder().siteName(result.siteName()).build()
            );
            validateServerVariables(result.siteName(), byName.entity(), 5);

            // Applying the same cleanup we do in the server to remove the duplicated variables
            final var uniqueSiteVariables = new ArrayList<>(siteWithVariables.variables().stream().
                    collect(Collectors.toMap(
                            SiteVariableView::key, // key is the HostVariable's `key`
                            siteVariable -> siteVariable,  // value is the HostVariable itself
                            (existing, replacement) -> existing)) // if there's a conflict, keep the existing
                    .values()
            );

            // Validating everything matches between the local site and the server
            validateVariablesMatches(uniqueSiteVariables, byName.entity());

        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Create a site with variables, push the changes to the server, update the site
     * variables including variables with empty keys and push those changes.
     * <br>
     * Expected result: The operation should fail because the keys are required for the variables.
     *
     * @throws IOException if there is an error creating the temporary folder or writing to files
     */
    @Test
    @Order(23)
    void Test_Command_Variables_With_Empty_Keys() throws IOException {

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

            var result = sitesTestHelper.createSiteDescriptor(workspace, false, false);

            // Pushing the changes to create the site
            int status = commandLine.execute(SiteCommand.NAME, SitePush.NAME,
                    workspace.sites().toAbsolutePath().toString(), "--fail-fast", "-e");
            Assertions.assertEquals(ExitCode.OK, status);

            // Validating the site was created
            status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME,
                    "--name", result.siteName());
            Assertions.assertEquals(ExitCode.OK, status);

            // ---
            // Now validating the updated the site descriptors
            var mappedSiteWithVariables = this.mapperService.map(
                    result.path().toFile(),
                    SiteView.class
            );
            validateLocalDescriptor(result.siteName(), mappedSiteWithVariables, 0);

            // ╔════════════════════════════════════╗
            // ║  Adding variables with empty keys  ║
            // ╚════════════════════════════════════╝
            var siteWithVariables = this.mapperService.map(
                    result.path().toFile(),
                    SiteView.class
            );
            siteWithVariables = siteWithVariables.withVariables(
                    SiteVariableView.builder()
                            .name("var1Name")
                            .key("var1Key")
                            .value("var1Value")
                            .build(),
                    SiteVariableView.builder()
                            .name("var2Name")
                            .key("")
                            .value("var2Value")
                            .build()
            );
            var jsonContent = this.mapperService
                    .objectMapper(result.path().toFile())
                    .writeValueAsString(siteWithVariables);
            Files.write(result.path(), jsonContent.getBytes());

            // Pushing again
            status = commandLine.execute(SiteCommand.NAME, SitePush.NAME,
                    workspace.sites().toAbsolutePath().toString(), "--fail-fast", "-e");
            Assertions.assertEquals(ExitCode.SOFTWARE, status);

            final String output = writer.toString();
            Assertions.assertTrue(
                    output.contains("Site Variable key is required"));
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Create a site with variables, push the changes to the server, update the site
     * variables including variables with keys with a invalid format and push those changes.
     * <br>
     * Expected result: The operation should fail because the keys should only contain characters.
     *
     * @throws IOException if there is an error creating the temporary folder or writing to files
     */
    @Test
    @Order(24)
    void Test_Command_Variables_With_Invalid_Key_Format() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);
            commandLine.setErr(out);

            final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

            // ╔══════════════════════╗
            // ║  Preparing the data  ║
            // ╚══════════════════════╝

            var result = sitesTestHelper.createSiteDescriptor(workspace, false, false);

            // Pushing the changes to create the site
            int status = commandLine.execute(SiteCommand.NAME, SitePush.NAME,
                    workspace.sites().toAbsolutePath().toString(), "--fail-fast", "-e");
            Assertions.assertEquals(ExitCode.OK, status);

            // Validating the site was created
            status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME,
                    "--name", result.siteName());
            Assertions.assertEquals(ExitCode.OK, status);

            // ---
            // Now validating the updated the site descriptors
            var mappedSiteWithVariables = this.mapperService.map(
                    result.path().toFile(),
                    SiteView.class
            );
            validateLocalDescriptor(result.siteName(), mappedSiteWithVariables, 0);

            // ╔══════════════════════════════════════╗
            // ║  Adding variables with invalid keys  ║
            // ╚══════════════════════════════════════╝
            var siteWithVariables = this.mapperService.map(
                    result.path().toFile(),
                    SiteView.class
            );
            siteWithVariables = siteWithVariables.withVariables(
                    SiteVariableView.builder()
                            .name("var1Name")
                            .key("var1Key")
                            .value("var1Value")
                            .build(),
                    SiteVariableView.builder()
                            .name("var2Name")
                            .key("var2Key invalid")
                            .value("var2Value")
                            .build()
            );
            var jsonContent = this.mapperService
                    .objectMapper(result.path().toFile())
                    .writeValueAsString(siteWithVariables);
            Files.write(result.path(), jsonContent.getBytes());

            // Pushing again
            status = commandLine.execute(SiteCommand.NAME, SitePush.NAME,
                    workspace.sites().toAbsolutePath().toString(), "--fail-fast", "-e");
            Assertions.assertEquals(ExitCode.SOFTWARE, status);

            final String output = writer.toString();
            Assertions.assertTrue(
                    output.contains("Site Variable key should only contains characters"));
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Given scenario: Find a site using the site name. Authentication is done using a token.
     * Expected Result: The site should be found
     *
     * @throws IOException
     */
    @Test
    @Order(25)
    void Test_Find_Site_Command_Authenticate_With_Token() throws IOException {
        final String token = requestToken();
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(SiteCommand.NAME, SiteFind.NAME, "--name",
                    siteName, "--token", token);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.startsWith("name:"));
        }
    }

    /**
     * Validate the site descriptor by comparing it with the result of site descriptor creation.
     *
     * @param result   The site descriptor creation result.
     * @param site     The site view to be validated.
     * @param archived A boolean indicating if the site is archived or not.
     */
    private void validateSiteDescriptor(final SiteDescriptorCreationResult result,
            final SiteView site, final boolean archived) {

        Assertions.assertEquals(result.siteName(), site.siteName());
        Assertions.assertEquals(1, site.languageId());
        Assertions.assertNotNull(site.identifier());
        Assertions.assertFalse(site.identifier().isBlank());
        Assertions.assertEquals(archived, site.isArchived());
    }

    /**
     * Validates the local descriptor of a site against the expected values.
     *
     * @param siteName                The expected site name.
     * @param mappedSiteWithVariables The site view object containing the mapped site with
     *                                variables.
     * @param expectedVariablesSize   The expected number of variables in the mapped site.
     */
    private void validateLocalDescriptor(final String siteName,
            final SiteView mappedSiteWithVariables, final int expectedVariablesSize) {

        Assertions.assertEquals(siteName, mappedSiteWithVariables.siteName());
        Assertions.assertEquals(1, mappedSiteWithVariables.languageId());
        Assertions.assertNotNull(mappedSiteWithVariables.identifier());
        Assertions.assertFalse(mappedSiteWithVariables.identifier().isBlank());
        Assertions.assertNotNull(mappedSiteWithVariables.variables());
        Assertions.assertEquals(expectedVariablesSize, mappedSiteWithVariables.variables().size());
    }

    /**
     * Validates the server variables against the expected values.
     *
     * @param siteName              The expected site name.
     * @param serverSite            The SiteView object representing the server site.
     * @param expectedVariablesSize The expected size of the server variables list.
     */
    private void validateServerVariables(final String siteName, final SiteView serverSite,
            final int expectedVariablesSize) {

        Assertions.assertEquals(siteName, serverSite.siteName());
        Assertions.assertNotNull(serverSite.variables());
        Assertions.assertEquals(expectedVariablesSize, serverSite.variables().size());
    }

    /**
     * Validates that the variables in the "localSite" match the variables in the "remoteSite".
     *
     * @param localSite  The local SiteView object containing the variables to be validated.
     * @param remoteSite The remote SiteView object containing the expected variables.
     */
    private void validateVariablesMatches(final SiteView localSite, final SiteView remoteSite) {
        validateVariablesMatches(localSite.variables(), remoteSite);
    }

    /**
     * Validates that the variables in the "localSite" match the variables in the "remoteSite".
     *
     * @param localSiteVariables The local SiteVariableView list containing the variables to be
     *                           validated.
     * @param remoteSite         The remote SiteView object containing the expected variables.
     */
    private void validateVariablesMatches(final List<SiteVariableView> localSiteVariables,
            final SiteView remoteSite) {

        var localSiteVariablesCopy = new ArrayList<>(localSiteVariables);
        localSiteVariablesCopy.sort(Comparator.comparing(SiteVariableView::key));
        var serverSiteVariables = remoteSite.variables();

        Assertions.assertEquals(localSiteVariablesCopy.size(), serverSiteVariables.size());

        for (int i = 0; i < localSiteVariablesCopy.size(); i++) {
            var localVar = localSiteVariablesCopy.get(i);
            var serverVar = serverSiteVariables.get(i);
            Assertions.assertEquals(localVar.name(), serverVar.name());
            Assertions.assertEquals(localVar.key(), serverVar.key());
            Assertions.assertEquals(localVar.value(), serverVar.value());
        }
    }

}
