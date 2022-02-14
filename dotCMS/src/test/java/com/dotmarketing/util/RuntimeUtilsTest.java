package com.dotmarketing.util;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Runtime Utils test.
 *
 * @author vico
 */
public class RuntimeUtilsTest {

    /**
     * Method to test: {@link RuntimeUtils#getRunProcessStream(String...)}
     * Given Scenario: Given a list of commands to be executed
     * ExpectedResult: a {@link InputStream} as a result of running the commands is returned
     */
    @Test
    public void test_getRunProcessStream() throws Exception {
        final InputStream input = RuntimeUtils.getRunProcessStream("uname", "-m");
        assertNotNull(input);
        final String text = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
        assertNotNull(text);
    }

    /**
     * Method to test: {@link RuntimeUtils#runProcessAndGetOutput(String...)}
     * Given Scenario: Given a list of commands to be executed
     * ExpectedResult: an empty {@link Optional<String>} as a result of running the invalid commands is returned
     */
    @Test
    public void test_runProcessAndGetOutput_withInvalidCmd() {
        final Optional<String> output = RuntimeUtils.runProcessAndGetOutput("some-invalid-command", "some-param");
        assertFalse(output.isPresent());
    }

    /**
     * Method to test: {@link RuntimeUtils#runProcessAndGetOutput(String...)}
     * Given Scenario: Given a list of commands to be executed
     * ExpectedResult: an empty {@link Optional<String>} as a result of running the commands with an invalid exit code
     * is returned
     */
    @Test
    public void test_runProcessAndGetOutput_withWaitAndInvalidErrorCode() {
        final Optional<String> output = RuntimeUtils.runProcessAndGetOutput(
                "uname", "-m", "&&", "sleep", "2", "&&", "exit 1");
        assertFalse(output.isPresent());
    }

    /**
     * Method to test: {@link RuntimeUtils#runProcessAndGetOutput(String...)}
     * Given Scenario: Given a list of commands to be executed
     * ExpectedResult: a {@link Optional<String>} as a result of running the commands is returned
     */
    @Test
    public void test_unProcessAndGetOutput() {
        final Optional<String> output = RuntimeUtils.runProcessAndGetOutput("uname", "-m");
        assertTrue(output.isPresent());
    }

}
