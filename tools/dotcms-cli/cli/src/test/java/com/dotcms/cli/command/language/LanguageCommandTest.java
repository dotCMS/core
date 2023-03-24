package com.dotcms.cli.command.language;

import com.dotcms.api.AuthenticationContext;
import com.dotcms.cli.command.CommandTest;
import com.dotcms.model.language.Language;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@QuarkusTest
public class LanguageCommandTest extends CommandTest {
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

    /**
     * Test the language pull command by id. This test will fail if the returned language is not English
     * @throws JsonProcessingException
     */
    @Test
    void Test_Command_Language_Pull_By_Id() throws JsonProcessingException {
        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(LanguageCommand.NAME, LanguagePull.NAME, "1");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            final ObjectMapper mapper = new ObjectMapper();
            Language result = mapper.readValue(output, Language.class);
            Assertions.assertEquals(1, result.id());
        }
    }

    /**
     * Test the language pull command by tag (en-US). This test will fail if the returned language is not English
     * @throws JsonProcessingException
     */
    @Test
    void Test_Command_Language_Pull_By_Tag() throws JsonProcessingException {
        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(LanguageCommand.NAME, LanguagePull.NAME, "en-US");
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            final ObjectMapper mapper = new ObjectMapper();
            Language result = mapper.readValue(output, Language.class);
            Assertions.assertEquals(1, result.id());
        }
    }

    /**
     * Test the language find command. This test will fail if no languages are returned
     */
    @Test
    void Test_Command_Language_Find() {
        final CommandLine commandLine = getFactory().create();
        final StringWriter writer = new StringWriter();
        try (PrintWriter out = new PrintWriter(writer)) {
            commandLine.setOut(out);
            final int status = commandLine.execute(LanguageCommand.NAME, LanguageFind.NAME);
            Assertions.assertEquals(CommandLine.ExitCode.OK, status);
            final String output = writer.toString();
            Assertions.assertTrue(output.contains("English"));
        }
    }
}
