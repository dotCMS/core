package com.dotcms.cli.command;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.client.model.AuthenticationParam;
import com.dotcms.api.client.model.RemoteURLParam;
import com.dotcms.cli.command.language.LanguageCommand;
import com.dotcms.cli.command.language.LanguagePull;
import com.dotcms.common.WorkspaceManager;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class RemoteURLIT extends CommandTest {

    @Inject
    WorkspaceManager workspaceManager;

    @BeforeEach
    void setUp() throws IOException {
        serviceManager.removeAll();
        resetParams();
    }

    @AfterAll
    static void tearDown() {
        resetParams();
    }

    /**
     * Test case for the scenario where the dotcms-url is used but the token is missing.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Test
    void Test_Missing_Token() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            commandLine.setErr(out);

            // Pulling all languages
            var status = commandLine.execute(LanguageCommand.NAME, LanguagePull.NAME,
                    "--workspace", tempFolder.toAbsolutePath().toString(),
                    "--dotcms-url", "http://localhost:8080");
            Assertions.assertEquals(ExitCode.USAGE, status);

            final String output = writer.toString();
            Assertions.assertTrue(
                    output.contains("The token is required when the dotCMS URL is set"));

        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Test case for the scenario where the dotcms-url is invalid.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Test
    void Test_Invalid_URL() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            commandLine.setErr(out);

            // Pulling all languages
            var status = commandLine.execute(LanguageCommand.NAME, LanguagePull.NAME,
                    "--workspace", tempFolder.toAbsolutePath().toString(),
                    "--dotcms-url", "not valid URL");
            Assertions.assertEquals(ExitCode.USAGE, status);

            final String output = writer.toString();
            Assertions.assertTrue(output.contains("Invalid dotCMS URL"));

        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Test case for the scenario where the dotcms-url is provided but the token is invalid. The
     * main idea of this test is to validate we are bypassing the configuration check when the token
     * and dotCMS URL are set.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Test
    void Test_Invalid_Token() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            commandLine.setErr(out);

            // Pulling all languages
            var status = commandLine.execute(LanguageCommand.NAME, LanguagePull.NAME,
                    "--workspace", tempFolder.toAbsolutePath().toString(),
                    "--dotcms-url", "http://localhost:8080",
                    "--token", "invalid token");
            Assertions.assertEquals(ExitCode.SOFTWARE, status);

            final String output = writer.toString();
            Assertions.assertTrue(output.contains("Invalid User"));

        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * This method tests the scenario where no configuration is found in the dotCMS instances. The
     * main idea of this test is to validate that the CLI is requesting the user to configure as it
     * used to do before the dotCMS URL and token were introduced.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Test
    void Test_No_Configuration_Found() throws IOException {

        // Create a temporal folder for the workspace
        var tempFolder = createTempFolder();
        workspaceManager.getOrCreate(tempFolder);

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            commandLine.setErr(out);

            // Pulling all languages
            var status = commandLine.execute(LanguageCommand.NAME, LanguagePull.NAME,
                    "--workspace", tempFolder.toAbsolutePath().toString());
            Assertions.assertEquals(ExitCode.SOFTWARE, status);

            final String output = writer.toString();
            Assertions.assertTrue(
                    output.contains("No dotCMS configured instances were found"));

        } finally {
            deleteTempDirectory(tempFolder);
        }
    }

    /**
     * Resets the parameters to avoid side effects between tests.
     */
    static void resetParams() {
        final var remoteURLParam = Arc.container().instance(RemoteURLParam.class);
        if (remoteURLParam.isAvailable() && remoteURLParam.get().getURL().isPresent()) {
            remoteURLParam.get().reset();
        }

        final var authenticationParam = Arc.container().instance(AuthenticationParam.class);
        if (authenticationParam.isAvailable() && authenticationParam.get().getToken().isPresent()) {
            authenticationParam.get().reset();
        }
    }

}
