package com.dotcms.publishing.output;

import com.dotcms.publishing.PublisherConfig;
import org.apache.commons.io.IOUtils;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.UtilMethods;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import javax.ws.rs.NotSupportedException;
import java.io.*;
import java.nio.file.Files;
import java.util.Collection;
import java.util.zip.GZIPOutputStream;

/**
 * {@link BundleOutput} implementation to create a bundle in a tar.gzip file
 */
public class TarGzipBundleOutput extends BundleOutput {
    private File tarGzipFile;
    private TarArchiveOutputStream tarArchiveOutputStream;
    private int GZIP_OUTPUT_STREAM_BUFFER_SIZE = Config
            .getIntProperty("GZIP_OUTPUT_STREAM_BUFFER_SIZE", 65536);

    public TarGzipBundleOutput(final PublisherConfig publisherConfig) throws IOException {
        super(publisherConfig);

        tarGzipFile = getBundleTarGzipFile(publisherConfig.getId());
    }

    @Override
    public void create() throws IOException {
        final OutputStream outputStream = Files.newOutputStream(tarGzipFile.toPath());

        tarArchiveOutputStream = new TarArchiveOutputStream(new GZIPOutputStream(outputStream, GZIP_OUTPUT_STREAM_BUFFER_SIZE));

        tarArchiveOutputStream.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
        // TAR originally didn't support long file names, so enable the support for it
        tarArchiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
    }

    public static File getBundleTarGzipFile(final String bundleId) {
        final String fileName = String.format("%s%s%s.tar.gz",ConfigUtils.getBundlePath(),File.separator,bundleId);
        return new File(fileName);
    }

    @Override
    public OutputStream addFile(final String path) {
        return new TarGzipPublisherOutputStream(path);
    }

    @Override
    public File getFile() {
        return tarGzipFile;
    }

    @Override
    public File getFile(String filePath) {
        throw new NotSupportedException();
    }

    @Override
    public void delete(final String filePath) {
        throw new NotSupportedException();
    }

    @Override
    public void close() throws IOException {
        if (UtilMethods.isSet(tarArchiveOutputStream)) {
            tarArchiveOutputStream.close();
        }
    }

    public long lastModified(String filePath){
        return 0;
    }

    @Override
    public Collection<File> getFiles(final FileFilter fileFilter){
        throw new NotSupportedException();
    }

    @Override
    public void setLastModified(String myFile, long timeInMillis){

    }

    @Override
    public void innerCopyFile(final File source, final String destinationPath) throws IOException {
        synchronized (tarArchiveOutputStream) {
            try {
                final TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(destinationPath);
                tarArchiveEntry.setSize(source.length());

                tarArchiveOutputStream.putArchiveEntry(tarArchiveEntry);
                IOUtils.copy(new FileInputStream(source), tarArchiveOutputStream);
            } finally {
                tarArchiveOutputStream.closeArchiveEntry();
            }
        }
    }

    public void mkdirs(final String path) {
        final TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(path);

        synchronized (tarArchiveOutputStream) {
            try {
                tarArchiveOutputStream.putArchiveEntry(tarArchiveEntry);
                tarArchiveOutputStream.closeArchiveEntry();
            } catch (IOException e) {
                throw new DotRuntimeException(e);
            }
        }
    }

    private class TarGzipPublisherOutputStream extends ByteArrayOutputStream {
        private boolean closed = false;
        private String filePath;

        public TarGzipPublisherOutputStream(final String filePath) {
            super();

            this.filePath = filePath;
        }

        @Override
        public void close() throws IOException {
            super.close();

            final byte[] bytes = this.toByteArray();

            final TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(filePath);
            tarArchiveEntry.setSize(bytes.length);

            putEntry(bytes, tarArchiveEntry);

            this.closed = true;
        }

        private void putEntry(byte[] bytes, TarArchiveEntry tarArchiveEntry) throws IOException {
            synchronized (tarArchiveOutputStream) {
                try {
                    tarArchiveOutputStream.putArchiveEntry(tarArchiveEntry);
                    IOUtils.copy(new ByteArrayInputStream(bytes), tarArchiveOutputStream);
                } finally {
                    tarArchiveOutputStream.closeArchiveEntry();
                }
            }
        }

        public boolean isClosed(){
            return closed;
        }
    }
}
