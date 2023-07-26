package com.dotcms.cli.command;

import com.dotcms.api.AuthenticationContext;
import com.dotcms.model.config.CredentialsBean;
import com.dotcms.model.config.ServiceBean;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.*;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

import javax.inject.Inject;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;


@QuarkusTest
public class StatusCommandTest extends CommandTest {

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
        //Destroy the config file that say what dotCMS instance we're connected to.
        resetServiceProfiles();
        //in case any other test has already logged in.
        authenticationContext.reset();
    }

    /**
     * Scenario: We want to see the status and no profile has been selected
     * Expected: We should get exit code 1 (Error) since we're not able to see our status unless at least 1 profile is selected and the respective message
     */
    @Test
    public void Test_Command_Status_No_Profiles() {

        serviceManager.removeAll();

        final Optional<ServiceBean> selected = serviceManager.selected();
        Assertions.assertTrue(selected.isEmpty());

        final CommandLine commandLine = factory.create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(StatusCommand.NAME);
            Assertions.assertEquals(ExitCode.SOFTWARE, status);
            Assertions.assertTrue(writer.toString()
                    .contains("No active profile is configured Please use instance Command."));
        }
    }

    /**
     * Scenario: In this scenario we want to see our status and we have selected a profile already
     * Expected: We should get informed what profile is active but we should be told we're not logged in again exit code 1 (Error)
     * @throws IOException
     */
    @Test
    public void Test_Command_Status_Default_Profile_Not_Logged_In() throws IOException {

        resetServiceProfiles();

        final CommandLine commandLine = factory.create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(StatusCommand.NAME);
            Assertions.assertEquals(ExitCode.SOFTWARE, status);
            Assertions.assertTrue(writer.toString().contains(
                    "Active instance is [default] API is [http://localhost:8080/api] No active user Use login Command."));
        }
    }

    /**
     * Scenario: In this scenario we want to see our status but the selected profile has expired credentials
     * Expected: We should get informed and get an Exit code 1
     * @throws IOException
     */
    @Test
    public void Test_Command_Status_Default_Profile_Invalid_Credentials() throws IOException {

        final String user = "admin@dotCMS.com";
        final String token = "not-a-valid-token"; //it could have expired

        serviceManager.removeAll().persist(ServiceBean.builder().name("default").active(true)
                .credentials(
                        CredentialsBean.builder().user(user).token(token.toCharArray()).build())
                .build());

        final CommandLine commandLine = factory.create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(StatusCommand.NAME);
            Assertions.assertEquals(ExitCode.SOFTWARE, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.contains(String.format(
                    "Active instance is [default] API is [http://localhost:8080/api] User [%s]",
                    user)));
            Assertions.assertTrue(
                    output.contains("Current profile does not have a logged in user."));
        }
    }

    /**
     * Scenario: In this scenario we want to see our status using a valid profile and credentials
     * Expected: Only in this case we should expect success exit code
     * @throws IOException
     */
    @Test
    public void Test_Command_Status_Valid_Profile_And_User() throws IOException {

        final String user = "admin@dotCMS.com";
        final String passwd = "admin";

        resetServiceProfiles();
        authenticationContext.login(user, passwd.toCharArray());

        //Test the user and credential got stored after an OK login
        final Optional<ServiceBean> selected = serviceManager.selected();
        Assertions.assertTrue(selected.isPresent());

        final CommandLine commandLine = factory.create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(StatusCommand.NAME);
            Assertions.assertEquals(ExitCode.OK, status);
            Assertions.assertTrue(writer.toString().contains("You're currently logged in as"));
        }
    }


}
