package com.dotmarketing.portlets.containers.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import com.dotmarketing.beans.Source;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vavr.control.Try;

/**
 * This is a {@link Container} plus a list of {@link FileAsset}
 * This is a representation of the Container based on a file assets in the file system instead of the db.
 * @author jsanca
 */
public class FileAssetContainer extends Container {

    @JsonIgnore
    private final Map<String, Object> metaDataMap;



    private long languageId;

    private String path;

    public FileAssetContainer() {

        this.source = Source.FILE;
        this.metaDataMap = new HashMap<>();
    }

    @JsonIgnore
    private String postLoopAsset = null;

    @JsonIgnore
    public FileAsset getPostLoopAsset() {
        return loadAsset(postLoopAsset);
    }

    public void setPostLoopAsset(final FileAsset postLoopAsset) {
        this.postLoopAsset = postLoopAsset.getInode();
    }

    @JsonIgnore
    private String preLoopAsset = null;

    @JsonIgnore
    public FileAsset getPreLoopAsset() {
       return loadAsset(preLoopAsset);
    }

    private FileAsset loadAsset(String inode) {
        return Try.of(()->APILocator.getFileAssetAPI().find(preLoopAsset, APILocator.systemUser(), false)).getOrNull();
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
        Contentlet testContentlet =  new Contentlet();
        testContentlet.setIdentifier(this.identifier);
        testContentlet.setInode(this.inode);
        testContentlet.setLanguageId(this.languageId);
        return testContentlet;
    }

    public void setLanguage(final long languageId) {
        this.languageId = languageId;
    }

    public long getLanguageId() {
        return languageId;
    }
}
