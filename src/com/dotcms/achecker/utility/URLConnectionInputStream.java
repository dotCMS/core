package com.dotcms.achecker.utility;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
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
