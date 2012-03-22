/**
 * 
 */
package com.dotmarketing.servlets;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.struts.Globals;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import com.dotmarketing.beans.BrowserSniffer;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.VirtualLinksCache;
import com.dotmarketing.cms.wiki.utils.WikiUtils;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.velocity.VelocityServlet;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.lowagie.text.DocumentException;

/**
 * @author Jason Tesser
 * @author Andres Olarte
 * @since 1.6.5
 * 
 */
public class HTMLPDFServlet extends VelocityServlet {

	private static final FileAPI fileAPI = APILocator.getFileAPI();
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private PermissionAPI perAPI;
	
	private User user;
	Map<String, String> map = new HashMap<String, String>();
	private ServletContext context;
	private List<String> hostList;
	private LanguageAPI langAPI = APILocator.getLanguageAPI();
	private HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		
		
		
		
		
		String fName = req.getParameter("fname");
		resp.setContentType("application/pdf");
		
		if(UtilMethods.isSet(fName)){
			if(!fName.toLowerCase().endsWith(".pdf")){
				fName= fName + ".pdf";
			}
		}
		else{
			fName = "document.pdf";
		}
		
		BrowserSniffer bs = new BrowserSniffer(req.getHeader("User-Agent"));
			
		if(bs.isBot()){
			resp.sendError(401, "No Bots Allowed");
			return;
		}
		
		
		
		resp.setHeader("Content-Disposition", "attachment; filename=" + fName);
		HttpSession session = req.getSession();
		String reqURI = req.getRequestURI();

		Logger.debug(this, "Starting PDFServlet at URI " + reqURI);
		
