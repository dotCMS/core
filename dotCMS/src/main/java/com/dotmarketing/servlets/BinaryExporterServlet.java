package com.dotmarketing.servlets;

import static com.dotmarketing.image.focalpoint.FocalPointAPIImpl.TMP;
import static com.liferay.util.HttpHeaders.CACHE_CONTROL;
import static com.liferay.util.HttpHeaders.EXPIRES;

import com.dotcms.storage.FileMetadataAPI;
import com.dotcms.storage.model.Metadata;
import com.dotcms.variant.business.web.VariantWebAPI.RenderContext;
import com.dotmarketing.util.UUIDUtil;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.util.PortalUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotcms.util.CloseUtils;
import com.dotcms.util.DownloadUtil;
import com.dotcms.util.SecurityUtils;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyException;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporter;
import com.dotmarketing.portlets.contentlet.business.BinaryContentExporterException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ResourceLink;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.UserAgent;

/**
 * This servlet allows you invoke content exporters over binary fields. With the following URL syntax you are able to
 * invoke an specific content exporter on a piece of content:
 *
 * <pre>
 * /contentAsset/{exporter path}/{content identifier}/{binary field - optional}?{byInode=true&}{exporter specific parameters}
 * </pre>
 *
 * <p>The {@code {exporter path}} is the exporter specific path set by the exporter class. I.E. every exporter must implement
 * an interface method call getPathMapping that defines the path of what the exporter is going to be bound. E.G. The
 * {@code com.dotmarketing.portlets.contentlet.business.exporter.ImageResizeFieldExporter} binds to the
 * {@code resize-image} path so it can be invoked as {@code /contentAsset/resize-image/...}<p/>
 *
 * <p>The {@code {content identifier}} is the identifier of the piece of content that wants to be retrieved. Special case
 * occurs when the url parameter {@code byInode=true} is set then the content specific inode must be passed here.<p/>
 *
 * <p>The {@code {binary field - optional}} is the binary field velocity name (refer to the structure manager to find out
 * which is your field velocity name). This url part could be obeyed for certain exporters that operate over the entire
 * content instead of an specific field like with an XML content exporter for example.</p>
 *
 * <p>The {@code {exporter specific parameters}} is for exporter specific parameters, refer to the exporter documentation.
 * Exporters like the thubmnail generator takes parameters like the width or height of the thumbnail to be generated.</p>
 *
 * @author David Torres 2010
 *
 */
public class BinaryExporterServlet extends HttpServlet {

	private static final FileAssetAPI fileAssetAPI = APILocator.getFileAssetAPI();
	private static final ShortyIdAPI shortyIdApi = APILocator.getShortyAPI();
	private final ContentletAPI contentAPI = APILocator.getContentletAPI();
	private final TempFileAPI tempFileAPI = APILocator.getTempFileAPI();
	private final FileMetadataAPI fileMetadataAPI = APILocator.getFileMetadataAPI();

	Map<String, BinaryContentExporter> exportersByPathMapping;

	private long defaultLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();

	
	private final boolean ASSETS_USE_WEAK_ETAGS = Config.getBooleanProperty("ASSETS_USE_WEAK_ETAGS", true);
	
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void init() throws ServletException {
		super.init();

		exportersByPathMapping = new HashMap<>();

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
		ImageIO.scanForPlugins();
        final IIORegistry registry = IIORegistry.getDefaultInstance();
        registry.registerServiceProvider(registry.getServiceProviderByClass(com.twelvemonkeys.imageio.plugins.svg.SVGImageReaderSpi.class));
	}

	private static final long serialVersionUID = 1L;

