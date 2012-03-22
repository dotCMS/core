package com.dotmarketing.scripting.servlet;

/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 * This file was originally taken from the Quercus PHP Servlet. 
 * The Servlet was modified for use within the Scripting Plugin
 * for dotCMS.  In accordance with the GPL this class remains with a 
 * GPL 2 License
 * 
 * Below is the original licene/notice from Resin.
 *   
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 * @author Jason Tesser
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import com.caucho.config.ConfigException;
import com.caucho.quercus.Quercus;
import com.caucho.quercus.QuercusDieException;
import com.caucho.quercus.QuercusErrorException;
import com.caucho.quercus.QuercusExitException;
import com.caucho.quercus.QuercusLineRuntimeException;
import com.caucho.quercus.QuercusRequestAdapter;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.QuercusValueException;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.vfs.Path;
import com.caucho.vfs.StreamImpl;
import com.caucho.vfs.VfsStream;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.WriterStreamImpl;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotReindexStateException;
import com.dotmarketing.scripting.util.php.DotCMSPHPCauchoVFS;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

/**
 * This class was written originally written for the Scripting Plugin
 * of dotCMS.
 * 
 * To configure the PHP Servlet add the following to your web-ext.xml inside the plugin.
 * 
 * <servlet>
 * 	<servlet-name>PHPServlet</servlet-name>
 * 	<servlet-class>com.dotmarketing.scripting.servlet.PHPServlet</servlet-class>
 * 	<init-param>
 * 			<param-name>compile</param-name>
 * 			<param-value>true</param-value>
 * 	</init-param>
 * </servlet>
 * 
 * <servlet-mapping>
 * 	<servlet-name>PHPServlet</servlet-name>
 * 	<url-pattern>*.php</url-pattern>
 * </servlet-mapping>
 * 
 * You can use the following init-params
 * compile
 * ini-file
 * mysql-version
 * php-version
 * script-encoding
 * strict
 * page-cache-entries
 * connection-pool
 * datasource
 * 
 * @author Jason Tesser
 *
 */
public class PHPServlet extends HttpServlet {

	private Quercus php;
	private ServletContext _servletContext;
	private ServletConfig _config;
	private boolean _isCompileSet;

