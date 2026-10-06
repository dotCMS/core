package com.dotcms.rest.api.v1.asset.bulkupload;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Passes a part's bytes through, and refuses the part the moment it crosses a ceiling.
 * <p>
 * <b>Why not let the staging layer do it.</b> It already does — and its refusal is
 * indistinguishable from a dropped connection, which needs the opposite handling. See
 * {@link PerFileCeilingExceededException}.
 * <p>
 * <b>Throws rather than truncates.</b> A stream that silently stopped at the ceiling would hand
 * staging a half-written file and report it as a successfully staged part, which is the worst of
 * the available outcomes: the author is told their file was accepted and gets a corrupt one.
 *
 * @author dotCMS
 */
class CeilingBoundedInputStream extends FilterInputStream {

    private final String fileName;
    private final long ceilingBytes;
    private long readSoFar;

    CeilingBoundedInputStream(final InputStream in, final String fileName,
                              final long ceilingBytes) {
        super(in);
        this.fileName = fileName;
        this.ceilingBytes = ceilingBytes;
    }

    @Override
    public int read() throws IOException {
        final int b = super.read();
        if (b != -1) {
            count(1);
        }
        return b;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        final int read = super.read(b, off, len);
        if (read > 0) {
            count(read);
        }
        return read;
    }

    private void count(final int bytes) {
        readSoFar += bytes;
        if (ceilingBytes > 0 && readSoFar > ceilingBytes) {
            throw new PerFileCeilingExceededException(fileName, ceilingBytes);
        }
    }
}
