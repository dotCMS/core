package com.dotcms.cli.command.language;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.AuthenticationContext;
import com.dotcms.api.LanguageAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.api.provider.ClientObjectMapper;
import com.dotcms.api.provider.YAMLMapperSupplier;
import com.dotcms.cli.command.CommandTest;
import com.dotcms.cli.common.InputOutputFormat;
import com.dotcms.common.WorkspaceManager;
import com.dotcms.model.config.Workspace;
import com.dotcms.model.language.Language;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.UUID;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class LanguageCommandIT extends CommandTest {

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    WorkspaceManager workspaceManager;

    @Inject
    RestClientFactory clientFactory;

    @BeforeEach
    public void setupTest() throws IOException {
        resetServiceProfiles();
        final String user = "admin@dotcms.com";
        final char[] passwd = "admin".toCharArray();
        authenticationContext.login(user, passwd);
    }

    /**
     * <b>Command to test:</b> language pull <br>
     * <b>Given Scenario:</b> Test the language pull command by id. This test will fail if the returned language is not English <br>
     * <b>Expected Result:</b> The language returned should be English
     * @throws JsonProcessingException
     */
    @Test
    void Test_Command_Language_Pull_By_Id() throws IOException {
        final Workspace workspace = workspaceManager.getOrCreate();
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(LanguageCommand.NAME, LanguagePull.NAME, "1", "--verbose", "--workspace", workspace.root().toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            final ObjectMapper mapper = new ClientObjectMapper().getContext(null);
            Language result = mapper.readValue(output, Language.class);
            Assertions.assertEquals(1, result.id().get());
        } finally {
            workspaceManager.destroy(workspace);
        }
    }

    /**
     * <b>Command to test:</b> language pull <br>
     * <b>Given Scenario:</b> Test the language pull command by iso code (en-US). This test will fail if the returned language is not English <br>
     * <b>Expected Result:</b> The language returned should be English
     * @throws JsonProcessingException
     */
    @Test
    void Test_Command_Language_Pull_By_IsoCode() throws IOException {
        final Workspace workspace = workspaceManager.getOrCreate();
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(LanguageCommand.NAME, LanguagePull.NAME, "en-US", "--verbose", "--workspace", workspace.root().toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            final ObjectMapper mapper = new ClientObjectMapper().getContext(null);
            Language result = mapper.readValue(output, Language.class);
            Assertions.assertEquals(1, result.id().get());
        } finally {
            workspaceManager.destroy(workspace);
        }
    }

    /**
     * <b>Command to test:</b> language pull <br>
     * <b>Given Scenario:</b> Test the language pull command by iso code (en-US). This test checks
     * if the JSON language file has a "dotCMSObjectType" field with the value "Language". <br>
     * <b>Expected Result:</b> The language returned should be English, and the JSON language file
     * should have a "dotCMSObjectType" field with the value "Language".
     *
     * @throws IOException if there is an error reading the JSON language file
     */
    @Test
    void Test_Command_Language_Pull_By_IsoCode_Checking_JSON_DotCMS_Type() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();

        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);

            int status = commandLine.execute(LanguageCommand.NAME, LanguagePull.NAME, "en-US",
                    "--workspace", workspace.root().toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // Reading the JSON language file to check if the json has a: "dotCMSObjectType" : "Language"
            final var languageFilePath = Path.of(workspace.languages().toString(), "en-us.json");
            var json = Files.readString(languageFilePath);
            Assertions.assertTrue(json.contains("\"dotCMSObjectType\" : \"Language\""));

            // And now pushing the language back to dotCMS to make sure the structure is still correct
            status = commandLine.execute(LanguageCommand.NAME, LanguagePush.NAME,
                    languageFilePath.toAbsolutePath().toString(), "-ff");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * <b>Command to test:</b> language pull <br>
     * <b>Given Scenario:</b> Test the language pull command by iso code (en-US). This test checks
     * if the YAML language file has a "dotCMSObjectType" field with the value "Language". <br>
     * <b>Expected Result:</b> The language returned should be English, and the YAML language file
     * should have a "dotCMSObjectType" field with the value "Language".
     *
     * @throws IOException if there is an error reading the YAML language file
     */
    @Test
    void Test_Command_Language_Pull_By_IsoCode_Checking_YAML_DotCMS_Type() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();

        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);

            int status = commandLine.execute(LanguageCommand.NAME, LanguagePull.NAME, "en-US",
                    "-fmt", InputOutputFormat.YAML.toString(), "--workspace",
                    workspace.root().toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // Reading the YAML language file to check if the yaml has a: "dotCMSObjectType" : "Language"
            final var languageFilePath = Path.of(workspace.languages().toString(), "en-us.yml");
            var json = Files.readString(languageFilePath);
            Assertions.assertTrue(json.contains("dotCMSObjectType: \"Language\""));

            // And now pushing the language back to dotCMS to make sure the structure is still correct
            status = commandLine.execute(LanguageCommand.NAME, LanguagePush.NAME,
                    languageFilePath.toAbsolutePath().toString(), "-ff");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * <b>Command to test:</b> language <br>
     * <b>Given Scenario:</b>Running language command will internally execute the language find command.
     * This test will fail if no languages are returned <br>
     * <b>Expected Result:</b> All the languages in the system should be returned
     */
    @Test
    void Test_Command_Language_Find() {
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(LanguageCommand.NAME, LanguageFind.NAME);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.contains("English"));
        }
    }

    /**
     * <b>Command to test:</b> language push<br>
     * <b>Given Scenario:</b>Test the language push command. A new language with iso code "es-VE" will be created.<br>
     * <b>Expected Result:</b> The language returned should be Spanish
     */
    @Test
    void Test_Command_Language_Push_byIsoCode() {
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(LanguageCommand.NAME, LanguagePush.NAME, "--byIso", "es-VE");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.contains("Spanish"));
        }
    }

    /**
     * <b>Command to test:</b> language push<br>
     * <b>Given Scenario:</b>Test the language push command. A new language with iso code "fr" will be created.<br>
     * <b>Expected Result:</b> The language returned should be French
     */
    @Test
    void Test_Command_Language_Push_byIsoCodeWithoutCountry() {
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(LanguageCommand.NAME, LanguagePush.NAME,
                    "--byIso", "fr");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // Checking we pushed the language correctly
            var foundLanguage = clientFactory.getClient(LanguageAPI.class).
                    getFromLanguageIsoCode("fr");
            Assertions.assertNotNull(foundLanguage);
            Assertions.assertNotNull(foundLanguage.entity());
            Assertions.assertTrue(foundLanguage.entity().language().isPresent());
            Assertions.assertEquals("French", foundLanguage.entity().language().get());

            // Cleaning up
            try {
                clientFactory.getClient(LanguageAPI.class).delete(
                        String.valueOf(foundLanguage.entity().id().get())
                );
            } catch (Exception e) {
                // Ignoring
            }
        }
    }

    /**
     * <b>Command to test:</b> language push<br>
     * <b>Given Scenario:</b> Test the language push command using a JSON file as an input.
     * A new language with iso code "it-IT" will be created.<br>
     * <b>Expected Result:</b> The language returned should be Italian
     */
    @Test
    void Test_Command_Language_Push_byFile_JSON() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            //Create a JSON file with the language to push
            final Language language = Language.builder().isoCode("it-it").languageCode("it-IT")
                    .countryCode("IT").language("Italian").country("Italy").build();
            final ObjectMapper mapper = new ClientObjectMapper().getContext(null);
            final var targetFilePath = Path.of(workspace.languages().toString(), "language.json");
            mapper.writeValue(targetFilePath.toFile(), language);
            commandLine.setOut(out);
            final int status = commandLine.execute(LanguageCommand.NAME, LanguagePush.NAME,
                    targetFilePath.toAbsolutePath().toString(), "-ff");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // Checking we pushed the language correctly
            var foundLanguage = clientFactory.getClient(LanguageAPI.class).
                    getFromLanguageIsoCode("it-IT");
            Assertions.assertNotNull(foundLanguage);
            Assertions.assertNotNull(foundLanguage.entity());
            Assertions.assertTrue(foundLanguage.entity().language().isPresent());
            Assertions.assertEquals("Italian", foundLanguage.entity().language().get());

            // Cleaning up
            try {
                clientFactory.getClient(LanguageAPI.class).delete(
                        String.valueOf(foundLanguage.entity().id().get())
                );
            } catch (Exception e) {
                // Ignoring
            }
        }
    }

    /**
     * <b>Command to test:</b> language push <br>
     * <b>Given Scenario:</b> Test the language push command using a YAML file as an input.
     * A new language with iso code "it-IT" will be created. <br>
     * <b>Expected Result:</b> The language returned should be Italian
     */
    @Test
    void Test_Command_Language_Push_byFile_YAML() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            //Create a YAML file with the language to push
            final Language language = Language.builder().isoCode("it-it").languageCode("it-IT")
                    .countryCode("IT").language("Italian").country("Italy").build();
            final ObjectMapper mapper = new YAMLMapperSupplier().get();
            final var targetFilePath = Path.of(workspace.languages().toString(), "language.yml");
            mapper.writeValue(targetFilePath.toFile(), language);
            commandLine.setOut(out);
            int status = commandLine.execute(LanguageCommand.NAME, LanguagePush.NAME,
                    targetFilePath.toAbsolutePath().toString(), "-ff");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // Checking we pushed the language correctly
            var foundLanguage = clientFactory.getClient(LanguageAPI.class).
                    getFromLanguageIsoCode("it-IT");
            Assertions.assertNotNull(foundLanguage);
            Assertions.assertNotNull(foundLanguage.entity());
            Assertions.assertTrue(foundLanguage.entity().language().isPresent());
            Assertions.assertEquals("Italian", foundLanguage.entity().language().get());

            // Cleaning up
            try {
                clientFactory.getClient(LanguageAPI.class).delete(
                        String.valueOf(foundLanguage.entity().id().get())
                );
            } catch (Exception e) {
                // Ignoring
            }
        }
    }

    /**
     * <b>Command to test:</b> language remove <br>
     * <b>Given Scenario:</b> Test the language remove command given a language iso code.<br>
     * <b>Expected Result:</b> The language with iso code "es-VE" should be removed
     */
    @Test
    void Test_Command_Language_Remove_byIsoCode() throws IOException {
        final Workspace workspace = workspaceManager.getOrCreate();
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);

            //A language with iso code "es-VE" is pushed
            commandLine.execute(LanguageCommand.NAME, LanguagePush.NAME, "--byIso", "es-VE");

            //We remove the language with iso code "es-VE"
            int status = commandLine.execute(LanguageCommand.NAME, LanguageRemove.NAME, "es-VE", "--cli-test");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            //We check that the language with iso code "es-VE" is not present
            status = commandLine.execute(LanguageCommand.NAME, LanguagePull.NAME, "es-VE", "--workspace", workspace.root().toString());
            Assertions.assertEquals(ExitCode.SOFTWARE, status);
        } finally {
            workspaceManager.destroy(workspace);
        }
    }

    /**
     * <b>Command to test:</b> language remove <br>
     * <b>Given Scenario:</b> Test the language remove command given a language id. <br>
     * <b>Expected Result:</b> The language with iso code "es-VE" should be removed
     */
    @Test
    void Test_Command_Language_Remove_byId() throws IOException {
        final Workspace workspace = workspaceManager.getOrCreate();
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            //A language with iso code "es-VE" is pushed (we are validating that the iso code is not case-sensitive)
            commandLine.execute(LanguageCommand.NAME, LanguagePush.NAME, "--byIso", "es-ve");
            commandLine.setOut(out);
            //we pull the language with iso code "es-VE" to get its id
            int status = commandLine.execute(LanguageCommand.NAME, LanguagePull.NAME, "es-VE", "--verbose", "--workspace", workspace.root().toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            final ObjectMapper mapper = new ClientObjectMapper().getContext(null);
            Language result = mapper.readValue(output, Language.class);


            //We remove the language with iso code "es-VE"
            status = commandLine.execute(LanguageCommand.NAME, LanguageRemove.NAME, String.valueOf(result.id().get()), "--cli-test");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            //We check that the language with iso code "es-VE" is not present
            status = commandLine.execute(LanguageCommand.NAME, LanguagePull.NAME, "es-VE", "--workspace", workspace.root().toString());
            Assertions.assertEquals(ExitCode.SOFTWARE, status);
        } finally {
            workspaceManager.destroy(workspace);
        }
    }

    /**
     * Given scenario: Despite the number of times the same lang gets pulled, it should only be created once
     * Expected result: The WorkspaceManager should be able to create and destroy a workspace
     * @throws IOException
     */
    @Test
    void Test_Pull_Same_Language_Multiple_Times() throws IOException {
        final Workspace workspace = workspaceManager.getOrCreate();
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String lang = "en-US".toLowerCase();
            for (int i=0; i<= 5; i++) {
                final int status = commandLine.execute(LanguageCommand.NAME, LanguagePull.NAME, lang, "--workspace", workspace.root().toString());
                Assertions.assertEquals(CommandLine.ExitCode.OK, status);
                System.out.println("Lang Pulled: " + i);
            }

            final String fileName = String.format("%s.json", lang);
            final Path path = Path.of(workspace.languages().toString(), fileName);
            Assertions.assertTrue(Files.exists(path),String.format("The file [%s] should exist", path));

            try (Stream<Path> walk = Files.walk(workspace.languages())) {
                long count = walk.filter(p -> Files.isRegularFile(p) && p.getFileName().toString()
                        .startsWith(lang.toLowerCase())).count();
                Assertions.assertEquals(1, count);
            }

        } finally {
            workspaceManager.destroy(workspace);
        }
    }

    /**
     * This tests will test the functionality of the language push command when pushing a folder,
     * checking that the languages are properly add, updated and removed on the remote server.
     */
    @Test
    void Test_Command_Language_Folder_Push() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        final Workspace workspace = workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {

            commandLine.setOut(out);

            // Pulling the en us language
            int status = commandLine.execute(LanguageCommand.NAME, LanguagePull.NAME, "en-US",
                    "-fmt", InputOutputFormat.YAML.toString(), "--workspace",
                    workspace.root().toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // Make sure the language it is really there
            final var languageUSPath = Path.of(workspace.languages().toString(), "en-us.yml");
            var json = Files.readString(languageUSPath);
            Assertions.assertTrue(json.contains("countryCode: \"US\""));

            //Now, create a couple of files with new languages to push
            // Italian
            final Language italian = Language.builder().
                    isoCode("it-it").
                    languageCode("it-IT").
                    countryCode("IT").
                    language("Italian").
                    country("Italy").
                    build();
            var mapper = new ClientObjectMapper().getContext(null);
            var targetItalianFilePath = Path.of(workspace.languages().toString(), "it-it.json");
            mapper.writeValue(targetItalianFilePath.toFile(), italian);

            // French
            final Language french = Language.builder().
                    isoCode("fr").
                    language("French").
                    build();
            var targetFrenchFilePath = Path.of(workspace.languages().toString(), "fr.json");
            mapper.writeValue(targetFrenchFilePath.toFile(), french);

            // ---
            // Pushing the languages
            commandLine.setOut(out);
            status = commandLine.execute(LanguageCommand.NAME, LanguagePush.NAME,
                    workspace.languages().toString(), "-ff");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            String output = writer.toString();
            Assertions.assertTrue(
                    output.contains(
                            "Push Data: [2] Languages to push: (2 New - 0 Modified)"
                    )
            );

            // ---
            // Checking we pushed the languages correctly
            // Italian
            var italianResponse = clientFactory.getClient(LanguageAPI.class).
                    getFromLanguageIsoCode("it-IT");
            Assertions.assertNotNull(italianResponse);
            Assertions.assertNotNull(italianResponse.entity());
            Assertions.assertTrue(italianResponse.entity().language().isPresent());
            Assertions.assertEquals("Italian", italianResponse.entity().language().get());

            // French
            var frenchResponse = clientFactory.getClient(LanguageAPI.class).
                    getFromLanguageIsoCode("fr");
            Assertions.assertNotNull(frenchResponse);
            Assertions.assertNotNull(frenchResponse.entity());
            Assertions.assertTrue(frenchResponse.entity().language().isPresent());
            Assertions.assertEquals("French", frenchResponse.entity().language().get());

            // ---
            // Pulling italian, we need the file with the updated id
            status = commandLine.execute(LanguageCommand.NAME, LanguagePull.NAME, "it-IT",
                    "-fmt", InputOutputFormat.JSON.toString().toUpperCase(),
                    "--workspace", workspace.root().toString());
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);

            // ---
            // Now we remove a file and test the removal is working properly
            Files.delete(targetFrenchFilePath);

            // Editing the italian file to validate the update works
            final Language updatedItalian = Language.builder().
                    isoCode("it-va").
                    id(italianResponse.entity().id().get()).
                    languageCode("it-va").
                    countryCode("VA").
                    language("Italian").
                    country("Vatican City").
                    build();
            mapper = new ClientObjectMapper().getContext(null);
            targetItalianFilePath = Path.of(workspace.languages().toString(), "it-it.json");
            mapper.writeValue(targetItalianFilePath.toFile(), updatedItalian);

            // Pushing the languages with the remove language flag
            status = commandLine.execute(LanguageCommand.NAME, LanguagePush.NAME,
                    workspace.languages().toString(), "-ff", "-rl");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            output = writer.toString();
            Assertions.assertTrue(
                    output.contains(
                            "Push Data: [2] Languages to push: (0 New - 1 Modified) - 1 to Delete"
                    )
            );

            // ---
            // Make sure Italian-VA is there and Italian-IT and French is not

            // Italian-Vatican city - Should be there
            italianResponse = clientFactory.getClient(LanguageAPI.class).
                    getFromLanguageIsoCode("it-VA");
            Assertions.assertNotNull(italianResponse);
            Assertions.assertNotNull(italianResponse.entity());
            Assertions.assertTrue(italianResponse.entity().language().isPresent());
            Assertions.assertEquals("Italian", italianResponse.entity().language().get());
            Assertions.assertEquals("Vatican City", italianResponse.entity().country().get());

            var allLanguages = clientFactory.getClient(LanguageAPI.class).list();
            Assertions.assertNotNull(allLanguages);
            Assertions.assertEquals(2, allLanguages.entity().size());
            Assertions.assertTrue(
                    allLanguages.entity().stream()
                            .anyMatch(l -> l.isoCode().equalsIgnoreCase("it-VA"))
            );
            Assertions.assertTrue(
                    allLanguages.entity().stream()
                            .anyMatch(l -> l.isoCode().equalsIgnoreCase("en-US"))
            );

        } finally {

            // Cleaning up
            try {
                var foundItalian = clientFactory.getClient(LanguageAPI.class).
                        getFromLanguageIsoCode("it-IT");
                clientFactory.getClient(LanguageAPI.class).delete(
                        String.valueOf(foundItalian.entity().id().get())
                );
            } catch (Exception e) {
                // Ignoring
            }

            // Cleaning up
            try {
                var foundItalian = clientFactory.getClient(LanguageAPI.class).
                        getFromLanguageIsoCode("it-VA");
                clientFactory.getClient(LanguageAPI.class).delete(
                        String.valueOf(foundItalian.entity().id().get())
                );
            } catch (Exception e) {
                // Ignoring
            }

            try {
                var foundFrench = clientFactory.getClient(LanguageAPI.class).
                        getFromLanguageIsoCode("fr");
                clientFactory.getClient(LanguageAPI.class).delete(
                        String.valueOf(foundFrench.entity().id().get())
                );
            } catch (Exception e) {
                // Ignoring
            }
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
