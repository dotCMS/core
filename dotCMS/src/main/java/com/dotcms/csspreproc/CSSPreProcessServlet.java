package com.dotcms.csspreproc;

import com.dotcms.csspreproc.CachedCSS.ImportedAsset;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.util.DownloadUtil;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import org.apache.http.HttpStatus;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;

/**
 * This Servlet handles all the requests related to compiling SCSS Files in dotCMS. It takes care of tasks such as:
 * <ul>
 *     <li>Verifying sure that the incoming SCSS file actually exists and is available to be processed by the appropriate
 *     implementation of the SASS compiler.</li>
 *     <li>Generating the contents of the resulting CSS file based on the results of the compilation.</li>
 *     <li>Generating the appropriate response in case a major problem occurred.</li>
 * </ul>
 * Keep in mind that the Servlet <b>will NOT fail in case a SCSS file cannot be compiled</b> as dotCMS must carry on with
 * the HTML Page rendering process.
 *
 * @author Jorge Urdaneta
 * @since Jan 23rd, 2014
 */
public class CSSPreProcessServlet extends HttpServlet {

    private static final long serialVersionUID = -3315180323197314439L;

    @Override
    public void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        String actualUri = StringPool.BLANK;
        try {
            Logger.debug(this, "---- CSS Pre-Process Compiler ----");
            final Host currentSite = WebAPILocator.getHostWebAPI().getCurrentHost(req);
            final boolean live = !WebAPILocator.getUserWebAPI().isLoggedToBackend(req);
            final User user = WebAPILocator.getUserWebAPI().getLoggedInUser(req);
            final String originalURI = req.getRequestURI();
            
            // Check for dotsass=true query parameter to enable SASS compilation for non-standard URLs
            final String dotsassParam = req.getParameter("dotsass");
            final boolean isDotsassParam = "true".equalsIgnoreCase(dotsassParam);
            
            String fileUri;
            if (isDotsassParam) {
                // Process request with dotsass=true parameter
                Logger.debug(this, "Processing request with dotsass=true parameter");
                
                // Check if this is a forwarded request from ShortyServlet or ScssQueryParamFilter
                String shortyURI = (String) req.getAttribute("originalShortyURI");
                String scssURI = (String) req.getAttribute("originalScssURI");
                String uriToProcess = originalURI;
                
                if (shortyURI != null) {
                    Logger.debug(this, "Received forwarded request from ShortyServlet with original URI: " + shortyURI);
                    // Use the original URI from the ShortyServlet
                    uriToProcess = shortyURI;
                } else if (scssURI != null) {
                    Logger.debug(this, "Received forwarded request from ScssQueryParamFilter with original URI: " + scssURI);
                    // Use the original URI from the ScssQueryParamFilter
                    uriToProcess = scssURI;
                }
                
                if (uriToProcess.startsWith("/dA/")) {
                    // Handle shorty ID pattern: /dA/{identifier}?dotsass=true
                    fileUri = handleShortyRequest(req, resp, uriToProcess, currentSite, live, user);
                    if (fileUri == null) {
                        // Error already handled in handleShortyRequest
                        return;
                    }
                } else {
                    // Handle direct file path pattern: /path/to/file.scss?dotsass=true
                    // For direct SCSS files with dotsass=true, we need to ensure they're processed as SASS
                    if (uriToProcess.toLowerCase().endsWith(".scss")) {
                        Logger.debug(this, "Processing direct SCSS file with dotsass=true: " + uriToProcess);
                        fileUri = uriToProcess;
                    } else {
                        // For other files, just use the original URI
                        fileUri = originalURI;
                    }
                }
            } else {
                // Handle standard SASS preprocessing request: /DOTSASS/path/to/file.scss
                fileUri = originalURI.replace(SassPreProcessServlet.DOTSASS_PREFIX,"");
            }
            
            Logger.debug(this, String.format("-> Original URI = %s", originalURI));
            Logger.debug(this, String.format("-> File URI     = %s", fileUri));
            final DotLibSassCompiler compiler = new DotLibSassCompiler(currentSite, fileUri, live, req);
            
            // Check if the asset exists
            actualUri = fileUri;
            if (!fileUri.toLowerCase().endsWith("." + compiler.getDefaultExtension())) {
                if (fileUri.contains(".")) {
                    actualUri = fileUri.substring(0, fileUri.lastIndexOf('.')) + "." + compiler.getDefaultExtension();
                } else {
                    actualUri = fileUri + "." + compiler.getDefaultExtension();
                }
            }
            
            Logger.debug(this, String.format("-> Actual URI   = %s", actualUri));
            final Identifier ident = APILocator.getIdentifierAPI().find(currentSite, actualUri);
            if (null == ident || !InodeUtils.isSet(ident.getId())) {
                Logger.error(this, String.format("Requested SASS file '%s' in Site '%s' does not exist", actualUri, currentSite));
                sendError(resp, HttpStatus.SC_NOT_FOUND);
                return;
            }
            
            // get the asset in order to build etag and check permissions
            final long defLang=APILocator.getLanguageAPI().getDefaultLanguage().getId();
            final Optional<FileAsset> fileAssetOptional = getFileAsset(live, user, ident, defLang);
            if(fileAssetOptional.isEmpty()) {
                Logger.error(this, String.format("Requested SASS file '%s' in Site '%s' does not exist or cannot " +
                        "be accessed by user '%s'", actualUri, currentSite, user.getUserId()));
                sendError(resp, HttpStatus.SC_FORBIDDEN);
                return;
            }

            final FileAsset fileAsset = fileAssetOptional.get();

            boolean userHasEditPerms = false;
            if(!live) {
                userHasEditPerms = APILocator.getPermissionAPI().doesUserHavePermission(fileAsset,PermissionAPI.PERMISSION_EDIT,user);
                if (req.getParameter("recompile") != null && userHasEditPerms) {
                    Logger.debug(this, String.format("The 'recompile' parameter is present. Force re-compiling " +
                            "file '%s:%s'", currentSite, actualUri));
                    CacheLocator.getCSSCache().remove(currentSite.getIdentifier(), actualUri, false);
                    CacheLocator.getCSSCache().remove(currentSite.getIdentifier(), actualUri, true);
                }
            }
            
            CachedCSS cache = CacheLocator.getCSSCache().get(currentSite.getIdentifier(), actualUri, live, user);
            
            byte[] responseData=null;
            Date cacheMaxDate=null;
            CachedCSS cacheObject=null;
            
            if(cache==null || cache.data==null) {
                // do compile!
                synchronized(ident.getId().intern()) {
                    cache = CacheLocator.getCSSCache().get(currentSite.getIdentifier(), actualUri, live, user);
                    if(cache==null || cache.data==null) {
                        Logger.debug(this, String.format("Compiling file '%s:%s' ...", currentSite, fileUri));
                        try {
                            compiler.compile();
                        } catch (final Throwable ex) {
                            Throwable throwable = ex;
                            Logger.error(this, String.format("An error occurred when compiling the SCSS file " +
                                    "'%s:%s': %s", currentSite, fileUri, ExceptionUtil.getErrorMessage(ex)), ex);
                          if (Config.getBooleanProperty("SHOW_SASS_ERRORS_ON_FRONTEND", true)) {
                            if(userHasEditPerms) {
                              throwable = (throwable.getCause()!=null) ? throwable.getCause() : throwable;
                              resp.getWriter().print("<html><body><h2>Error compiling sass</h2><p>(this only shows if you are an editor in dotCMS)</p><pre>");
                              throwable.printStackTrace(resp.getWriter());
                              resp.getWriter().print("</pre></body></html>");
                            }
                          }
                          throw new Exception(throwable);
                        }
                        
                        // build cache object
                        final Optional<ContentletVersionInfo> verInfo = APILocator.getVersionableAPI().getContentletVersionInfo(ident.getId(), defLang);

                        if(verInfo.isEmpty()) {
                            Logger.error(CSSPreProcessServlet.class, String.format("No Version info was found for [%s] with lang [%d]",ident.getId(), defLang));
                            sendError(resp, HttpStatus.SC_NOT_FOUND);
                            return;
                        }

                        final CachedCSS newCache = new CachedCSS();
                        newCache.data = compiler.getOutput();
                        newCache.hostId = currentSite.getIdentifier();
                        newCache.uri = actualUri;
                        newCache.live = live;
                        newCache.modDate = verInfo.get().getVersionTs();
                        newCache.imported = new ArrayList<>();
                        Logger.debug(this, String.format("Processing %d SCSS files imported by the requested file '%s:%s'",
                                null != compiler.getAllImportedURI() ?
                                        compiler.getAllImportedURI().size() : 0, currentSite, actualUri));
                        for (String importUri : compiler.getAllImportedURI()) {
                            // newcache entry for the imported asset
                            final ImportedAsset asset = new ImportedAsset();
                            asset.uri = importUri;
                            Identifier importUriIdentifier;
                            if(importUri.startsWith("//")) {
                                importUri=importUri.substring(2);
                                final String siteName = importUri.substring(0, importUri.indexOf('/'));
                                final String url = importUri.substring(importUri.indexOf('/'));
                                importUriIdentifier = APILocator.getIdentifierAPI().find(APILocator.getHostAPI().findByName(siteName, user, live), url);
                            }
                            else {
                                importUriIdentifier = APILocator.getIdentifierAPI().find(currentSite, importUri);
                            }
                            final Optional<ContentletVersionInfo> impInfo = APILocator.getVersionableAPI()
                                    .getContentletVersionInfo(importUriIdentifier.getId(), defLang);

                            if (impInfo.isEmpty()) {
                                Logger.error(this, String.format("VersionInfo for imported URI '%s' in language " +
                                                "'%s' was not found",
                                        importUriIdentifier.getId(), defLang));
                                sendError(resp, HttpStatus.SC_NOT_FOUND);
                                return;
                            }

                            asset.modDate = impInfo.get().getVersionTs();
                            newCache.imported.add(asset);
                            Logger.debug(this, String.format("-> Importing file: '%s'", importUri));
                            
                            // actual cache entry for the imported asset. If needed
                            synchronized(importUriIdentifier.getId().intern()) {
                                if(CacheLocator.getCSSCache().get(importUriIdentifier.getHostId(), importUri, live, user)==null) {
                                    CachedCSS entry = new CachedCSS();
                                    entry.data = null;
                                    entry.hostId = importUriIdentifier.getHostId();
                                    entry.imported = new ArrayList<>();
                                    entry.live = live;
                                    entry.modDate = impInfo.get().getVersionTs();
                                    entry.uri = importUri;
                                    CacheLocator.getCSSCache().add(entry);
                                }
                            }
                        }
                        CacheLocator.getCSSCache().add(newCache);
                        cacheMaxDate = newCache.getMaxDate();
                        cacheObject = newCache;
                        responseData = compiler.getOutput();
                    }
                }
            }
            
            if(responseData == null) {
                if(cache!=null && cache.data!=null) {
                    // if css is cached an valid is used as response
                    responseData = cache.data;
                    cacheMaxDate = cache.getMaxDate();
                    cacheObject = cache;
                    Logger.debug(this, String.format("The SASS Compiler generated a null output for file '%s:%s', " +
                                    "but cached data can be returned.", currentSite, fileUri));
                } else {
                    Logger.error(this, String.format("The SASS Compiler generated a null output for file '%s:%s'",
                            currentSite, fileUri));
                    sendError(resp, HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    return;
                }
            }
            boolean doDownload = true;
            
            if(live) {
                // we use etag dot:inode:cacheMaxDate:filesize and lastMod:cacheMaxDate
                // so the browser downloads it if any of the imported files changes
                doDownload = DownloadUtil.isModifiedEtag(req, resp, fileAsset.getInode(),
                        cacheMaxDate.getTime(), fileAsset.getFileSize());
                if (!doDownload) {
                    Logger.debug(this, String.format("Contents of file '%s:%s' have not been modified. Returning " +
                                    "status %s.", currentSite, fileUri, HttpServletResponse.SC_NOT_MODIFIED));
                }
            }
            
            if(doDownload) {
                // write the actual response to the user
                resp.setContentType("text/css");
                resp.setHeader("Content-Disposition", 
                        "inline; filename=\"" + fileUri.substring(fileUri.lastIndexOf('/')) + "\"");

                final boolean verbose = !live && userHasEditPerms && req.getParameter("debug") !=null;
                Logger.debug(this, String.format("Returning contents of file '%s:%s' [ verbose = %s ]", currentSite,
                        fileUri, verbose));
                writeResponseData(verbose, actualUri, currentSite, cacheObject, responseData, resp);
            }
        } catch (final Exception ex) {
        	try {
				final Class<?> clazz = Class.forName("org.apache.catalina.connector.ClientAbortException");
				if(ex.getClass().equals(clazz)){
					Logger.debug(this, "ClientAbortException while serving compiled css file:" + ex.getMessage(), ex);
				} else {
                    Logger.error(this, String.format("Exception while serving compiled css file '%s' : %s", actualUri
                            , ex.getMessage()), ex);
				}
			//if we are not running on tomcat
			} catch (final ClassNotFoundException e) {
                Logger.error(this, String.format("Exception while serving compiled css file '%s' : %s", actualUri,
                        ex.getMessage()), ex);
			}
        } finally {
            try {
                HibernateUtil.closeSession();
            } catch (final DotHibernateException e) {
                Logger.warn(this, "Exception while hibernate session close",e);
            }
        }
    }

