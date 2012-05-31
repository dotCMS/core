package com.dotmarketing.filters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.Locale;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.dotcms.publishing.BundlerUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

/**
 * Servlet Filter implementation class TestFilter
 */
public class TimeMachineFilter implements Filter {

	private File adjustFileIfNotExists(String url, File file) {
		if ( url.trim().equals("/")) {
			return new File(file.getAbsoluteFile() + File.separator + "home" + File.separator + "index.dot");
		}
		if ( url.trim().equals("/index.dot")) {
			return new File(file.getAbsoluteFile() + File.separator + "home" + File.separator + "index.dot");
		}
		if ( file.exists() && file.isDirectory() ) {
			return new File(file, "index.dot");
		}
		return file;
	}
		
	private int joinStreams(InputStream in, OutputStream out) throws IOException {
		int copied = 0;
		int length;
		byte[] tmp = new byte[20480];
		while ((length = in.read(tmp, 0, 20480)) != -1) {
			out.write(tmp, 0, length);
			copied += length;
		}
		out.flush();
		return copied;
	}
	
	private String getHostName(ServletRequest request) {
		String remoteHost = request.getRemoteHost();
		Host host = null;
		try {
			host = APILocator.getHostAPI().resolveHostName(remoteHost, APILocator.getUserAPI().getSystemUser(), true);
		} catch (DotDataException e) {
			e.printStackTrace();
		} catch (DotSecurityException e) {
			e.printStackTrace();
		}
		return host.getHostname();
	}

	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		HttpServletRequest req = (HttpServletRequest) request;

		String tmbundle = (String) req.getSession().getAttribute("tmbundle");
		if ( tmbundle == null ) {
			chain.doFilter(request, response);
			return;
		}
		
		String hostname = getHostName(request);
		if ( hostname == null ) {
			System.out.println("Host not found");
			chain.doFilter(request, response);
			return;
		}
		
		String uri = req.getRequestURI();
		String[] components = uri.split("/");
				
		File timeMachineBundleDir = BundlerUtil.getBundleRoot("tm_" + tmbundle);
		String finalPath = timeMachineBundleDir + File.separator + "live" + File.separator + hostname + File.separator;

		for ( int i = 1; i < components.length; i ++ ) {
			finalPath += components[i];
			if ( i != components.length - 1 )
				finalPath += File.separator;
		}
		
		timeMachineBundleDir = new File(finalPath);

		// Adjust url for default file (index.dot) and directories
		timeMachineBundleDir = adjustFileIfNotExists(uri, timeMachineBundleDir);
		
		if ( ! timeMachineBundleDir.exists() || ! timeMachineBundleDir.isFile()) {
			System.out.println("NOT FOUND: " + timeMachineBundleDir);
			chain.doFilter(request, response);
			return;
		}

		System.out.println("TIME MACHINE: " + timeMachineBundleDir);
		joinStreams(new FileInputStream(timeMachineBundleDir), response.getOutputStream());
		        
	}
	
	private void dump(HttpServletRequest request) {

		// Render the generic servlet request properties
		StringWriter sw = new StringWriter();
		PrintWriter writer = new PrintWriter(sw);
		writer.println("Request Received at " + (new Timestamp(System.currentTimeMillis())));
		writer.println(" characterEncoding=" + request.getCharacterEncoding());
		writer.println("     contentLength=" + request.getContentLength());
		writer.println("       contentType=" + request.getContentType());
		writer.println("            locale=" + request.getLocale());
		writer.print("           locales=");
		Enumeration locales = request.getLocales();
		boolean first = true;
		while (locales.hasMoreElements()) {
			Locale locale = (Locale) locales.nextElement();
			if (first)
				first = false;
			else
				writer.print(", ");
			writer.print(locale.toString());
		}
		writer.println();
		Enumeration names = request.getParameterNames();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			writer.print("         parameter=" + name + "=");
			String values[] = request.getParameterValues(name);
			for (int i = 0; i < values.length; i++) {
				if (i > 0)
					writer.print(", ");
				writer.print(values[i]);
			}
			writer.println();
		}
		writer.println("          protocol=" + request.getProtocol());
		writer.println("        remoteAddr=" + request.getRemoteAddr());
		writer.println("        remoteHost=" + request.getRemoteHost());
		writer.println("            scheme=" + request.getScheme());
		writer.println("        serverName=" + request.getServerName());
		writer.println("        serverPort=" + request.getServerPort());
		writer.println("          isSecure=" + request.isSecure());

		// Render the HTTP servlet request properties
		if (request instanceof HttpServletRequest) {
			writer.println("---------------------------------------------");
			HttpServletRequest hrequest = (HttpServletRequest) request;
			writer.println("       contextPath=" + hrequest.getContextPath());
			Cookie cookies[] = hrequest.getCookies();
			if (cookies == null)
				cookies = new Cookie[0];
			for (int i = 0; i < cookies.length; i++) {
				writer.println("            cookie=" + cookies[i].getName() +
						"=" + cookies[i].getValue());
			}
			names = hrequest.getHeaderNames();
			while (names.hasMoreElements()) {
				String name = (String) names.nextElement();
				String value = hrequest.getHeader(name);
				writer.println("            header=" + name + "=" + value);
			}
			writer.println("            method=" + hrequest.getMethod());
			writer.println("          pathInfo=" + hrequest.getPathInfo());
			writer.println("       queryString=" + hrequest.getQueryString());
			writer.println("        remoteUser=" + hrequest.getRemoteUser());
			writer.println("requestedSessionId=" +
					hrequest.getRequestedSessionId());
			writer.println("        requestURI=" + hrequest.getRequestURI());
			writer.println("       servletPath=" + hrequest.getServletPath());
		}
		writer.println("=============================================");

		// Log the resulting string
		writer.flush();
		
		System.out.println(sw.getBuffer().toString());
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}

}
