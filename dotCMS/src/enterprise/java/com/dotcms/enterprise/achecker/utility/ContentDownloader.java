/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.achecker.utility;

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
