package com.dotmarketing.servlets;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.io.Files;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

public class SpeedyAssetServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static String realPath = null;
	private static String assetPath = "/assets";
	private static class ThreadLocalHTTPDate extends ThreadLocal<java.text.SimpleDateFormat>{
		@Override
		protected SimpleDateFormat initialValue() {
			SimpleDateFormat sdf = new java.text.SimpleDateFormat(Constants.RFC2822_FORMAT);
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			return sdf;
		}
	}

	private static final ThreadLocalHTTPDate httpDate = new SpeedyAssetServlet.ThreadLocalHTTPDate();

	private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();

    public void init(ServletConfig config) throws ServletException {
        // Set the asset paths
        try {
            realPath = Config.getStringProperty("ASSET_REAL_PATH");
        } catch (Exception e) { }
        try {
            assetPath = Config.getStringProperty("ASSET_PATH");
        } catch (Exception e) { }
        httpDate.get().setTimeZone(TimeZone.getTimeZone("GMT"));
    }




    protected void service(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {

		if (Config.CONTEXT == null) {
			Config.CONTEXT = this.getServletContext();
			Logger.error(this, "Config.CONTEXT is null. RESETTING  Cannot Serve Files without this!!!!!!");
		}



		File f;
		boolean PREVIEW_MODE = false;
		boolean EDIT_MODE = false;
		HttpSession session = request.getSession(false);

		if(session != null) {
			PREVIEW_MODE = ((session.getAttribute(com.dotmarketing.util.WebKeys.PREVIEW_MODE_SESSION) != null));
			try {
				EDIT_MODE = (((session.getAttribute(com.dotmarketing.util.WebKeys.EDIT_MODE_SESSION) != null)) || WebAPILocator.getUserWebAPI().isLoggedToBackend(request));
			} catch (DotRuntimeException e) {
				Logger.error(this, "Error: Unable to determine if there's a logged user.", e);
			} catch (PortalException e) {
				Logger.error(this, "Error: Unable to determine if there's a logged user.", e);
			} catch (SystemException e) {
				Logger.error(this, "Error: Unable to determine if there's a logged user.", e);
			}

		}

        User user = null;
        try {
            if (session != null)
                user = (com.liferay.portal.model.User) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_USER);
        	if(user==null){
				user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
			}
        } catch (Exception nsue) {
            Logger.warn(this, "Exception trying to getUser: " + nsue.getMessage(), nsue);
        }

		String uri = "";
		try {

			String relativePath = null;
			if(request.getParameter("path") == null) {

				// Getting the identifier from the path like /dotAsset/{identifier}.{ext} E.G. /dotAsset/1234.js
				StringTokenizer _st = new StringTokenizer(request.getRequestURI(), "/");

				Logger.debug(this, "Requesting by url: " + request.getRequestURI());

				String _fileName = null;
				while(_st.hasMoreElements()){
					_fileName = _st.nextToken();
				}

				Logger.debug(this, "Parsed filename: " + _fileName);

				String identifier = UtilMethods.getFileName(_fileName);
				Identifier ident = null;
				Logger.debug(SpeedyAssetServlet.class, "Loading identifier: " + identifier);
				try{
					ident = APILocator.getIdentifierAPI().find(identifier);
				}catch(Exception ex){
					Logger.debug(SpeedyAssetServlet.class, "Identifier not found going to try as a File Asset", ex);
				}
				if(ident != null && ident.getURI() != null && !ident.getURI().equals("")){

					if(PREVIEW_MODE || EDIT_MODE){
						uri = WorkingCache.getPathFromCache(ident.getURI(), ident.getHostId());
						if(!UtilMethods.isSet(realPath)){
							f = new File(FileUtil.getRealPath(assetPath + uri));
						}else{
							f = new File(realPath + uri);
						}
						if(uri == null || !f.exists() || !f.canRead()) {
							if(uri == null){
								Logger.warn(SpeedyAssetServlet.class, "URI is null");
							}
							if( !f.exists()){
								Logger.warn(SpeedyAssetServlet.class, "f does not exist : Config.CONTEXT=" +Config.CONTEXT);
								Logger.warn(SpeedyAssetServlet.class, "f does not exist : " + f.getAbsolutePath());
							}
							if( !f.canRead()){
								Logger.warn(SpeedyAssetServlet.class, "f cannot be read : Config.CONTEXT=" +Config.CONTEXT);
								Logger.warn(SpeedyAssetServlet.class, "f cannot be read : " + f.getAbsolutePath());
							}
							response.sendError(404);
							return;
						}

					}else {
						uri = LiveCache.getPathFromCache(ident.getURI(), ident.getHostId());
						if(!UtilMethods.isSet(realPath)){
							f = new File(FileUtil.getRealPath(assetPath + uri));
						}else{
							f = new File(realPath + uri);
						}
						if(uri == null || !f.exists() || !f.canRead()) {
							if(uri == null){
								Logger.warn(SpeedyAssetServlet.class, "URI is null");
							}
							if( !f.exists()){
								Logger.warn(SpeedyAssetServlet.class, "f does not exist : Config.CONTEXT=" +Config.CONTEXT);
								Logger.warn(SpeedyAssetServlet.class, "f does not exist : " + f.getAbsolutePath());
							}
							if( !f.canRead()){
								Logger.warn(SpeedyAssetServlet.class, "f cannot be read  : Config.CONTEXT=" +Config.CONTEXT);
								Logger.warn(SpeedyAssetServlet.class, "f cannot be read : " + f.getAbsolutePath());
							}



							response.sendError(404);
							return;
						}
					}
				} else {
					Logger.warn(this, "Invalid identifier passed: url = " + request.getRequestURI());
					//Invalid identifier number passed
					response.sendError(404);
					return;
				}

			} else{
				relativePath = request.getParameter("path");
				f = new File(APILocator.getFileAPI().getRealAssetsRootPath() + relativePath);
				if(!f.exists() || !f.canRead()) {
					Logger.warn(this, "Invalid path passed: path = " + relativePath + ", file doesn't exists.");
					//Invalid path given
					response.sendError(404);
					return;
				}
			}

			if(f.getPath().endsWith(".groovy") || f.getPath().endsWith(".php") || f.getPath().endsWith(".rb")  
					|| f.getPath().endsWith(".vtl")|| f.getPath().endsWith(".vm")){
				Logger.warn(this, "SpeedyAsset servlet should not serve this types: .groovy, .php, .rb, .vtl and .vm  ");
				return;
				
			}

			String inode = null;

			IFileAsset file = null;
			Identifier identifier = null;
			boolean canRead = false;
			if(UtilMethods.isSet(relativePath) && relativePath.contains("fileAsset")){
				String[] splits = relativePath.split(Pattern.quote(File.separator));
				if(splits.length>0){
					inode = splits[3];
					Contentlet cont =  null;
					try{
						cont = APILocator.getContentletAPI().find(inode, user, true);
						file = APILocator.getFileAssetAPI().fromContentlet(cont);
						identifier = APILocator.getIdentifierAPI().find(cont);
						canRead = true;
					}catch(Exception e){	
						Logger.warn(this, "Unable to find file asset contentlet with inode " + inode, e);
					}
				}
			}else if(UtilMethods.isSet(uri) && uri.contains("fileAsset")){
				String[] splits = uri.split(Pattern.quote(File.separator));
				if(splits.length>0){
					inode = splits[3];
					Contentlet cont =  null;
					try{
						cont = APILocator.getContentletAPI().find(inode, user, true);
						file = APILocator.getFileAssetAPI().fromContentlet(cont);
						identifier = APILocator.getIdentifierAPI().find(cont);
						canRead = true;
					}catch(Exception e){	
						Logger.warn(this, "Unable to find file asset contentlet with inode " + inode, e);
					}
				}
			}else{
			
				inode = UtilMethods.getFileName(f.getName());
				file = APILocator.getFileAPI().find(inode, user, true);
				identifier = APILocator.getIdentifierAPI().find((com.dotmarketing.portlets.files.model.File)file);
				com.dotmarketing.portlets.files.model.File fProxy = new com.dotmarketing.portlets.files.model.File();
	            fProxy.setIdentifier(identifier.getInode());
	            canRead = permissionAPI.doesUserHavePermission(fProxy, PERMISSION_READ, user, true);
			}
//			Identifier identifier = APILocator.getIdentifierAPI().findFromInode(Long.parseLong(inode));
			

			String mimeType = APILocator.getFileAPI().getMimeType(f.getName());
			if (mimeType == null)
				mimeType = "application/octet-stream";

			response.setContentType(mimeType);

			//Checking permissions
        	/**
        	 * Build a fake proxy file object so we
        	 * can get inheritable permissions on it
        	 * without having to hit cache or db
        	 */
            

            if (!canRead) {
            	if(user == null){
            		request.getSession(true).setAttribute(com.dotmarketing.util.WebKeys.REDIRECT_AFTER_LOGIN, request.getRequestURI());//DOTCMS-5682
            		//Sending user to unauthorized the might send him to login
            		response.sendError(401, "The requested file is unauthorized");
            	}else{
            		//sending the user to forbidden
            		response.sendError(403, "The requested file is forbidden");
            	}
           		return;
            }
            
            response.setHeader("Content-Disposition","filename=\"" + file.getFileName() + "\"");

			/*
			 * Setting the proper content headers
			 */
			if(request.getParameter("dotcms_force_download") != null || request.getParameter("force_download") != null) {
				String url = request.getRequestURL().toString();
				String filename = url.substring(url.lastIndexOf("/") + 1, url.length());
				filename = file.getFileName();
				response.setHeader("Content-Type", "application/force-download");
				response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
			} else {
				boolean _adminMode = false;
				try {
				    _adminMode = (session!=null && session.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
				}catch(Exception e){

				}

			    // Set the expiration time
				if (!_adminMode) {

				    int _daysCache = 30;
				    GregorianCalendar expiration = new GregorianCalendar();
					expiration.add(java.util.Calendar.DAY_OF_MONTH, _daysCache);
					int seconds = (_daysCache * 24 * 60 * 60);

					long _lastModified = f.lastModified();
					if(_lastModified < 0) {
					    _lastModified = 0;
					}
					// we need to round the _lastmodified to get rid of the milliseconds.
					_lastModified = _lastModified / 1000;
					_lastModified = _lastModified * 1000;
					Date _lastModifiedDate = new java.util.Date(_lastModified);


					long _fileLength = f.length();
					String _eTag = "dot:" + inode + ":" + _lastModified + ":" + _fileLength;


	                /* Setting cache friendly headers */
                    response.setHeader("Expires", httpDate.get().format(expiration.getTime()));
                    response.setHeader("Cache-Control", "public, max-age="+seconds);


                    String ifModifiedSince = request.getHeader("If-Modified-Since");
                    String ifNoneMatch = request.getHeader("If-None-Match");

                    /*
                     * If the etag matches then the file is the same
                     *
                    */
                    if(ifNoneMatch != null){
                        if(_eTag.equals(ifNoneMatch) || ifNoneMatch.equals("*")){
                            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED );
                            f = null;
                            return;
                        }
                    }

                    /* Using the If-Modified-Since Header */
                     if(ifModifiedSince != null){
					    try{
					        Date ifModifiedSinceDate = httpDate.get().parse(ifModifiedSince);

					        if(_lastModifiedDate.getTime() <= ifModifiedSinceDate.getTime()){

					            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED );
					            f = null;
					            return;
					        }
					    }
					    catch(Exception e){}

					}

                    response.setHeader("Last-Modified", httpDate.get().format(_lastModifiedDate));
                    response.setHeader("ETag", _eTag);

                /* if we are in ADMIN MODE, don't cache */
				}else{
				    GregorianCalendar expiration = new GregorianCalendar();
					expiration.add(java.util.Calendar.MONTH, -1);
					response.setHeader("Expires", httpDate.get().format(expiration.getTime()));
					response.setHeader("Cache-Control", "max-age=-1");
				}

			}


			ServletOutputStream out = null;
			FileChannel from = null;
			WritableByteChannel to = null;
			RandomAccessFile input = null;
			try {
				out = response.getOutputStream();
				from = new FileInputStream(f).getChannel();
				to = Channels.newChannel(out);
				//DOTCMS-5716
				//32 MB at a time	
				int maxTransferSize = (32 * 1024 * 1024) ;
				long size = from.size();
				long position = 0;
				
				boolean responseSent = false;
				byte[] data = Files.toByteArray(f);
	            //ServletOutputStream sos = response.getOutputStream();
				//extract range header
				String rangeHeader = request.getHeader("range");
				if(UtilMethods.isSet(rangeHeader)){
					 response.setHeader("Accept-Ranges", "bytes");
					// Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
					if (!rangeHeader.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
						response.setHeader("Content-Range", "bytes */" + data.length); // Required in 416.
						response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
						return;
					}
					//parse multiple range bytes
					ArrayList<SpeedyAssetServletUtil.ByteRange> ranges = SpeedyAssetServletUtil.parseRange(rangeHeader, data.length);
					if (ranges != null){
						SpeedyAssetServletUtil.ByteRange full = new SpeedyAssetServletUtil.ByteRange(0, f.length() - 1, f.length());
						if (ranges.isEmpty() || ranges.get(0).equals(full)) {
							// Return full file.
							input = new RandomAccessFile(f, "r");
							SpeedyAssetServletUtil.ByteRange r = full;
							response.setContentType(file.getMimeType());
							response.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);
							response.setHeader("Content-Length", String.valueOf(r.length));
							// Copy full range.
							SpeedyAssetServletUtil.copy(input, out, r.start, r.length);
						} else if (ranges.size() == 1){
							SpeedyAssetServletUtil.ByteRange range = ranges.get(0);
							input = new RandomAccessFile(f, "r");
							// Check if Range is syntactically valid. If not, then return 416.
							if (range.start > range.end) {
								response.setHeader("Content-Range", "bytes */" + data.length); // Required in 416.
								response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
								return;
							}
							response.setContentType(file.getMimeType());
							response.setHeader("Content-Range", "bytes " + range.start + "-" + range.end + "/" + range.total);
							response.setHeader("Content-Length", String.valueOf(range.length));
				            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.
							SpeedyAssetServletUtil.copy(input, out, range.start, range.length);
						}else{
							response.setContentType("multipart/byteranges; boundary=" + SpeedyAssetServletUtil.MULTIPART_BOUNDARY);
							response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
						    input = new RandomAccessFile(f, "r");
							for (SpeedyAssetServletUtil.ByteRange r : ranges) {
								if (r.start > r.end) {
									response.setHeader("Content-Range", "bytes */" + data.length); // Required in 416.
									response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
									return;
								}
								// Add multipart boundary and header fields for every range.
								out.println();
								out.println("--" + SpeedyAssetServletUtil.MULTIPART_BOUNDARY);
								out.println("Content-Type: " + file.getMimeType());
								out.println("Content-Range: bytes " + r.start + "-" + r.end + "/" + r.total);
								out.println();

								// Copy single part range of multi part range.
								SpeedyAssetServletUtil.copy(input, out, r.start, r.length);
							}
							// End with multipart boundary.
							out.println();
							out.println("--" + SpeedyAssetServletUtil.MULTIPART_BOUNDARY + "--");
						}
						responseSent = true;
					}	
				}
				if(!responseSent){
					response.setContentLength(data.length);
					response.setHeader("Content-Length", String.valueOf(f.length()));
					while (position < size) {
						position +=  from.transferTo(position, maxTransferSize, to);
					}
				}
			} catch (Exception e) {
				Logger.warn(this, e + " Error for = " + request.getRequestURI() + (request.getQueryString() != null?"?"+request.getQueryString():"") );
				Logger.debug(this, "Error serving asset = " + request.getRequestURI() + (request.getQueryString() != null?"?"+request.getQueryString():""), e);

			} finally {
				if(to != null)
					to.close();
				if(from != null)
					from.close();
				if(out != null)
					out.close();
				if(input !=null)
					input.close();
			}

		} catch (Exception e) {
			Logger.debug(this, "General Error occurred serving asset = " + request.getRequestURI() + (request.getQueryString() != null?"?"+request.getQueryString():""), e);
			//DOTCMS-1981
			//response.sendError(404, "Asset not Found");
		}
	}
}
