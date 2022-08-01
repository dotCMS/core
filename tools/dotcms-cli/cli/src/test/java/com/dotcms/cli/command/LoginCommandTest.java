package com.dotcms.cli.command;

import com.dotcms.api.client.ServiceManager;
import com.dotcms.model.config.ServiceBean;
import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@QuarkusTest
public class LoginCommandTest {

    @Inject
    PicocliCommandLineFactory factory;

    @Inject
    ServiceManager serviceManager;

    @BeforeEach
    public void setupTest() throws IOException {
        serviceManager.removeAll().persist(ServiceBean.builder().name("default").active(true).build());
    }

    /**
     * Scenario: If we do not pass the required params we should get exit code 2
     */
    @Test
    @Order(1)
    public void Test_Command_Login_No_Params()  {
        final CommandLine commandLine = factory.create();
        final StringWriter writer = new StringWriter();
        try(PrintWriter out = new PrintWriter(writer)){
            commandLine.setErr(out);
            final int status = commandLine.execute(LoginCommand.NAME);
            Assertions.assertEquals(ExitCode.USAGE, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.contains("Missing required options: '--user', '--password'"));
            Assertions.assertTrue(output.contains("Once a profile is selected. Use this command to open a session Expects a user"));
        }
    }

    /**
     * Scenario: Pass valid credentials
     * Expect: A Success message and exit code 0
     */
    @Test
    @Order(2)
    public void Test_Command_Login_With_Params_Expect_Successful_Login()  {
        final String user = "admin@dotCMS.com";
        final String passwd = "admin";
        final CommandLine commandLine = factory.create();
        final StringWriter writer = new StringWriter();
        try(PrintWriter out = new PrintWriter(writer)){
            commandLine.setOut(out);
            final int status = commandLine.execute(LoginCommand.NAME,String.format("--user=%s",user),String.format("--password=%s",passwd));
            Assertions.assertEquals(ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.contains(String.format("Successfully logged-in as [%s]",user)));
        }
    }

    /**
     * Scenario: Pass invalid credentials
     * Expect: Should fail inform about that and exit code 1
     */
    @Test
    @Order(3)
    public void Test_Command_Login_With_Params_Expect_Login_Reject()  {
        final String user = "admin@dotCMS.com";
        final String passwd = "lol";
        final CommandLine commandLine = factory.create();
        final StringWriter writer = new StringWriter();
        try(PrintWriter out = new PrintWriter(writer)){
            commandLine.setErr(out);
            final int status = commandLine.execute(LoginCommand.NAME,String.format("--user=%s",user),String.format("--password=%s",passwd));
            Assertions.assertEquals(ExitCode.SOFTWARE, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.contains("[ERROR]"));
            Assertions.assertTrue(output.contains("Unable to login."));
        }
    }



}