    /**
     * Writes the generated CSS file to the HTTP Response.
     *
     * @param verbose      If debugging information must be included in the response, set this to
     *                     {@code true}
     * @param actualUri    The URI of the SCSS file that is being processed.
     * @param site         The {@link Host} where the SCSS file is located.
     * @param cacheObject  The {@link CachedCSS} object that contains the information about the SCSS
     *                     file.
     * @param responseData The contents of the generated CSS file.
     * @param resp         The current instance of the {@link HttpServletResponse}.
     */
    private void writeResponseData(final boolean verbose, final String actualUri, final Host site,
            final CachedCSS cacheObject, final byte[] responseData,
            final HttpServletResponse resp) {
        try {
            final PrintWriter out = resp.getWriter();
            if (verbose) {
                // debug information requested
                out.println("/*");
                out.println("- Cached CSS   : " + site + ":" + actualUri);
                out.println("- Size         : " + cacheObject.data.length + " bytes");
                out.println("- Imported URIs:");
                for (final ImportedAsset asset : cacheObject.imported) {
                    out.println("-> " + asset.uri);
                }
                out.println("*/");
                out.println(new String(responseData));
            } else {
                out.write(new String(responseData));
            }
        } catch (final IOException io) {
             Logger.error(CSSPreProcessServlet.class, String.format("Failed to write response data for file " +
                     "'%s:%s' [ verbose = %s ]: %s", site, actualUri, verbose, ExceptionUtil.getErrorMessage(io)), io);
        }
    }

