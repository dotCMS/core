/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.staticpublishing;

import com.dotcms.enterprise.publishing.bundlers.StaticUnpublishMarker;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.IPublisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.Lists;
import com.liferay.portal.model.User;

import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class StaticDependencyBundler implements IBundler {

    PublisherConfig config;
    LanguageAPI languageAPI;
    HostAPI hostAPI;
    UserAPI userAPI;
    FolderAPI folderAPI;
    IdentifierAPI identifierAPI;
    ContentletAPI contentletAPI;

    User systemUser;

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void setConfig(PublisherConfig publisherConfig) {
        config = publisherConfig;

        //Setting up APIs.
        languageAPI = APILocator.getLanguageAPI();
        hostAPI = APILocator.getHostAPI();
        userAPI = APILocator.getUserAPI();
        folderAPI = APILocator.getFolderAPI();
        identifierAPI = APILocator.getIdentifierAPI();
        contentletAPI = APILocator.getContentletAPI();
    }

    @Override
    public void setPublisher(IPublisher publisher) {
    }

    @Override
    public void generate(BundleOutput output, BundlerStatus status) throws DotBundleException {
        try {
            systemUser = userAPI.getSystemUser();
            buildPathIncludes(output);
        } catch (Exception e) {
            throw new DotBundleException("Error generating StaticDependencyBundler:", e);
        }
    }


    private void buildPathIncludes(final BundleOutput output)
            throws DotDataException, DotSecurityException, IOException {
        List<PublishQueueElement> assets = config.getAssets();
        List<String> includes = (config.getIncludePatterns() == null) ? new ArrayList<>() : config.getIncludePatterns();
        Set<Host> hosts = (config.getHosts() == null) ? new HashSet<>() : new HashSet<>(config.getHosts());
        Set<String> languages = config.getLanguages();
        Set<String> folders = config.getFolders();
        if (languages == null || languages.isEmpty()) {
            languages = new HashSet<>();
            for (Language l : languageAPI.getLanguages()) {
                languages.add(String.valueOf(l.getId()));
            }
        }

        // On un-publish, this bundler is the single place that knows the actual content being
        // removed (the publish queue deltas) and resolves its path even when no live version
        // remains. It writes /live/ path markers that the static publishers turn into endpoint
        // deletions. The content bundlers only export live content, so they cannot do this. See
        // issue #35365.
        final boolean staticUnpublish = StaticUnpublishMarker.isStaticUnpublish(config);

        for (PublishQueueElement asset : assets) {
            //If Asset is HOST/SITE
            if ( asset.getType().equals(PusheableAsset.SITE.getType()) ){
                Host h = hostAPI.find(asset.getAsset(), systemUser, false);
                if (null != h) {
                    hosts.add(h);
                    includes.add("/*");
                } else {
                    continue;
                }
                //If Asset is FOLDER
            } else if (asset.getType().equals(PusheableAsset.FOLDER.getType())) {
                Folder folder = folderAPI.find(asset.getAsset(), systemUser, false);
                Host h = hostAPI.find(folder.getHostId(), systemUser, false);
                hosts.add(h);

                includes.add(folder.getPath() + "*");
                folders.add(folder.getIdentifier());
                //If Asset is CONTENTLET
            } else{
                // everything care about is going to have an id
                Identifier id = identifierAPI.find(asset.getAsset());
                if (id == null){
                    continue;
                }

                Host h = hostAPI.find(id.getHostId(), systemUser, false);
                if ( (asset.getType().equals(PusheableAsset.CONTENTLET.getType())
                    || asset.getType().equals(PusheableAsset.SITE.getType()) )
                    && h != null
                    && !h.equals(hostAPI.findSystemHost())) {
                    hosts.add(h);
                }

                if (asset.getType().equals(PusheableAsset.CONTENTLET.getType())) {
                    // Guard the identifier before building a Lucene query with it (avoids malformed
                    // queries / unintended matches), mirroring AWSS3Publisher.
                    if (!UUIDUtil.isUUID(asset.getAsset())) {
                        continue;
                    }
                    List<Contentlet> cons = contentletAPI.search("+identifier:\"" + asset.getAsset() + "\"",
                            0, 0, null, systemUser, false);
                    // No version found in the index (e.g. fully archived/deleted content): nothing to
                    // resolve a path or languages from, so skip rather than crash on cons.get(0).
                    if (cons.isEmpty()) {
                        continue;
                    }

                    final Contentlet contentlet = cons.get(0);
                    Folder folder = folderAPI.find(contentlet.getFolder(), systemUser, false);
                    if (folder != null) {
                        folders.add(folder.getIdentifier());
                    }

                    String contentPath = null;
                	if (contentlet.isFileAsset() || contentlet.isHTMLPage()) {
                        contentPath = id.getPath();
                        includes.add(contentPath);
                    } else if (UtilMethods.isSet(contentlet.getStructure().getUrlMapPattern())) {
                        String urlMap = contentletAPI.getUrlMapForContentlet(contentlet, systemUser, true);
                        if (urlMap != null) {
                            contentPath = urlMap;
                            includes.add(urlMap);
                        }
                    }

                    if (staticUnpublish && contentPath != null && h != null) {
                        // A marker is only needed for languages where the content has NO live
                        // version: live content is rendered by the content bundlers and removed by
                        // the publisher's delete branch, so it needs no marker. The publish queue
                        // does not track the asset language (PublisherAPIImpl hardcodes
                        // language_id=1), so derive the languages from the content's own versions
                        // rather than the bundle languages. See issue #35365.
                        StaticUnpublishMarker.writeContentMarkers(config, output, h.getHostname(),
                                nonLiveLanguages(id.getId()), contentPath);
                    }
                }
            }
        }

        config.setHosts(Lists.newArrayList(hosts));
        config.setIncludePatterns(includes);
        config.setLanguages(languages);
        config.setFolders(folders);
    }

    /**
     * Resolves the languages (as ids) in which the content has NO live version — the only languages
     * that need an un-publish marker. Languages where a live version exists are rendered by the
     * content bundlers and removed by the publisher's delete branch, so they are excluded.
     *
     * <p>Uses a single cache-backed {@code findContentletVersionInfos(identifier)} lookup that
     * returns every language version at once, rather than one query per language.</p>
     *
     * @param identifierId the content identifier
     * @return the language ids (as strings) with no live version
     */
    private Collection<String> nonLiveLanguages(final String identifierId) throws DotDataException {
        final Set<String> nonLive = new LinkedHashSet<>();
        for (final ContentletVersionInfo versionInfo :
                APILocator.getVersionableAPI().findContentletVersionInfos(identifierId)) {
            if (!UtilMethods.isSet(versionInfo.getLiveInode())) {
                nonLive.add(String.valueOf(versionInfo.getLang()));
            }
        }
        return nonLive;
    }

    @Override
    public FileFilter getFileFilter() {
        return null;
    }
}
