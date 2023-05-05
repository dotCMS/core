package com.dotcms.cli.command.files;

import com.dotcms.api.AuthenticationContext;
import com.dotcms.cli.command.CommandTest;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@QuarkusTest
public class FilesCommandTest extends CommandTest {

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
    void Test_Command_Files_Tree_Option() {

        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s/%s", "demo.dotcms.com", "images");
            final int status = commandLine.execute(FilesCommand.NAME, FilesTree.NAME, path);
            Assertions.assertEquals(ExitCode.OK, status);
        }
    }

    @Test
    void Test_Command_Files_Tree_Option_Invalid_Protocol() {

        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("%s/%s", "demo.dotcms.com", "images");
            final int status = commandLine.execute(FilesCommand.NAME, FilesTree.NAME, path);
            Assertions.assertEquals(ExitCode.USAGE, status);
        }
    }

    @Test
    void Test_Command_Files_Tree_Option_Valid_Protocol() {

        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("%s/%s", "//demo.dotcms.com", "images");
            final int status = commandLine.execute(FilesCommand.NAME, FilesTree.NAME, path);
            Assertions.assertEquals(ExitCode.OK, status);
        }
    }

    @Test
    void Test_Command_Files_Tree_Option_Exclude_Empty() {

        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("%s/%s", "//demo.dotcms.com", "images");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesTree.NAME, path, "-ee");
            Assertions.assertEquals(ExitCode.OK, status);
        }
    }

}
