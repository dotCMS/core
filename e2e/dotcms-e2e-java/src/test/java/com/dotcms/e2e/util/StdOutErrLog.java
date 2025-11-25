package com.dotcms.e2e.util;

import com.dotcms.e2e.logging.Logger;

import java.io.PrintStream;

/**
 * This class provides utility methods to redirect the standard output and error streams
 * to a custom logging mechanism.
 *
 * The main purpose of this class is to tie the System.out and System.err streams to a logger,
 * so that all output can be captured and logged appropriately.
 *
 * The {@link Logger} class is used to log the messages.
 *
 * @author vico
 */
public class StdOutErrLog {

    /**
     * Redirects System.out and System.err to the logger.
     */
    public static void tieSystemOutAndErrToLog() {
        System.setOut(createLoggingProxy(System.out));
        System.setErr(createLoggingProxy(System.err));
    }

    /**
     * Creates a proxy for the given PrintStream that logs all output.
     *
     * @param realPrintStream the original PrintStream to be proxied
     * @return a new PrintStream that logs all output
     */
    public static PrintStream createLoggingProxy(final PrintStream realPrintStream) {
        return new PrintStream(realPrintStream) {
            public void print(final String string) {
                //realPrintStream.print(string);
                Logger.info(StdOutErrLog.class, string);
            }
        };
    }

}