	/**
	 * Returns the Quercus instance.
	 */
	protected Quercus getQuercus()
	{
		synchronized (this) {
			if (php == null){
				php = new Quercus();
			}
		}

		return php;
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		if(!Config.getBooleanProperty("ENABLE_SCRIPTING", false)){
			return;
		}
		_config = config;
		_servletContext = config.getServletContext();
		php = getQuercus();
		try {
			php.setPwd(new DotCMSPHPCauchoVFS(APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), true)));
		} catch (DotDataException e) {
			Logger.error(PHPServlet.class,e.getMessage(),e);
			throw new DotReindexStateException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(PHPServlet.class,e.getMessage(),e);
			throw new DotReindexStateException(e.getMessage(), e);
		}
		php.setDatabase(DbConnectionFactory.getDataSource());
		Enumeration paramNames = config.getInitParameterNames();
		while (paramNames.hasMoreElements()) {
			String paramName = String.valueOf(paramNames.nextElement());
			String paramValue = config.getInitParameter(paramName);
			setInitParam(paramName, paramValue);
		}
		php.init();

		php.setIni("allow_url_include", BooleanValue.create(true).toStringBuilder());
	}

	@Override
	public void service(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
		if(!Config.getBooleanProperty("ENABLE_SCRIPTING", false)){
			return;
		}
		php.setWorkDir(getPath(request));
		Env env = null;
		WriteStream ws = null;

		try {
			Path path = getPath(request);

			QuercusPage page;

			try {
				page = php.parse(path);
			}
			catch (FileNotFoundException ex) {

				Logger.warn(this, ex.getMessage());
				Logger.debug(this, ex.getMessage(), ex);
				response.sendError(HttpServletResponse.SC_NOT_FOUND);

				return;
			}catch (NullPointerException ex) {
				Logger.warn(this, ex.getMessage());
				Logger.debug(this, ex.getMessage(), ex);
				response.sendError(HttpServletResponse.SC_NOT_FOUND);

				return;
			}

			StreamImpl out;

			try {
				out = new VfsStream(null, response.getOutputStream());
			}
			catch (IllegalStateException e) {
				WriterStreamImpl writer = new WriterStreamImpl();
				writer.setWriter(response.getWriter());

				out = writer;
			}

			ws = new WriteStream(out);

			ws.setNewlineString("\n");


			php.setServletContext(_servletContext);

			env = php.createEnv(page, ws, request, response);
			try {
				env.start();
				env.setPwd(path);
				env.setGlobalValue("request", env.wrapJava(request));
				env.setGlobalValue("response", env.wrapJava(response));
				env.setGlobalValue("session", env.wrapJava(request.getSession()));
				env.setGlobalValue("servletContext", env.wrapJava(_servletContext));

				String prepend = env.getIniString("auto_prepend_file");
				if (prepend != null) {
					Path prependPath = env.lookup(env.createString(prepend));

					if (prependPath == null)
						env.error("auto_prepend_file '{0}' not found.", prepend);
					else {
						QuercusPage prependPage = php.parse(prependPath);
						prependPage.executeTop(env);
					}
				}

				page.executeTop(env);

				String append = env.getIniString("auto_append_file");
				if (append != null) {
					Path appendPath = env.lookup(env.createString(append));

					if (appendPath == null)
						env.error("auto_append_file '{0}' not found.", append);
					else {
						QuercusPage appendPage = php.parse(appendPath);
						appendPage.executeTop(env);
					}
				}
				//   return;
			}
			catch (QuercusExitException e) {
				throw e;
			}
			catch (QuercusErrorException e) {
				throw e;
			}
			catch (QuercusLineRuntimeException e) {
				Logger.error(this, e.getMessage(), e);

				//  return;
			}
			catch (QuercusValueException e) {
				Logger.error(this, e.getMessage(), e);

				ws.println(e.toString());

				//  return;
			}
			catch (Throwable e) {
				if (response.isCommitted())
					e.printStackTrace(ws.getPrintWriter());

				ws = null;

				throw e;
			}
			finally {
				if (env != null)
					env.close();

				// don't want a flush for a thrown exception
				if (ws != null)
					ws.close();
			}
		}
		catch (QuercusDieException e) {
			Logger.debug(this, e.getMessage(), e);
		}
		catch (QuercusExitException e) {
			// normal exit
			Logger.debug(this, e.getMessage(), e);
		}
		catch (QuercusErrorException e) {
			// error exit
			Logger.debug(this, e.getMessage(), e);
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Throwable e) {
			throw new ServletException(e);
		}
	}

	/**
	 * Sets a named init-param to the passed value.
	 *
	 * @throws ServletException if the init-param is not recognized
	 */
	protected void setInitParam(String paramName, String paramValue)
	throws ServletException
	{
		if ("compile".equals(paramName)) {
			setCompile(paramValue);
		}
		else if ("ini-file".equals(paramName)) {
			Quercus quercus = getQuercus();

			String realPath = getServletContext().getRealPath(paramValue);

			Path path = quercus.getPwd().lookup(realPath);

			setIniFile(path);
		}
		else if ("mysql-version".equals(paramName)) {
			setMysqlVersion(paramValue);
		}
		else if ("php-version".equals(paramName)) {
			setPhpVersion(paramValue);
		}
		else if ("script-encoding".equals(paramName)) {
			setScriptEncoding(paramValue);
		}
		else if ("strict".equals(paramName)) {
			setStrict("true".equals(paramValue));
		}
		else if ("page-cache-entries".equals(paramName)) {
			setPageCacheEntries(Integer.parseInt(paramValue));
		}
		else if ("connection-pool".equals(paramName)) {
			setConnectionPool("true".equals(paramValue));
		}else if ("datasource".equals(paramName)) {
			setDatabase(paramValue);
		}
		
		else
			throw new ServletException("'{0}' is not a recognized init-param");
	}

	/**
	 * Adds a quercus.ini configuration
	 */
	public void setIniFile(Path path)
	{
		php.setIniFile(path);
	}

	public void setDatabase(String datasource){
		php.setDatabase(DbConnectionFactory.getDataSource(datasource));
	}
	
	/**
	 * Set true if quercus should be compiled into Java.
	 */
	public void setCompile(String isCompile)
	throws ConfigException
	{
		_isCompileSet = true;
		
		if ("true".equals(isCompile) || "".equals(isCompile)) {
			php.setCompile(true);
			php.setLazyCompile(false);
		} else if ("false".equals(isCompile)) {
			php.setCompile(false);
			php.setLazyCompile(false);
		} else if ("lazy".equals(isCompile)) {
			php.setLazyCompile(true);
		} else
			throw new ConfigException(
					"'{0}' is an unknown compile value.  Values are 'true', 'false', or 'lazy'.");
	}

	/**
	 * Sets the script encoding.
	 */
	public void setScriptEncoding(String encoding)
	throws ConfigException
	{
		php.setScriptEncoding(encoding);
	}

	/**
	 * Sets the version of the client php library.
	 */
	public void setMysqlVersion(String version)
	{
		php.setMysqlVersion(version);
	}

	/**
	 * Sets the php version that Quercus is implementing.
	 */
	public void setPhpVersion(String version)
	{
		php.setPhpVersion(version);
	}

	/**
	 * Sets the strict mode.
	 */
	public void setStrict(boolean isStrict)
	{
		php.setStrict(isStrict);
	}

	/*
	 * Sets the max size of the page cache.
	 */
	public void setPageCacheEntries(int entries)
	{
		php.setPageCacheEntries(entries);
	}

	/*
	 * Turns connection pooling on or off.
	 */
	public void setConnectionPool(boolean isEnable)
	{
		php.setConnectionPool(isEnable);
	}

	private Path getPath(HttpServletRequest req)
	{
		String scriptPath = QuercusRequestAdapter.getPageServletPath(req);
		String pathInfo = QuercusRequestAdapter.getPagePathInfo(req);
		Path pwd;
		try {
			pwd = new DotCMSPHPCauchoVFS(WebAPILocator.getHostWebAPI().getCurrentHost(req));
		} catch (Exception e) {
			Logger.error(PHPServlet.class,e.getMessage(),e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		Path path = pwd.lookup(scriptPath);
		return path;
	}

}
