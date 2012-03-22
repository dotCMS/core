/*
 * WebSessionFilter
 *
 * A filter that recognizes return users who have
 * chosen to have their login information remembered.
 * Creates a valid WebSession object and
 * passes it a contact to use to fill its information
 *
 */
package com.dotmarketing.cms.urlmap.filters;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryParser.ParseException;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.cache.VirtualLinksCache;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.structure.StructureUtil;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.RegExMatch;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public class URLMapFilter implements Filter {

	private List<PatternCache> patternsCache = new ArrayList<PatternCache>();
	private ContentletAPI conAPI;
	private UserWebAPI wuserAPI;
	private HostWebAPI whostAPI;
	private boolean urlFallthrough;

	public void destroy() {

	}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
			ServletException {
		
		

		
		
		HttpServletRequest request = (HttpServletRequest) req;
		HttpSession session = request.getSession();
		String uri = request.getRequestURI();
		uri = URLDecoder.decode(uri, "UTF-8");
		if(CMSFilter.excludeURI(uri)){
			chain.doFilter(req, res);
			return;
		}
		/*
		 * Getting host object form the session
		 */
		Host host;
		try {
			host = whostAPI.getCurrentHost(request);
		} catch (PortalException e) {
			Logger.error(this, "Unable to retrieve current request host for URI " + uri);
			throw new ServletException(e.getMessage(), e);
		} catch (SystemException e) {
			Logger.error(this, "Unable to retrieve current request host for URI  " + uri);
			throw new ServletException(e.getMessage(), e);
		} catch (DotDataException e) {
			Logger.error(this, "Unable to retrieve current request host for URI  " + uri);
			throw new ServletException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(this, "Unable to retrieve current request host for URI  " + uri);
			throw new ServletException(e.getMessage(), e);
		}

		// http://jira.dotmarketing.net/browse/DOTCMS-6079
		if (uri.endsWith("/"))
			uri = uri.substring(0, uri.length() - 1);
	    
		String pointer = null;
		
		if(host!=null){
			pointer = VirtualLinksCache.getPathFromCache(host.getHostname() + ":" + uri);
		}
		if (!UtilMethods.isSet(pointer)) {
			pointer = VirtualLinksCache.getPathFromCache(uri);
		}
		if(UtilMethods.isSet(pointer)){
			uri = pointer;
		}

		
		
		String mastRegEx = null;
		StringBuilder query = null;
		try {
			mastRegEx = StructureCache.getURLMasterPattern();
		} catch (DotCacheException e2) {
			Logger.error(URLMapFilter.class, e2.getMessage(), e2);
		}
		if (mastRegEx == null) {
			synchronized (StructureCache.MASTER_STRUCTURE) {
				try {
					mastRegEx = buildCacheObjects();
				} catch (DotDataException e) {
					Logger.error(URLMapFilter.class, e.getMessage(), e);
					throw new ServletException("Unable to build URLMap patterns", e);
				}
			}
		}
		boolean trailSlash = uri.endsWith("/");
		boolean isDotPage = uri.substring(uri.lastIndexOf(".")+1).equals(Config.getStringProperty("VELOCITY_PAGE_EXTENSION"));
		String url = (!trailSlash && !isDotPage)?uri+'/':uri;
		if (!UtilMethods.isSet(mastRegEx) || uri.contains("webdav")) {
			chain.doFilter(req, res);
			return;
		}
		if (RegEX.contains(url, mastRegEx)) {
			boolean ADMIN_MODE = (session.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
			boolean EDIT_MODE = ((session.getAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION) != null) && ADMIN_MODE);

			Structure structure = null;
			User user = null;
			try {
				user = wuserAPI.getLoggedInUser(request);
			} catch (DotRuntimeException e1) {
				Logger.error(URLMapFilter.class, e1.getMessage(), e1);
			} catch (PortalException e1) {
				Logger.error(URLMapFilter.class, e1.getMessage(), e1);
			} catch (SystemException e1) {
				Logger.error(URLMapFilter.class, e1.getMessage(), e1);
			}
	
			List<ContentletSearch> cons = null;
			Collections.sort(patternsCache, new Comparator<PatternCache>(){
				public int compare(PatternCache o1, PatternCache o2) {
					String regex1 = o1.getRegEx();
					String regex2 = o2.getRegEx();
					if(!regex1.endsWith("/")){
						regex1+="/";
					}
					if(!regex2.endsWith("/")){
						regex2+="/";
					}
				
					int regExLength1 = getSlashCount(regex1);
			    	int regExLength2 = getSlashCount(regex2);
					if(regExLength1 < regExLength2){
			    	    return 1;
			    	}else if(regExLength1 > regExLength2){
			    	    return -1;
			        }else{
			    	    return 0;  
			        }
				}
			});
			for (PatternCache pc : patternsCache) {
				List<RegExMatch> matches = RegEX.findForUrlMap(url, pc.getRegEx());
				if (matches != null && matches.size() > 0) {
					query = new StringBuilder();
					List<RegExMatch> groups = matches.get(0).getGroups();
					List<String> fieldMatches = pc.getFieldMatches();
					structure = StructureCache.getStructureByInode(pc.getStructureInode());
					List<Field> fields = FieldsCache.getFieldsByStructureInode(structure.getInode());
					query.append("+structureName:" + structure.getVelocityVarName() + " +deleted:false ");
					if (EDIT_MODE || ADMIN_MODE) {
						query.append("+working:true ");
					} else {
						query.append("+live:true ");
					}

					// Set Host Stuff
					boolean hasHostField = false;
					Boolean hostIsRequired = false;
					for (Field field : fields) {
						if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
							hasHostField = true;
							if (field.isRequired()) {
								hostIsRequired = true;
							}
							break;
						}
					}
					if (hasHostField) {
						if (host != null) {
							//if (hostIsRequired) {
							//query.append("+conhost:" + host.getIdentifier() + " ");
							//} else {
							try {
								query.append("+(conhost:" + host.getIdentifier() + " "
										+ "conhost:"
										+ whostAPI.findSystemHost(wuserAPI.getSystemUser(), true).getIdentifier()
										+ ") ");
							} catch (Exception e) {
								Logger.error(URLMapFilter.class, e.getMessage()
										+ " : Unable to build host in query : ", e);
							}
							//}
						}
					}

					// build fields
					int counter = 0;
					for (RegExMatch regExMatch : groups) {
						String value = regExMatch.getMatch();
						if (value.endsWith("/")) {
							value = value.substring(0, value.length() - 1);
						}
						query.append("+" + structure.getVelocityVarName() + "." + fieldMatches.get(counter) + ":"
								+ value + " ");
						counter++;
					}
					
					try {
						cons = conAPI.searchIndex(query.toString(), 1, 0, (hostIsRequired?"conhost, modDate": "modDate"), user, true);
						ContentletSearch c = cons.get(0);
						request.setAttribute(WebKeys.WIKI_CONTENTLET, c.getIdentifier());
						request.setAttribute(WebKeys.WIKI_CONTENTLET_INODE, c.getInode());
						request.setAttribute(WebKeys.CLICKSTREAM_IDENTIFIER_OVERRIDE, c.getIdentifier());
						request.setAttribute(WebKeys.WIKI_CONTENTLET_URL, url);
						String[] x = url.split("/");
						for(int i=0;i<x.length;i++){
							if(UtilMethods.isSet(x[i])){
								request.setAttribute("URL_ARG" + i, x[i]);
							}
						}
						
						break;
					} catch (DotDataException e) {
						Logger.warn(this, "DotDataException", e);
					} catch (DotSecurityException e) {
						Logger.warn(this, "DotSecurityException", e);
					} catch (ParseException e) {
						Logger.warn(this, "ParseException", e);
					} catch(java.lang.IndexOutOfBoundsException iob){
						Logger.warn(this, "No urlmap contentlent found uri:" + url + " query:" + query.toString());
					}catch(Exception e){
						Logger.warn(this, "No index?" + e.getMessage());
					}
				}
			}
			
		
			if (structure != null && UtilMethods.isSet(structure.getDetailPage())) {
				Identifier ident;
				try {
					ident = APILocator.getIdentifierAPI().find(structure.getDetailPage());
					if(ident ==null || ! UtilMethods.isSet(ident.getInode())){
						throw new DotRuntimeException("No valid detail page for structure '" + structure.getName() + "'. Looking for detail page id=" + structure.getDetailPage());
					}

					
					if((cons != null && cons.size() > 0) || !urlFallthrough){
						
						if (request.getParameter("livePage") != null && request.getParameter("livePage").equals("1")) {
							EDIT_MODE = false;
							session.setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, null);
							request.setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, null);
							session.setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, null);
							request.setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, null);
							LogFactory.getLog(this.getClass()).debug("URLMAP FILTER Cleaning PREVIEW_MODE_SESSION LIVE!!!!");

						}

						if (request.getParameter("previewPage") != null && request.getParameter("previewPage").equals("1")) {
							EDIT_MODE = true;
							session.setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, null);
							request.setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, null);
							session.setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, "true");
							request.setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, "true");
							LogFactory.getLog(this.getClass()).debug("URLMAP FILTER Cleaning EDIT_MODE_SESSION PREVIEW!!!!");
						}

						if (request.getParameter("previewPage") != null && request.getParameter("previewPage").equals("2")) {
							EDIT_MODE = false;
							session.setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, "true");
							request.setAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION, "true");
							session.setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, null);
							request.setAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION, null);
							LogFactory.getLog(this.getClass()).debug("URLMAP FILTER Cleaning PREVIEW_MODE_SESSION PREVIEW!!!!");
						}
						
						request.getRequestDispatcher(ident.getURI()).forward(req, res);
					}else{
						chain.doFilter(req, res);
					}
					return;
				} catch (Exception e) {
					Logger.error(URLMapFilter.class, e.getMessage(), e);
				}
			}

		}
		chain.doFilter(req, res);
	}

	public void init(FilterConfig config) throws ServletException {
		Config.setMyApp(config.getServletContext());
		conAPI = APILocator.getContentletAPI();
		wuserAPI = WebAPILocator.getUserWebAPI();
		whostAPI = WebAPILocator.getHostWebAPI();
		
		// persistant on disk cache makes this necessary
		StructureCache.clearURLMasterPattern();
		urlFallthrough = Config.getBooleanProperty("URLMAP_FALLTHROUGH", true);
	}

	private synchronized String buildCacheObjects() throws DotDataException {
		List<SimpleStructureURLMap> urlMaps = StructureFactory.findStructureURLMapPatterns();
		StringBuilder masterRegEx = new StringBuilder();
		boolean first = true;
		patternsCache.clear();
		for (SimpleStructureURLMap urlMap : urlMaps) {
			PatternCache pc = new PatternCache();
			String regEx = StructureUtil.generateRegExForURLMap(urlMap.getURLMapPattern());
			// if we have an empty string, move on
			if (!UtilMethods.isSet(regEx) || regEx.trim().length() < 3) {
				continue;

			}
			pc.setRegEx(regEx);
			pc.setStructureInode(urlMap.getInode());
			pc.setURLpattern(urlMap.getURLMapPattern());
			List<RegExMatch> fieldMathed = RegEX.find(urlMap.getURLMapPattern(), "{([^{}]+)}");
			List<String> fields = new ArrayList<String>();
			for (RegExMatch regExMatch : fieldMathed) {
				fields.add(regExMatch.getGroups().get(0).getMatch());
			}
			pc.setFieldMatches(fields);
			patternsCache.add(pc);
			if (!first) {
				masterRegEx.append("|");
			}
			masterRegEx.append(regEx);
			first = false;
		}

		StructureCache.addURLMasterPattern(masterRegEx.toString());
		return masterRegEx.toString();
	}

	private class PatternCache {
		private String regEx;
		private String structureInode;
		private String URLpattern;
		private List<String> fieldMatches;

		public void setRegEx(String regEx) {
			this.regEx = regEx;
		}

		public String getRegEx() {
			return regEx;
		}

		public void setStructureInode(String structureInode) {
			this.structureInode = structureInode;
		}

		public String getStructureInode() {
			return structureInode;
		}

		public void setURLpattern(String uRLpattern) {
			URLpattern = uRLpattern;
		}

		@SuppressWarnings("unused")
		public String getURLpattern() {
			return URLpattern;
		}

		public void setFieldMatches(List<String> fieldMatches) {
			this.fieldMatches = fieldMatches;
		}

		public List<String> getFieldMatches() {
			return fieldMatches;
		}
	}
	
	private int getSlashCount(String string){
		int ret = 0;
		if(UtilMethods.isSet(string)){
			for(int i=0;i<string.length();i++){
				if(string.charAt(i)=='/'){
					ret+=1;
				}
			}
		}
		return ret;
	}
}