    /**
     * Returns the File Asset associated to the given Identifier and language ID.
     *
     * @param live    If the live version of the File Asset must be returned, set this to
     *                {@code true}.
     * @param user    The {@link User} requesting the File Asset.
     * @param ident   The {@link Identifier} of the File Asset.
     * @param defLang The language ID of the File Asset.
     *
     * @return An {@link Optional} containing the File Asset, if it exists, and the user has
     * permissions to access it. Otherwise, an empty Optional is returned.
     */
    private Optional<FileAsset> getFileAsset(final boolean live, final User user, final Identifier ident, final long defLang) {
        try {
            final ContentletAPI contentletAPI = APILocator.getContentletAPI();
            final FileAssetAPI fileAssetAPI = APILocator.getFileAssetAPI();
            return Optional.of(fileAssetAPI.fromContentlet(
             contentletAPI.findContentletByIdentifier(ident.getId(), live, defLang, user, true)));
        } catch (final DotDataException | DotSecurityException e) {
            Logger.error(CSSPreProcessServlet.class, String.format("User '%s' failed to retrieve " +
                    "File Asset '%s' in language '%s': %s", user.getUserId(), ident, defLang,
                    ExceptionUtil.getErrorMessage(e)), e);
        }
        return Optional.empty();
    }

    private void sendError( final HttpServletResponse resp, final int code){
        try {
            resp.sendError(code);
        } catch (IOException e) {
            Logger.error(CSSPreProcessServlet.class, String.format("Error writing response code [%d]", code), e);
        }
    }
    
