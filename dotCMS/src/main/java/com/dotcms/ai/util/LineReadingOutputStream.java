package com.dotcms.ai.util;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/***
 * This class takes an OutputStream and only writes to it when it gets a newline.  This
 * is very helpful for streaming OpenAI responses because the responses are formated as lines of
 * json.  If the Response.OutputStream flushes at mid-line, the client only gets 1/2 a line of
 * json which fails to parse.  The problem can be handled on the client but it is clunky
 */
public class LineReadingOutputStream extends OutputStream {

    private final OutputStream consumer;
    private final byte[] buffer = new byte[8192]; // 8KB buffer
    private int bufferPos = 0;
    private boolean lastWasCR = false;

    public LineReadingOutputStream(final OutputStream consumer) {
        this.consumer = Objects.requireNonNull(consumer);
    }

    @Override
    public void write(final int b) throws IOException {
        // Handle single byte write
        byte[] singleByte = {(byte) b};
        write(singleByte, 0, 1);
    }

    @Override
    public void write(final byte[] b, int start, final int len) {
        if (b == null) {
            throw new NullPointerException();
        }
        if (len < 0) {
            throw new IllegalArgumentException();
        }
        final int end = start + len;
        if ((start < 0) || (start > b.length) || (end < 0) || (end > b.length)) {
            throw new IndexOutOfBoundsException();
        }

        try {
            // Process each byte in the input
            for (int i = start; i < end; i++) {
                byte currentByte = b[i];

                // Check for line endings
                if (currentByte == '\n' || (currentByte == '\r' && !lastWasCR)) {
                    // We found a line ending, flush the buffer
                    flushBuffer();
                    // Write the newline character to ensure proper line breaks
                    consumer.write('\n');
                    consumer.flush();
                    lastWasCR = (currentByte == '\r');
                } else if (lastWasCR) {
                    // Previous was CR but current is not LF, so CR was a line ending
                    // Add the current byte to the buffer
                    addToBuffer(currentByte);
                    lastWasCR = false;
                } else {
                    // Regular character, add to buffer
                    addToBuffer(currentByte);
                }
            }
        } catch (IOException e) {
            Logger.error(this, "Error writing to output stream", e);
            throw new DotRuntimeException(e);
        }
    }

    private void addToBuffer(byte b) {
        // Expand buffer if needed
        if (bufferPos >= buffer.length) {
            flushBuffer();
        }
        buffer[bufferPos++] = b;
    }

    private void flushBuffer() {
        try {
            if (bufferPos > 0) {
                // Write the buffered content directly to the consumer
                consumer.write(buffer, 0, bufferPos);
                bufferPos = 0; // Reset buffer position
            }
        } catch (IOException e) {
            Logger.error(this, "Error flushing buffer", e);
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public void flush() throws IOException {
        flushBuffer();
        consumer.flush();
    }

    @Override
    public void close() {
        try {
            flushBuffer(); // Make sure any remaining content is written
            consumer.flush();
            consumer.close();
        } catch (IOException e) {
            Logger.error(this, "Error closing output stream", e);
            throw new DotRuntimeException(e);
        }
    }
}
