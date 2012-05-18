package com.dotmarketing.servlets.image;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.time.FastDateFormat;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public class ThumbnailImage extends HttpServlet {

    private static final FileAPI fileAPI = APILocator.getFileAPI();
	private static final long serialVersionUID = 1L;
    private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();

    private final String RFC2822_FORMAT = Constants.RFC2822_FORMAT;
    FastDateFormat df = FastDateFormat.getInstance(RFC2822_FORMAT, TimeZone.getTimeZone("GMT"), Locale.US);
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        long time = System.currentTimeMillis();

        HttpSession session = request.getSession(false);
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

        String inode = request.getParameter("inode");
        String identifier = request.getParameter("id");
        Identifier ident = null;

        if( InodeUtils.isSet(identifier) ) {
        	// If identifier was given, get the live inode from it
			try{
				ident = APILocator.getIdentifierAPI().find(identifier);
				String path = LiveCache.getPathFromCache(ident.getURI(), ident.getHostId());
        		inode = UtilMethods.getFileName(path);
			}catch(Exception ex){
				Logger.debug(ResizeImageServlet.class, "Identifier not found going to try as a File Asset");
				inode = identifier;
			}
        } else if( InodeUtils.isSet(inode) ) {
        	try {
        		ident = APILocator.getIdentifierAPI().findFromInode(inode);
        	}
        	catch(Exception ex) {
        		Logger.error(this, ex.getMessage() ,ex);
        		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        		return;
        	}
        } else {
            String url = request.getParameter("url");
            if(url == null || !UtilMethods.isSet(url))
                url = request.getParameter("path");

            //If path is the dotasset portlet
            if(url != null && url.startsWith("/dotAsset")) {

        		StringTokenizer _st = new StringTokenizer(url, "/");
        		String _fileName = null;
        		while(_st.hasMoreElements()){
        			_fileName = _st.nextToken();
        		}
                inode = UtilMethods.getFileName(_fileName); // Sets the identifier

    			try{
    				ident = APILocator.getIdentifierAPI().find(inode);
    				String path = LiveCache.getPathFromCache(ident.getURI(), ident.getHostId());
            		inode = UtilMethods.getFileName(path);
    			}catch(Exception ex){
    				Logger.debug(ResizeImageServlet.class, "Identifier not found going to try as a File Asset");
    			}

            } else if(url != null){
        		//If it's a regular path
            	Host currentHost;
				try {
					currentHost = WebAPILocator.getHostWebAPI().getCurrentHost(request);
				} catch (PortalException e) {
					Logger.error(ResizeImageServlet.class, e.getMessage(), e);
					throw new ServletException(e.getMessage(), e);
				} catch (SystemException e) {
					Logger.error(ResizeImageServlet.class, e.getMessage(), e);
					throw new ServletException(e.getMessage(), e);
				} catch (DotDataException e) {
					Logger.error(ResizeImageServlet.class, e.getMessage(), e);
					throw new ServletException(e.getMessage(), e);
				} catch (DotSecurityException e) {
					Logger.error(ResizeImageServlet.class, e.getMessage(), e);
					throw new ServletException(e.getMessage(), e);
				}
            	String path = "";
				try {
					path = LiveCache.getPathFromCache(url, currentHost);
				} catch (Exception e) {
					Logger.error(this,e.getMessage(), e);
				}
            	inode = UtilMethods.getFileName(path);
            }

        }

        try {
            if (!InodeUtils.isSet(inode)) {
                response.sendError(404);

                try {
					HibernateUtil.closeSession();
				} catch (DotHibernateException e) {
					Logger.error(this.getClass(), e.getMessage(), e);
				}
        		DbConnectionFactory.closeConnection();
                return;
            }
        } catch (NumberFormatException e) {
            Logger.error(this, "service: invalid inode (" + inode + ") or identifier("+ identifier +") given to the service.");
            response.sendError(404);

            try {
				HibernateUtil.closeSession();
			} catch (DotHibernateException e1) {
				Logger.error(this.getClass(),e.getMessage(),e);
			}
    		DbConnectionFactory.closeConnection();
            return;
        }

        try {
			//Checking permissions
        	/**
        	 * Build a fake proxy file object so we
        	 * can get inheritable permissions on it
        	 * without having to hit cache or db
        	 */
            com.dotmarketing.portlets.files.model.File fProxy = new com.dotmarketing.portlets.files.model.File();
            if(ident != null && UtilMethods.isSet(ident.getInode())){//DOTCMS-4969
            	fProxy.setIdentifier(ident.getInode());
            	if(InodeUtils.isSet(inode))//DOTCMS-5265
           		 fProxy.setInode(inode);
            }else{
            	fProxy.setInode(inode);
            	fProxy.setIdentifier(inode);//DOTCMS-5669
            }

            if (!permissionAPI.doesUserHavePermission(fProxy, PERMISSION_READ, user)) {
            	if(user == null)
            		//Sending user to unauthorized the might send him to login
            		response.sendError(401, "The requested file is unauthorized");
            	else
            		//sending the user to forbidden
            		response.sendError(403, "The requested file is forbidden");
           		return;
            }
		} catch (DotDataException e1) {
			Logger.error(this,e1.getMessage());
			response.sendError(500,e1.getMessage());
			return;
		}



        String h = request.getParameter("h");
        String w = request.getParameter("w");
        String r = request.getParameter("r");
        String g = request.getParameter("g");
        String b = request.getParameter("b");

        int rInt = -1;
        int gInt = -1;
        int bInt = -1;

        try {
            rInt = Integer.parseInt(r);
            gInt = Integer.parseInt(g);
            bInt = Integer.parseInt(b);
        } catch (Exception e) {
            Logger.debug(ThumbnailImage.class, "Error with RGB number");
        }

        if ((rInt < 0) || (255 < rInt))
        	rInt = Config.getIntProperty("DEFAULT_BG_R_COLOR");
        if ((gInt < 0) || (255 < gInt))
        	gInt = Config.getIntProperty("DEFAULT_BG_G_COLOR");
        if ((bInt < 0) || (255 < bInt))
        	bInt = Config.getIntProperty("DEFAULT_BG_B_COLOR");

        String fileBgColorName = rInt + "_" + gInt + "_" + bInt;

        try {

            int height = (UtilMethods.isInt(h) ? Integer.parseInt(h) : Config.getIntProperty("DEFAULT_HEIGHT"));
            int width = (UtilMethods.isInt(w) ? Integer.parseInt(w) : Config.getIntProperty("DEFAULT_WIDTH"));

            if( inode != null && inode.length() > 0 && InodeUtils.isSet(inode) )
            {

            	boolean isSet = false;
            	boolean isCont = false;
            	String fileName = "";
            	String inodeOrId = "";
            	Identifier id=null;
            	if(UtilMethods.isSet(identifier))
            	    id = APILocator.getIdentifierAPI().find(identifier);
            	String contAssetPath = "";
            	if(id!=null && InodeUtils.isSet(id.getId()) && id.getAssetType().equals("contentlet")){
            		Contentlet cont = APILocator.getContentletAPI().findContentletByIdentifier(identifier, false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), user, false);
            		FileAsset fa = APILocator.getFileAssetAPI().fromContentlet(cont);
            		isSet = InodeUtils.isSet(cont.getInode());
            		fileName = fa.getFileName();
            		isCont = true;
            		inodeOrId = cont.getInode();
        			contAssetPath = APILocator.getFileAssetAPI().getRealAssetPath(cont.getInode());
        			if(!contAssetPath.endsWith(java.io.File.separator)){
        				contAssetPath += java.io.File.separator;
        			}
            	}else if(id!=null && InodeUtils.isSet(id.getId())){
                    File file = fileAPI.find(inode,user,true);
                    isSet =InodeUtils.isSet(file.getInode());
                    fileName = file.getFileName();
                    inodeOrId = inode;
            	}


                if (isSet) {
                	// gets file extension
                	String suffix = UtilMethods.getFileExtension(fileName);

                	// gets the real path to the assets directory
                	String filePath = fileAPI.getRealAssetsRootPath();

                	// creates the path where to save the working file based on
                	// the inode
                	String workingFileInodePath = "";
                	String thumbnailFilePath = "";
                    String generatedKey = WebKeys.GENERATED_FILE + height + "_" + width + "_" + fileBgColorName;
                    workingFileInodePath = String.valueOf(inodeOrId);
            		if (workingFileInodePath.length() == 1) {
            			workingFileInodePath = workingFileInodePath + "0";
            		}
                	if(!isCont){
                		// creates the path with id{1} + id{2}
                		workingFileInodePath = workingFileInodePath.substring(0, 1) + java.io.File.separator + workingFileInodePath.substring(1, 2);
                		thumbnailFilePath = filePath + java.io.File.separator + workingFileInodePath + java.io.File.separator
                                + generatedKey + "-" +  inode+ "." + suffix;
                	}else{
                    	workingFileInodePath = workingFileInodePath.substring(0, 1) + java.io.File.separator + workingFileInodePath.substring(1, 2);
                    	thumbnailFilePath = filePath + java.io.File.separator + workingFileInodePath + java.io.File.separator + inodeOrId  + java.io.File.separator + "fileAsset"  + java.io.File.separator
                                + generatedKey + "-" +  inode+ "." + suffix;
                	}


                	String realPathAux= "";
                	if(isCont){
                		realPathAux = contAssetPath;
                	}else{
                		realPathAux = filePath + java.io.File.separator + workingFileInodePath + java.io.File.separator;
                	}

                	if(!ident.getParentPath().contains("template")) {
                		thumbnailFilePath = realPathAux + fileName;
                	}
                    java.io.File thumbFile = new java.io.File(thumbnailFilePath);

                    if(!thumbFile.exists()) {
                    	thumbnailFilePath = realPathAux + inode+ "." + suffix;
                    	thumbFile = new java.io.File(thumbnailFilePath);
                    }
                    Color bgColor = new Color(rInt, gInt, bInt);

                    synchronized (inode.intern()) {

	                    if (!thumbFile.exists() || (request.getParameter("nocache") != null)) {
	                        com.dotmarketing.util.ImageResizeUtils.generateThumbnail(realPathAux, fileName, suffix, generatedKey, width, height, bgColor);
	                        thumbFile = new java.io.File(thumbnailFilePath);
	                    }

                    }


                    //  -------- HTTP HEADER/ MODIFIED SINCE CODE -----------//

                    long _lastModified = thumbFile.lastModified();
                    long _fileLength = thumbFile.length();
					String _eTag = "dot:" + inode + "-" + _lastModified/1000 + "-" + _fileLength;
                    String ifModifiedSince = request.getHeader("If-Modified-Since");
                    String ifNoneMatch = request.getHeader("If-None-Match");

                    /*
                     * If the etag matches then the file is the same
                     *
                    */

                    if(ifNoneMatch != null){
                        if(_eTag.equals(ifNoneMatch) || ifNoneMatch.equals("*")){
                            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED );
                            thumbFile = null;
                            return;
                        }
                    }
                    /* Using the If-Modified-Since Header */
                    if(ifModifiedSince != null){
					    try{
					    	java.text.SimpleDateFormat httpDate = new java.text.SimpleDateFormat(RFC2822_FORMAT, Locale.US);
					    	httpDate.setTimeZone(TimeZone.getDefault());
					        Date ifModifiedSinceDate = httpDate.parse(ifModifiedSince);

					        if(_lastModified <= ifModifiedSinceDate.getTime()){

					            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED );
					            thumbFile = null;
					            return;
					        }
					    }
					    catch(Exception e){}
					}

                    response.setHeader("Content-Length", String.valueOf(_fileLength));
                    response.setHeader("Last-Modified", df.format(_lastModified));
                    response.setHeader("ETag", "\"" + _eTag +"\"");
                    // Set the expiration time
                    GregorianCalendar expiration = new GregorianCalendar();
                    expiration.add(java.util.Calendar.YEAR, 1);
                    response.setHeader("Expires", df.format(expiration.getTime()));
                    response.setHeader("Cache-Control", "max-age=31104000");
                    // END Set the expiration time

                    //  -------- /HTTP HEADER/ MODIFIED SINCE CODE -----------//




                    // set the content type and get the output stream
                    response.setContentType("image/png");

                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(thumbFile));
                    OutputStream os = response.getOutputStream();
                    byte[] buf = new byte[4096];
                    int i = 0;

                    while ((i = bis.read(buf)) != -1) {
                        os.write(buf, 0, i);
                    }

                    os.flush();
                    os.close();
                    bis.close();
                    Logger.debug(this.getClass(), "time to serve thumbnail: " + (System.currentTimeMillis() - time) + "ms");
                } else {
                    // set the content type and get the output stream
                	 // set the content type and get the output stream
                    response.setContentType("image/jpeg");
                    // Construct the image
                    String path = fileAPI.getRealAssetsRootPath() + java.io.File.separator + ".." + java.io.File.separator + "html" + java.io.File.separator + "js" + java.io.File.separator +
                    		"editor" + java.io.File.separator + "images" + java.io.File.separator + "spacer.gif";
                    java.io.File f = new java.io.File(path);

                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
                    OutputStream os = response.getOutputStream();
                    byte[] buf = new byte[4096];
                    int i = 0;

                    while ((i = bis.read(buf)) != -1) {
                        os.write(buf, 0, i);
                    }

                    os.flush();
                    os.close();
                    bis.close();
                }
            }
        } catch (Exception e) {
            Logger.error(ThumbnailImage.class, "Error creating thumbnail from servlet: " + e.getMessage(),e);
        }

        try {
			HibernateUtil.closeSession();
		} catch (DotHibernateException e) {
			Logger.error(this.getClass(),e.getMessage(),e);
		}
		DbConnectionFactory.closeConnection();
        return;
    }
}