    /**
     * Handles requests with shorty IDs like /dA/{identifier}?dotsass=true
     * This method extracts the shorty ID from the URI, resolves it to a file asset,
     * verifies it's a SCSS file, and returns the file path for SASS compilation.
     * 
     * @param req The HTTP request
     * @param resp The HTTP response
     * @param originalURI The original request URI
     * @param currentSite The current site/host
     * @param live Whether to use the live version
     * @param user The current user
     * @return The file URI to use for SASS compilation, or null if an error occurred
     * @throws ServletException If a servlet error occurs
     * @throws IOException If an I/O error occurs
     */
    private String handleShortyRequest(final HttpServletRequest req, final HttpServletResponse resp, 
                                      final String originalURI, final Host currentSite, final boolean live, final User user) 
                                      throws ServletException, IOException {
        
        try {
            // Extract the shorty ID from the URI
            String[] uriParts = originalURI.split("/");
            if (uriParts.length < 3) {
                sendError(resp, HttpStatus.SC_BAD_REQUEST);
                Logger.error(this, String.format("Invalid shorty ID format in URI: %s", originalURI));
                return null;
            }
            
            String inodeOrIdentifier = uriParts[2];
            // Remove any API suffix that might be present
            if (inodeOrIdentifier.endsWith("API")) {
                inodeOrIdentifier = inodeOrIdentifier.substring(0, inodeOrIdentifier.length() - 3);
            }
            
            final Optional<ShortyId> shortOpt = APILocator.getShortyAPI().getShorty(inodeOrIdentifier);
            
            if (shortOpt.isEmpty()) {
                sendError(resp, HttpStatus.SC_NOT_FOUND);
                Logger.error(this, String.format("Shorty ID not found: %s", inodeOrIdentifier));
                return null;
            }
            
            final ShortyId shorty = shortOpt.get();
            
            // Get the file path from the shorty ID
            if (shorty.type == ShortType.IDENTIFIER || shorty.type == ShortType.INODE) {
                // Get the file asset from the identifier
                final long defLang = APILocator.getLanguageAPI().getDefaultLanguage().getId();
                final FileAsset fileAsset = (shorty.type == ShortType.IDENTIFIER) 
                    ?  APILocator.getFileAssetAPI().fromContentlet(APILocator.getContentletAPI().findContentletByIdentifier(shorty.longId, live, defLang, user, true))
                    : APILocator.getFileAssetAPI().fromContentlet(APILocator.getContentletAPI().find(shorty.longId, user, true));
                
                // Check if it's a SCSS file
                if (!fileAsset.getFileName().toLowerCase().endsWith(".scss")) {
                    sendError(resp, HttpStatus.SC_BAD_REQUEST);
                    Logger.error(this, String.format("Not a SCSS file: %s", fileAsset.getFileName()));
                    return null;
                }
                
                // Get the file path
                String filePath = fileAsset.getPath() + fileAsset.getFileName();
                Logger.debug(this, String.format("-> File path from shorty ID = %s", filePath));
                return filePath;
            } else {
                // Handle other shorty types if needed
                sendError(resp, HttpStatus.SC_BAD_REQUEST);
                Logger.error(this, String.format("Unsupported shorty type: %s", shorty.type));
                return null;
            }
        } catch (DotStateException | DotDataException | DotSecurityException e) {
            Logger.error(this, "Error retrieving file from shorty ID: " + e.getMessage(), e);
            sendError(resp, HttpStatus.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

}
