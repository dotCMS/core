package com.dotcms.cli.command;

import com.dotcms.model.config.ServiceBean;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@QuarkusTest
class InstanceCommandTest extends CommandTest {

    @BeforeEach
    public void setupTest() throws IOException {
        resetServiceProfiles();
    }

    /**
     * Scenario: Build a yml file with available profiles then Only pass the param list
     * Expected: We should be able to see the list of available instances
     */
    @Test
    @Order(2)
    void Test_Command_Instance_Pass_Only_List_Param() throws IOException {

            resetServiceProfiles().persist(ServiceBean.builder().name("demo").active(false).url(new URL("https://demo.dotcms.com")).build());

            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(InstanceCommand.NAME);
                Assertions.assertEquals(ExitCode.OK, status);
                final String output = writer.toString();
                Assertions.assertTrue(
                        output.contains("Profile [default], Uri [http://localhost:8080]"),()->output);
                Assertions.assertTrue(
                        output.contains("Profile [demo], Uri [https://demo.dotcms.com]"), ()->output);
            }
    }

    /**
     * Scenario: -a or --activate expects an argument We're not passing it here.
     * Expected: We should get the USAGE code
     */
    @Test
    @Order(3)
    void Test_Command_Instance_Pass_Activate_Param_No_Profile() {
        final String[] options = {"-act", "--activate"};
        for (final String option : options) {
            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setErr(out);
                final int status = commandLine.execute(InstanceCommand.NAME, option);
                Assertions.assertEquals(ExitCode.USAGE, status);
                final String output = writer.toString();
                Assertions.assertTrue(output.contains("Missing required parameter "));
            }
        }
    }

    /**
     * Scenario: -a or --activate expects an argument We're passing an invalid value here
     * Expected: Error Code 1. Software error
     */
    @Test
    @Order(4)
    void Test_Command_Instance_Pass_Activate_Param_Include_Invalid_Instance() {
        final String instance = "lol";
        final String[] options = {"-act", "--activate"};
        for (final String option : options) {
            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setErr(out);
                final int status = commandLine.execute(InstanceCommand.NAME,
                        String.format("%s=%s", option, instance));
                Assertions.assertEquals(ExitCode.SOFTWARE, status);
                final String output = writer.toString();
                Assertions.assertTrue(output.contains(String.format(
                        "The instance name [%s] does not match any configured server! Use --list option.",
                        instance)),()->output);
            }
        }
    }

    /**
     * Scenario: -a or --activate expects an argument We're passing a valid instance
     * Expected: Success code
     * @throws IOException
     */
    @Test
    @Order(5)
    void Test_Command_Instance_Pass_Activate_Param_Include_Valid_Instance()
            throws IOException {

        final Optional<ServiceBean> active = serviceManager.selected();
        Assertions.assertTrue(active.isPresent());
        //Let's activate demo since we know is an included profile
        final String newProfile = "demo";
        serviceManager.persist(ServiceBean.builder().name(newProfile).url(new URL("https://demo.dotcms.com")).active(false).build());
        final Optional<ServiceBean> inactive = serviceManager.services().stream()
                .filter(serviceBean -> !serviceBean.active()).findFirst();
        Assertions.assertTrue(inactive.isPresent());

        final String[] options = {"-act", "--activate"};
        for (final String option : options) {
            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(InstanceCommand.NAME,
                        String.format("%s=%s", option, newProfile));
                Assertions.assertEquals(ExitCode.OK, status);
                final String output = writer.toString();
                Assertions.assertTrue(output.contains(
                        String.format("The instance name [%s] is now the active one.",
                                newProfile)),()->output);
            }
        }
    }


}
