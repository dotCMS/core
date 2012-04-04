package com.dotmarketing.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.LRUMap;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporter;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporterException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

/**
 * 
 * This servlet allows you invoke content exporters over binary fields.
 * With the following URL syntax you are able to invoke an specific content exporter on a piece of content.
 * 
 * /contentAsset/{exporter path}/{content identifier}/{binary field - optional}?{byInode=true&}{exporter specific parameters}
 * 
 * {exporter path} is the exporter specific path set by the exporter class. I.E. every exporter must implement an interface method call getPathMapping that 
 * defines the path of what the exporter is going to be bound. E.G. The com.dotmarketing.portlets.contentlet.business.exporter.ImageResizeFieldExporter binds 
 * to the resize-image path so it can be invoked as /contentAsset/resize-image/...
 * 
 * {content identifier} is the identifier of the piece of content that wants to be retrieved. Special case occurs when the url parameter "byInode=true" is set
 * then the content specific inode must be passed here.
 * 
 * {binary field - optional} is the binary field velocity name (refer to the structure manager to fidn out which is your field velocity name). This url part could be
 * obeyed for certain exporters that operate over the entire content instead of an specific field like with an XML content exporter for example.
 * 
 * {exporter specific parameters} is for exporter specific parameters, refer to the exporter documentation. Exporters like the thubmnail generator takes parameters
 * like the width or height of the thumbnail to be generated.
 * 
 * @author David Torres 2010
 * 
 */
public class BinaryExporterServlet extends HttpServlet {

	private static final FileAPI fileAPI = APILocator.getFileAPI();
	private static final UserAPI userAPI = APILocator.getUserAPI();
	Map<String, BinaryContentExporter> exportersByPathMapping;
	private static String assetPath = "/assets";
	private static String realPath = null;
	
	@SuppressWarnings("unchecked")
	@Override
	public void init() throws ServletException {
		super.init();
        if(UtilMethods.isSet(Config.getStringProperty("ASSET_REAL_PATH"))){
            realPath = Config.getStringProperty("ASSET_REAL_PATH");
        } 
        if(UtilMethods.isSet(Config.getStringProperty("ASSET_PATH"))){
            assetPath = Config.getStringProperty("ASSET_PATH");
        } 
		exportersByPathMapping = new HashMap<String, BinaryContentExporter>();
		
		Iterator<String> keys = Config.getKeys();
		
		while(keys.hasNext()) {
			String key = keys.next();
			if(key.startsWith("CONTENT_EXPORTERS")) {
				String[]  exporterClasses = Config.getStringArrayProperty(key);
				for(String exporterClassName : exporterClasses) {
					try {
						Class<BinaryContentExporter> exporterClass = (Class<BinaryContentExporter>) Class.forName(exporterClassName);
						BinaryContentExporter exporter = exporterClass.newInstance();
						if(exportersByPathMapping.containsKey(exporter.getPathMapping()))
							Logger.warn(BinaryExporterServlet.class, "There is already an exporter registered to path " + exporter.getPathMapping() + 
									" this new exporter: " + exporter.getName() + " will replace the previously registered: " + 
									exportersByPathMapping.get(exporter.getPathMapping()).getName());
						
						Logger.info(this, "Exporter \"" + exporter.getName() + "\" registered for path /" + exporter.getPathMapping());
						exportersByPathMapping.put(exporter.getPathMapping(), exporter);
							
					} catch (ClassNotFoundException e) {
						Logger.warn(BinaryExporterServlet.class, e.getMessage(), e);
					} catch (InstantiationException e) {
						Logger.warn(BinaryExporterServlet.class, e.getMessage(), e);
					} catch (IllegalAccessException e) {
						Logger.warn(BinaryExporterServlet.class, e.getMessage(), e);
					}
				}
			}
		}
	}
	
	private static final long serialVersionUID = 1L;
	
	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	
       
        String servletPath = req.getServletPath();
		String uri = req.getRequestURI().substring(servletPath.length());
		
		String[] uriPieces = uri.split("/");
		String exporterPath = uriPieces[1];
		String uuid = uriPieces[2];
		
		Map<String, String[]> params = new HashMap<String, String[]>();
		params.putAll(req.getParameterMap());
		params = sortByKey(params);
		
