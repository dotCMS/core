package com.dotmarketing.filters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.publishing.BundlerUtil;
import com.dotmarketing.business.TimeMachineSessionBean;
import com.dotmarketing.util.Logger;

/**
 * Servlet Filter implementation class TestFilter
 */
public class TimeMachineFilter implements Filter {
		
	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		HttpServletRequest req = (HttpServletRequest) request;
		
		TimeMachineSessionBean config = (TimeMachineSessionBean) req.getSession().getAttribute(TimeMachineSessionBean.SESSION_KEY);
		if ( config == null ) {
			chain.doFilter(request, response);
			return;
		}
		
		if ( ! config.isActive() ) {
			chain.doFilter(request, response);
			return;
		}

		if ( config.getHost() == null ) {
			Logger.error(this, "Session is in time machine mode but no host specified");
			chain.doFilter(request, response);
			return;
		}
		
		if ( config.getDate() == null ) {
			Logger.error(this, "Session is in time machine mode but no date specified");
			chain.doFilter(request, response);
			return;
		}

		File timeMachineBundleDir = BundlerUtil.getBundleRoot("tm_" + config.getDate().getTime());
		if ( timeMachineBundleDir == null ) {
			Logger.error(this, "Session is in time machine mode but bundle not found: " + "'tm_" + config.getDate().getTime() + "'");
			chain.doFilter(request, response);
			return;
		}

		// Get time machine path from URI
		String hostname = config.getHost().getHostname();
		String uri = req.getRequestURI();
		String[] components = uri.split("/");
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
			Logger.debug(this, "Time Machine Page not found: " + timeMachineBundleDir);
			
			if ( config.isNotFoundGoOnMainSite() ) {
				chain.doFilter(request, response);
				return;
			}
			else {
				((HttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
		}

		Logger.debug(this, "Served by Time Machine: " + timeMachineBundleDir);
		joinStreams(new FileInputStream(timeMachineBundleDir), response.getOutputStream());
		        
	}

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

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}

}
