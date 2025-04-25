package com.dotcms.ema.proxy;

import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.NotNull;

public class MockPrintWriter extends PrintWriter {

    private final OutputStream outputStream;
    private boolean closed = false;

    public MockPrintWriter(final OutputStream out) {
        // Use a dummy OutputStream to avoid double writing
        super(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                // No-op dummy implementation
            }
        });
        this.outputStream = out;
    }

    @Override
    public void write(int c) {
        if (closed) return;
        try {
            outputStream.write(c);
        } catch (IOException e) {
            Logger.error(MockPrintWriter.class, e);
            setError();
        }
    }

    @Override
    public void write(char[] buf, int off, int len) {
        if (closed) return;
        try {
            outputStream.write(new String(buf, off, len).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Logger.error(MockPrintWriter.class, e);
            setError();
        }
    }

    @Override
    public void write(@NotNull String s, int off, int len) {
        if (closed) return;
        try {
            outputStream.write(s.substring(off, off + len).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Logger.error(MockPrintWriter.class, e);
            setError();
        }
    }

    @Override
    public void write(char @NotNull [] buf) {
        write(buf, 0, buf.length);
    }

    @Override
    public void write(@NotNull String s) {
        if (closed) return;
        try {
            outputStream.write(s.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Logger.error(MockPrintWriter.class, e);
            setError();
        }
    }

    @Override
    public void print(boolean b) {
        write(String.valueOf(b));
    }

    @Override
    public void print(char c) {
        write(c);
    }

    @Override
    public void print(int i) {
        write(String.valueOf(i));
    }

    @Override
    public void print(long l) {
        write(String.valueOf(l));
    }

    @Override
    public void print(float f) {
        write(String.valueOf(f));
    }

    @Override
    public void print(double d) {
        write(String.valueOf(d));
    }

    @Override
    public void print(char[] s) {
        write(s);
    }

    @Override
    public void print(String s) {
        write(s != null ? s : "null");
    }

    @Override
    public void print(Object obj) {
        write(String.valueOf(obj));
    }

    @Override
    public void println() {
        write(System.lineSeparator());
    }

    @Override
    public void println(boolean x) {
        print(x);
        println();
    }

    @Override
    public void println(char x) {
        print(x);
        println();
    }

    @Override
    public void println(int x) {
        print(x);
        println();
    }

    @Override
    public void println(long x) {
        print(x);
        println();
    }

    @Override
    public void println(float x) {
        print(x);
        println();
    }

    @Override
    public void println(double x) {
        print(x);
        println();
    }

    @Override
    public void println(char[] x) {
        print(x);
        println();
    }

    @Override
    public void println(String x) {
        print(x);
        println();
    }

    @Override
    public void println(Object x) {
        print(x);
        println();
    }

    @Override
    public void flush() {
        if (closed) return;
        try {
            outputStream.flush();
        } catch (IOException e) {
            Logger.error(MockPrintWriter.class, e);
            setError();
        }
    }

    @Override
    public void close() {
        if (closed) return;
        flush();
        closed = true;
        // Lets no close the outputStream here as it might be used externally
    }
}