		String assetInode = null;
		String assetIdentifier = null;
		boolean byInode = (req.getParameter("byInode") != null);
		if (byInode){
			assetInode = uuid;
		}
		else{
			assetIdentifier = uuid;
		}

		String fieldVarName = uriPieces.length > 3?uriPieces[3]:null;
		
		BinaryContentExporter exporter = exportersByPathMapping.get(exporterPath);
		if(exporter == null) {
			Logger.warn(this, "No exporter for path " + exporterPath + " is registered. Requested url = " + uri);
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
		ContentletAPI contentAPI = APILocator.getContentletAPI();
		BinaryContentExporter.BinaryContentExporterData data = null;
		File inputFile = null;
		try {
			User user = userWebAPI.getLoggedInUser(req);
			boolean respectFrontendRoles = !userWebAPI.isLoggedToBackend(req);
			
			String downloadName = "file_asset";
			
			HttpSession session =req.getSession(false); 
			long lang =APILocator.getLanguageAPI().getDefaultLanguage().getId();
			try{
				String x  = (String) session.getAttribute(WebKeys.HTMLPAGE_LANGUAGE);
				lang  = Long.parseLong(x);
			}
			catch(Exception e){
				try{
					String x  = (String) req.getAttribute(WebKeys.HTMLPAGE_LANGUAGE);
					lang  = Long.parseLong(x);
				}
				catch(Exception ex){

				}
				
			}
			
			
			
			boolean isContent = isContent(uuid, byInode, lang);

			
			if(isContent){
				Contentlet content = null;
				if(byInode) {
					content = contentAPI.find(assetInode, user, respectFrontendRoles);
					assetIdentifier = content.getIdentifier();
				} else {
					content = contentAPI.findContentletByIdentifier(assetIdentifier, userWebAPI.isLoggedToFrontend(req), lang, user, respectFrontendRoles);
					assetInode = content.getInode();
				}
				Field field = content.getStructure().getFieldVar(fieldVarName);
				
				if(field == null){
					throw new ServletException("Field " + fieldVarName + " does not exists within structure " + content.getStructure().getVelocityVarName());
				}

				inputFile = contentAPI.getBinaryFile(content.getInode(), field.getVelocityVarName(), user);
				if(inputFile == null){
					throw new ServletException("binary file '" + fieldVarName + "' does not exist for inode " + content.getInode());
				}
				downloadName = inputFile.getName();
				
			}
			else{
				// if we are using this as a "Save as" from the image too
				fieldVarName = WebKeys.EDITED_IMAGE_FILE_ASSET;
				com.dotmarketing.portlets.files.model.File dotFile = null;
				
				
				// get the identifier from cache
				if(byInode) {
					dotFile = fileAPI.find(assetInode,user,false);
					downloadName = dotFile.getFileName();
					//com.dotmarketing.portlets.files.model.File dotFile = APILocator.getFileAPI().get(assetIdentifier, user, respectFrontendRoles);
					assetIdentifier = dotFile.getIdentifier();
				}				
				Identifier id = APILocator.getIdentifierAPI().find(assetIdentifier);
				
				
				// no identifier, no soup!
				if(id == null || ! UtilMethods.isSet(id.getInode())){
					throw new DotContentletStateException("Identifier: " + assetIdentifier +"not found");
				}
				
				boolean hasLive = (LiveCache.getPathFromCache(id.getURI(), id.getHostId()) != null);
				
				// no live version and front end, no soup
				if(respectFrontendRoles && ! hasLive){
					throw new DotSecurityException("File :" + id.getInode() +"is not live");
				}
				// no permissions, no soup!
				if(!APILocator.getPermissionAPI().doesUserHavePermission(id, PermissionAPI.PERMISSION_READ, user)){
					throw new DotSecurityException("user: " + user + " does not have read on File :" + id.getInode());
				}
				
	
				if(assetInode != null){
					inputFile = new File(fileAPI.getRealAssetPath(assetInode, UtilMethods.getFileExtension(dotFile.getFileName())));
				}
				else if(respectFrontendRoles){
					if(realPath != null){
						inputFile = new File(realPath + LiveCache.getPathFromCache(id.getURI(), id.getHostId()));
					}else{
						inputFile = new File(Config.CONTEXT.getRealPath(assetPath + LiveCache.getPathFromCache(id.getURI(), id.getHostId()))); 
					}
				}else{
					if(realPath != null){
						inputFile = new File(realPath + WorkingCache.getPathFromCache(id.getURI(), id.getHostId()));
					}else{
						inputFile = new File(Config.CONTEXT.getRealPath(assetPath + WorkingCache.getPathFromCache(id.getURI(), id.getHostId()))); 
					}
				}
					

				
			}
			
			if(UtilMethods.isSet(fieldVarName))//DOTCMS-5674
				params.put("fieldVarName", new String[]{fieldVarName});
			
			data = exporter.exportContent(inputFile, params);
			
			
			
			// THIS IS WHERE THE MAGIC HAPPENS
			// save to session if user looking to edit a file
			if(req.getParameter(WebKeys.IMAGE_TOOL_SAVE_FILES) != null){ 
				

				
				
		    	Map<String, String> files = (Map<String, String>) session.getAttribute(WebKeys.IMAGE_TOOL_SAVE_FILES);
		    	if(files == null){
		    		files = new HashMap<String, String>();
		    	}
		    	session.setAttribute(WebKeys.IMAGE_TOOL_SAVE_FILES, files);
		    	
		    	
		    	String ext = UtilMethods.getFileExtension(data.getDataFile().getName());
		    	File tmp = File.createTempFile(data.getDataFile().getName(), "." +ext);
		    	FileUtil.copyFile(data.getDataFile(), tmp);
		    	tmp.deleteOnExit(); 
		    	if(req.getParameter("binaryFieldId") != null){
		    		files.put(req.getParameter("binaryFieldId"), tmp.getCanonicalPath());
		    	}
		    	else{
		    		files.put(fieldVarName, tmp.getCanonicalPath());
		    	}
		    	session.setAttribute(WebKeys.IMAGE_TOOL_SAVE_FILES, files);

				
		    	resp.getWriter().println(PublicEncryptionFactory.encryptString(tmp.getAbsolutePath()));
		    	resp.getWriter().close();
		    	resp.flushBuffer();
		    	
		    	return;
			
			}

			
			/*******************************
			 * 
			 *  Start serving the data
			 * 
			 *******************************/
			
			
			String mimeType = fileAPI.getMimeType(data.getDataFile().getName());

			if (mimeType == null) 
				mimeType = "application/octet-stream";
			resp.setContentType(mimeType);

			if(req.getParameter("force_download") != null) {
				
				// if we are downloading a jpeg version of a png or gif
				String x = UtilMethods.getFileExtension(downloadName);
				String y = UtilMethods.getFileExtension(data.getDataFile().getName());
				if(!x.equals(y)){
					downloadName = downloadName.replaceAll("\\." + x, "\\." + y);
				}
				resp.setHeader("Content-Disposition", "attachment; filename=" + downloadName);
				resp.setHeader("Content-Type", "application/force-download");
			}			
			
		    int _daysCache = 365;
		    GregorianCalendar expiration = new GregorianCalendar();
			expiration.add(java.util.Calendar.DAY_OF_MONTH, _daysCache);
			int seconds = (_daysCache * 24 * 60 * 60);
			
			long _lastModified = data.getDataFile().lastModified();
			if(_lastModified < 0) {
			    _lastModified = 0; 
			}
			// we need to round the _lastmodified to get rid of the milliseconds.
			_lastModified = _lastModified / 1000;
			_lastModified = _lastModified * 1000;
			Date _lastModifiedDate = new java.util.Date(_lastModified);
	
			
			long _fileLength = data.getDataFile().length();
			String _eTag = "dot:" + assetInode + ":" + _lastModified + ":" + _fileLength;
			
			SimpleDateFormat httpDate = new SimpleDateFormat(Constants.RFC2822_FORMAT);
			httpDate.setTimeZone(TimeZone.getTimeZone("GMT"));
            /* Setting cache friendly headers */
            resp.setHeader("Expires", httpDate.format(expiration.getTime()));
            resp.setHeader("Cache-Control", "public, max-age="+seconds);
			
            String ifModifiedSince = req.getHeader("If-Modified-Since");
            String ifNoneMatch = req.getHeader("If-None-Match");
            
            /*
             * If the etag matches then the file is the same
             * 
            */
            if(ifNoneMatch != null){
                if(_eTag.equals(ifNoneMatch) || ifNoneMatch.equals("*")){
                    resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED );
                    return;
                }
            }
       
            /* Using the If-Modified-Since Header */
             if(ifModifiedSince != null){
			    try{
			        Date ifModifiedSinceDate = httpDate.parse(ifModifiedSince);
			        if(_lastModifiedDate.getTime() <= ifModifiedSinceDate.getTime()){
			            resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED );
			            return;
			        }
			    }
			    catch(Exception e){}
			}
             
