package com.dotcms.ema.proxy;

import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A specialized implementation of PrintWriter that directly forwards all write operations
 * to an underlying OutputStream while maintaining the PrintWriter interface.
 *
 * This class bypasses the standard buffering and character encoding handling of
 * the traditional PrintWriter to provide direct access to the underlying stream.
 *
 * This implementation is thread-safe regarding the closing operation and subsequent
 * write attempts.
 *
 * Usage example:
 * <pre>
 *     OutputStream outputStream = ...;
 *     MockPrintWriter writer = new MockPrintWriter(outputStream);
 *     writer.println("Hello, world!");
 *     writer.flush();
 * </pre>
 */
public class MockPrintWriter extends PrintWriter {

    /** The actual output stream where data will be written */
    private final OutputStream outputStream;

    /**
     * Flag indicating whether this writer has been closed.
     * Using AtomicBoolean for thread safety.
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates a new MockPrintWriter that forwards all write operations to the specified output stream.
     *
     * @param out The target OutputStream where all data will be written
     */
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

    /**
     * Writes a single character.
     *
     * @param c The character to write
     */
    @Override
    public void write(int c) {
        if (closed.get()) {
            return;
        }
        try {
            outputStream.write(c);
        } catch (IOException e) {
            Logger.error(MockPrintWriter.class, e);
            setError();
        }
    }

    /**
     * Writes a portion of a character array.
     *
     * @param buf The array of characters
     * @param off The offset from which to start writing
     * @param len The number of characters to write
     */
    @Override
    public void write(char[] buf, int off, int len) {
        if (closed.get()) {
            return;
        }
        try {
            outputStream.write(new String(buf, off, len).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Logger.error(MockPrintWriter.class, e);
            setError();
        }
    }

    /**
     * Writes a portion of a string.
     *
     * @param s The string to write (must not be null)
     * @param off The offset from which to start writing
     * @param len The number of characters to write
     */
    @Override
    public void write(String s, int off, int len) {
        if (closed.get()) {
            return;
        }
        try {
            outputStream.write(s.substring(off, off + len).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Logger.error(MockPrintWriter.class, e);
            setError();
        }
    }

    /**
     * Writes an array of characters.
     *
     * @param buf The array of characters to write (must not be null)
     */
    @Override
    public void write(char [] buf) {
        write(buf, 0, buf.length);
    }

    /**
     * Writes a string.
     *
     * @param s The string to write (must not be null)
     */
    @Override
    public void write(String s) {
        if (closed.get()) {
            return;
        }
        try {
            outputStream.write(s.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Logger.error(MockPrintWriter.class, e);
            setError();
        }
    }

    /**
     * Writes the string representation of a boolean value.
     *
     * @param b The boolean value to write
     */
    @Override
    public void print(boolean b) {
        write(String.valueOf(b));
    }

    /**
     * Writes a character.
     *
     * @param c The character to write
     */
    @Override
    public void print(char c) {
        write(c);
    }

    /**
     * Writes the string representation of an integer.
     *
     * @param i The integer value to write
     */
    @Override
    public void print(int i) {
        write(String.valueOf(i));
    }

    /**
     * Writes the string representation of a long integer.
     *
     * @param l The long integer value to write
     */
    @Override
    public void print(long l) {
        write(String.valueOf(l));
    }

    /**
     * Writes the string representation of a float value.
     *
     * @param f The float value to write
     */
    @Override
    public void print(float f) {
        write(String.valueOf(f));
    }

    /**
     * Writes the string representation of a double value.
     *
     * @param d The double value to write
     */
    @Override
    public void print(double d) {
        write(String.valueOf(d));
    }

    /**
     * Writes an array of characters.
     *
     * @param s The array of characters to write
     */
    @Override
    public void print(char[] s) {
        write(s);
    }

    /**
     * Writes a string or "null" if the string is null.
     *
     * @param s The string to write
     */
    @Override
    public void print(String s) {
        write(s != null ? s : "null");
    }

    /**
     * Writes the string representation of an object.
     *
     * @param obj The object to write
     */
    @Override
    public void print(Object obj) {
        write(String.valueOf(obj));
    }

    /**
     * Writes a line separator.
     */
    @Override
    public void println() {
        write(System.lineSeparator());
    }

    /**
     * Writes the string representation of a boolean value followed by a line separator.
     *
     * @param x The boolean value to write
     */
    @Override
    public void println(boolean x) {
        print(x);
        println();
    }

    /**
     * Writes a character followed by a line separator.
     *
     * @param x The character to write
     */
    @Override
    public void println(char x) {
        print(x);
        println();
    }

    /**
     * Writes the string representation of an integer followed by a line separator.
     *
     * @param x The integer value to write
     */
    @Override
    public void println(int x) {
        print(x);
        println();
    }

    /**
     * Writes the string representation of a long integer followed by a line separator.
     *
     * @param x The long integer value to write
     */
    @Override
    public void println(long x) {
        print(x);
        println();
    }

    /**
     * Writes the string representation of a float value followed by a line separator.
     *
     * @param x The float value to write
     */
    @Override
    public void println(float x) {
        print(x);
        println();
    }

    /**
     * Writes the string representation of a double value followed by a line separator.
     *
     * @param x The double value to write
     */
    @Override
    public void println(double x) {
        print(x);
        println();
    }

    /**
     * Writes an array of characters followed by a line separator.
     *
     * @param x The array of characters to write
     */
    @Override
    public void println(char[] x) {
        print(x);
        println();
    }

    /**
     * Writes a string followed by a line separator.
     *
     * @param x The string to write
     */
    @Override
    public void println(String x) {
        print(x);
        println();
    }

    /**
     * Writes the string representation of an object followed by a line separator.
     *
     * @param x The object to write
     */
    @Override
    public void println(Object x) {
        print(x);
        println();
    }

    /**
     * Flushes the stream.
     * If the stream has been closed, this method has no effect.
     */
    @Override
    public void flush() {
        if (closed.get()) {
            return;
        }
        try {
            outputStream.flush();
        } catch (IOException e) {
            Logger.error(MockPrintWriter.class, e);
            setError();
        }
    }

    /**
     * Closes the stream after flushing it.
     * Once the stream has been closed, further write() or flush() invocations will have no effect.
     * Note: This method does not close the underlying outputStream as it might be used externally.
     *
     * This operation is thread-safe.
     */
    @Override
    public void close() {
        // Only perform closing actions if this is the first call to close()
        if (closed.compareAndSet(false, true)) {
            flush();
            // Lets not close the outputStream here as it might be used externally
        }
    }

    /**
     * Returns whether this writer has been closed.
     *
     * @return true if this writer is closed, false otherwise
     */
    public boolean isClosed() {
        return closed.get();
    }
}