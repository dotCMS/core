/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.sitesearch;

import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.bundlers.FileAssetBundler;
import com.dotcms.enterprise.publishing.bundlers.FileAssetWrapper;
import com.dotcms.enterprise.publishing.bundlers.HTMLPageAsContentBundler;
import com.dotcms.enterprise.publishing.bundlers.HTMLPageAsContentWrapper;
import com.dotcms.enterprise.publishing.bundlers.URLMapBundler;
import com.dotcms.enterprise.publishing.bundlers.URLMapWrapper;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.storage.FileMetadataAPI;
import com.dotcms.storage.model.Metadata;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.liferay.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class ESSiteSearchPublisher extends Publisher {

    private final ESIndexAPI esIndexAPI;
    private final SiteSearchAPI siteSearchAPI;
    private final HostAPI hostAPI;
    private final FileAssetAPI fileAssetAPI;
    private final FileMetadataAPI fileMetadataAPI;
    private final UserAPI userAPI;

    public ESSiteSearchPublisher() {
        this(APILocator.getESIndexAPI(), APILocator.getSiteSearchAPI(),
             APILocator.getHostAPI(), APILocator.getFileAssetAPI(),
             APILocator.getFileMetadataAPI(), APILocator.getUserAPI());
    }

    @VisibleForTesting
    public ESSiteSearchPublisher(
            final ESIndexAPI esIndexAPI,
            final SiteSearchAPI siteSearchAPI,
            final HostAPI hostAPI,
            final FileAssetAPI fileAssetAPI,
            final FileMetadataAPI fileMetadataAPI,
            final UserAPI userAPI) {
        this.esIndexAPI = esIndexAPI;
        this.siteSearchAPI = siteSearchAPI;
        this.hostAPI = hostAPI;
        this.fileAssetAPI = fileAssetAPI;
        this.fileMetadataAPI = fileMetadataAPI;
        this.userAPI = userAPI;
    }

    private SiteSearchConfig getSiteSearchConfig() {
        return (SiteSearchConfig) config;
    }

    @Override
    public PublisherConfig init(PublisherConfig config) throws DotPublishingException {
        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            throw new RuntimeException("need an enterprise licence to run this.");
        }
        this.config = super.init(config);
        final SiteSearchConfig myConf = (SiteSearchConfig) config;
        this.config = myConf;
        if (myConf.isIncremental()) {
            if (UtilMethods.isNotSet(myConf.getIndexName())) {
                throw new DotPublishingException(
                        " For incremental mode You must either specify a valid site search index in your PublishingConfig or have current active site search index. "
                                + "Make sure you have a current active site search index by going to the site search admin portlet and creating one");
            }
        } else {
            if (UtilMethods.isNotSet(myConf.getIndexName()) && UtilMethods
                    .isNotSet(myConf.getNewIndexName())) {
                throw new DotPublishingException(
                        "For non-incremental mode You must either specify a valid site search index and the new replacement index in your PublishingConfig.");
            }
        }
        return this.config;

    }

    @Override
    public PublisherConfig process(final PublishStatus status) throws DotPublishingException {

        if (LicenseUtil.getLevel() < LicenseLevel.STANDARD.level) {
            throw new RuntimeException("need an enterprise licence to run this.");
        }
        final SiteSearchConfig config = getSiteSearchConfig();
        try {
            final File bundleRoot = BundlerUtil.getBundleRoot(config);
            for (final Class<? extends IBundler> clazz : getBundlers()) {
                final IBundler bundler = clazz.getDeclaredConstructor().newInstance();
                final List<File> files = FileUtil
                        .listFilesRecursively(bundleRoot, bundler.getFileFilter());

                final List<File> filteredFiles = files.stream().filter(file -> {
                    boolean process = false;
                    try {
                        process = shouldProcess(file);
                    } catch (IOException | DotPublishingException e) {
                        Logger.error(ESSiteSearchPublisher.class, e);
                    }

                    if (process) {
                        Logger.debug(this, () -> "######### processing: " + file);
                    } else {
                        Logger.debug(this, () -> "######### skipping  : " + file);
                    }
                    return process;
                }).sorted(new FileDateSortComparator()).collect(Collectors.toList());

                final BiFunction<File, String, Boolean> processFunction = processors.get(clazz);
                final String targetIndexName = config.isIncremental() ? config.getIndexName() : config.getNewIndexName();
                final List<List<File>> partitions = Lists.partition(filteredFiles, 10);
                for (final List<File> partition : partitions) {
                    partition.forEach(file -> {
                        if (processFunction.apply(file,   targetIndexName)) {
                            status.addCurrentProgress();
                        }
                    });
                }
            }

            if (!config.isIncremental()) {
                switchIndex(config);
            }
            return config;

        } catch (Exception e) {
            Logger.error(this.getClass(), e.getMessage(), e);
            throw new DotPublishingException(e.getMessage(), e);
        }
    }

    private void switchIndex(final SiteSearchConfig config) throws DotDataException {
        if (config.switchIndexWhenDone()) {
            final String oldIndexName = config.getIndexName();
            final String newIndex = config.getNewIndexName();

            // there is the previous search index.
            if (null != oldIndexName) {
                //Now if there was a previous index that we need to replace
                //We make the new one the default only if the previous one was the default.
                if (siteSearchAPI.isDefaultIndex(oldIndexName)) {
                    Logger.info(this.getClass(), String.format(
                            "New index `%s` will be activated and set as the new DEFAULT.",
                            newIndex));
                    //This does make it the default.
                    siteSearchAPI.activateIndex(newIndex);
                }
                Logger.info(
                   ESSiteSearchPublisher.class, String.format("New index is `%s`. Old index `%s` will be removed.", newIndex, oldIndexName)
                );

                final String alias = config.getIndexAlias();
                esIndexAPI.delete(oldIndexName);

                if (null != alias) {
                    siteSearchAPI.setAlias(newIndex, alias);
                } else {
                    Logger.warn(
                        ESSiteSearchPublisher.class, String.format(
                        "Unable to get old Index alias New index `%s` will no longer have an alias assigned.",
                        newIndex)
                    );
                }

            }
            //Always verify if there isn't a DEFAULT index.
            setDefaultIndexIfNone(newIndex);
        }
    }

    /**
     * If there isn't another index marked as the DEFAULT We'll attempt to make the current one.
     * @param newIndex new index.
     */
    private void setDefaultIndexIfNone(final String newIndex) {
        final List<String> indices = siteSearchAPI.listIndices();
        if (indices.contains(newIndex) && indices.stream().noneMatch(s -> {
            try {
                return siteSearchAPI.isDefaultIndex(s);
            } catch (DotDataException e) {
                Logger.error(ESSiteSearchPublisher.class, e);
            }
            return false;
        })) {
            //If no default index is set at the moment then we attempt marking this one.
            try {
                siteSearchAPI.activateIndex(newIndex);
            } catch (DotDataException e) {
                Logger.error(ESSiteSearchPublisher.class, String.format("Exception trying to DEFAULT index with named `%s`.", newIndex),  e);
            }
        }
    }

    //These Functions allow me to group them within a map and get rid of the nested if instanceOf anti-pattern.

    private BiFunction<File, String, Boolean> processFileAsset = this::processFileObject;

    private BiFunction<File, String, Boolean> processUrlMap = this::processUrlMap;

    private BiFunction<File, String, Boolean> processHTMLPageAsContent = this::processHTMLPageAsContent;

    private Map<Class<? extends IBundler>, BiFunction<File, String, Boolean>> processors = ImmutableMap.of(
            FileAssetBundler.class, processFileAsset,
            URLMapBundler.class, processUrlMap,
            HTMLPageAsContentBundler.class,
            processHTMLPageAsContent
    );

    private boolean processUrlMap(final File file, final String indexName)  {
        String docId = null;
        try {
            final URLMapWrapper wrap = (URLMapWrapper) BundlerUtil.xmlToObject(file);
            if (wrap == null) {
                return false;
            }

            final File htmlFile = new File(
                    file.getAbsolutePath().replaceAll(URLMapBundler.FILE_ASSET_EXTENSION, ""));

            docId = wrap.getId().getId() + "_" + config.getLanguage();;

            // is the live guy
            if (UtilMethods.isSet(wrap.getInfo().getLiveInode()) && wrap.getInfo().getLiveInode()
                    .equals(wrap.getContent().getInode())) {
                final Metadata meta = fileMetadataAPI.getFullMetadataNoCache(htmlFile,
                        wrap::getContent);

                if (null != meta && null != meta.getFieldsMeta()) {
                    final Map<String, Serializable> map = new HashMap<>(meta.getFieldsMeta());
                    if (map.get("content") != null) {
                        final String content = (String)map.get("content");
                        map.put("content", content.replaceAll("\\s+", " "));
                    }

                    if (!UtilMethods.isSet(map.get("title")) && UtilMethods
                            .isSet(wrap.getContent().getTitle())) {
                        map.put("title", wrap.getContent().getTitle());
                    }

                    final SiteSearchResult res = new SiteSearchResult(map);

                    // map contains [fileSize, content, author, title, keywords,
                    // description, contentType, contentEncoding]

                    res.setContentLength(htmlFile.length());
                    res.setLanguage(wrap.getContent().getLanguageId());
                    res.setMimeType((String)map.get("contentType"));
                    res.setFileName(htmlFile.getName().replaceAll(".dotUrlMap", ""));

                    res.setModified(wrap.getInfo().getVersionTs());
                    res.setUri(getUriFromFilePath(htmlFile));
                    res.setUrl(getUrlFromFilePath(htmlFile));
                    res.setHost(getHostFromFilePath(htmlFile).getIdentifier());
                    res.setId(docId);

                    siteSearchAPI.putToIndex(indexName, res, "URLMap");
                }

                // if this is the deleted guy
            } else if (!UtilMethods.isSet(wrap.getInfo().getLiveInode())) {
                siteSearchAPI.deleteFromIndex(indexName, docId);
            }

        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "site search  failed: " + docId + " error" + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean processFileObject(final File file, final String indexName)  {

        // Logger.info(this.getClass(), "processing: " +
        // file.getAbsolutePath());

        FileAssetWrapper wrap = (FileAssetWrapper) BundlerUtil.xmlToObject(file);
        if (wrap == null) {
            return false;
        }
        final FileAsset asset = wrap.getAsset();

        final String docId = wrap.getId().getId();

        // is the live guy
        if (UtilMethods.isSet(wrap.getInfo().getLiveInode()) && !wrap.getInfo().isDeleted()
                && wrap.getInfo().getLiveInode().equals(wrap.getAsset().getInode())) {
            try {
                    final SiteSearchResult res = new SiteSearchResult();
                    res.setContentLength(asset.getFileSize());
                    res.setHost(asset.getHost());

                    final Host host = hostAPI.find(asset.getHost(), userAPI.getSystemUser(), true);
                    res.setUri(asset.getPath() + asset.getFileName());
                    res.setUrl(host.getHostname() + res.getUri());
                    res.setFileName(asset.getFileName());
                    res.setTitle(asset.getTitle());
                    res.setMimeType(fileAssetAPI
                            .getMimeType(asset.getUnderlyingFileName()));
                    res.setModified(asset.getModDate());
                    res.setModified(wrap.getInfo().getVersionTs());

                    res.setLanguage(asset.getLanguageId());

                    res.setId(docId);

                    final Map<String, Serializable> metaData = asset.getMetaDataMap();
                    if (metaData != null) {

                        res.setAuthor((String) metaData.get("author"));
                        res.setDescription((String) metaData.get("description"));

                        if (UtilMethods.isSet(metaData.get("content"))) {
                            res.setContent((String) metaData.get("content"));
                        } else {
                            final Metadata metadataNoCache = fileMetadataAPI
                                    .getFullMetadataNoCache(asset, FileAssetAPI.BINARY_FIELD);
                            final Map<String, Serializable> meta = metadataNoCache.getFieldsMeta();
                            if (null != meta) {
                                final String contentData = (String) meta.get("content");
                                res.setContent(contentData);
                            }
                        }
                    }

                    siteSearchAPI.putToIndex(indexName, res, "FileObject");

            } catch (Exception e) {
                Logger.error(this.getClass(), "site search indexPut failed: " + e.getMessage());
                return false;
            }
            // if we need to delete
        } else {
            siteSearchAPI.deleteFromIndex(indexName, docId);
        }

        return true;
    }

    private boolean processHTMLPageAsContent(final File file, final String indexName) {
        String docId = null;
        try {
            final HTMLPageAsContentWrapper wrap = (HTMLPageAsContentWrapper) BundlerUtil.xmlToObject(file);
            if (wrap == null) {
                return false;
            }

            final File htmlFile = new File(file.getAbsolutePath()
                    .replaceAll(HTMLPageAsContentBundler.HTMLPAGE_ASSET_EXTENSION, ""));

            final IHTMLPage page = wrap.getAsset();

            final Host host = APILocator.getHostAPI()
                    .find(wrap.getId().getHostId(), userAPI.getSystemUser(), true);
            docId = wrap.getId().getId() + "_" + config.getLanguage();

            // is the live guy
            if (UtilMethods.isSet(wrap.getInfo().getLiveInode()) && !wrap.getInfo().isDeleted()
                    && wrap.getInfo().getLiveInode().equals(wrap.getAsset().getInode())) {

                final Metadata meta = fileMetadataAPI.getFullMetadataNoCache(htmlFile,
                        ()-> (HTMLPageAsset) page);

                if (null != meta && null != meta.getFieldsMeta()) {
                    final Map<String, Serializable> map = new HashMap<>(meta.getFieldsMeta());
                    if (map.get("content") != null) {
                        final String content = (String)map.get("content");
                        map.put("content", content.replaceAll("\\s+", " "));
                    }
                    SiteSearchResult res = new SiteSearchResult(map);
                    res.setLanguage(((Contentlet) page).getLanguageId());
                    // map contains [fileSize, content, author, title, keywords,
                    // description, contentType, contentEncoding]
                    res.setTitle(page.getTitle());

                    res.setContentLength(htmlFile.length());
                    res.setMimeType((String)map.get("contentType"));
                    res.setFileName(htmlFile.getName().replaceAll(".dotUrlMap", ""));
                    res.setModified(wrap.getInfo().getVersionTs());

                    res.setHost(host.getIdentifier());

                    res.setUri(getUriFromFilePath(htmlFile));
                    res.setUrl(getUrlFromFilePath(htmlFile));

                    res.setId(docId);

                    siteSearchAPI.putToIndex(indexName, res, "HTMLPageAsContent");
                }

                // if this is the deleted guy
            } else if (!UtilMethods.isSet(wrap.getInfo().getLiveInode())) {
                siteSearchAPI.deleteFromIndex(indexName, docId);
            }

        } catch (Exception e) {
            Logger.error(this.getClass(),
                    "site search  failed: " + docId + " error" + e.getMessage());
            return false;
        }
      return true;
    }

    @Override
    public List<Class> getBundlers() {
        return ImmutableList
                .of(FileAssetBundler.class, URLMapBundler.class, HTMLPageAsContentBundler.class);
    }

    private static class FileDateSortComparator implements Comparator<File> {
        @Override
        public int compare(final File o1, final File o2) {
            if (o1 == null || o2 == null) {
                return 0;
            } else {
                return Long.compare(o1.lastModified(), o2.lastModified());
            }
        }
    }

}
