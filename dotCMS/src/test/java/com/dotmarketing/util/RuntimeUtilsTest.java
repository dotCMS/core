package com.dotmarketing.util;

import com.dotmarketing.exception.DotRuntimeException;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RuntimeUtilsTest {

    @Test
    public void test_runProcess() {
        try {
            final Process process = RuntimeUtils.runProcess("uname", "-m");
            assertNotNull(process);
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void test_runProcess_invalidCmd() {
        try {
            RuntimeUtils.runProcess("some-invalid-command", "some-param");
        } catch (Exception e) {
            assertTrue(e instanceof DotRuntimeException);
        }
    }

    @Test
    public void test_getRunProcessStream() throws Exception {
        final InputStream input = RuntimeUtils.getRunProcessStream("uname", "-m");
        assertNotNull(input);
        final String text = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
        assertNotNull(text);
    }

}
