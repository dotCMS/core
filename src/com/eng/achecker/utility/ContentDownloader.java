package com.eng.achecker.utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ContentDownloader {
	
	public static String getContent(InputStream in) throws IOException {
		final int bufferSize = 1024 * 10;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[bufferSize];
		int len = 0;
		while ((len = in.read(buffer, 0, bufferSize)) > 0 ) {
			out.write(buffer, 0, len);
			out.flush();
		}
		return out.toString();
	}

}
