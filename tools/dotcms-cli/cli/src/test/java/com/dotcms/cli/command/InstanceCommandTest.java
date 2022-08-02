package com.dotcms.cli.command;

import com.dotcms.api.client.ServiceManager;
import com.dotcms.model.config.ServiceBean;
import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@QuarkusTest
public class InstanceCommandTest {

    @Inject
    PicocliCommandLineFactory factory;

    @Inject
    ServiceManager serviceManager;

    @BeforeEach
    public void setupTest() throws IOException {
        serviceManager.removeAll()
                .persist(ServiceBean.builder().name("default").active(true).build());
    }

    /**
     * Scenario: If we do not pass the required params
     * Expected: Instance expects at least one param or both
     */
    @Test
    @Order(1)
    public void Test_Command_Instance_No_Params_Expect_Usage_Print() {
        final CommandLine commandLine = factory.create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setErr(out);
            final int status = commandLine.execute(InstanceCommand.NAME);
            Assertions.assertEquals(ExitCode.USAGE, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.contains(
                    "Prints a list of available dotCMS instances. Use to activate/switch dotCMS"));
        }
    }

    /**
     * Scenario: Only pass the param list
     * Expected: We should be able to see the list of available instances
     */
    @Test
    @Order(2)
    public void Test_Command_Instance_Pass_Only_List_Param() {
        final String[] options = {"-l", "--list"};
        for (final String option : options) {
            final CommandLine commandLine = factory.create();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(InstanceCommand.NAME, option);
                Assertions.assertEquals(ExitCode.OK, status);
                final String output = writer.toString();
                Assertions.assertTrue(
                        output.contains("Profile [default], Uri [http://localhost:8080/api]"));
                Assertions.assertTrue(
                        output.contains("Profile [demo], Uri [https://demo.dotcms.com/api]"));
            }
        }
    }

    /**
     * Scenario: -a or --activate expects an argument We're not passing it here.
     * Expected: We should get the USAGE code
     */
    @Test
    @Order(3)
    public void Test_Command_Instance_Pass_Activate_Param_No_Profile() {
        final String[] options = {"-a", "--activate"};
        for (final String option : options) {
            final CommandLine commandLine = factory.create();
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
    public void Test_Command_Instance_Pass_Activate_Param_Include_Invalid_Instance() {
        final String instance = "lol";
        final String[] options = {"-a", "--activate"};
        for (final String option : options) {
            final CommandLine commandLine = factory.create();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setErr(out);
                final int status = commandLine.execute(InstanceCommand.NAME,
                        String.format("%s=%s", option, instance));
                Assertions.assertEquals(ExitCode.SOFTWARE, status);
                final String output = writer.toString();
                Assertions.assertTrue(output.contains(String.format(
                        "The instance name [%s] does not match any configured server! Use --list option.",
                        instance)));
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
    public void Test_Command_Instance_Pass_Activate_Param_Include_Valid_Instance()
            throws IOException {

        final Optional<ServiceBean> active = serviceManager.selected();
        Assertions.assertTrue(active.isPresent());
        //Let's activate demo since we know is an included profile
        final String newProfile = "demo";
        serviceManager.persist(ServiceBean.builder().name(newProfile).active(false).build());
        final Optional<ServiceBean> inactive = serviceManager.services().stream()
                .filter(serviceBean -> !serviceBean.active()).findFirst();
        Assertions.assertTrue(inactive.isPresent());

        final String[] options = {"-a", "--activate"};
        for (final String option : options) {
            final CommandLine commandLine = factory.create();
            final StringWriter writer = new StringWriter();
            try (PrintWriter out = new PrintWriter(writer)) {
                commandLine.setOut(out);
                final int status = commandLine.execute(InstanceCommand.NAME,
                        String.format("%s=%s", option, newProfile));
                Assertions.assertEquals(ExitCode.OK, status);
                final String output = writer.toString();
                Assertions.assertTrue(output.contains(
                        String.format("The instance name [%s] is now the active profile.",
                                newProfile)));
            }
        }
    }


}
