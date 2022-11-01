package com.ettrema.httpclient.zsyncclient;

import com.bradmcevoy.http.Range;
import com.ettrema.httpclient.File;
import com.ettrema.httpclient.HttpException;
import com.ettrema.httpclient.ProgressListener;
import com.ettrema.httpclient.Utils.CancelledException;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author HP
 */
public class HttpRangeLoader implements RangeLoader {

	private static final Logger log = LoggerFactory.getLogger(HttpRangeLoader.class);
	private final File file;
	private final ProgressListener listener;
	private long numBytes;

	public HttpRangeLoader(File file, final ProgressListener listener) {
		this.file = file;
		this.listener = listener;
	}

	@Override
	public byte[] get(List<Range> rangeList) {
		log.info("get: rangelist: " + rangeList.size());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			file.download(out, listener, rangeList);
		} catch (HttpException ex) {
			
		} catch (CancelledException ex) {
			throw new RuntimeException("Cancelled, which is odd because no progress listener was provided");
		}

		byte[] bytes = out.toByteArray();
		int expectedLength = calcExpectedLength(rangeList);
//		if( expectedLength != bytes.length) {
//			log.warn("Got an unexpected data size!!");
//		}
		numBytes += bytes.length;
		return bytes;
	}

	public static int calcExpectedLength(List<Range> rangeList) {
		int l = 0;
		for (Range r : rangeList) {
			l += (r.getFinish() - r.getStart());
		}
		return l;
	}

	public long getBytesDownloaded() {
		return numBytes;
	}
}
