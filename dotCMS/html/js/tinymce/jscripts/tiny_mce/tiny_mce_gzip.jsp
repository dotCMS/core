<%@page import="java.io.*,java.util.zip.*" %>
<%
/**
 * $Id: tiny_mce_gzip.jsp 535 2008-01-14 15:01:34Z spocke $
 *
 * @author Moxiecode
 * @copyright Copyright © 2006, Moxiecode Systems AB, All rights reserved.
 *
 * This file compresses the TinyMCE JavaScript using GZip and
 * enables the browser to do two requests instead of one for each .js file.
 *
 * It's a good idea to use the diskcache option since it reduces the servers workload.
 */

	String cacheKey = "", cacheFile = "", content = "", enc, suffix, cachePath;
	String plugins[], languages[], themes[];
	boolean diskCache, supportsGzip, isJS, compress, core;
	int i, x, bytes, expiresOffset;
	ServletOutputStream outStream = response.getOutputStream();
	OutputStreamWriter bow;
	ByteArrayOutputStream bos;
	GZIPOutputStream gzipStream;
	FileOutputStream fout;
	FileInputStream fin;
	byte buff[];

	// Get input
	plugins = getParam(request, "plugins", "").split(",");
	languages = getParam(request, "languages", "").split(",");
	themes = getParam(request, "themes", "").split(",");
	diskCache = getParam(request, "diskcache", "").equals("true");
	isJS = getParam(request, "js", "").equals("true");
	compress = getParam(request, "compress", "true").equals("true");
	core = getParam(request, "core", "true").equals("true");
	suffix = getParam(request, "suffix", "").equals("_src") ? "_src" : "";
	cachePath = mapPath(request, "."); // Cache path, this is where the .gz files will be stored
	expiresOffset = 3600 * 24 * 10; // Cache for 10 days in browser cache

	// Custom extra javascripts to pack
	String custom[] = {/*
		"some custom .js file",
		"some custom .js file"
	*/};

	response.reset();
	// Headers
	response.setContentType("text/javascript");
	response.addHeader("Vary", "Accept-Encoding"); // Handle proxies
	response.setDateHeader("Expires", System.currentTimeMillis() + (expiresOffset * 1000));

	// Is called directly then auto init with default settings
	if (!isJS) {
		out.print(getFileContents(mapPath(request, "tiny_mce_gzip.js")));
		out.print("tinyMCE_GZ.init({});");
		return;
	}

	// Setup cache info
	if (diskCache) {
		cacheKey = getParam(request, "plugins", "") + getParam(request, "languages", "") + getParam(request, "themes", "");

		for (i=0; i<custom.length; i++)
			cacheKey += custom[i];

		cacheKey = md5(cacheKey);

		if (compress)
			cacheFile = cachePath + File.separatorChar + "tiny_mce_" + cacheKey + ".gz";
		else
			cacheFile = cachePath + File.separatorChar + "tiny_mce_" + cacheKey + ".js";
	}

	// Check if it supports gzip
	supportsGzip = false;
	enc = request.getHeader("Accept-Encoding");
	if (enc != null) {
		enc.replaceAll("\\s+", "").toLowerCase();
		supportsGzip = enc.indexOf("gzip") != -1 || request.getHeader("---------------") != null;
		enc = enc.indexOf("x-gzip") != -1 ? "x-gzip" : "gzip";
	}

	// Use cached file disk cache
	if (diskCache && supportsGzip && new File(cacheFile).exists()) {
		if (compress)
			response.addHeader("Content-Encoding", enc);

		fin = new FileInputStream(cacheFile);
		buff = new byte[1024];

		while ((bytes = fin.read(buff, 0, buff.length)) != -1)
			outStream.write(buff, 0, bytes);

		fin.close();
		outStream.close();
		return;
	}

	// Add core
	if (core) {
		content += getFileContents(mapPath(request, "tiny_mce" + suffix + ".js"));

		// Patch loading functions
		content += "tinyMCE_GZ.start();";
	}

	// Add core languages
	for (x=0; x<languages.length; x++)
		content += getFileContents(mapPath(request, "langs/" + languages[x] + ".js"));

	// Add themes
	for (i=0; i<themes.length; i++) {
		content += getFileContents(mapPath(request, "themes/" + themes[i] + "/editor_template" + suffix + ".js"));

		for (x=0; x<languages.length; x++)
			content += getFileContents(mapPath(request, "themes/" + themes[i] + "/langs/" + languages[x] + ".js"));
	}

	// Add plugins
	for (i=0; i<plugins.length; i++) {
		content += getFileContents(mapPath(request, "plugins/" + plugins[i] + "/editor_plugin" + suffix + ".js"));

		for (x=0; x<languages.length; x++)
			content += getFileContents(mapPath(request, "plugins/" + plugins[i] + "/langs/" + languages[x] + ".js"));
	}

	// Add custom files
	for (i=0; i<custom.length; i++)
		content += getFileContents(mapPath(request, custom[i]));

	// Restore loading functions
	if (core)
		content += "tinyMCE_GZ.end();";

	// Generate GZIP'd content
	if (supportsGzip) {
		if (compress)
			response.addHeader("Content-Encoding", enc);

		if (diskCache && cacheKey != "") {
			bos = new ByteArrayOutputStream();

			// Gzip compress
			if (compress) {
				gzipStream = new GZIPOutputStream(bos);
				gzipStream.write(content.getBytes("iso-8859-1"));
				gzipStream.close();
			} else {
				bow = new OutputStreamWriter(bos);
				bow.write(content);
				bow.close();
			}

			// Write to file
			try {
				fout = new FileOutputStream(cacheFile);
				fout.write(bos.toByteArray());
				fout.close();
			} catch (IOException e) {
				// Ignore
			}

			// Write to stream
			outStream.write(bos.toByteArray());
		} else {
			gzipStream = new GZIPOutputStream(outStream);
			gzipStream.write(content.getBytes("iso-8859-1"));
			gzipStream.close();
		}
	} else
		out.write(content);
%><%!
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

	public String getParam(HttpServletRequest request, String name, String def) {
		String value = request.getParameter(name) != null ? "" + request.getParameter(name) : def;

		return value.replaceAll("[^0-9a-zA-Z\\-_,]+", "");
	}

	public String getFileContents(String path) {
		try {
			if (!new File(path).exists())
				return "";

			FileInputStream fis = new FileInputStream(path);
			int x = fis.available();
			byte b[] = new byte[x];

			fis.read(b);

			return new String(b);
		} catch (IOException e) {
			// Ignore
		}

		return "";
	}

	public String mapPath(HttpServletRequest request, String path) {
		String absPath = getServletConfig().getServletContext().getRealPath(request.getRequestURI());

		absPath = absPath.substring(0, absPath.lastIndexOf(File.separatorChar) + 1);

		return absPath + path.replace('/', File.separatorChar);
	}

	public String md5(String str) {
		try {
			java.security.MessageDigest md5 = java.security.MessageDigest.getInstance("MD5");

			char[] charArray = str.toCharArray();
			byte[] byteArray = new byte[charArray.length];

			for (int i=0; i<charArray.length; i++)
				byteArray[i] = (byte) charArray[i];

			byte[] md5Bytes = md5.digest(byteArray);
			StringBuffer hexValue = new StringBuffer();

			for (int i=0; i<md5Bytes.length; i++) {
				int val = ((int) md5Bytes[i] ) & 0xff;

				if (val < 16)
					hexValue.append("0");

				hexValue.append(Integer.toHexString(val));
			}

			return hexValue.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
			// Ignore
		}

		return "";
	}
%>
