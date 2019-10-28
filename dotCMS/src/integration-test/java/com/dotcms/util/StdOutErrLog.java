package com.dotcms.util;

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
                com.dotmarketing.util.Logger.info(StdOutErrLog.class, string);
            }
        };
    }

}