            resp.setHeader("Last-Modified", httpDate.format(_lastModifiedDate));  
            resp.setHeader("Content-Length", String.valueOf(_fileLength));
            resp.setHeader("ETag", _eTag);
            //resp.setHeader("Content-Disposition", "attachment; filename=" + data.getDataFile().getName());

            FileInputStream is = new FileInputStream(data.getDataFile());
            
            int count = 0;
            byte[] buffer = new byte[4096];
            OutputStream servletOutput = resp.getOutputStream();
            while((count = is.read(buffer)) > 0) 
            	servletOutput.write(buffer, 0, count);
            
            servletOutput.close();
            
		} catch (DotContentletStateException e) {
			Logger.error(BinaryExporterServlet.class, e.getMessage());
			Logger.debug(BinaryExporterServlet.class, e.getMessage(),e);
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		} catch (DotRuntimeException e) {
			//Logger.error(BinaryExporterServlet.class, e.getMessage());
			Logger.debug(BinaryExporterServlet.class, e.getMessage(),e);
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		} catch (PortalException e) {
			Logger.error(BinaryExporterServlet.class, e.getMessage());
			Logger.debug(BinaryExporterServlet.class, e.getMessage(),e);
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		} catch (SystemException e) {
			Logger.error(BinaryExporterServlet.class, e.getMessage());
			Logger.debug(BinaryExporterServlet.class, e.getMessage(),e);
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		} catch (DotDataException e) {
			Logger.error(BinaryExporterServlet.class, e.getMessage());
			Logger.debug(BinaryExporterServlet.class, e.getMessage(),e);
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		} catch (DotSecurityException e) {
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
		} catch (BinaryContentExporterException e) {
			Logger.debug(BinaryExporterServlet.class, e.getMessage(),e);
			Logger.error(BinaryExporterServlet.class, e.getMessage());
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private Map sortByKey(Map map) {
		List list = new LinkedList(map.entrySet());
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o1)).getKey()).compareTo(((Map.Entry) (o2)).getKey());
			}
		});
		// logger.info(list);
		Map result = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
	
	
	
	// Tries to find out whether this is content or a file
	private boolean isContent(String id, boolean byInode, long langId) throws DotStateException, DotDataException, DotSecurityException{


		
		if(cacheMisses.containsKey(id)){
			throw new DotStateException("404 - Unable to find id:" + id);
		}

		
		if(byInode){
			try {
				Contentlet c =APILocator.getContentletAPI().find(id, userAPI.getSystemUser(), true);
				if(c != null && c.getInode() != null)
					return true;
			} catch (Exception e) {
				Logger.debug(this.getClass(), "Unable to find contentlet " + id);
			}
			try {
				if(fileAPI.find(id,userAPI.getSystemUser(),false) != null){
					return false;
				}
			} catch (DotHibernateException e) {
				Logger.debug(this.getClass(), "cant find file with inode " + id);
			}
		}

		else{
			try {
				
				Identifier identifier = APILocator.getIdentifierAPI().loadFromCache(id);
				if(identifier != null){
					return "contentlet".equals(identifier.getAssetType());
				}
				
				//second check content check from lucene
				String luceneQuery = "+identifier:" + id;
				Contentlet c = APILocator.getContentletAPI().findContentletByIdentifier(id, false,langId, userAPI.getSystemUser(), false);
				if(c != null && UtilMethods.isSet(c.getInode())){
					return true;
				}

			} catch (Exception e) {
				Logger.debug(this.getClass(), "cant find identifier " + id);
			}


		}
		cacheMisses.put(id, true);
		throw new DotStateException("404 - Unable to find id:" + id);
		
		
	}
	@SuppressWarnings("deprecation")
	private Map cacheMisses = new LRUMap(1000);
	
}
