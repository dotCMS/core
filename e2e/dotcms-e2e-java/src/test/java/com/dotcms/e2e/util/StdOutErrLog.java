package com.dotcms.e2e.util;

import com.dotcms.e2e.logging.Logger;

import java.io.PrintStream;

public class StdOutErrLog {

    public static void tieSystemOutAndErrToLog() {
        System.setOut(createLoggingProxy(System.out));
        System.setErr(createLoggingProxy(System.err));
    }

    public static PrintStream createLoggingProxy(final PrintStream realPrintStream) {
        return new PrintStream(realPrintStream) {
            public void print(final String string) {
                //realPrintStream.print(string);
                Logger.info(StdOutErrLog.class, string);
            }
        };
    }

}