/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.achecker.utility;

import com.dotcms.repackage.org.apache.commons.httpclient.HttpClient;
import com.dotcms.repackage.org.apache.commons.httpclient.UsernamePasswordCredentials;
import com.dotcms.repackage.org.apache.commons.httpclient.auth.AuthScope;
import com.dotcms.repackage.org.apache.commons.httpclient.methods.GetMethod;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.lang.StringUtils;

/**
 * This utility allows to asynchronousely connect to an URL to get
 * data as a transparent InputStream. It will connect to server on 
 * first byte required (not before) and stream is closed after last 
 * read. 
 * 
 * This utility allows to setup proxy settings in order to obtain
 * the connection to URL passing through a proxy.
 * 
 * Example:
 * <code>
 * URLConnectionInputStream in = new URLConnectionInputStream("http://www.apple.com");
 * in.setupProxy("proxy.host.com", "3128", "user", "password");
 * 
 * byte[] buffer = new byte[1024];
 * int len = 0;
 * while ((len = in.read(buffer, 0, 1024)) != -1) {
 * 		System.out.println(new String(buffer, 0, len));
 * }
 * 
 * in.close();
 * </code>
 * 
 * @author Michele Mastrogiovanni
 */
public class URLConnectionInputStream extends InputStream {

	// Internal logger
	// private static final Log logger = LogFactory.getLog(URLConnectionInputStream.class);

	private GetMethod get;
	
	private InputStream in;
	
	/** This flag is true if connection reached the end */
	private boolean isClosed;

	/** This flag is true if connection was established */
	private boolean started;

	/** The Constant PROXY_IP. */
	private String proxyHost;

	/** The Constant PROXY_PORT. */
	private String proxyPort;

	/** The Constant PROXY_USR. */
	private String proxyUser;

	/** The Constant PROXY_PWD. */
	private String proxyPass;
	
	/** URL where download file */
	private String url;
	
	public URLConnectionInputStream(String url) {
		this.started = false;
		this.isClosed = true;
		this.url = url;
	}
	
	public void setupProxy(String proxyHost, String proxyPort, String proxyUser, String proxyPass) {
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
		this.proxyUser = proxyUser;
		this.proxyPass = proxyPass;
	}
	
	public void reset() {
		if ( ! started ) {
			return;
		}
		if ( ! isClosed ) {
			get.releaseConnection();
			get = null;
			in = null;
			isClosed = true;
		}
		started = false;
	}	

	@Override
	protected void finalize() throws Throwable {
		if ( ! started )
			return;
		if ( isClosed )
			return;
		get.releaseConnection();
		in = null;
		isClosed = true;
		super.finalize();
	}

	private void startup() throws IOException {
		
		if ( started )
			return;
		
		started = true;
		
		HttpClient client = new HttpClient();
		
		if ( StringUtils.isNotBlank(proxyHost) && StringUtils.isNotBlank(proxyPort)) {
			client.getHostConfiguration().setProxy(proxyHost, Integer.valueOf(proxyPort));
		}
		
		if ( StringUtils.isNotBlank(proxyUser) && StringUtils.isNotBlank(proxyPass)) {
			client.getState().setProxyCredentials(new AuthScope(proxyHost, Integer.valueOf(proxyPort)), new UsernamePasswordCredentials(proxyUser, proxyPass));
		}

		get = new GetMethod(url);

		try {
			client.executeMethod(get);
		} catch (Throwable t) {
			get.releaseConnection();
			throw new IOException("Error in connection with url: " + url, t);
		}

		if (get.getStatusCode() == 200) {
			in = get.getResponseBodyAsStream();
			if ( in != null ) {
				isClosed = false;
				return;
			}
		}

		int statusCode = get.getStatusCode();
		String statusText = get.getStatusText();
		get.releaseConnection();
		throw new IOException("Error in connection: " + statusCode + ": " + statusText );

	}
	
	@Override
	public int read() throws IOException {
		
		// Startup if it is needed
		startup();
		
		if ( isClosed ) {
			return -1;
			// throw new IOException("This URLConnectionInputStream is closed");
		}
		
		int value = in.read();
		
		if ( value == -1 ) {
			// Close connection
			get.releaseConnection();
			in = null;
			isClosed = true;
			return -1;
		}

		return value;
	}
	
	@Override
	public void close() throws IOException {
		// reset();
		
		// Close connection
		get.releaseConnection();
		in = null;
		isClosed = true;
		// super.close();
	}

	@Override
	public int available() throws IOException {
		int available = super.available();
		return available;
	}

	@Override
	public synchronized void mark(int arg0) {
		super.mark(arg0);
	}

	@Override
	public boolean markSupported() {
		boolean test = super.markSupported();
		return test;
	}

	@Override
	public int read(byte[] arg0, int arg1, int arg2) throws IOException {
		int len = super.read(arg0, arg1, arg2);
		return len;
	}

	@Override
	public int read(byte[] arg0) throws IOException {
		int len = super.read(arg0);
		return len;
	}

	@Override
	public long skip(long arg0) throws IOException {
		long skip = super.skip(arg0);
		return skip;
	}

	
}
