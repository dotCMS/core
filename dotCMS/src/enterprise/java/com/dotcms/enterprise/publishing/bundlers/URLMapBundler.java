/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.bundlers;

import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.remote.bundler.ExtensionFileFilter;
import com.dotcms.publishing.*;
import com.dotcms.publishing.output.BundleOutput;
import com.dotcms.publishing.output.FileCreationException;
import com.dotcms.repackage.com.google.common.collect.Lists;
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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * This bundler will take the list of URL Maps and export them as XML files for:
 * <ul>
 *     <li>Site Search.</li>
 *     <li>Static Publishing, such as AWS S3.</li>
 * </ul>
 * <p>Notice that this Bundler is used by Site Search feature as well, but it's <b>NOT USED</b> by the Dynamic Push
 * Publishing mechanism.</p>
 *
 * @author Jorge Urdaneta
 * @since Jun 11, 2012
 */
public class URLMapBundler implements IBundler {

    private PublisherConfig config;
    private ContentletAPI capi = APILocator.getContentletAPI();
    private LanguageAPI langAPI = APILocator.getLanguageAPI();
    private HostAPI hostAPI = APILocator.getHostAPI();

    public final static String[] URLMAP_EXTENSIONS = {".dotUrlMap.xml", ".dotUrlMap.json"};

    @Override
    public String getName () {
        return "Static URL Map Bundler";
    }

    @Override
    public void setConfig(final PublisherConfig pc) {
        this.config = pc;
    }

    @Override
    public void setPublisher(final IPublisher publisher) {
    }

