package com.ettrema.httpclient;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

class NotifyingFileInputStream extends InputStream {
	private InputStream fin;
	private final InputStream wrapped;
    private final ProgressListener listener;
    private final String fileName;
    private long pos;
    private Long totalLength;
    // the system time we last notified the progress listener
    private long timeLastNotify;
    private long bytesSinceLastNotify;

    public NotifyingFileInputStream(File f, ProgressListener listener) throws FileNotFoundException, IOException {
        this.fin = FileUtils.openInputStream(f);
		this.wrapped = new BufferedInputStream(fin);
        this.listener = listener;
        this.totalLength = f.length();
        this.fileName = f.getAbsolutePath();
        this.timeLastNotify = System.currentTimeMillis();
    }
	
	/**
	 * 
	 * @param in - the input stream containing file data
	 * @param length - maybe null if unknown
	 * @param path
	 * @param listener
	 * @throws IOException 
	 */
    public NotifyingFileInputStream(InputStream in, Long length, String path, ProgressListener listener) throws IOException {
        this.fin = in;
		this.wrapped = new BufferedInputStream(fin);
        this.listener = listener;
        this.totalLength = length;
        this.fileName = path;
        this.timeLastNotify = System.currentTimeMillis();
    }	

    @Override
    public int read() throws IOException {
        increment(1);
        return wrapped.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        increment(b.length);
        return wrapped.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        increment(len);
        return wrapped.read(b, off, len);
    }

    private void increment(int len) {
        pos += len;
        notifyListener(len);
    }

    void notifyListener(int numBytes) {
		if( listener == null ) {
			return ;
		}
		listener.onRead(numBytes);
        bytesSinceLastNotify += numBytes;
        if (bytesSinceLastNotify < 1000) {
            //                log.trace( "notifyListener: not enough bytes: " + bytesSinceLastNotify);
            return;
        }
        int timeDiff = (int) (System.currentTimeMillis() - timeLastNotify);
        if (timeDiff > 10) {
            timeLastNotify = System.currentTimeMillis();
			listener.onProgress(pos, totalLength, fileName);
            bytesSinceLastNotify = 0;
        }
    }

	@Override
	public void close() throws IOException {
		IOUtils.closeQuietly(wrapped);
		IOUtils.closeQuietly(fin);
		super.close();
	}
	
	
}
