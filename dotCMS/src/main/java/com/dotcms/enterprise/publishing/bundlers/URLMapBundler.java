package com.dotcms.enterprise.publishing.bundlers;

import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.IPublisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.org.apache.commons.io.output.FileWriterWithEncoding;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class URLMapBundler implements IBundler {


    private PublisherConfig config;
    private ContentletAPI capi = APILocator.getContentletAPI();
    private LanguageAPI langAPI = APILocator.getLanguageAPI();
    private HostAPI hostAPI = APILocator.getHostAPI();

    public final static String FILE_ASSET_EXTENSION = ".dotUrlMap.xml";

    @Override
    public String getName () {
        return "Static URL Map Bundler";
    }

    @Override
    public void setConfig ( PublisherConfig pc ) {
        this.config = pc;

    }

    @Override
    public void setPublisher(IPublisher publisher) {
    }

    @Override
    public void generate ( File bundleRoot, BundlerStatus status ) throws DotBundleException {

        if ( LicenseUtil.getLevel() < LicenseLevel.STANDARD.level ) {
            throw new RuntimeException( "need an enterprise license to run this bundler" );
        }
        Logger.info(URLMapBundler.class,"Start bundler");
        try {
            //Get APIs
            User systemUser = APILocator.getUserAPI().getSystemUser();

            List<String> structsToAdd = new ArrayList<String>();
            if ( config.getStructures() == null || config.getStructures().size() == 0 ) {
            	// Ignore static-publishing over structures with empty include patterns (https://github.com/dotCMS/core/issues/10504)
            	if (!config.isStatic() || !config.getIncludePatterns().isEmpty()) {
	                //Search for all the structures with a URL Map Pattern
	                List<SimpleStructureURLMap> urlMaps = StructureFactory.findStructureURLMapPatterns();
	                for ( SimpleStructureURLMap map : urlMaps ) {
	                    structsToAdd.add( map.getInode() );
	                }
            	}
            } else {
            	// Ignore static-publishing over user-selected structures (https://github.com/dotCMS/core/issues/10504)
            	if (!config.isStatic()) {
            		structsToAdd.addAll( config.getStructures() );
            	}
            }

            StringBuilder bob = new StringBuilder();

            // if we have no urlmap structures...
            if ( structsToAdd.size() == 0 ) {
                return;
            }

            bob.append( "+(" );
            for ( String s : structsToAdd ) {
                Structure struc = CacheLocator.getContentTypeCache().getStructureByInode( s );
                bob.append( "structureName:").append(struc.getVelocityVarName()).append(" " );
            }
            bob.append( ") " );

            if ( config.getExcludePatterns() != null && config.getExcludePatterns().size() > 0 ) {
                bob.append( "-(" );
                for ( String p : config.getExcludePatterns() ) {
                    if ( !UtilMethods.isSet( p ) ) {
                        continue;
                    }
                    //p = p.replace(" ", "+");
                    bob.append( "urlMap:").append(p).append(" " );
                }
                bob.append( ") " );
            } else if ( config.getIncludePatterns() != null && config.getIncludePatterns().size() > 0 ) {
                bob.append( "+(" );
                for ( String p : config.getIncludePatterns() ) {
                    if ( !UtilMethods.isSet( p ) ) {
                        continue;
                    }
                    //p = p.replace(" ", "+");
                    bob.append( "urlMap:").append(p).append(" " );
                }
                bob.append( ") " );
            }

            if ( config.isIncremental() ) {
                Date start;
                Date end;
                if ( config.getStartDate() != null ) {
                    start = config.getStartDate();
                } else {
                    Calendar cal = Calendar.getInstance();
                    cal.set( Calendar.YEAR, 1900 );
                    start = cal.getTime();
                }
                end = config.getEndDate();
                
                // check for modified detail pages
                // https://github.com/dotCMS/dotCMS/issues/4261
                List<String> structsWithModDetail = new ArrayList<String>();
                Set<String> modPages = new HashSet<String>();
                
                if(modPages.size()>0) {
                    for ( String s : structsToAdd ) {
                        Structure st=CacheLocator.getContentTypeCache().getStructureByInode(s);
                        if(InodeUtils.isSet(st.getDetailPage()) && modPages.contains(st.getDetailPage())) {
                            structsWithModDetail.add(s);
                        }
                    }
                }

                if(structsWithModDetail.size()>0) {
                    bob.append(" +( ");
                    for ( String s : structsWithModDetail ) {
                        Structure st=CacheLocator.getContentTypeCache().getStructureByInode(s);
                        bob.append("structureName:").append(st.getVelocityVarName()).append(" ");
                    }
                }
                
                
                bob.append(structsWithModDetail.isEmpty() ? "+" : " ");
                bob.append( "versionTs:[").append(ESMappingAPIImpl.datetimeFormat.format( start ))
                        .append(" TO ").append(ESMappingAPIImpl.datetimeFormat.format( end )).append("] " );
                
                if(structsWithModDetail.size()>0) {
                    bob.append(" ) ");
                }
            }

            if ( config.getHosts() != null && !config.getHosts().isEmpty() ) {

                List<Host> hostsInQuery = Lists.newArrayList(hostAPI.findSystemHost());
                for (Host host : config.getHosts()) {
                    hostsInQuery.add(host);
                }

                //Hosts query
                bob.append( " +(" );
                for ( Host h : hostsInQuery ) {
                    bob.append( "conhost:").append(h.getIdentifier()).append(" " );
                }
                bob.append( " ) " );
            }

            Logger.info( this.getClass(), bob.toString() );

            for(Long languageId : getSortedConfigLanguages(this.config, langAPI.getDefaultLanguage().getId())){
                processURLMaps(bundleRoot, status, systemUser, bob.toString() + " +languageid:" + languageId + " ");
            }

        } catch ( Exception e ) {
            status.addFailure();
        }
        Logger.info(URLMapBundler.class,"End bundler");
    }

    private void processURLMaps(File bundleRoot, BundlerStatus status, User systemUser, String bob) {
        int limit = 200;
        int page = 0;
        status.setTotal( 0 );
        final String bobLive= bob.toString() + "+live:true" ;
        final String bobWorking= bob.toString() + "+working:true";
        final String bobDeleted= bob.toString() + "+deleted:true";

        while ( true ) {
            List<Contentlet> cs = new ArrayList<Contentlet>();
            try {
                cs.addAll( capi.search( bobLive, limit, page * limit, "inode", systemUser, true ) );
            } catch ( Exception e ) {
                Logger.debug( FileAssetBundler.class, e.getMessage(), e );
            }
            try {
                if(!config.liveOnly() || config.isIncremental() || config.isSameIndexNotIncremental()){
                    cs.addAll( capi.search( bobWorking, limit, page * limit, "inode", systemUser, true ) );
                    cs.addAll( capi.search( bobDeleted, limit, page * limit, "inode", systemUser, true ) );
                }

            } catch ( Exception e ) {
                Logger.debug( FileAssetBundler.class, e.getMessage(), e );
            }

            page++;

            // no more content
            if ( cs.size() == 0 || page > 1000000 ) {
                break;
            }

            status.setTotal( status.getTotal() + cs.size() );

            for ( Contentlet con : cs ) {
                try {
                    writeURLMap( bundleRoot, con );
                    status.addCount();
                } catch ( Exception e ) {
                    Logger.warn( this.getClass(), "Bundle Failed: " + e.getMessage() );
                    status.addFailure();
                }
            }
        }
    }

    private void writeURLMap(File bundleRoot, Contentlet contentlet) throws DotBundleException{
        try{
            Host h = APILocator.getHostAPI().find( contentlet.getHost(), APILocator.getUserAPI().getSystemUser(), true );
            String url = capi.getUrlMapForContentlet( contentlet, APILocator.getUserAPI().getSystemUser(), true );

            if ( url == null ) {
                throw new DotBundleException( "contentlet:" + contentlet.getInode() + " does not have a urlmap" );
            }
            //url = FileUtil.sanitizeFileName(url);
            ContentletVersionInfo info = APILocator.getVersionableAPI().getContentletVersionInfo( contentlet.getIdentifier(), contentlet.getLanguageId() );

            URLMapWrapper wrap = new URLMapWrapper();
            wrap.setInfo( info );
            wrap.setContent( contentlet );
            wrap.setId( APILocator.getIdentifierAPI().find( contentlet.getIdentifier() ) );

            if (Config.getBooleanProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", false) &&
                contentlet.getLanguageId() == langAPI.getDefaultLanguage().getId() &&
                !this.config.getLanguages().isEmpty()
        	) {
                for(String languageId : this.config.getLanguages()) {
                    writeFileToDisk(bundleRoot, contentlet, new Long(languageId), wrap, h, url);
                }
            } else {
                writeFileToDisk(bundleRoot, contentlet, contentlet.getLanguageId(), wrap, h, url);
            }
        } catch(Exception e){
            throw new DotBundleException("Cant get URL Map wrapper info for " + contentlet + " reason " + e.getMessage(), e);
        }
    }

    private void writeFileToDisk ( File bundleRoot, Contentlet contentlet, Long languageId, URLMapWrapper wrap, Host h, String url)
        throws IOException, DotBundleException, DotDataException, DotSecurityException {


        if (Host.SYSTEM_HOST.equals(h.getIdentifier())){

          // system host means all hosts
          List<Host> allHosts = hostAPI.findAll(APILocator.getUserAPI().getSystemUser(), false);

            for (Host host : allHosts) {
                if (!Host.SYSTEM_HOST.equals(host.getIdentifier()) && !host.isArchived()){
                    writeFileToDiskByHost(bundleRoot, contentlet, languageId, wrap, host, url);
                }
            }
        } else {
            writeFileToDiskByHost(bundleRoot, contentlet, languageId, wrap, h, url);
        }
    }

    /**
     * Generates and writes into a file under the bundler folder the content of a detail html page for a given Contentlet
     *
     * @param bundleRoot
     * @param contentlet
     * @throws IOException
     * @throws DotBundleException
     */
    private void writeFileToDiskByHost ( File bundleRoot, Contentlet contentlet, Long languageId, URLMapWrapper wrap, Host h, String url) throws IOException, DotBundleException {

        if ( contentlet == null ) {
            throw new DotBundleException( "null contentlet passed in " );
        }
        Calendar cal = Calendar.getInstance();

        BufferedWriter writer = null;


        try {
            String liveWorking = (contentlet.getInode().equals( wrap.getInfo().getLiveInode() )) ? "live" : "working";

            String myFileUrl = bundleRoot.getPath() + File.separator
                    + liveWorking + File.separator
                    + h.getHostname() + File.separator + languageId
                    + url.replace( "/", File.separator );
            File contentletBundlerFile = new File( myFileUrl );

            // Should we write or is the file already there:

            cal.setTime( wrap.getInfo().getVersionTs() );
            cal.set( Calendar.MILLISECOND, 0 );

            if(config.liveOnly() && config.isIncremental() && !contentlet.isLive()) {
                if(contentletBundlerFile.exists()) {
                    contentletBundlerFile.delete();
                }
                myFileUrl = bundleRoot.getPath() + File.separator
                        + "live" + File.separator
                        + h.getHostname() + File.separator + languageId
                        + url.replace( "/", File.separator );
                contentletBundlerFile = new File( myFileUrl );
                if(contentletBundlerFile.exists()) {
                    contentletBundlerFile.delete();
                }
            }
            else if ( !contentletBundlerFile.exists() || contentletBundlerFile.lastModified() != cal.getTimeInMillis() || config.isIncremental() ) {

                Structure struc = contentlet.getStructure();
                String detailPageId = struc.getDetailPage();
                Identifier identifier = APILocator.getIdentifierAPI().find( detailPageId );

                /*
                 If the contentlet belongs to a Host we should use the detail page of that host, a item could be display different on each host
                 */
                IHTMLPage htmlPage = getDetailPage(contentlet, detailPageId, languageId, identifier, h);

                //Let's try with the DEFAULT Language.
                if( htmlPage == null ) {
                    htmlPage = getDetailPage(contentlet, detailPageId, langAPI.getDefaultLanguage().getId(), identifier, h);
                }

                //If it is still null, nothing we can do.
                if( htmlPage == null ) {
                    return;
                }

                try {

                    String pageString = APILocator.getHTMLPageAssetAPI().getHTML(htmlPage, true,
                        contentlet.getInode(), APILocator.getUserAPI().getSystemUser(),
                        languageId, getUserAgent(config));

                    if (!UtilMethods.isSet(pageString)){
                        //Let's try with the default Lang.
                        pageString = APILocator.getHTMLPageAssetAPI().getHTML(htmlPage, true,
                            contentlet.getInode(), APILocator.getUserAPI().getSystemUser(),
                            langAPI.getDefaultLanguage().getId(), getUserAgent(config));
                    }

                    String dir = myFileUrl.substring(0, myFileUrl.lastIndexOf(File.separator));
                    new File(dir).mkdirs();

                    if (contentletBundlerFile.exists()) {
                        contentletBundlerFile.delete();
                    }

                    writer = new BufferedWriter(new FileWriterWithEncoding(contentletBundlerFile, "UTF-8"));
                    writer.write(pageString);
                    writer.close();
                    contentletBundlerFile.setLastModified(cal.getTimeInMillis());
                    writer = null;

                }catch(DotContentletStateException e) {
                    System.out.println(e);
                    APILocator.getHTMLPageAssetAPI().getHTML(htmlPage, true, contentlet.getInode(),
                        APILocator.getUserAPI().getSystemUser(), languageId, getUserAgent(config));
                }
            }

            contentletBundlerFile = new File( contentletBundlerFile.getAbsolutePath() + FILE_ASSET_EXTENSION );
            if ( !contentletBundlerFile.exists() || contentletBundlerFile.lastModified() != cal.getTimeInMillis() ) {
                if ( contentletBundlerFile.exists() ) contentletBundlerFile.delete();
                BundlerUtil.objectToXML( wrap, contentletBundlerFile, true );
                contentletBundlerFile.setLastModified( cal.getTimeInMillis() );
            }

            // set the time of the file

        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Unable to generate Bundler file object for contentlet: " + contentlet.getInode(), e );
            throw new DotBundleException( "Unable to generate Bundler file object for contentlet: " + contentlet.getInode(), e );
        } finally {
            if ( writer != null ) {
                try {
                    writer.close();
                } catch ( Exception ex ) {
                    Logger.error( this.getClass(), "Error closing writer: " + ex.getMessage(), ex );
                }
            }
        }

    }

    private IHTMLPage getDetailPage(Contentlet contentlet, String detailPageId, Long languageId, Identifier identifier, Host h) throws DotDataException, DotSecurityException{
        /*
         If the contentlet belongs to a Host we should use the detail page of that host, a item could be display different on each host
         */
        IHTMLPage htmlPage = null;

        if ( contentlet.getHost().equals( "SYSTEM_HOST" ) ) {
            if(identifier.getAssetType().equals("contentlet")) {
                htmlPage = APILocator.getHTMLPageAssetAPI().fromContentlet(
                    APILocator.getContentletAPI().findContentletByIdentifier(detailPageId, true, languageId , APILocator.getUserAPI().getSystemUser(), true));
            }
        } else {
            if(identifier.getAssetType().equals("contentlet")) {
                htmlPage = APILocator.getHTMLPageAssetAPI().getPageByPath(identifier.getURI(), h, languageId, true);
            }
        }

        return  htmlPage;
    }

    /**
     * Validates if it is required to add the SYSTEM HOST to the list of current hosts.
     *
     * @param hosts
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private void validateHosts ( List<Host> hosts ) throws DotDataException, DotSecurityException {

        User systemUser = APILocator.getUserAPI().getSystemUser();

        //Verify if we already have the SYSTEM_HOST in the list of hosts
        Boolean addedSystemHost = false;
        for ( Host host : hosts ) {
            if ( host.getIdentifier().equals( "SYSTEM_HOST" ) ) {
                addedSystemHost = true;
                break;
            }
        }

        /*
         Content under the SYSTEM_HOST should be treated as content that actually lives on ALL hosts. So event if we have a query filtering
         by host we should add the SYSTEM_HOST as the content under it belongs as well to the specific host we are looking for...
         */
        if ( !addedSystemHost ) {
            //Search for the system host and add it to the current list
            Host systemHost = APILocator.getHostAPI().findSystemHost( systemUser, true );
            hosts.add( systemHost );
        }
    }

    @Override
    public FileFilter getFileFilter () {
        return new URLMapBundlerFilter();
    }

    public class URLMapBundlerFilter implements FileFilter {

        @Override
        public boolean accept ( File pathname ) {

            return (pathname.isDirectory() || pathname.getName().endsWith( FILE_ASSET_EXTENSION ));
        }

    }

}