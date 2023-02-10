package com.dotcms.cli.command;

import com.dotcms.api.AuthenticationContext;
import com.dotcms.cli.command.site.SiteCommand;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.*;
import picocli.CommandLine;

import javax.inject.Inject;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

@QuarkusTest
public class SiteCommandTest extends CommandTest{

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
        resetServiceProfiles();
        final String user = "admin@dotcms.com";
        final char[] passwd = "admin".toCharArray();
        authenticationContext.login(user, passwd);
    }

    @Test
    void Test_Command_Site_List_Option() {
        final CommandLine commandLine = factory.create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(SiteCommand.NAME, "--list", "--interactive=false");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            //Assertions.assertTrue(output.startsWith("varName:"));
        }
    }

}
