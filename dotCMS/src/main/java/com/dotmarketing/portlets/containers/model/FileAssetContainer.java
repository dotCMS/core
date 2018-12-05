package com.dotmarketing.portlets.containers.model;

import com.dotmarketing.beans.Source;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a {@link Container} plus a list of {@link FileAsset}
 * This is a representation of the Container based on a file assets in the file system instead of the db.
 * @author jsanca
 */
public class FileAssetContainer extends Container {

    @JsonIgnore
    @com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore
    private transient final Map<String, Object> metaDataMap;

    @JsonIgnore
    @com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore
    private transient final Contentlet contentlet = new Contentlet();

    private long languageId;

    public FileAssetContainer() {

        this.source = Source.FILE;
        this.metaDataMap = new HashMap<>();
    }

    @JsonIgnore
    @com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore
    private transient List<FileAsset> containerStructuresAssets = Collections.emptyList();

    @JsonIgnore
    @com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore
    public List<FileAsset> getContainerStructuresAssets() {
        return containerStructuresAssets;
    }

    public void setContainerStructuresAssets(List<FileAsset> containerStructuresAssets) {
        this.containerStructuresAssets = containerStructuresAssets;
    }

    public void addMetaData(final String key, final Object value) {

        this.metaDataMap.put (key, value);
    }

    @JsonIgnore
    @com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore
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

    private Versionable toContentlet() {

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
}
