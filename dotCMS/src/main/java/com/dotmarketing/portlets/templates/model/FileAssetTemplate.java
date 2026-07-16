package com.dotmarketing.portlets.templates.model;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Source;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.templates.business.FileAssetTemplateUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vavr.control.Try;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a {@link Template} plus a list of {@link FileAsset}
 * This is a representation of the Template based on a file assets in the file system instead of the db.
 * @author jsanca
 */
public class FileAssetTemplate extends Template {

    @JsonIgnore
    private transient final Map<String, Object> metaDataMap;

    private long languageId;
    private Host host;
    private String path;

    @JsonIgnore
    private transient FileAsset body = null;

    @JsonIgnore
    private transient FileAsset layout = null;

    public FileAssetTemplate() {

        this.source = Source.FILE;
        this.metaDataMap = new HashMap<>();
    }

    // we override it, in order to do the permissionable behind a contentlet object
    @Override
    public String getPermissionType() {
        return Contentlet.class.getCanonicalName();
    }
    
    @JsonIgnore
    public FileAsset getBodyAsset() {
        return body;
    }

    @JsonIgnore
    public FileAsset getLayoutAsset() {
        return layout;
    }

    public void setBodyAsset(final FileAsset body) {
        this.body = body;
    }

    public void setLayoutAsset(final FileAsset layout) {
        this.layout = layout;
    }

    public void addMetaData(final String key, final Object value) {

        this.metaDataMap.put (key, value);
    }

    @JsonIgnore
    public Map<String, Object> getMetaDataMap() {
        return metaDataMap;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
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

    public Host getHost() {
        return host;
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

    public Versionable toContentlet() {
        //inode of the template is the inode of the properties.vtl
        return
                Try.of(()->APILocator.getContentletAPI().find(this.inode,APILocator.systemUser(),false)).getOrNull();
    }

    @Override
    public String getTheme() {
        return Try.of(()-> FileAssetTemplateUtil.getInstance().getThemeIdFromPath(super.getTheme())).getOrNull();
    }

    @Override
    public ManifestInfo getManifestInfo(){
        return ManifestInfoBuilder.merge(super.getManifestInfo(),
                new ManifestInfoBuilder()
                    .site(this.getHost())
                    .path(this.getPath())
                    .build());
    }
}
