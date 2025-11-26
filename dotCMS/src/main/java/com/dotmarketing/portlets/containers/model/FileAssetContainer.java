package com.dotmarketing.portlets.containers.model;


import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Source;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vavr.control.Try;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This is a {@link Container} plus a list of {@link FileAsset}
 * This is a representation of the Container based on a file assets in the file system instead of the db.
 * @author jsanca
 */
public class FileAssetContainer extends Container {

    @JsonIgnore
    private final Map<String, Object> metaDataMap;



    private long languageId;
    private Host host;
    private String path;

    public FileAssetContainer() {

        this.source = Source.FILE;
        this.metaDataMap = new HashMap<>();
    }

    @JsonIgnore
    private String postLoopAsset = null;

    @JsonIgnore
    private transient FileAsset defaultContainerLayoutAsset = null;

    @JsonIgnore
    public FileAsset getPostLoopAsset() {
        return loadAsset(postLoopAsset);
    }

    @JsonIgnore
    public FileAsset getDefaultContainerLayoutAsset() {
        return defaultContainerLayoutAsset;
    }

    public void setPostLoopAsset(final FileAsset postLoopAsset) {
        this.postLoopAsset = postLoopAsset.getInode();
    }

    public void setDefaultContainerLayoutAsset(final FileAsset defaultContainerLayoutAsset) {
        this.defaultContainerLayoutAsset = defaultContainerLayoutAsset;
    }


    @JsonIgnore
    private String preLoopAsset = null;

    @JsonIgnore
    public FileAsset getPreLoopAsset() {
       return loadAsset(preLoopAsset);
    }

    private FileAsset loadAsset(String inode) {
        return Try.of(()->APILocator.getFileAssetAPI().find(inode, APILocator.systemUser(), false)).getOrNull();
    }
    
    
    public void setPreLoopAsset(final FileAsset preLoopAsset) {
        this.preLoopAsset = preLoopAsset.getInode();
    }

    /////
    @JsonIgnore
    private List<String> containerStructuresAssets = Collections.emptyList();

    @JsonIgnore
    public List<FileAsset> getContainerStructuresAssets() {
        return containerStructuresAssets.stream().map(s->loadAsset(s)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public void setContainerStructuresAssets(List<FileAsset> containerStructuresAssets) {
        this.containerStructuresAssets = containerStructuresAssets.stream().map(f->f.getInode()).filter(Objects::nonNull).collect(Collectors.toList());
    }

    // we override it, in order to do the permissionable behind a contentlet object
    @Override
    public String getPermissionType() {
        return Contentlet.class.getCanonicalName();
    }

    public void addMetaData(final String key, final Object value) {

        this.metaDataMap.put (key, value);
    }

    @JsonIgnore
    public Map<String, Object> getMetaDataMap() {
        return metaDataMap;
    }

    @Override
    public boolean isLocked() throws DotStateException, DotDataException, DotSecurityException {
        return APILocator.getVersionableAPI().isLocked(toContentlet());
    }

    @Override
    public boolean isDeleted() throws DotStateException, DotDataException, DotSecurityException {
        return APILocator.getVersionableAPI().isDeleted(toContentlet());
    }

    @Override
    public boolean isLive() throws DotStateException, DotDataException, DotSecurityException {
        return APILocator.getVersionableAPI().isLive(toContentlet());
    }

    @Override
    public boolean isWorking() throws DotStateException, DotDataException, DotSecurityException {
        return APILocator.getVersionableAPI().isWorking(toContentlet());
    }

    @Override
    public boolean hasLiveVersion() throws DotStateException, DotDataException {
        return APILocator.getVersionableAPI().hasLiveVersion(toContentlet());
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    private Versionable toContentlet() {
        final Contentlet contentlet =  new Contentlet();
        contentlet.setIdentifier(this.identifier);
        contentlet.setInode(this.inode);
        contentlet.setLanguageId(this.languageId);
        return contentlet;
    }

    public void setLanguage(final long languageId) {
        this.languageId = languageId;
    }

    public long getLanguageId() {
        return languageId;
    }

    public void setHost(final Host host) {
        this.host = host;
    }

    @JsonIgnore
    public Host getHost() {
        return host;
    }

    public String getHostId() {
        return host.getIdentifier();
    }
    public String getHostName() {
        return host.getHostname();
    }
    

    @Override
    @JsonIgnore
    public ManifestInfo getManifestInfo(){
        return ManifestInfoBuilder.merge(super.getManifestInfo(),
                new ManifestInfoBuilder()
                    .site(this.getHost())
                    .path(this.getPath())
                    .build());
    }
}
