//package com.dotcms.cli.command;
//
//import com.dotcms.api.client.ServiceManager;
//import com.dotcms.model.config.ServiceBean;
//import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
//import io.quarkus.test.junit.QuarkusTest;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.io.StringWriter;
//import javax.inject.Inject;
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Order;
//import org.junit.jupiter.api.Test;
//import picocli.CommandLine;
//import picocli.CommandLine.ExitCode;
//
//@QuarkusTest
//public class LoginCommandTest extends CommandTest {
//
//    @BeforeAll
//    public static void beforeAll() {
//        disableAnsi();
//    }
//
//    @AfterAll
//    public static void afterAll() {
//        enableAnsi();
//    }
//
//    @BeforeEach
//    public void setupTest() throws IOException {
//        resetServiceProfiles();
//    }
//
//    /**
//     * Scenario: If we do not pass the required params we should get exit code 2
//     */
//    @Test
//    @Order(1)
//    void Test_Command_Login_No_Params()  {
//        final CommandLine commandLine = factory.create();
//        final StringWriter writer = new StringWriter();
//        try(PrintWriter out = new PrintWriter(writer)){
//            commandLine.setErr(out);
//            final int status = commandLine.execute(LoginCommand.NAME);
//            Assertions.assertEquals(ExitCode.USAGE, status);
//            final String output = writer.toString();
//            Assertions.assertTrue(output.contains("Missing required options:"));
//            Assertions.assertTrue(output.contains("Once an instance is selected. Use this command to open a session"));
//        }
//    }
//
//    /**
//     * Scenario: Pass valid credentials
//     * Expect: A Success message and exit code 0
//     */
//    @Test
//    @Order(2)
//    void Test_Command_Login_With_Params_Expect_Successful_Login()  {
//        final String user = "admin@dotCMS.com";
//
//        final String [][] options = {
//                {"--user=admin@dotCMS.com","--password=admin"},
//                {"-u=admin@dotCMS.com","-p=admin"}
//        };
//
//        for (final String [] option:options) {
//            final CommandLine commandLine = factory.create();
//            final StringWriter writer = new StringWriter();
//            try(PrintWriter out = new PrintWriter(writer)){
//                commandLine.setOut(out);
//                final int status = commandLine.execute(LoginCommand.NAME,option[0],option[1]);
//                Assertions.assertEquals(ExitCode.OK, status);
//                final String output = writer.toString();
//                Assertions.assertTrue(output.contains(String.format("Successfully logged-in as [%s]",user)));
//            }
//        }
//    }
//
//    /**
//     * Scenario: Pass invalid credentials
//     * Expect: Should fail inform about that and exit code 1
//     */
//    @Test
//    @Order(3)
//    void Test_Command_Login_With_Params_Expect_Login_Reject()  {
//
//        final String [][] options = {
//                {"--user=admin@dotCMS.com","--password=lol"},
//                {"-u=admin@dotCMS.com","-p=lol"}
//        };
//
//        for (final String [] option:options) {
//
//            final CommandLine commandLine = factory.create();
//            final StringWriter writer = new StringWriter();
//            try(PrintWriter out = new PrintWriter(writer)){
//                commandLine.setErr(out);
//                final int status = commandLine.execute(LoginCommand.NAME,option[0],option[1]);
//                Assertions.assertEquals(ExitCode.SOFTWARE, status);
//                final String output = writer.toString();
//                Assertions.assertTrue(output.contains("[ERROR]"));
//                Assertions.assertTrue(output.contains("Unable to login."));
//            }
//        }
//
//    }
//
//}
