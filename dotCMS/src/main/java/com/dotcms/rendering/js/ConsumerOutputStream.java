package com.dotcms.rendering.js;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * {@link OutputStream} implementation that will log the output to a {@link Consumer<String>}
 * @author jsanca
 */
public class ConsumerOutputStream extends OutputStream {

    final Consumer<String> loggerConsumer;
    private StringBuilder builder = new StringBuilder();

    public ConsumerOutputStream(final Consumer<String> loggerConsumer) {
        this.loggerConsumer = loggerConsumer;
    }


    /**
     * Writes a byte to the output stream. This method flushes automatically at the end of a line.
     *
     * @param b
     */
    public void write (final int b) {
        byte[] bytes = new byte[1];
        bytes[0] = (byte) (b & 0xff);
        final String currentString = new String(bytes);
        builder.append(currentString);

        if (currentString.endsWith ("\n")) {
            builder = builder.deleteCharAt(builder.length() - 1); // delete the last one
            flush ();
        }
    }

    /**
     * Writes an array of bytes to the output stream. This method flushes automatically at the end of a line.
     * @param bytes     the data.
     * @param off   the start offset in the data.
     * @param len   the number of bytes to write.
     * @throws IOException
     */
    @Override
    public void write(final byte[] bytes, final int off, final int len) throws IOException {
        Objects.checkFromIndexSize(off, len, bytes.length);
        for (int i = 0 ; i < len ; i++) {
            write(bytes[off + i]);
        }
    }

    /**
     * Flushes the output stream.
     */
    @Override
    public void flush () {
        this.loggerConsumer.accept(builder.toString());
        builder.setLength(0);
    }
}
