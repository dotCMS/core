package com.dotmarketing.servlets.image;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;

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

import org.apache.commons.lang.time.FastDateFormat;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.files.business.FileAPI;
import com.dotmarketing.portlets.files.model.File;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

/**
 * This servlet resize an image proportionally without placing that image into a
 * box background. The image generated is with the .png extension
 *
 * @author Oswaldo
 *
 */
public class ResizeImageServlet extends HttpServlet {
    private static final FileAPI fileAPI = APILocator.getFileAPI();
	/**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1L;
    private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();

    /* This is a thread safe date formatter */
    FastDateFormat df = FastDateFormat.getInstance(Constants.RFC2822_FORMAT, TimeZone.getTimeZone("GMT"), Locale.US);
    /**
     * resize an image proportionally without placing that image into a box
     * background. The image generated is with the .png extension
     */
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        long time = System.currentTimeMillis();

		User user = null;
		try {
			user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
		} catch (Exception nsue) {
			Logger.warn(this, "Exception trying to getUser: " + nsue.getMessage(), nsue);
		}

		boolean liveMode = ((request.getSession().getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) == null));
        String id = request.getParameter("id");
        if(!UtilMethods.isSet(id)){
        	id = request.getParameter("inode");
        }
        String url = (request.getParameter("path") != null) ? request.getParameter("path"): request.getParameter("url");

        Identifier identifier = null;
        File file = null;
        //Contentlet contentlet = null;


        String inode = null;
        Boolean fileAsContent = false;


        if(UtilMethods.isSet(url)) {
            //If path is the dotasset portlet
            if(url.startsWith("/dotAsset")) {
        		StringTokenizer _st = new StringTokenizer(url, "/");
        		String _fileName = null;
        		while(_st.hasMoreElements()){
        			_fileName = _st.nextToken();
        		}
                id = UtilMethods.getFileName(_fileName); // Sets the identifier
            } else {
        		//If it's a regular path
            	Host currentHost;
				try {
					currentHost = WebAPILocator.getHostWebAPI().getCurrentHost(request);
					String path = LiveCache.getPathFromCache(url, currentHost);
					String cinode=path.split("/")[3];
					id=APILocator.getIdentifierAPI().findFromInode(cinode).getId();
				} catch (Exception e) {
					Logger.error(ResizeImageServlet.class, e.getMessage(), e);
					throw new ServletException(e.getMessage(), e);
				}


            }
        }

        // if we have an id
        if( UtilMethods.isSet(id) ) {

        	try {
        		identifier = APILocator.getIdentifierAPI().loadFromCache(id);
	        	if(identifier != null && (InodeUtils.isSet(identifier.getInode()))) {
	        		//it's an identifier
	        		String path = null;
	        		try {
	        			path = (liveMode) ? LiveCache.getPathFromCache(identifier.getURI(), identifier.getHostId())
							: WorkingCache.getPathFromCache(identifier.getURI(), identifier.getHostId());
	        		} catch(DotContentletStateException e) {
	        			path = WorkingCache.getPathFromCache(identifier.getURI(), identifier.getHostId());
	        		}

	        		if(path==null) {
            			Logger.debug(this.getClass(), "Can't find path with URI " + identifier.getURI());
            			return;
            		}

            		inode = UtilMethods.getFileName(path);
	        	} else {
	        		//it might be an inode
	        		try {
	        			file = APILocator.getFileAPI().find(id, user, true);
	        		} catch(Exception e) {
	        			Contentlet c = APILocator.getContentletAPI().find(id, user, true);
	        			fileAsContent = UtilMethods.isSet(c);
	        		}
	                if(file != null && (InodeUtils.isSet(file.getInode()))) {
	                	inode = file.getInode();
	                	identifier = APILocator.getIdentifierAPI().find(file.getIdentifier());
	                } else{
	                	//Finally it's not a file and we did the first round trip to database
	                	//then we do the second round trip to database to find the identifier,
	                	//but after this the next hit will find it cached.
	                	identifier = APILocator.getIdentifierAPI().find(id);
	                	if(identifier != null && (InodeUtils.isSet(identifier.getInode())) ) {
	        				String path = LiveCache.getPathFromCache(identifier.getURI(), identifier.getHostId());
	                		inode = UtilMethods.getFileName(path);
	                	}
	                }
	        	}
			} catch (Exception e) {
				System.out.println(e);
			}

        }


        if (!InodeUtils.isSet(inode) && !fileAsContent) {
            response.sendError(404);
            return;
        }

        try {
			//Checking permissions
        	/**
        	 * Build a fake proxy file object so we
        	 * can get inheritable permissions on it
        	 * without having to hit cache or db
        	 */

            file = null;
            if(identifier!=null && UtilMethods.isSet(identifier.getInode())){
            	file = new File();
            	file.setIdentifier(identifier.getInode());
            }
            else{
            	try {
            		file = APILocator.getFileAPI().find(inode, user, true);
            		identifier.setId(file.getIdentifier());
				} catch (Exception e) {
					Logger.error(ResizeImageServlet.class,e.getMessage(),e);
				}

            }

            if (file!=null && !permissionAPI.doesUserHavePermission(file, PERMISSION_READ, user, true)) {
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

        String maxh = request.getParameter("maxh");
        String maxw = request.getParameter("maxw");

        int height = Config.getIntProperty("DEFAULT_HEIGHT");
        int width = Config.getIntProperty("DEFAULT_WIDTH");
        int imageWidth = 0;
        int imageHeight = 0;
        double imageRatio = 0;

        try {

            if (fileAsContent || inode != null && inode.length() > 0 && InodeUtils.isSet(inode)) {

            	boolean isSet = false;
            	boolean isCont = false;
            	String fileName = "";
            	String inodeOrId = "";

            	String contAssetPath = "";
            	int imgWidth = 0;
            	int imgLength = 0;
            	String ext = "";
            	if(fileAsContent || id!=null && InodeUtils.isSet(identifier.getId()) && identifier.getAssetType().equals("contentlet")){
            		Contentlet cont = null;

            		if(fileAsContent) {
            			cont = APILocator.getContentletAPI().find(id, user, true);
            			inode = id;
            		} else {
	            		try {
	            			cont = APILocator.getContentletAPI().findContentletByIdentifier(id, true, APILocator.getLanguageAPI().getDefaultLanguage().getId(), user, true);
	            		}
	            		catch(DotContentletStateException e) {
	            			cont = APILocator.getContentletAPI().findContentletByIdentifier(id, false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), user, true);
	            		}

	            		if(cont==null) {
	            			Logger.debug(this.getClass(), "Can't find content with id " + id);
	            			return;
	            		}
            		}

            		FileAsset fa = APILocator.getFileAssetAPI().fromContentlet(cont);
            		isSet = InodeUtils.isSet(cont.getInode());
            		fileName = fa.getFileName();
            		isCont = true;
            		inodeOrId = cont.getInode();
        			contAssetPath = APILocator.getFileAssetAPI().getRealAssetPath(cont.getInode());
        			java.util.Map<String, Object> keyValueMap = null;
        			String JSONValue = cont.getStringProperty(FileAssetAPI.META_DATA_FIELD);
        			//Convert JSON to Table Display {key, value, order}
        			if(UtilMethods.isSet(JSONValue)){
        				keyValueMap =  com.dotmarketing.portlets.structure.model.KeyValueFieldUtil.JSONValueToHashMap(JSONValue);
        				if(UtilMethods.isSet((String)keyValueMap.get("width"))){
        					imgWidth = Integer.parseInt((String)keyValueMap.get("width"));
        				}
        				if(UtilMethods.isSet((String)keyValueMap.get("height"))){
        					imgLength = Integer.parseInt((String)keyValueMap.get("height"));
        				}
        			}
        			if(!contAssetPath.endsWith(java.io.File.separator)){
        				contAssetPath += java.io.File.separator;
        			}
        			ext = fa.getExtension();
            	}else if(InodeUtils.isSet(identifier.getId())){
                    file = fileAPI.find(inode,user,true);
                    isSet =InodeUtils.isSet(file.getInode());
                    fileName = file.getFileName();
                    inodeOrId = inode;
                    imgWidth = file.getWidth();
                    imgLength = file.getHeight();
                    ext = file.getExtension();
            	}


                if (isSet) {
                    // Set the size
                    if (UtilMethods.isSet(h) || UtilMethods.isSet(w)) {

                        if (UtilMethods.isInt(h) && UtilMethods.isInt(w)) {

                            height = (Integer.parseInt(h) > 0 ? Integer.parseInt(h) : Config.getIntProperty("DEFAULT_HEIGHT"));
                            width = (Integer.parseInt(w) > 0 ? Integer.parseInt(w) : Config.getIntProperty("DEFAULT_WIDTH"));

                        } else if (UtilMethods.isInt(h)) {

                            height = (Integer.parseInt(h) > 0 ? Integer.parseInt(h) : Config.getIntProperty("DEFAULT_HEIGHT"));

                            // determine thumbnail size from WIDTH and HEIGHT
                            imageWidth = imgWidth == 0?width:imgWidth;
                            imageHeight = imgLength == 0?height:imgLength;

                            // JIRA: http://jira.dotmarketing.net/browse/DOTCMS-1340
                            width = (int) Math.ceil((imageWidth * height)/imageHeight);

                        } else if (UtilMethods.isInt(w)) {

                            width = (Integer.parseInt(w) > 0 ? Integer.parseInt(w) : Config.getIntProperty("DEFAULT_WIDTH"));

                            // determine thumbnail size from WIDTH and HEIGHT
                            imageWidth = imgWidth == 0?width:imgWidth;
                            imageHeight = imgLength == 0?height:imgLength;

                            // JIRA: http://jira.dotmarketing.net/browse/DOTCMS-1340
                            height = (int) Math.ceil((imageHeight * width)/imageWidth);

                        }

                    } else if (UtilMethods.isSet(maxh) && UtilMethods.isSet(maxw)) {

                        int maxhint = Integer.parseInt(maxh);
                        int maxwint = Integer.parseInt(maxw);
                        int decrease = 0;

                        imageWidth = imgWidth == 0?width:imgWidth;
                        imageHeight = imgLength == 0?height:imgLength;

                        imageRatio = (double) imageWidth / (double) imageHeight;
                        boolean imageFinishied = true;

                        width = maxwint;
                        height = maxhint;

                        do {

                            if (width <= height) {

                                width = width - decrease;
                                height = (int) Math.ceil((width / imageRatio));

                            } else {

                                height = height - decrease;
                                width = (int) (height * imageRatio);
                            }

                            decrease = 1;

                            if (height <= maxhint && width <= maxwint) {
                                imageFinishied = false;
                            }

                        } while (imageFinishied);

                    }

                    // gets file extension
                    String suffix = ext;

                    // gets the real path to the assets directory
                    String filePath = fileAPI.getRealAssetsRootPath();

                    // creates the path where to save the working file based on
                    // the inode
                    String workingFileInodePath = String.valueOf(inode);
                    if(fileAsContent || id != null && identifier.getAssetType().equals("contentlet"))//DOTCMS-6531
                    	workingFileInodePath = String.valueOf(inodeOrId);
                    if (workingFileInodePath.length() == 1) {
                        workingFileInodePath = workingFileInodePath + "0";
                    }

                    // creates the path with inode{1} + inode{2}
                    workingFileInodePath = workingFileInodePath.substring(0, 1) + java.io.File.separator + workingFileInodePath.substring(1, 2);

                    String thumbExtension = WebKeys.GENERATED_FILE  +inodeOrId +  height  + "_w_" + width ;
                    String thumbnailFilePath = "";
                    if(!isCont){
                		// creates the path with id{1} + id{2}

                        thumbnailFilePath = filePath + java.io.File.separator + workingFileInodePath + java.io.File.separator
                        		+thumbExtension + "." + suffix;
                    }else if(fileAsContent){

                        thumbnailFilePath = filePath + java.io.File.separator + workingFileInodePath + java.io.File.separator + inodeOrId  + java.io.File.separator + "fileAsset"  + java.io.File.separator
                                +fileName;
                    } else {
                    	 thumbnailFilePath = filePath + java.io.File.separator + workingFileInodePath + java.io.File.separator + inodeOrId  + java.io.File.separator + "fileAsset"  + java.io.File.separator
                                 +thumbExtension + "." + suffix;
                    }



                    java.io.File thumbFile = null;
                    synchronized (inode.intern()) {

                        thumbFile = new java.io.File(thumbnailFilePath);
                        Logger.debug(this, "Checking resized image for " + thumbnailFilePath);
                        if (!thumbFile.exists() || (request.getParameter("nocache") != null)) {
                            Logger.debug(this, "File doesn't exists creating it");
                            String realPathAux= "";
	                    	if(isCont){
	                    		realPathAux = contAssetPath;
	                    	}else{
	                    		realPathAux = filePath + java.io.File.separator + workingFileInodePath+ java.io.File.separator;
	                    	}
                            com.dotmarketing.util.ImageResizeUtils.resizeImage(realPathAux, inode, suffix, thumbExtension, width, height);
                            thumbFile = new java.io.File(thumbnailFilePath);
                            Logger.debug(this, "File created thumbFile.exists() = " + thumbFile.exists());
                        }

                    }
                    if(thumbFile ==null){
                    	response.sendError(404);
                    	return;
                    }
                    Logger.debug(this, "Streaming the image");



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
					    	java.text.SimpleDateFormat httpDate = new java.text.SimpleDateFormat(Constants.RFC2822_FORMAT, Locale.US);
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
                    response.setContentType(this.getServletContext().getMimeType(thumbnailFilePath));
                    Logger.debug(this, "Image mime type " + this.getServletContext().getMimeType(thumbnailFilePath));

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
                    Logger.debug(this.getClass(), "time to serve ResizeImage thumbnail: " + (System.currentTimeMillis() - time) + "ms");
                } else {
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
            Logger.error(ResizeImageServlet.class, "Error creating thumbnail from ResizeImage servlet: " + e.getMessage(),e);
        }
        return;
    }
}