    @Override
    public void generate(final BundleOutput output, final BundlerStatus status) throws DotBundleException {

        if ( LicenseUtil.getLevel() < LicenseLevel.STANDARD.level ) {
            throw new RuntimeException( "Need an enterprise license to run this bundler" );
        }
        Logger.info(URLMapBundler.class,"Start bundler");
        try {
            //Get APIs
            final User systemUser = APILocator.getUserAPI().getSystemUser();

            final List<String> contentTypesToAdd = new ArrayList<>();
            if (!UtilMethods.isSet(this.config.getStructures())) {
            	// Ignore static-publishing over structures with empty include patterns (https://github.com/dotCMS/core/issues/10504)
            	if (!this.config.isStatic() || !this.config.getIncludePatterns().isEmpty()) {
	                // Search for all the Content Types with a URL Map Pattern
	                final List<SimpleStructureURLMap> urlMaps = StructureFactory.findStructureURLMapPatterns();
	                for (final SimpleStructureURLMap map : urlMaps) {
	                    contentTypesToAdd.add( map.getInode() );
	                }
            	}
            } else {
            	// Ignore static-publishing over user-selected structures (https://github.com/dotCMS/core/issues/10504)
            	if (!this.config.isStatic()) {
            		contentTypesToAdd.addAll(this.config.getStructures());
            	}
            }
            // if we have no urlmap Content Types...
            if (contentTypesToAdd.isEmpty()) {
                return;
            }

               StringBuilder luceneQuery = new StringBuilder();
            luceneQuery.append( "+(" );
            for (final String contentTypeId : contentTypesToAdd) {
                final Structure contentType = CacheLocator.getContentTypeCache().getStructureByInode( contentTypeId );
                luceneQuery.append( "structureName:").append(contentType.getVelocityVarName()).append(" " );
            }
            luceneQuery.append( ") " );

            if (UtilMethods.isSet(this.config.getExcludePatterns())) {
                luceneQuery.append( "-(" );
                for (final String pattern : config.getExcludePatterns()) {
                    if (UtilMethods.isSet(pattern)) {
                        // Adding double quotes for the query to work with URL Map paths containing blank spaces
                        luceneQuery.append("urlMap:").append(pattern.replace(" ", "\\ ")).append(" ");
                    }
                }
                luceneQuery.append( ") " );
            } else if (UtilMethods.isSet(this.config.getIncludePatterns())) {
                luceneQuery.append( "+(" );
                for (final String pattern : this.config.getIncludePatterns()) {
                    if (UtilMethods.isSet(pattern)) {
                        // Adding double quotes for the query to work with URL Map paths containing blank spaces
                        luceneQuery.append("urlMap:").append(pattern.replace(" ", "\\ ")).append(" ");
                    }
                }
                luceneQuery.append(") ");
            }

            if (this.config.isIncremental()) {
                Date start;
                Date end;
                if (this.config.getStartDate() != null) {
                    start = this.config.getStartDate();
                } else {
                    Calendar cal = Calendar.getInstance();
                    cal.set( Calendar.YEAR, 1900 );
                    start = cal.getTime();
                }
                end = this.config.getEndDate();
                
                // Check for modified detail pages
                final List<String> contentTypesWithModDetail = new ArrayList<>();
                final Set<String> modPages = new HashSet<>();
                
                if (!modPages.isEmpty()) {
                    for (final String contentTypeId : contentTypesToAdd) {
                        final Structure contentType = CacheLocator.getContentTypeCache().getStructureByInode(contentTypeId);
                        if (InodeUtils.isSet(contentType.getDetailPage()) && modPages.contains(contentType.getDetailPage())) {
                            contentTypesWithModDetail.add(contentTypeId);
                        }
                    }
                }

                if (!contentTypesWithModDetail.isEmpty()) {
                    luceneQuery.append(" +( ");
                    for (final String contentTypeId : contentTypesWithModDetail) {
                        final Structure contentType = CacheLocator.getContentTypeCache().getStructureByInode(contentTypeId);
                        luceneQuery.append("structureName:").append(contentType.getVelocityVarName()).append(" ");
                    }
                }

                luceneQuery.append(contentTypesWithModDetail.isEmpty() ? "+" : " ");
                luceneQuery.append( "versionTs:[").append(ESMappingAPIImpl.datetimeFormat.format( start ))
                        .append(" TO ").append(ESMappingAPIImpl.datetimeFormat.format( end )).append("] " );
                
                if (!contentTypesWithModDetail.isEmpty()) {
                    luceneQuery.append(" ) ");
                }
            }

            if (UtilMethods.isSet(this.config.getHosts())) {
                final List<Host> sitesInQuery = Lists.newArrayList(this.hostAPI.findSystemHost());
                for (final Host site : this.config.getHosts()) {
                    sitesInQuery.add(site);
                }

                // Sites query
                luceneQuery.append(" +(" );
                for (final Host site : sitesInQuery) {
                    luceneQuery.append( "conhost:").append(site.getIdentifier()).append(" " );
                }
                luceneQuery.append(" ) ");
            }
            Logger.info( this.getClass(), luceneQuery.toString() );

            for (final Long languageId : getSortedConfigLanguages(this.config, this.langAPI.getDefaultLanguage().getId())){
                processURLMaps(output, status, systemUser, luceneQuery.toString() + " +languageid:" + languageId + " ");
            }
        } catch (final Exception e) {
            status.addFailure();
        }
        Logger.info(URLMapBundler.class,"End bundler");
    }

    /**
     * Takes the URL Maps that will be added to the bundle and process them in order to determine which specific maps
     * will be added to the actual bundle.
     *
     * @param bundleOutput The location where bundle data will be added to the bundle.
     * @param status       The object that provides status information for the bundle.
     * @param systemUser
     * @param luceneQuery  The Lucene query that allows dotCMS to find and filter the URL Maps that will be added to the
     *                     bundle.
     */
    private void processURLMaps(final BundleOutput bundleOutput, final BundlerStatus status, final User systemUser, final String luceneQuery) {
        final int limit = 200;
        int page = 0;
        status.setTotal( 0 );
        final String bobLive = luceneQuery.toString() + "+live:true" ;
        final String bobWorking = luceneQuery.toString() + "+working:true";
        final String bobDeleted = luceneQuery.toString() + "+deleted:true";

        while ( true ) {
            final List<Contentlet> searchResults = new ArrayList<>();
            try {
                searchResults.addAll(this.capi.search( bobLive, limit, page * limit, "inode", systemUser, true ) );
            } catch (final Exception e) {
                Logger.debug( FileAssetBundler.class, e.getMessage(), e );
            }
            try {
                if (!this.config.liveOnly() || this.config.isIncremental() || this.config.isSameIndexNotIncremental()) {
                    searchResults.addAll(this.capi.search( bobWorking, limit, page * limit, "inode", systemUser, true ) );
                    searchResults.addAll(this.capi.search( bobDeleted, limit, page * limit, "inode", systemUser, true ) );
                }
            } catch (final Exception e) {
                Logger.debug( FileAssetBundler.class, e.getMessage(), e);
            }
            page++;
            // no more content
            if (searchResults.isEmpty() || page > 1000000) {
                break;
            }
            status.setTotal( status.getTotal() + searchResults.size() );

            for (final Contentlet contentlet : searchResults) {
                try {
                    writeURLMap(bundleOutput, contentlet );
                    status.addCount();
                } catch (final Exception e) {
                    Logger.warn(this.getClass(), String.format("An error occurred when processing writing of URL Map " +
                            "for content with with ID '%s': %s", contentlet.getIdentifier(), e.getMessage()));
                    status.addFailure();
                }
            }
        }
    }

