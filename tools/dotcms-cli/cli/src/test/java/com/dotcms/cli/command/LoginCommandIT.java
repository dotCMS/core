package com.dotcms.cli.command;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.model.config.ServiceBean;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class LoginCommandIT extends CommandTest {

    @Inject
    RestClientFactory clientFactory;

    @Inject
    ServiceManager serviceManager;

    @BeforeEach
    public void setupTest() throws IOException {
        resetServiceProfiles();
    }

    /**
     * Scenario: If we do not pass the required params we should get exit code 2
     */
    @Test
    @Order(1)
    void Test_Command_Login_No_Params()  {
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try(PrintWriter out = new PrintWriter(writer)){
            commandLine.setErr(out);
            final int status = commandLine.execute(LoginCommand.NAME);
            Assertions.assertEquals(ExitCode.USAGE, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.contains("Missing required options:"));
            Assertions.assertTrue(output.contains("Once an instance is selected. Use this command to open a session"));
        }
    }

    /**
     * Scenario: Pass valid credentials
     * Expect: A Success message and exit code 0
     */
    @Test
    @Order(2)
    void Test_Command_Login_With_Params_Expect_Successful_Login()  {
        final String user = "admin@dotCMS.com";

        final String [][] options = {
                {"--user=admin@dotCMS.com","--password=admin"},
                {"-u=admin@dotCMS.com","-p=admin"}
        };

        for (final String [] option:options) {
            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try(PrintWriter out = new PrintWriter(writer)){
                commandLine.setOut(out);
                final int status = commandLine.execute(LoginCommand.NAME,option[0],option[1]);
                Assertions.assertEquals(ExitCode.OK, status);
                final String output = writer.toString();
                Assertions.assertTrue(output.contains(String.format("Successfully logged-in as [%s]",user)));
            }
        }
    }

    /**
     * Scenario: Pass invalid credentials
     * Expect: Should fail inform about that and exit code 1
     */
    @Test
    @Order(3)
    void Test_Command_Login_With_Params_Expect_Login_Reject()  {

        final String [][] options = {
                {"--user=admin@dotCMS.com","--password=lol"},
                {"-u=admin@dotCMS.com","-p=lol"}
        };

        for (final String [] option:options) {

            final CommandLine commandLine = createCommand();
            final StringWriter writer = new StringWriter();
            try(PrintWriter out = new PrintWriter(writer)){
                commandLine.setErr(out);
                final int status = commandLine.execute(LoginCommand.NAME,option[0],option[1]);
                Assertions.assertEquals(ExitCode.SOFTWARE, status);
                final String output = writer.toString();
                Assertions.assertTrue(output.contains("[ERROR]"));
                Assertions.assertTrue(output.contains(
                        "HTTP 401 Forbidden: You don't have permission to access this resource."));
            }
        }

    }

    /**
     * Scenario: Pass invalid token
     * Expect: Should fail inform about that and exit code 1
     */
    @Test
    @Order(4)
    void Test_Command_Login_With_Invalid_Token() {
        final String token = RandomStringUtils.randomAlphabetic(100);
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try(PrintWriter out = new PrintWriter(writer)){
            commandLine.setErr(out);
            final int status = commandLine.execute(LoginCommand.NAME, "--token", token);
            Assertions.assertEquals(ExitCode.SOFTWARE, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.contains("[ERROR]"));
            Assertions.assertTrue(output.contains(
                    "HTTP 401 Forbidden: You don't have permission to access this resource."));
        }
    }


    /**
     * Scenario: Pass valid token
     * Expect: Should login successfully and exit code 0 finally we validate the token is stored
     */
    @Test
    @Order(5)
    void Test_Command_Login_With_Valid_Token() throws IOException{
        final String token = requestToken();
        //Now that we have the token, let's use it to login
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try(PrintWriter out = new PrintWriter(writer)){
            commandLine.setOut(out);
            final int status = commandLine.execute(LoginCommand.NAME, "--token", token);
            Assertions.assertEquals(ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.contains("Successfully logged-in with token"));
        }

        final Optional<ServiceBean> selected = serviceManager.selected();
        Assertions.assertTrue(selected.isPresent());
        final ServiceBean serviceBean = selected.get();
        Assertions.assertNotNull(serviceBean.credentials());
        Assertions.assertNotNull(serviceBean.credentials().token());
        Assertions.assertEquals(token, new String(serviceBean.credentials().token()));

    }

}
