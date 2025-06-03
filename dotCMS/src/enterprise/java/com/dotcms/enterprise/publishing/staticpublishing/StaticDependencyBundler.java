/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.staticpublishing;

import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.*;
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
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.Lists;
import com.liferay.portal.model.User;

import java.io.FileFilter;
import java.util.*;

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
            buildPathIncludes();
        } catch (Exception e) {
            throw new DotBundleException("Error generating StaticDependencyBundler:", e);
        }
    }


    private void buildPathIncludes() throws DotDataException, DotSecurityException {
        List<PublishQueueElement> assets = config.getAssets();
        List<String> includes = (config.getIncludePatterns() == null) ? new ArrayList<>() : config.getIncludePatterns();
        Set<Host> hosts = (config.getHosts() == null) ? new HashSet<>() : new HashSet<>(config.getHosts());
        Set<String> languages = config.getLanguages();
        Set<String> folders = config.getFolders();
        if (languages == null || languages.isEmpty()) {
            for (Language l : languageAPI.getLanguages()) {
                languages.add(l.getId() + "");
            }
        }

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
                	List<Contentlet> cons = contentletAPI.search("+identifier:"+asset.getAsset(), 0, 0, null, systemUser, false);

                    Folder folder = folderAPI.find(cons.get(0).getFolder(), systemUser, false);
                    if (folder != null) {
                        folders.add(folder.getIdentifier());
                    }

                	if (!cons.isEmpty() && cons.get(0).isFileAsset() || cons.get(0).isHTMLPage()) {
                        includes.add(id.getPath());
                    } else if (!cons.isEmpty() && UtilMethods.isSet(cons.get(0).getStructure().getUrlMapPattern())) {
                        String urlMap = contentletAPI.getUrlMapForContentlet(cons.get(0), systemUser, true);
                        if (urlMap != null) {
                            includes.add(urlMap);
                        }
                    }
                }
            }
        }

        config.setHosts(Lists.newArrayList(hosts));
        config.setIncludePatterns(includes);
        config.setLanguages(languages);
        config.setFolders(folders);
    }

    @Override
    public FileFilter getFileFilter() {
        return null;
    }
}