    /**
     * Writes the specified URL Map from the {@link Contentlet} object to the bundle contents pointed by the {@link
     * BundleOutput}.
     *
     * @param bundleOutput The location where bundle data is being written.
     * @param contentlet   The Contentlet containing the URL Map that will be written to the bundle.
     *
     * @throws DotBundleException An error occurred with the writing process.
     */
    private void writeURLMap(final BundleOutput bundleOutput, final Contentlet contentlet) throws DotBundleException{
        try {
            Host site = this.hostAPI.find( contentlet.getHost(), APILocator.getUserAPI().getSystemUser(), true );
            String url = this.capi.getUrlMapForContentlet( contentlet, APILocator.getUserAPI().getSystemUser(), true );
            if (!UtilMethods.isSet(url)) {
                throw new DotBundleException( "contentlet:" + contentlet.getInode() + " does not have a urlmap" );
            }
            final Optional<ContentletVersionInfo> info = APILocator.getVersionableAPI()
                    .getContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId());

            if (info.isEmpty()) {
                throw new DotDataException("Can't find ContentletVersionInfo for Identifier: "
                        + contentlet.getIdentifier() + ". Lang: " + contentlet.getLanguageId());
            }

            final URLMapWrapper wrap = new URLMapWrapper();
            wrap.setInfo(info.get());
            wrap.setContent( contentlet );
            wrap.setId( APILocator.getIdentifierAPI().find( contentlet.getIdentifier() ) );

            if (Config.getBooleanProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", false) &&
                contentlet.getLanguageId() == this.langAPI.getDefaultLanguage().getId() &&
                !this.config.getLanguages().isEmpty()) {
                for (final String languageId : this.config.getLanguages()) {
                    writeFileToDisk(bundleOutput, contentlet, Long.valueOf(languageId), wrap, site, url);
                }
            } else {
                writeFileToDisk(bundleOutput, contentlet, contentlet.getLanguageId(), wrap, site, url);
            }
        } catch (final Exception e) {
            throw new DotBundleException(String.format("An error occurred when writing URL Map from Content with ID " +
                    "'%s' to disk: %s", contentlet.getIdentifier(), e.getMessage()), e);
        }
    }

    private void writeFileToDisk (BundleOutput bundleOutput, Contentlet contentlet, Long languageId, URLMapWrapper wrap, Host h, String url)
        throws IOException, DotBundleException, DotDataException, DotSecurityException {


        if (Host.SYSTEM_HOST.equals(h.getIdentifier())){

          // system host means all hosts
          List<Host> allHosts = hostAPI.findAll(APILocator.getUserAPI().getSystemUser(), false);

            for (Host host : allHosts) {
                if (!Host.SYSTEM_HOST.equals(host.getIdentifier()) && !host.isArchived()){
                    writeFileToDiskByHost(bundleOutput, contentlet, languageId, wrap, host, url);
                }
            }
        } else {
            writeFileToDiskByHost(bundleOutput, contentlet, languageId, wrap, h, url);
        }
    }

    /**
     * Generates and writes into a file under the bundler folder the content of a detail html page for a given Contentlet
     *
     * @param bundleOutput
     * @param contentlet
     * @throws IOException
     * @throws DotBundleException
     */
    private void writeFileToDiskByHost (
            final BundleOutput bundleOutput,
            final Contentlet contentlet,
            final Long languageId,
            final URLMapWrapper wrap,
            final Host h,
            final String url) throws IOException, DotBundleException {

        if ( contentlet == null ) {
            throw new DotBundleException( "null contentlet passed in " );
        }
        Calendar cal = Calendar.getInstance();

        try {
            String liveWorking = (contentlet.getInode().equals( wrap.getInfo().getLiveInode() )) ? "live" : "working";

            String myFileUrl = File.separator
                    + liveWorking + File.separator
                    + h.getHostname() + File.separator + languageId
                    + url.replace( "/", File.separator );
            String contentletBundlerFilePath = myFileUrl;

            // Should we write or is the file already there:

            cal.setTime( wrap.getInfo().getVersionTs() );
            cal.set( Calendar.MILLISECOND, 0 );

            if(config.liveOnly() && config.isIncremental() && !contentlet.isLive()) {
                if(bundleOutput.exists(contentletBundlerFilePath)) {
                    bundleOutput.delete(contentletBundlerFilePath);
                }

                myFileUrl = File.separator
                        + "live" + File.separator
                        + h.getHostname() + File.separator + languageId
                        + url.replace( "/", File.separator );
                contentletBundlerFilePath = myFileUrl;
                if(bundleOutput.exists(contentletBundlerFilePath)) {
                    bundleOutput.delete(contentletBundlerFilePath);
                }
            }
            else if ( !bundleOutput.exists(contentletBundlerFilePath) || bundleOutput.lastModified(contentletBundlerFilePath) != cal.getTimeInMillis() || config.isIncremental() ) {

                Structure struc = contentlet.getStructure();
                String detailPageId = struc.getDetailPage();
                Identifier identifier = APILocator.getIdentifierAPI().find( detailPageId );

                if (null == identifier || !UtilMethods.isSet(identifier.getId())) {
                    throw new DotDataException(String.format(
                            "Detail page [%s] for Content Type [%s] [%s] not found, please verify the Content Type and "
                                    + "the detail page is properly configured.", detailPageId,
                            struc.id(), struc.getName()));
                }

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

                    if(bundleOutput.exists(contentletBundlerFilePath)) {
                        bundleOutput.delete(contentletBundlerFilePath);
                    }

                    try(final OutputStream outputStream = bundleOutput.addFile(contentletBundlerFilePath)) {
                        outputStream.write(pageString.getBytes());
                    }

                    bundleOutput.setLastModified(contentletBundlerFilePath, cal.getTimeInMillis());
                }catch(DotContentletStateException e) {
                    Logger.error(this.getClass(), e);
                    APILocator.getHTMLPageAssetAPI().getHTML(htmlPage, true, contentlet.getInode(),
                        APILocator.getUserAPI().getSystemUser(), languageId, getUserAgent(config));
                }
            }

            for (String extension : URLMAP_EXTENSIONS) {
                contentletBundlerFilePath = contentletBundlerFilePath + extension;
                if (!bundleOutput.exists(contentletBundlerFilePath)
                        || bundleOutput.lastModified(contentletBundlerFilePath) != cal.getTimeInMillis()) {
                    if (bundleOutput.exists(contentletBundlerFilePath)) {
                        bundleOutput.delete(contentletBundlerFilePath);
                    }

                    try (final OutputStream outputStream = bundleOutput.addFile(contentletBundlerFilePath)) {
                        BundlerUtil.writeObject(wrap, outputStream, contentletBundlerFilePath);
                        bundleOutput.setLastModified(contentletBundlerFilePath, cal.getTimeInMillis());
                    }
                }
            }

            // set the time of the file

        } catch (FileCreationException e) {
            Logger.warn(URLMapBundler.class, () -> e.getMessage());
        } catch ( Exception e ) {
            Logger.error( this.getClass(), "Unable to generate Bundler file object for contentlet: " + contentlet.getInode(), e );
            throw new DotBundleException( "Unable to generate Bundler file object for contentlet: " + contentlet.getInode(), e );
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

    @Override
    public FileFilter getFileFilter () {
        return new ExtensionFileFilter(URLMAP_EXTENSIONS);
    }



}
