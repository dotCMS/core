package com.dotcms.ai.util;

import com.dotmarketing.exception.DotRuntimeException;
import com.liferay.util.StringPool;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/***
 * This class takes an OutputStream and only writes to it when it gets a newline.  This
 * is very helpful for streaming OpenAI responses because the responses are formated as lines of
 * json.  If the Response.OutputStream flushes at mid-line, the client only gets 1/2 a line of
 * json which fails to parse.  The problem can be handled on the client but it is clunky
 */
public class LineReadingOutputStream extends OutputStream {

    private static final byte CR = '\r';
    private static final byte LF = '\n';

    private final OutputStream consumer;
    private final StringBuilder stringBuilder = new StringBuilder();
    private boolean lastCR = false;

    public LineReadingOutputStream(final OutputStream consumer) {
        this.consumer = Objects.requireNonNull(consumer);
    }

    @Override
    public void write(final int b) throws IOException {
        write(new byte[]{(byte) b}, 0, 1);
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

        if (this.lastCR && start < end && b[start] == LF) {
            start++;
            this.lastCR = false;
        } else if (start < end) {
            this.lastCR = b[end - 1] == CR;
        }

        int base = start;
        for (int i = start; i < end; i++) {
            if (b[i] == LF || b[i] == CR) {
                final String chunk = asString(b, base, i);
                this.stringBuilder.append(chunk);
                consume();
            }
            if (b[i] == LF) {
                base = i + 1;
            } else if (b[i] == CR) {
                if (i < end - 1 && b[i + 1] == LF) {
                    base = i + 2;
                    i++;
                } else {
                    base = i + 1;
                }
            }
        }
        final String chunk = asString(b, base, end);
        this.stringBuilder.append(chunk);
    }

    @Override
    public void close() {
        if (this.stringBuilder.length() != 0) {
            consume();
        }
    }

    private static String asString(final byte[] b, final int start, final int end) {
        if (start > end) {
            throw new IllegalArgumentException();
        }
        if (start == end) {
            return StringPool.BLANK;
        }
        final byte[] copy = Arrays.copyOfRange(b, start, end);
        return new String(copy, StandardCharsets.UTF_8);
    }

    private void consume() {
        try {
            this.consumer.write(this.stringBuilder.toString().getBytes());
            this.consumer.write('\n');
            this.consumer.flush();
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
        this.stringBuilder.delete(0, Integer.MAX_VALUE);
    }
}
