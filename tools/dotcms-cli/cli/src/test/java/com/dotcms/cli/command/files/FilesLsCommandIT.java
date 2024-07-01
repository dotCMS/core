package com.dotcms.cli.command.files;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.AuthenticationContext;
import com.dotcms.cli.command.CommandTest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class FilesLsCommandIT extends CommandTest {

    @Inject
    AuthenticationContext authenticationContext;

    @BeforeEach
    public void setupTest() throws IOException {
        resetServiceProfiles();
        final String user = "admin@dotcms.com";
        final char[] passwd = "admin".toCharArray();
        authenticationContext.login(user, passwd);
    }

    @Test
    void Test_Command_Files_Ls_Option_Invalid_Protocol() {
        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME, FilesLs.NAME, path);
            Assertions.assertEquals(ExitCode.USAGE, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Valid_Protocol() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME, FilesLs.NAME, path);
            Assertions.assertEquals(ExitCode.OK, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Valid_Protocol2() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s/", "default");
            final int status = commandLine.execute(FilesCommand.NAME, FilesLs.NAME, path);
            Assertions.assertEquals(ExitCode.OK, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Exclude_Empty() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "-ee");
            Assertions.assertEquals(ExitCode.OK, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Exclude_Empty2() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "--excludeEmptyFolders");
            Assertions.assertEquals(ExitCode.OK, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Exclude_Folders() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "-ef", "folder1");
            Assertions.assertEquals(ExitCode.OK, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Exclude_Folders2() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "-ef", "folder1,folder2");
            Assertions.assertEquals(ExitCode.OK, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Exclude_Folders3() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "--excludeFolder", "folder1");
            Assertions.assertEquals(ExitCode.OK, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Exclude_Folders4() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "--excludeFolder", "folder1,folder2");
            Assertions.assertEquals(ExitCode.OK, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Exclude_Folders_Missing_Parameter() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "-ef");
            Assertions.assertEquals(ExitCode.USAGE, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Exclude_Folders_Missing_Parameter2() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "--excludeFolder");
            Assertions.assertEquals(ExitCode.USAGE, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Exclude_Assets() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "-ea", "file1.png");
            Assertions.assertEquals(ExitCode.OK, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Exclude_Assets2() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "-ea", "file1.png,file2.png");
            Assertions.assertEquals(ExitCode.OK, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Exclude_Assets3() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "--excludeAsset", "file1.png");
            Assertions.assertEquals(ExitCode.OK, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Exclude_Assets4() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "--excludeAsset", "file1.png,file2.png");
            Assertions.assertEquals(ExitCode.OK, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Exclude_Assets_Missing_Parameter() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "-ea");
            Assertions.assertEquals(ExitCode.USAGE, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Exclude_Assets_Missing_Parameter2() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "--excludeAsset");
            Assertions.assertEquals(ExitCode.USAGE, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Include_Folders() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "-if", "folder1");
            Assertions.assertEquals(ExitCode.OK, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Include_Folders2() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "-if", "folder1,folder2");
            Assertions.assertEquals(ExitCode.OK, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Include_Folders3() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "--includeFolder", "folder1");
            Assertions.assertEquals(ExitCode.OK, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Include_Folders4() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "--includeFolder", "folder1,folder2");
            Assertions.assertEquals(ExitCode.OK, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Include_Folders_Missing_Parameter() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "-if");
            Assertions.assertEquals(ExitCode.USAGE, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Include_Folders_Missing_Parameter2() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "--includeFolder");
            Assertions.assertEquals(ExitCode.USAGE, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Include_Assets() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "-ia", "file1.png");
            Assertions.assertEquals(ExitCode.OK, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Include_Assets2() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "-ia", "file1.png,file2.png");
            Assertions.assertEquals(ExitCode.OK, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Include_Assets3() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "--includeAsset", "file1.png");
            Assertions.assertEquals(ExitCode.OK, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Include_Assets4() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "--includeAsset", "file1.png,file2.png");
            Assertions.assertEquals(ExitCode.OK, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Include_Assets_Missing_Parameter() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "-ia");
            Assertions.assertEquals(ExitCode.USAGE, status);
        }
    }

    @Test
    void Test_Command_Files_Ls_Option_Glob_Include_Assets_Missing_Parameter2() {

        final CommandLine commandLine = createCommand();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final String path = String.format("//%s", "default");
            final int status = commandLine.execute(FilesCommand.NAME,
                    FilesLs.NAME, path, "--includeAsset");
            Assertions.assertEquals(ExitCode.USAGE, status);
        }
    }

}