	/**
	 * Processes incoming requests for binary files. Requests issued to this servlet might come directly
	 * to it or through another servlet, such as the {@code SpeedyAssetServlet} class which is accessed using
	 * the legacy {@code /dotAsset/} path to display files.
	 *
	 * @param req  The {@link HttpServletRequest} object.
	 * @param resp The {@link HttpServletResponse} object.
	 *
	 * @throws ServletException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String servletPath = req.getServletPath();
		String uri = req.getRequestURI().substring(servletPath.length());
		String[] uriPieces = uri.split("/");
		String exporterPath = uriPieces[1];
		String uuid = uriPieces[2];

		Map<String, String[]> params = new LinkedHashMap<>();
		params.putAll(req.getParameterMap());
		// only set uri params if they are not set in the query string - meaning
		// the query string will override the uri params.
		Map<String, String[]> uriParams = getURIParams(req);
		for(String x: uriParams.keySet()){
			if(!params.containsKey(x)){
				params.put(x, uriParams.get(x));
			}
		}

		boolean byInode = params.containsKey("byInode");

		ServletOutputStream out = null;
		RandomAccessFile input = null;
		InputStream is = null;
		// Default to a no-shortyId value
		try {
			ShortyId shorty = shortyIdApi.noShorty(uuid);
			try {
				// Try to get the ShortyId from a valid expected UUID value
				final Optional<ShortyId> shortOpt = shortyIdApi.getShorty(uuid);
				if (shortOpt.isPresent()) {
					shorty = shortOpt.get();
					// Double-check if the passed-in value is an Inode or not
					byInode = byInode || shorty.type == ShortType.INODE;
				} else {
					// If the value is not a valid UUID, it probably is a legacy Identifier/Inode
					shortyIdApi.validShorty(uuid);
				}
			} catch (final ShortyException e) {
				// The Inode/Identifier length and/or format is not valid for a UUID, probably belonged
				// to a Legacy File which is just a consecutive number. This old format needs to be supported
				final ShortType shortType = (byInode ? ShortType.INODE : ShortType.IDENTIFIER);
				shorty = new ShortyId(uuid, uuid, shortType, ShortType.CONTENTLET);
			}
			uuid = shorty.longId;
			final boolean isContent= (shorty.subType == ShortType.CONTENTLET);
			String assetInode = null;
			String assetIdentifier = null;
			if (byInode){
				assetInode = uuid;
			}
			else{
				assetIdentifier = uuid;
			}

			String fieldVarName = uriPieces.length > 3?uriPieces[3]:"fileAsset";
			BinaryContentExporter exporter = exportersByPathMapping.get(exporterPath);
			if(exporter == null) {
				Logger.warn(this, "No exporter for path " + exporterPath + " is registered. Requested url = " + uri);
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			UserWebAPI userWebAPI = WebAPILocator.getUserWebAPI();
			BinaryContentExporter.BinaryContentExporterData data = null;
			File inputFile = null;
			HttpSession session = req.getSession(false);
			List<String> tempBinaryImageInodes = null;
			if ( session != null && session.getAttribute( Contentlet.TEMP_BINARY_IMAGE_INODES_LIST ) != null ) {
				tempBinaryImageInodes = (List<String>) session.getAttribute( Contentlet.TEMP_BINARY_IMAGE_INODES_LIST );
			} else {
				tempBinaryImageInodes = new ArrayList<>();
			}

			boolean isTempBinaryImage = tempBinaryImageInodes.contains(assetInode);


			final User user = PortalUtil.getUser(req);

			final PageMode mode = PageMode.get(req);

			String downloadName = "file_asset";
			final long lang = WebAPILocator.getLanguageWebAPI().getLanguage(req).getId();


			if (isContent){
				Contentlet content = null;
				if(byInode) {

					if (isTempBinaryImage)
						content = contentAPI.find(assetInode, APILocator.getUserAPI().getSystemUser(), mode.respectAnonPerms);
					else {
						try {
							content = contentAPI.find(assetInode, user, mode.respectAnonPerms);
						} catch(DotSecurityException e) {
							if (!mode.respectAnonPerms) {
								content = getContentletLiveVersion(assetInode, user, lang);
							} else {
								throw e;
							}
						}
					}
				} else {
				    boolean live=mode.showLive;

				    if (req.getSession(false) != null && req.getSession().getAttribute("tm_date")!=null) {
				        live=true;
				        Identifier ident=APILocator.getIdentifierAPI().find(assetIdentifier);
				        if(UtilMethods.isSet(ident.getSysPublishDate()) || UtilMethods.isSet(ident.getSysExpireDate())) {
				            Date fdate=new Date(Long.parseLong((String)req.getSession().getAttribute("tm_date")));
				            if(UtilMethods.isSet(ident.getSysPublishDate()) && ident.getSysPublishDate().before(fdate))
				                live=false;
				            if(UtilMethods.isSet(ident.getSysExpireDate()) && ident.getSysExpireDate().before(fdate))
				                return; // expired!
				        }
				    }

					//If the DEFAULT_FILE_TO_DEFAULT_LANGUAGE is true and the default language is NOT equals to the language we have in request/session...
					if ( Config.getBooleanProperty("DEFAULT_FILE_TO_DEFAULT_LANGUAGE", false)
							&& defaultLang != lang ) {

						ContentletAPI contentletAPI = APILocator.getContentletAPI();

						//Build the lucene query with the identifier and both languages, the default and one in session to see what we can find
						StringBuilder query = new StringBuilder();
						query.append("+(languageId:").append(defaultLang).append(" languageId:").append(lang).append(") ");
						query.append("+identifier:").append(assetIdentifier).append(" +deleted:false ");
						if ( live ) {
							query.append("+live:true ");
						} else {
							query.append("+working:true ");
						}

						List<Contentlet> foundContentlets = contentletAPI.search(query.toString(), 2, -1, null, user, mode.respectAnonPerms);
						if ( foundContentlets != null && !foundContentlets.isEmpty() ) {
							//Prefer the contentlet with the session language
							content = foundContentlets.get(0);
							if ( content.getLanguageId() != lang && foundContentlets.size() == 2 ) {
								content = foundContentlets.get(1);
							}
						} else {
							Logger.error(this, "Content with Identifier [" + assetIdentifier + "] not found.");
							resp.sendError(HttpServletResponse.SC_NOT_FOUND);
							return;
						}

					}
					else {
						/*
						If the property DEFAULT_FILE_TO_DEFAULT_LANGUAGE is false OR the language in request/session
						is equals to the default language, continue with the default behavior.
						 */
						content = getContentletByIdentifier(mode, assetIdentifier, lang, user);

					}
					assetInode = content.getInode();
				}

                // If the user is NOT logged in the backend then we cannot show content that is NOT live.
                // Temporal files should be allowed any time
                if(!isTempBinaryImage && !WebAPILocator.getUserWebAPI().isLoggedToBackend(req)) {
                    if (!APILocator.getVersionableAPI().hasLiveVersion(content) && mode.respectAnonPerms) {
						Logger.debug(this, "Content '" + fieldVarName + "' with inode: " + content.getInode() + " is not published");
						resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                }

				//Find the contentlet content type
				ContentType type = APILocator.getContentTypeAPI(APILocator.systemUser()).find((content.getContentTypeId()));
				//And the file asset field
				com.dotcms.contenttype.model.field.Field field;

				try {
					field = APILocator.getContentTypeFieldAPI().byContentTypeAndVar(type, fieldVarName);
				} catch (NotFoundInDbException e) {
					Logger.debug(this,"Field " + fieldVarName + " does not exist within Content Type: " + type.variable());
					resp.sendError(HttpServletResponse.SC_NOT_FOUND);
					return;
				}

				if(isTempBinaryImage)
					inputFile = contentAPI.getBinaryFile(content.getInode(), field.variable(), APILocator.getUserAPI().getSystemUser());
				else
					inputFile = contentAPI.getBinaryFile(content.getInode(), field.variable(), user);
				if(inputFile == null){
					Logger.debug(this,"Binary file '" + fieldVarName + "' does not exist for inode " + content.getInode());
					resp.sendError(HttpServletResponse.SC_NOT_FOUND);
					return;
				}
				downloadName = inputFile.getName();

				//if we're looking at a front end user then we should always restrict (vtl, vm) files, .
				if(ResourceLink.isDownloadRestricted(downloadName, content, user, req)){
					resp.sendError(HttpServletResponse.SC_NOT_FOUND);
					DbConnectionFactory.closeSilently();
					return;
				}

			}
			
			// if this is file uploaded by the tempResource
      if (shorty.type == ShortType.TEMP_FILE) {
        if (!Config.getBooleanProperty(TempFileAPI.TEMP_RESOURCE_ALLOW_NO_REFERER, false) && !new SecurityUtils().validateReferer(req)) {
          resp.sendError(HttpServletResponse.SC_NOT_FOUND);
          DbConnectionFactory.closeSilently();
          return;
        }
        inputFile = tempFileAPI.getTempFile(req, shorty.longId).get().file;
      }
      

      if(params.get("quality_q")!=null) {
      	final UserAgent userAgent = new UserAgent(req.getHeader("user-agent"));
        if(isBrowserSafariAndVersionBelow14(userAgent)){
          params.put("jpeg_q", params.get("quality_q"));
          params.put("jpeg_p",  new String[] {"1"});
        }else {
          params.put("webp_q", params.get("quality_q"));
        }
      }


			
			//DOTCMS-5674
			if(UtilMethods.isSet(fieldVarName)){
				params.put("fieldVarName", new String[]{fieldVarName});
				params.put("assetInodeOrIdentifier", new String[]{uuid});
			}
			data = exporter.exportContent(inputFile, params);

      // THIS IS WHERE THE MAGIC HAPPENS
      // this creates a temp resource using the altered file
      if (req.getParameter(WebKeys.IMAGE_TOOL_SAVE_FILES) != null && user!=null && !user.equals(APILocator.getUserAPI().getAnonymousUser())) {
        final DotTempFile temp = tempFileAPI.createEmptyTempFile(inputFile.getName(), req);
        FileUtil.copyFile(data.getDataFile(), temp.file);
        //Temp files time-mark must be updated so they can be recognized by the tempFileAPI
        temp.file.setLastModified(System.currentTimeMillis());
		copyMetadata(uuid, fieldVarName, temp);
		resp.getWriter().println(DotObjectMapperProvider.getInstance().getDefaultObjectMapper().writeValueAsString(temp));
        resp.getWriter().close();
        resp.flushBuffer();
        return;
      }

      if(req.getParameter(WebKeys.IMAGE_TOOL_CLIPBOARD) != null && user!=null && !user.equals(APILocator.getUserAPI().getAnonymousUser())) {
        List<String> list = (List<String>) req.getSession().getAttribute(WebKeys.IMAGE_TOOL_CLIPBOARD);
        if (list == null) {
          list = new ArrayList<>();
        }
        // we only show nine images in clipboard
        if (list.size() > 8) {
          list = list.subList(0, 8);
        }
  
        if (list.contains(req.getRequestURI())) {
          list.remove(req.getRequestURI());
        }
        list.add(0, req.getRequestURI());
  
        req.getSession().setAttribute(WebKeys.IMAGE_TOOL_CLIPBOARD, list);
  
        resp.getWriter().println("success");
        resp.getWriter().println(req.getRequestURI());
        return;
      }
      

			/*******************************
			 *
			 *  Start serving the data
			 *
			 *******************************/
			long _fileLength = data.getDataFile().length();
			
			String mimeType = fileAssetAPI.getMimeType(data.getDataFile().getName());

			if (mimeType == null) {
				mimeType = "application/octet-stream";
			}
			
			resp.setHeader("Content-Disposition", "inline; filename=\"" + UtilMethods.encodeURL(downloadName) + "\"" );
			resp.setHeader("Content-Length", String.valueOf(_fileLength));

			resp.setContentType(mimeType);
			
			if (req.getParameter("dotcms_force_download") != null || req.getParameter("force_download") != null) {

				// if we are downloading a jpeg version of a png or gif
				String x = UtilMethods.getFileExtension(downloadName);
				String y = UtilMethods.getFileExtension(data.getDataFile().getName());
				if(!x.equals(y)){
					downloadName = downloadName.replaceAll("\\." + x, "\\." + y);
				}
				resp.setHeader("Content-Disposition", "attachment; filename=\"" + UtilMethods.encodeURL(downloadName) + "\"");
			
			} else {



			    // Set the expiration time
				if (!mode.isAdmin) {

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

					
					String _eTag = (ASSETS_USE_WEAK_ETAGS ? "W/" : "") + "dot:" + assetInode + ":" + _lastModified + ":" + _fileLength;

					SimpleDateFormat httpDate = new SimpleDateFormat(Constants.RFC2822_FORMAT);
					httpDate.setTimeZone(TimeZone.getTimeZone("GMT"));
		            /* Setting cache friendly headers */
					if (!resp.containsHeader(EXPIRES)) {
						resp.setHeader(EXPIRES, httpDate.format(expiration.getTime()));
					}

		            if (!resp.containsHeader(CACHE_CONTROL)) {
						resp.setHeader(CACHE_CONTROL, "public, max-age=" + seconds);
					}
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

		            resp.setHeader("Last-Modified", httpDate.format(_lastModifiedDate));
		            resp.setHeader("Content-Length", String.valueOf(_fileLength));
		            resp.setHeader("ETag", _eTag);

                /* if we are in ADMIN MODE, don't cache */
				}else{
				    GregorianCalendar expiration = new GregorianCalendar();
					expiration.add(java.util.Calendar.MONTH, -1);
					if (!resp.containsHeader(EXPIRES)) {
						resp.setHeader(EXPIRES, DownloadUtil.httpDate.get().format(expiration.getTime()));
					}

					if (!resp.containsHeader(CACHE_CONTROL)) {
						resp.setHeader(CACHE_CONTROL, "max-age=-1");
					}
				}
			}
			if(resp.getHeader("Accept-Ranges")==null) {
			  resp.setHeader("Accept-Ranges","bytes");
			}
			String rangeHeader = req.getHeader("range");
			if(UtilMethods.isSet(rangeHeader)){

				try {
					out = resp.getOutputStream();
					long fileLen = data.getDataFile().length();


					//extract range header
					 resp.setHeader("Accept-Ranges", "bytes");
					// Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
					if (!rangeHeader.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
						resp.setHeader("Content-Range", "bytes */" + fileLen); // Required in 416.
						resp.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
						return;
					}
					//parse multiple range bytes
					ArrayList<SpeedyAssetServletUtil.ByteRange> ranges = SpeedyAssetServletUtil.parseRange(rangeHeader, fileLen);
					if (ranges != null){
						SpeedyAssetServletUtil.ByteRange full = new SpeedyAssetServletUtil.ByteRange(0, data.getDataFile().length() - 1, data.getDataFile().length());
						if (ranges.isEmpty() || ranges.get(0).equals(full)) {
							// Return full file.
							input = new RandomAccessFile(data.getDataFile(), "r");
							SpeedyAssetServletUtil.ByteRange r = full;
							resp.setContentType(fileAssetAPI.getMimeType(data.getDataFile().getName()));
							resp.setHeader("Content-Range", "bytes " + r.start + "-" + r.end + "/" + r.total);
							resp.setHeader("Content-Length", String.valueOf(r.length));
							// Copy full range.
							SpeedyAssetServletUtil.copy(input, out, r.start, r.length);
						} else if (ranges.size() == 1){
							SpeedyAssetServletUtil.ByteRange range = ranges.get(0);
							input = new RandomAccessFile(data.getDataFile(), "r");
							// Check if Range is syntactically valid. If not, then return 416.
							if (range.start > range.end) {
								resp.setHeader("Content-Range", "bytes */" + fileLen); // Required in 416.
								resp.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
								return;
							}
							resp.setContentType(fileAssetAPI.getMimeType(data.getDataFile().getName()));
							resp.setHeader("Content-Range", "bytes " + range.start + "-" + range.end + "/" + range.total);
							resp.setHeader("Content-Length", String.valueOf(range.length));
				            resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206.
							SpeedyAssetServletUtil.copy(input, out, range.start, range.length);
						}else{
							resp.setContentType("multipart/byteranges; boundary=" + SpeedyAssetServletUtil.MULTIPART_BOUNDARY);
							resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
						    input = new RandomAccessFile(data.getDataFile(), "r");
							for (SpeedyAssetServletUtil.ByteRange r : ranges) {
								if (r.start > r.end) {
									resp.setHeader("Content-Range", "bytes */" + fileLen); // Required in 416.
									resp.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
									return;
								}
								// Add multipart boundary and header fields for every range.
								out.println();
								out.println("--" + SpeedyAssetServletUtil.MULTIPART_BOUNDARY);
								out.println("Content-Type: " + fileAssetAPI.getMimeType(data.getDataFile().getName()));
								out.println("Content-Range: bytes " + r.start + "-" + r.end + "/" + r.total);
								out.println();

								// Copy single part range of multi part range.
								SpeedyAssetServletUtil.copy(input, out, r.start, r.length);
							}
							// End with multipart boundary.
							out.println();
							out.println("--" + SpeedyAssetServletUtil.MULTIPART_BOUNDARY + "--");
						}
					}
				} catch (Exception e) {
					Logger.warn(this, e + " Error for = " + req.getRequestURI() + (req.getQueryString() != null?"?"+req.getQueryString():"") );
					Logger.debug(this, "Error serving asset = " + req.getRequestURI() + (req.getQueryString() != null?"?"+req.getQueryString():""), e);

				}
			}else{
				is = java.nio.file.Files.newInputStream(data.getDataFile().toPath());
	            int count = 0;
	            byte[] buffer = new byte[4096];
	            out = resp.getOutputStream();
	            
	            while((count = is.read(buffer)) > 0) {
	            	out.write(buffer, 0, count);
	            }
	            
			}
            
		} catch (DotRuntimeException e) {
			Logger.debug(BinaryExporterServlet.class, e.getMessage(),e);
            if(!resp.isCommitted()){
              resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
		} catch (PortalException e) {
			Logger.error(BinaryExporterServlet.class, "[PortalException] An error occurred when accessing '" + uri + "': " + e.getMessage());
			Logger.debug(BinaryExporterServlet.class, e.getMessage(),e);
            if(!resp.isCommitted()){
              resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
		} catch (SystemException e) {
			Logger.error(BinaryExporterServlet.class, "[SystemException] An error occurred when accessing '" + uri + "': " + e.getMessage());
			Logger.debug(BinaryExporterServlet.class, e.getMessage(),e);
            if(!resp.isCommitted()){
              resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
		} catch (DotDataException e) {
			Logger.error(BinaryExporterServlet.class, "[DotDataException] An error occurred when accessing '" + uri + "': " + e.getMessage());
			Logger.debug(BinaryExporterServlet.class, e.getMessage(),e);
	         if(!resp.isCommitted()){
	           resp.sendError(HttpServletResponse.SC_NOT_FOUND);
	         }
		} catch (DotSecurityException e) {
			try {
			  if(req.getSession()!=null){
				if(WebAPILocator.getUserWebAPI().isLoggedToBackend(req)){
				    req.getSession().removeAttribute(com.dotmarketing.util.WebKeys.REDIRECT_AFTER_LOGIN);
					resp.sendError(HttpServletResponse.SC_FORBIDDEN);
				}else{
					final String requestUri = (String) req.getAttribute("javax.servlet.forward.request_uri");
					Logger.debug(this, "Setting redirect after login to requested uri: " + requestUri);
				    req.getSession().setAttribute(com.dotmarketing.util.WebKeys.REDIRECT_AFTER_LOGIN, requestUri);
					resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				}
			  }
			} catch (Exception e1) {
				Logger.error(BinaryExporterServlet.class, "An error occurred when accessing '" + uri + "': " + e1.getMessage(), e1);
	            if(!resp.isCommitted()){
	              resp.sendError(HttpServletResponse.SC_NOT_FOUND);
	            }
			}
		} catch (BinaryContentExporterException e) {
			Logger.debug(BinaryExporterServlet.class, e.getMessage(),e);
			Logger.error(BinaryExporterServlet.class, "[BinaryContentExporterException] An error occurred when accessing '" + uri + "': " + e.getMessage());
			if(!resp.isCommitted()){
			  resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
		}catch (Exception e) {
			Logger.debug(BinaryExporterServlet.class, e.getMessage(),e);
			Logger.error(BinaryExporterServlet.class, "[Exception] An error occurred when accessing '" + uri + "': " + e.getMessage());
            if(!resp.isCommitted()){
              resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
		}
		// close our resources no matter what
		finally{
			
		  CloseUtils.closeQuietly(input, is, out);

		}
		
	}

	private Contentlet getContentletByIdentifier(final PageMode pageMode,
			final String identifier, final long languageId, final User user)
			throws DotDataException, DotSecurityException {

		final String currentVariantId = WebAPILocator.getVariantWebAPI().currentVariantId();

		final Contentlet contentletByIdentifier = contentAPI.findContentletByIdentifier(identifier,
				pageMode.showLive, languageId, currentVariantId, user, pageMode.respectAnonPerms);

		if (UtilMethods.isSet(contentletByIdentifier)) {
			return contentletByIdentifier;
		}

		return contentAPI.findContentletByIdentifier(identifier, pageMode.showLive, languageId, user, pageMode.respectAnonPerms);
	}

	private boolean isBrowserSafariAndVersionBelow14(final UserAgent userAgent) {
		return userAgent.getBrowser() == Browser.SAFARI
				&& Integer.parseInt(userAgent.getBrowserVersion().getMajorVersion())<14;
	}

	/**
	 * This does transfer the custom metadata from temp files into the target inode and viceversa
	 * @param id
	 * @param fieldVarName
	 * @param temp
	 * @throws DotDataException
	 */
	private void copyMetadata(final String id, final String fieldVarName,
			final DotTempFile temp) {
		//Basically here when a new temp file has been generated and we transfer any custom generated metadata
		if (UtilMethods.isSet(fieldVarName) && UtilMethods.isSet(id)) {
			try {
				if (UUIDUtil.isUUID(id)) {
                    // FocalPointImageFilter does prepend a 'TMP::' to the inodes to avoid setting the metadata directly to the inode
                    // leaving that work to the check-in process.
					final Optional<Metadata> optionalMetadata = fileMetadataAPI.getMetadata( TMP +  id);
					if (optionalMetadata.isPresent()) {
						fileMetadataAPI.putCustomMetadataAttributes(temp.id, ImmutableMap
								.of(fieldVarName, optionalMetadata.get().getCustomMeta()));
					}
				} else {
				    //Temp resource id
					final Optional<Metadata> optionalMetadata = fileMetadataAPI.getMetadata(id);
					if (optionalMetadata.isPresent()) {
						fileMetadataAPI.putCustomMetadataAttributes(temp.id, ImmutableMap
								.of(fieldVarName, optionalMetadata.get().getCustomMeta()));
					}
				}
			} catch (Exception e) {
			   Logger.error(BinaryExporterServlet.class, "Exception copying metadata", e);
			}
		}
	}

	private Contentlet getContentletLiveVersion(String assetInode, User user, long lang) throws DotDataException, DotSecurityException {
		Contentlet content;
		final Contentlet contentTemp = contentAPI.find(assetInode,
				APILocator.getUserAPI().getSystemUser(), false);
		content = contentAPI.findContentletByIdentifier(contentTemp.getIdentifier(),
				true, lang, user, true);
		return content;
	}


	private Map<String,String[]> getURIParams(HttpServletRequest request){
		String url = request.getRequestURI().toString();
		url = (url.startsWith("/")) ? url.substring(1, url.length()) : url;
		String p[] = url.split("/");
		Map<String, String[]> map = new LinkedHashMap<>();

		String key =null;
		for(String x : p){
			if(key ==null){
				key = x;
			}
			else{
				map.put(key, new String[]{x});
				key = null;
			}
		}

		return map;
	}

}
