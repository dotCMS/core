package com.dotcms.api.client.files;

import static org.mockito.Mockito.when;

import com.dotcms.cli.common.DotExitCodeExceptionMapper;
import com.dotcms.cli.common.OutputOptionMixin;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.mockito.Mockito;
import picocli.CommandLine;

public class MockOutputOptionMixin extends OutputOptionMixin {

    private StringWriter mockOut;
    private StringWriter mockErr;
    private CommandLine mockCommandLine;

    public MockOutputOptionMixin() {
        // Create StringWriter instances for mock output and error streams
        mockOut = new StringWriter();
        mockErr = new StringWriter();
        mockCommandLine = Mockito.mock(CommandLine.class);
        when(mockCommandLine.getExitCodeExceptionMapper()).thenReturn(new DotExitCodeExceptionMapper());
    }

    @Override
    public CommandLine commandLine() {
        return mockCommandLine;
    }

    @Override
    public void info(String msg) {
        out().print(msg);
    }

    @Override
    public void error(String msg) {
        err().print(msg);
    }

    @Override
    public void debug(String msg) {
        out().print(msg);
    }

    @Override
    public PrintWriter out() {
        // Return a PrintWriter that writes to the mock output stream
        return new PrintWriter(mockOut);
    }

    @Override
    public PrintWriter err() {
        // Return a PrintWriter that writes to the mock error stream
        return new PrintWriter(mockErr);
    }

    @Override
    public void print(String text) {
        out().print(text);
    }

    @Override
    public void println(String text) {
        out().println(text);
    }

    @Override
    public void printText(String... text) {
        for (String line : text) {
            println(line);
        }
    }

    @Override
    public void printErrorText(String[] text) {
        for (String line : text) {
            err().println(line);
        }
    }

    @Override
    public void printStackTrace(Exception ex) {
        if (isShowErrors()) {
            ex.printStackTrace(new PrintWriter(err()));
        }
    }

    public String getMockOut() {
        return mockOut.toString();
    }

    public String getMockErr() {
        return mockErr.toString();
    }
}