package com.dotcms.csspreproc;

import com.dotcms.csspreproc.CachedCSS.ImportedAsset;
import com.dotcms.enterprise.csspreproc.CSSCompiler;
import com.dotcms.enterprise.csspreproc.LessCompiler;
import com.dotcms.enterprise.csspreproc.SassCompiler;
import com.dotcms.util.DownloadUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;

public class CSSPreProcessServlet extends HttpServlet {
    private static final long serialVersionUID = -3315180323197314439L;

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        
        try {
            Host host = WebAPILocator.getHostWebAPI().getCurrentHost(req);
            boolean live = !WebAPILocator.getUserWebAPI().isLoggedToBackend(req);
            User user = WebAPILocator.getUserWebAPI().getLoggedInUser(req);
            String reqURI=req.getRequestURI();
            String uri = reqURI.substring(reqURI.indexOf('/', 1));

            /*
              First we need to figure it out the host of the requested file, not the current host.
              We could be in host B and the file be living in host A
            */
            if ( uri.startsWith( "//" ) ) {

                String tempURI = uri.replaceFirst( "//", "" );
                String externalHost = tempURI.substring( 0, tempURI.indexOf( "/" ) );

                //Avoid unnecessary queries, we may be already in the host where the file lives
                if ( !externalHost.equals( host.getHostname() ) && !host.getAliases().contains( externalHost ) ) {

                    Host fileHost = null;
                    try {
                        fileHost = APILocator.getHostAPI().findByName( externalHost, user, true );
                        if ( !UtilMethods.isSet( fileHost ) || !InodeUtils.isSet( fileHost.getInode() ) ) {
                            fileHost = APILocator.getHostAPI().findByAlias( externalHost, user, true );
                        }
                    } catch ( Exception e ) {
                        Logger.error( CSSPreProcessServlet.class, "Error searching host [" + externalHost + "].", e );
                    }

                    if ( UtilMethods.isSet( fileHost ) && InodeUtils.isSet( fileHost.getInode() ) ) {
                        host = fileHost;
                    }
                }

                //Remove the host from the URI now that we have the host owner of the file
                uri = uri.replace( "//" + externalHost, "" );

            }

            if(!reqURI.endsWith(".css")) {
                String path=live ? LiveCache.getPathFromCache(uri, host.getIdentifier()) : WorkingCache.getPathFromCache(uri, host.getIdentifier());
                if(UtilMethods.isSet(path)) {
                    req.getRequestDispatcher("/dotAsset?path=" + path).forward(req, resp);
                }
                else {
                    resp.sendError(404);
                }
                return;
            }
            
            // choose compiler based on the request URI
            Class<? extends CSSCompiler> compilerClass = reqURI.startsWith("/DOTSASS/") ? SassCompiler.class
                                           :  (reqURI.startsWith("/DOTLESS/") ? LessCompiler.class : null);
            CSSCompiler compiler = compilerClass.getConstructor(Host.class,String.class,boolean.class).newInstance(host,uri,live);
            
            // check if the asset exists
            String actualUri =  uri.substring(0, uri.lastIndexOf('.')) + "." + compiler.getDefaultExtension();
            Identifier ident = APILocator.getIdentifierAPI().find(host, actualUri);
            if(ident==null || !InodeUtils.isSet(ident.getId())) {
                resp.sendError(404);
                return;
            }
            
            // get the asset in order to build etag and check permissions
            long defLang=APILocator.getLanguageAPI().getDefaultLanguage().getId();
            FileAsset fileasset;
            try {
                fileasset = APILocator.getFileAssetAPI().fromContentlet(
                    APILocator.getContentletAPI().findContentletByIdentifier(ident.getId(), live, defLang, user, true));
            }
            catch(DotSecurityException ex) {
                resp.sendError(403);
                return;
            }
            
            boolean userHasEditPerms = false;
            if(!live) {
                userHasEditPerms = APILocator.getPermissionAPI().doesUserHavePermission(fileasset,PermissionAPI.PERMISSION_EDIT,user);
                if(req.getParameter("recompile")!=null && userHasEditPerms) {
                    CacheLocator.getCSSCache().remove(host.getIdentifier(), actualUri, false);
                    CacheLocator.getCSSCache().remove(host.getIdentifier(), actualUri, true);
                }
            }
            
            CachedCSS cache = CacheLocator.getCSSCache().get(host.getIdentifier(), actualUri, live, user);
            
            byte[] responseData=null;
            Date cacheMaxDate=null;
            CachedCSS cacheObject=null;
            
            if(cache==null || cache.data==null) {
                // do compile!
                synchronized(ident.getId().intern()) {
                    cache = CacheLocator.getCSSCache().get(host.getIdentifier(), actualUri, live, user);
                    if(cache==null || cache.data==null) {
                        Logger.debug(this, "compiling css data for "+host.getHostname()+":"+uri);
                        
                        try {
                            compiler.compile();
                        }
                        catch(Throwable ex) {
                            Logger.error(this, "Error compiling "+host.getHostname()+":"+uri, ex);
                            throw new Exception(ex);
                        }
                        
                        // build cache object
                        ContentletVersionInfo vinfo = APILocator.getVersionableAPI().getContentletVersionInfo(ident.getId(), defLang);
                        CachedCSS newcache = new CachedCSS();
                        newcache.data = compiler.getOutput();
                        newcache.hostId = host.getIdentifier();
                        newcache.uri = actualUri;
                        newcache.live = live;
                        newcache.modDate = vinfo.getVersionTs();
                        newcache.imported = new ArrayList<ImportedAsset>();
                        for(String importUri : compiler.getAllImportedURI()) {
                            // newcache entry for the imported asset
                            ImportedAsset asset = new ImportedAsset();
                            asset.uri = importUri;
                            Identifier ii;
                            if(importUri.startsWith("//")) {
                                importUri=importUri.substring(2);
                                String hn=importUri.substring(0, importUri.indexOf('/'));
                                String uu=importUri.substring(importUri.indexOf('/'));
                                ii = APILocator.getIdentifierAPI().find(APILocator.getHostAPI().findByName(hn, user, live),uu);
                            }
                            else {
                                ii = APILocator.getIdentifierAPI().find(host, importUri);
                            }
                            ContentletVersionInfo impInfo = APILocator.getVersionableAPI().getContentletVersionInfo(ii.getId(), defLang);
                            asset.modDate = impInfo.getVersionTs();
                            newcache.imported.add(asset);
                            Logger.debug(this, host.getHostname()+":"+actualUri+" imports-> "+importUri);
                            
                            // actual cache entry for the imported asset. If needed
                            synchronized(ii.getId().intern()) {
                                if(CacheLocator.getCSSCache().get(ii.getHostId(), importUri, live, user)==null) {
                                    CachedCSS entry = new CachedCSS();
                                    entry.data = null;
                                    entry.hostId = ii.getHostId();
                                    entry.imported = new ArrayList<ImportedAsset>();
                                    entry.live = live;
                                    entry.modDate = impInfo.getVersionTs();
                                    entry.uri = importUri;
                                    CacheLocator.getCSSCache().add(entry);
                                }
                            }
                        }
                        CacheLocator.getCSSCache().add(newcache);
                        cacheMaxDate = newcache.getMaxDate();
                        cacheObject = newcache;
                        responseData = compiler.getOutput();
                    }
                }
            }
            
            if(responseData==null) {
                if(cache!=null && cache.data!=null) {
                    // if css is cached an valid is used as response
                    responseData = cache.data;
                    cacheMaxDate = cache.getMaxDate();
                    cacheObject = cache;
                    Logger.debug(this, "using cached css data for "+host.getHostname()+":"+uri);
                }
                else {
                    resp.sendError(500, "no data!");
                    return;
                }
            }
            
            
            boolean doDownload = true;
            
            if(live) {
                // we use etag dot:inode:cacheMaxDate:filesize and lastMod:cacheMaxDate
                // so the browser downloads it if any of the imported files changes
                doDownload = DownloadUtil.isModifiedEtag(req, resp, fileasset.getInode(), 
                        cacheMaxDate.getTime(), fileasset.getFileSize());
            }
            
            if(doDownload) {
                // write the actual response to the user
                resp.setContentType("text/css");
                resp.setHeader("Content-Disposition", 
                        "inline; filename=\"" + uri.substring(uri.lastIndexOf('/'), uri.length()) + "\"");
                
                if(!live && userHasEditPerms && req.getParameter("debug")!=null) {
                    // debug information requested
                    PrintWriter out = resp.getWriter();
                    out.println("/*");
                    out.println("Cached CSS: "+host.getHostname()+":"+actualUri);
                    out.println("Size: "+cacheObject.data.length+" bytes");
                    out.println("Imported uris:");
                    for(ImportedAsset asset : cacheObject.imported) {
                        out.println("  "+asset.uri);
                    }
                    out.println("*/");
                    out.println(new String(responseData));
                }
                else {
                    resp.getOutputStream().write(responseData);
                }
            }
            
        }
        catch(Exception ex) {
        	
        	try {
				Class clazz = Class.forName("org.apache.catalina.connector.ClientAbortException");
				if(ex.getClass().equals(clazz)){
					Logger.debug(this, "ClientAbortException while serving compiled css file:" + ex.getMessage(), ex);
				}
				else{
					Logger.error(this, "Exception while serving compiled css file",ex);
				}
			//if we are not running on tomcat
			} catch (ClassNotFoundException e) {
				 Logger.error(this, "Exception while serving compiled css file",ex);
				
			}
        }
        finally {
            try {
                HibernateUtil.closeSession();
            } catch (DotHibernateException e) {
                Logger.warn(this, "Exception while hibernate session close",e);
            }
        }
    }
}