		// Copied directly out of VelocityServlet
		String language = String.valueOf(langAPI.getDefaultLanguage().getId());
		// set default page language
		if (session.isNew()	|| !UtilMethods.isSet((String) session.getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE))) {
			Logger.debug(VelocityServlet.class, "session new: "	+ session.isNew());
			Language l = langAPI.getLanguage(language);
			Locale locale = new Locale(l.getLanguageCode() + "_" + l.getCountryCode().toUpperCase());
			boolean ADMIN_MODE = (session.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
            if(ADMIN_MODE==false){session.setAttribute(Globals.LOCALE_KEY, locale);}
			session.setAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE, language);
		}

		// update page language
		if (UtilMethods.isSet(req.getParameter(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE))	|| UtilMethods.isSet(req.getParameter("language_id"))) {
			if (UtilMethods.isSet(req.getParameter(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE))) {
				language = req.getParameter(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);
			} else {
				language = req.getParameter("language_id");
			}
			Language l = langAPI.getLanguage(language);
			Locale locale = new Locale(l.getLanguageCode() + "_" + l.getCountryCode());
			boolean ADMIN_MODE = (session.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
            if(ADMIN_MODE==false){session.setAttribute(Globals.LOCALE_KEY, locale);}
			session.setAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE, language);
		}

		// get out of session
		language = (String) session.getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_LANGUAGE);
		// Copied directly out of VelocityServlet -- END

		UserWebAPI uAPI = WebAPILocator.getUserWebAPI();

		user = null;
		try {
			user = uAPI.getLoggedInUser(req);
		} catch (DotRuntimeException e2) {
			Logger.debug(this, "DotRuntimeException: " + e2.getMessage(), e2);
		} catch (PortalException e2) {
			Logger.debug(this, "PortalException: " + e2.getMessage(), e2);
		} catch (SystemException e2) {
			Logger.debug(this, "SystemException: " + e2.getMessage(), e2);
		}
		
		if(user != null){
			Logger.debug(this, "The user is " + user.getUserId());
		}else{
			Logger.debug(this, "The user is null");
		}
		
		boolean working = false;
		boolean live = false;

		String VELOCITY_HTMLPAGE_EXTENSION = Config.getStringProperty("VELOCITY_HTMLPAGE_EXTENSION");

		String location = "live";
		if (working) {
			location = "working";
		}

		Logger.debug(this, "The location is " + location);
		
		boolean PREVIEW_MODE = ((session.getAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION) != null));
		boolean EDIT_MODE = ((session.getAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION) != null));

		if (PREVIEW_MODE || EDIT_MODE) {
			working = true;
			Logger.debug(this, "Working is true");
		} else {
			live = true;
			Logger.debug(this, "Live is true");
		}

		if (EDIT_MODE) {
			try {
				user = uAPI.getLoggedInUser(req);
			} catch (DotRuntimeException e) {
				Logger.debug(this, "DotRuntimeException: " + e.getMessage(), e);
			} catch (PortalException e) {
				Logger.debug(this, "PortalException: " + e.getMessage(), e);
			} catch (SystemException e) {
				Logger.debug(this, "PortalException: " + e.getMessage(), e);
			}
		}

		perAPI = APILocator.getPermissionAPI();
		String pageID = req.getParameter("_dot_pdfpage");
		if (pageID == null || pageID.length() < 1) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		Logger.debug(this, "Page to pdf is " + pageID);
		Identifier ident = null;
		Host host;
		try {
			host = hostWebAPI.getCurrentHost(req);
		} catch (PortalException e3) {
			Logger.error(HTMLPDFServlet.class, e3.getMessage(), e3);
			throw new ServletException(e3.getMessage(), e3);
		} catch (SystemException e3) {
			Logger.error(HTMLPDFServlet.class, e3.getMessage(), e3);
			throw new ServletException(e3.getMessage(), e3);
		} catch (DotDataException e3) {
			Logger.error(HTMLPDFServlet.class, e3.getMessage(), e3);
			throw new ServletException(e3.getMessage(), e3);
		} catch (DotSecurityException e3) {
			Logger.error(HTMLPDFServlet.class, e3.getMessage(), e3);
			throw new ServletException(e3.getMessage(), e3);
		}
		try {
			//Long id = Long.valueOf(pageID);
			ident = APILocator.getIdentifierAPI().find(pageID);
		} catch (NumberFormatException e2) {
			boolean external = false;
			String uri = pageID;
			String pointer = null;
			// Is it a virtual link?
			if (uri.endsWith("/"))
				uri = uri.substring(0, uri.length() - 1);
			pointer = VirtualLinksCache.getPathFromCache(host.getHostname()	+ ":" + uri);

			if (!UtilMethods.isSet(pointer)) {
				pointer = VirtualLinksCache.getPathFromCache(uri);
			}

			if (UtilMethods.isSet(pointer)) { // is it a virtual link?
				Logger.debug(this, "CMS found virtual link pointer = " + uri + ":" + pointer);

				String auxPointer = pointer;
				if (auxPointer.indexOf("http://") != -1	|| auxPointer.indexOf("https://") != -1) {
					auxPointer = pointer.replace("https://", "");
					auxPointer = pointer.replace("http://", "");
					int startIndex = 0;
					int endIndex = auxPointer.indexOf("/");
					if (startIndex < endIndex) {
						String localHostName = auxPointer.substring(startIndex,	endIndex);
						Host localHost;
						try {
							localHost = hostWebAPI.findByName(localHostName, APILocator.getUserAPI().getSystemUser(), false);
						} catch (DotDataException e) {
							Logger.error(HTMLPDFServlet.class, e.getMessage(), e);
							throw new ServletException(e.getMessage(), e);
						} catch (DotSecurityException e) {
							Logger.error(HTMLPDFServlet.class, e.getMessage(), e);
							throw new ServletException(e.getMessage(), e);
						}
						external = (localHost == null || !InodeUtils.isSet(localHost.getInode() ) ? true : false);
					} else {
						external = true;
						pageID = pointer;
						uri = pointer;
					}
				}
				if (!external) {
					String ext = Config.getStringProperty("VELOCITY_PAGE_EXTENSION");
					if (!pointer.endsWith(ext)) {
						if (!pointer.endsWith("/"))
							pointer += "/";
						pointer += "index." + ext;

					}
					pageID = pointer;
					uri = pointer;
				}

			}

			// Is it external?
			if (pageID.startsWith("http://") || pageID.startsWith("https://")) {
				if (!external) {
					// Didn't come form a virtual link, so we have to make sure
					// that we should PDF it according to web.xml
					URL url = new URL(pageID);
					if (!hostList.contains(url.getHost())) {
						resp.sendError(HttpServletResponse.SC_FORBIDDEN);
						return;
					}
				}

				ITextRenderer renderer = new ITextRenderer();

				renderer.setDocument(pageID);
				Logger.debug(this, "Calling iText render");
				renderer.layout();
				try {
					Logger.debug(this, "Using iText to Create PDF");
					renderer.createPDF(resp.getOutputStream());

				} catch (DocumentException e) {
					Logger.error(this, e.getMessage(), e);
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				}
				return;
			}

			// Is it a wiki?
			String[] path = pageID.split("/");
			if (path.length > 2) {
				String wiki = map.get("/" + path[1]);
				String wikiName = pageID.substring(1); // String of first /
				// Get everything after the second /
				wikiName = wikiName.substring(wikiName.indexOf("/")+1); // Get
				// everything
				// after
				// the
				// second
				// /
				String title = WikiUtils.normalizeTitle(wikiName);
				if (wiki != null) {
					String struct = wiki.split("\\|")[0];
					String field = wiki.split("\\|")[1];

					ContentletAPI capi = APILocator.getContentletAPI();
					String query = "+structureInode:" + struct + " +" + field
							+ ":\"" + title
							+ "\" +languageId:1* +deleted:false +live:true";
					List<com.dotmarketing.portlets.contentlet.model.Contentlet> cons = null;
					try {
						cons = capi.search(query, 1, 0, "text1", user, true);
					} catch (DotDataException e) {
						Logger.debug(this, "DotDataException: "	+ e.getMessage(), e);
					} catch (DotSecurityException e) {
						Logger.debug(this, "DotSecurityException: "	+ e.getMessage(), e);
					} catch (Exception e) {
						Logger.debug(this, "ParseException: " + e.getMessage(),	e);
					}
					if (cons != null && cons.size() > 0) {
						com.dotmarketing.portlets.contentlet.model.Contentlet c = cons.get(0);
						req.setAttribute(WebKeys.CLICKSTREAM_IDENTIFIER_OVERRIDE, c.getIdentifier());
					} else {
						resp.sendError(HttpServletResponse.SC_NOT_FOUND);
						return;
					}
					pageID = "/"
							+ path[1]
							+ "/index."
							+ Config
									.getStringProperty("VELOCITY_PAGE_EXTENSION");
					uri=pageID;

				}
			}

			if (pageID.endsWith("/")) {
				uri = pageID + "index."
						+ Config.getStringProperty("VELOCITY_PAGE_EXTENSION");
			} else {
				if (!pageID.endsWith("."
						+ Config.getStringProperty("VELOCITY_PAGE_EXTENSION"))) {
					uri = pageID
							+ "/index."
							+ Config
									.getStringProperty("VELOCITY_PAGE_EXTENSION");
				}
			}
			try {
				ident = APILocator.getIdentifierAPI().find(host, uri);
			} catch (Exception e) {
				Logger.debug(this, "Exception: " + e.getMessage(), e);			} 
		} catch (Exception e1) {
			Logger.debug(this, e1.getMessage(), e1);
		}
		if (ident == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		if (!InodeUtils.isSet(ident.getInode() )) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		String url = ident.getURI();
		url = url.substring(0, url.lastIndexOf("/")) + "/";
		try {
			if (!perAPI.doesUserHavePermission(ident,PermissionAPI.PERMISSION_READ, user, true)) {
				resp.sendError(HttpServletResponse.SC_FORBIDDEN);
			}
		} catch (DotDataException e1) {
			Logger.error(HTMLPDFServlet.class,e1.getMessage(),e1);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.getMessage());
		}

		StringWriter sw = new StringWriter();

		Context context;

		context = VelocityUtil.getWebContext(req, resp);
		context.put("pdfExport", true);

		String pageIdent = ident.getInode() + "";

		try {
			VelocityEngine ve = VelocityUtil.getEngine();
			ve.getTemplate("/" + location + "/" + pageIdent + "." + VELOCITY_HTMLPAGE_EXTENSION).merge(context, sw);
			ITextRenderer renderer = new ITextRenderer();
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setValidating(false);
			DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
			builder.setEntityResolver(new DTDResolver());
			String s = sw.toString();
			s = escapeEspecialCharacter(s);

			s = processCSSPath(s, host, "css", "\\(", "\\)", ")", url);
			s = processCSSPath(s, host, "css", "\\\"", "\\\"", "\"", url);
			Tidy tidy = new Tidy();
			tidy.setXHTML(true);

			ByteArrayInputStream is = new ByteArrayInputStream(s.getBytes());
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			tidy.parse(is, os);
			s = os.toString();

			is = new ByteArrayInputStream(s.getBytes());
			Document doc = builder.parse(is);

			NodeList nl = doc.getElementsByTagName("img");
			for (int i = 0; i < nl.getLength(); i++) {
				Node n = nl.item(i);
				Node srcNode = n.getAttributes().getNamedItem("src");
				String srcText = srcNode.getNodeValue();
				String newText = getRealPath(srcText, host, url);
				String cleanText = cleanPath(newText);
				srcNode.setNodeValue(cleanText);
			}

			renderer.setDocument(doc, null);
			// renderer.setDocument("http://www.w3.org/");
			renderer.layout();
			renderer.createPDF(resp.getOutputStream());

		} catch (ParseErrorException e) {
			Logger.error(this, "ParseErrorException: "
					+ e.getMessage(), e);
		} catch (ResourceNotFoundException e) {
			Logger.error(this, "ResourceNotFoundException: "
					+ e.getMessage(), e);
		} catch (MethodInvocationException e) {
			Logger.error(this, "MethodInvocationException: "
					+ e.getMessage(), e);
		} catch (Exception e) {
			Logger.error(this, "Exception: "
					+ e.getMessage(), e);
		}
	}

	private String getRealPath(String path, Host host, String url)
			throws Exception {
		String relativePath = null;
		if (path.toLowerCase().startsWith("http://")
				|| path.toLowerCase().startsWith("https://")) {
			return path;
		}
		if (!path.startsWith("/")) {
			path = cleanPath(url + path);
		}
		if (path.startsWith("/dotAsset/")) {
			if (path.startsWith("/dotAsset?path=")) {
				int index = path.indexOf("/dotAsset?path=");
				relativePath = path.substring(index);

			} else {
				String identifier = path.substring(path.lastIndexOf("/") + 1,
						path.indexOf("."));
				Identifier ident = APILocator.getIdentifierAPI().find(identifier);
				relativePath = LiveCache.getPathFromCache(ident.getURI(), ident.getHostId());
			}

		} else if(path.startsWith("/resize_image?")){
			Logger.debug(this, "Fixing resize image servlet");
			return "http://" + host.getHostname() + path;
		}else {
			relativePath = LiveCache.getPathFromCache(path, host);
		}
		File f = null;
		if (relativePath == null) {
			f = new File(context.getRealPath(path));
		} else {

			f = new File(fileAPI.getRealAssetsRootPath() + relativePath);
		}
		if (!f.exists()) {
			Logger.warn(this, "Invalid path passed: path = " + relativePath	+ ", file doesn't exists.");

			// /WHAT ???
		}
		return (f.toURI().toString());
	}

	private String processCSSPath(String text, Host host, String extension,
			String delimiter1, String delimiter2, String endDelimiter,
			String url) throws Exception {
		Pattern p = Pattern.compile(delimiter1 + "[^" + delimiter1 + "]*\\."
				+ extension + delimiter2);
		Matcher m = p.matcher(text);
		StringBuilder sb = new StringBuilder();
		int prevIndex = 0;
		while (m.find()) {
			String match = m.group();
			match = match.substring(1, match.length() - 1);
			sb.append(text.substring(prevIndex, m.start() + 1));
			prevIndex = m.end();
			if (match.toLowerCase().startsWith("http://")
					|| match.toLowerCase().startsWith("https://")) {
				// Absolute
				sb.append(match + endDelimiter);
				sb.append(text.substring(prevIndex));
				return sb.toString();
			}
			if (!match.startsWith("/")) {
				// Relative
				match = url + match;
				match = cleanPath(match);

			}
			String relativePath = LiveCache.getPathFromCache(match, host);

			File f = new File(fileAPI.getRealAssetsRootPath()
					+ relativePath);
			if (!f.exists()) {
				Logger.warn(this, "Invalid path passed: path = " + relativePath
						+ ", file doesn't exists.");

				f = new File(context.getRealPath(match));
				if (f.exists()) {
					sb.append(f.toURI().toString()).append(endDelimiter);
				}

			} else {
				String inode = UtilMethods.getFileName(f.getName());
				com.dotmarketing.portlets.files.model.File file = fileAPI.find(inode, user, false);
				// Identifier identifier =
				// APILocator.getIdentifierAPI().findFromInode(Long.parseLong(inode));
				Identifier identifier =APILocator.getIdentifierAPI().find(file);
				if (!perAPI.doesUserHavePermission(identifier, PERMISSION_READ,
						user)) {
					Logger.warn(this, "Not authorized: path = " + relativePath);
				} else {
					//String path = f.getAbsolutePath();
					// path=path.replace("\\", "\\\\");
					// path = path.replace("\\", "/");
					 sb.append(f.toURI().toString()).append(endDelimiter);
				}
			}
		}
		sb.append(text.substring(prevIndex));
		return sb.toString();
	}

	private String cleanPath(String path) {
		if (!path.contains("..")) {
			return path;
		}
		int index = path.indexOf("..");
		String prev = path.substring(0, index - 1);
		String post = path.substring(index + 2);
		int lastIndex = prev.lastIndexOf("/"); // Normal unix slashes
		int lastIndex2 = prev.lastIndexOf("\\"); // In case we're using
													// Windows slashes
		if (lastIndex > 0 || lastIndex2 > 0) {
			if (lastIndex2 > lastIndex) {
				lastIndex = lastIndex2;
			}
			prev = prev.substring(0, lastIndex);
		} else {
			prev = "";
		}

		return cleanPath(prev + post);

	}

	/**
	 * escape esepcial cases character to make html code acceptable for document parsing
	 * @param text
	 * @return String
	 */
	private String escapeEspecialCharacter(String text){
		text = text.replaceAll("&amp;", "&");
		text = text.replaceAll("&", "&amp;");
		text = text.replaceAll("&amp;#", "&#");
		text = text.replaceAll("&amp;copy;", "&copy;");
		text = UtilMethods.escapeUnicodeCharsForHTML(text);

		return text;
	}
	
	@Override
	public void _setClientVariablesOnContext(HttpServletRequest request,
			ChainedContext context) {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(ServletConfig config) throws ServletException {

		super.init(config);
		context = config.getServletContext();
		hostList = new ArrayList<String>();

		String hosts = config.getInitParameter("external-hosts");
		if (hosts != null) {
			hosts = hosts.replace(" ", "");
			for (String s : hosts.split(",")) {
				hostList.add(s);
			}

		}
		List<Host> systemHosts;
		try {
			systemHosts = hostWebAPI.findAll(APILocator.getUserAPI().getSystemUser(), false);
		} catch (DotDataException e) {
			Logger.error(HTMLPDFServlet.class, e.getMessage(), e);
			throw new ServletException(e.getMessage(), e);
			
		} catch (DotSecurityException e) {
			Logger.error(HTMLPDFServlet.class, e.getMessage(), e);
			throw new ServletException(e.getMessage(), e);
		}
		for (Host h : systemHosts) {
			String s = h.getHostname();
			hostList.add(s);
		}

		for (int i = 0; i < 100; i++) {

			String url = Config.getStringProperty("dotcms.wiki" + i + ".uri");
			String structure = Config.getStringProperty("dotcms.wiki" + i
					+ ".structure");
			String field = Config.getStringProperty("dotcms.wiki" + i
					+ ".field");
			if (!UtilMethods.isSet(url) || !UtilMethods.isSet(structure)
					|| !UtilMethods.isSet(field)) {
				break;
			}

			List<Structure> l = StructureFactory.getStructures();
			for (Structure struct : l) {
				if (structure.equals(struct.getName())) {
					map.put(url, struct.getInode() + "|" + field);
				}
			}
		}

	}
	
	private static class DTDResolver implements EntityResolver {
		public InputSource resolveEntity(String publicId, String systemId) {
			String uri = null;
			if (systemId.startsWith("http:")) {
				ClassLoader cl = Thread.currentThread().getContextClassLoader();
				return new InputSource(cl.getResource("xhtml" + systemId.substring(systemId.lastIndexOf('/'))).getFile());
			} else if (systemId.startsWith("file:")) {
				uri = systemId;
				return new InputSource(uri);
			}
			return null;
		}
	}
}
