package com.dotcms.ema.proxy;

import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public class MockPrintWriter extends PrintWriter {

    final OutputStream outputStream;

    public MockPrintWriter(final OutputStream out) {
        super(out);
        this.outputStream = out;
    }

    @Override
    public void write(String s) {
        try {
            outputStream.write(s.getBytes());
        } catch (IOException e) {
            Logger.error(MockPrintWriter.class,e);
        }
        super.write(s);
    }

    @Override
    public void println(String x) {
        try {
            outputStream.write(x.getBytes());
        } catch (IOException e) {
            Logger.error(MockPrintWriter.class, e);
        }
        super.println(x);
    }
}
