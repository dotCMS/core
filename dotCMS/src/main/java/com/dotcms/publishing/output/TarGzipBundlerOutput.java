package com.dotcms.publishing.output;

import com.dotcms.publishing.PublisherConfig;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotmarketing.util.ConfigUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import javax.ws.rs.NotSupportedException;
import java.io.*;
import java.nio.file.Files;
import java.util.zip.GZIPOutputStream;

public class TarGzipBundlerOutput extends BundlerOutput {
    private File tarGzipFile;
    private TarArchiveOutputStream tarArchiveOutputStream;

    public TarGzipBundlerOutput(final PublisherConfig publisherConfig) throws IOException {
        super(publisherConfig);

        final String fileName = String.format(
                ConfigUtils.getBundlePath() + File.separator + publisherConfig.getId() + ".tar.gz"
        );

        tarGzipFile = new File(fileName);
        final OutputStream outputStream = Files.newOutputStream(tarGzipFile.toPath());

        tarArchiveOutputStream = new TarArchiveOutputStream(new GZIPOutputStream(outputStream));

        tarArchiveOutputStream.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
        // TAR originally didn't support long file names, so enable the support for it
        tarArchiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
    }

    @Override
    protected OutputStream innerAddFile(final File file) {
        return new TarGzipPublisherOutputStream(file);
    }

    @Override
    public File getFile() {
        return tarGzipFile;
    }

    @Override
    public void delete(File f) {
        throw new NotSupportedException();
    }

    @Override
    public void close() throws IOException {
        tarArchiveOutputStream.close();
    }

    private class TarGzipPublisherOutputStream extends ByteArrayOutputStream {
        private boolean closed = false;
        private File file;

        public TarGzipPublisherOutputStream(final File file) {
            super();

            this.file = file;
        }

        @Override
        public void close() throws IOException {
            super.close();

            final byte[] bytes = this.toByteArray();

            final TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(file.getPath());
            tarArchiveEntry.setSize(bytes.length);

            putEntry(bytes, tarArchiveEntry);


            this.closed = true;
        }

        private void putEntry(byte[] bytes, TarArchiveEntry tarArchiveEntry) throws IOException {
            synchronized (tarArchiveOutputStream) {
                tarArchiveOutputStream.putArchiveEntry(tarArchiveEntry);
                IOUtils.copy(new ByteArrayInputStream(bytes), tarArchiveOutputStream);
                tarArchiveOutputStream.closeArchiveEntry();
            }
        }

        public boolean isClosed(){
            return closed;
        }
    }
}
