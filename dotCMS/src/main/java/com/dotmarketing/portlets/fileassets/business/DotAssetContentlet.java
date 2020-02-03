package com.dotmarketing.portlets.fileassets.business;

import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.ImageUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.awt.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;

/**
 * Encapsulates the instance of a type which is similar to the {@link FileAsset} but only requiries the binary field called asset to exists
 * @author jsanca
 */
class DotAssetContentlet extends Contentlet implements DotAsset {

    private String metaData;
    private String mimeType;
    private File asset;
    private long fileAssetInternal = 0;
    private Dimension fileDimension = null;
    /**
     * This access the physical file on disk
     *
     * @return
     */
    private  String underlyingFileName = null;

    public DotAssetContentlet() {
        super();

    }

    @Override
    public String getMetaData(){
        if(metaData ==null){
            metaData=(String) super.get(DotAssetContentType.FILEASSET_METADATA_FIELD_VAR);
        }
        return metaData;

    }

    @Override
    public void setMetaData(final String metaData) {
        this.metaData = metaData;
    }

    public String getPath() {
        // todo: see if needed
        return null;
    }

    @Override
    public String getParent() {
        if (FolderAPI.SYSTEM_FOLDER.equals(getFolder())) {
            return getHost();
        } else {
            return getFolder();
        }
    }

    @Override
    public long getAssetSize() {
        if(this.fileAssetInternal == 0) {
            this.fileAssetInternal = computeFileSize(getFileAsset());
        }
        return this.fileAssetInternal > 0 ? this.fileAssetInternal : 0;
    }

    private long computeFileSize(final File fileAsset){
        return (fileAssetInternal = fileAsset == null ? 0 : fileAsset.length());
    }

    @Override
    public int getHeight() {
        if (null == fileDimension) {
            fileDimension = computeFileDimension(getFileAsset());
        }
        return  fileDimension == null ? 0 : fileDimension.height;
    }

    @Override
    public int getWidth() {
        if (null == fileDimension) {
            fileDimension = computeFileDimension(getFileAsset());
        }
        return fileDimension == null ? 0 : fileDimension.width;
    }

    private Dimension computeFileDimension(final File file) {
        try {
            return (fileDimension = ImageUtil.getInstance().getDimension(file));
        } catch (Exception e) {
            Logger.debug(this,
                    "Error computing dimensions for file asset with id: " + getIdentifier(), e);
        }
        return null;
    }

    @Override
    public String getUnderlyingFileName() {
        if (underlyingFileName != null) {
            return underlyingFileName;
        }
        this.underlyingFileName = computeUnderlyingFileName(getFileAsset());
        return this.underlyingFileName;
    }

    /**
     *
     * @param fileAsset
     * @return
     */
    private String computeUnderlyingFileName(final File fileAsset){
        return (this.underlyingFileName = fileAsset != null ? fileAsset.getName() : null);
    }

    /***
     * This access the logical file name stored on the table Identifier
     * @return
     */
    @Override
    public String getFileName() {
        try {
            return UtilMethods.isSet(getIdentifier()) ?
                    APILocator.getIdentifierAPI().find(getIdentifier()).getAssetName()
                    : StringPool.BLANK;
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public String getMimeType() {

        String mimeType = APILocator.getFileAssetAPI().getMimeType(getUnderlyingFileName());

        if (mimeType == null || UNKNOWN_MIME_TYPE.equals(mimeType)){
            mimeType = "application/octet-stream";
        }

        return mimeType;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new BufferedInputStream(Files.newInputStream(getFileAsset().toPath()));
    }

    @Override
    public void setBinary(final String velocityVarName, final File newFile)throws IOException{
        asset = null;
        super.setBinary(velocityVarName, newFile);
    }

    @Override
    public void setBinary(final com.dotcms.contenttype.model.field.Field field, final File newFile)throws IOException{
        asset = null;
        super.setBinary(field, newFile);
    }

    @Override
    public File getFileAsset() {
        // calling getBinary can be relatively expensive since it constantly verifies the existence of the file on disk.
        // Therefore we'll keep a file reference at hand.
        if (null == asset) {
            try {
                asset = getBinary(FileAssetAPI.BINARY_FIELD);
            } catch (IOException e) {
                throw new DotStateException("Unable to find the fileAsset for :" + this.getInode());
            }
        }
        return asset;
    }

    @Override
    public boolean isDeleted() throws DotStateException, DotDataException, DotSecurityException {
        return super.isArchived();
    }

    public boolean isShowOnMenu() {
        String isShowOnMenu = super.getStringProperty(FileAssetAPI.SHOW_ON_MENU);//DOTCMS-6968
        if(UtilMethods.isSet(isShowOnMenu) && isShowOnMenu.contains("true")){
            return true;
        }else{
            return false;
        }
    }

    public void setShowOnMenu(boolean showOnMenu) {
        super.setStringProperty(FileAssetAPI.SHOW_ON_MENU, Boolean.toString(showOnMenu));

    }

    public void setSortOrder(int sortOrder) {
        super.setSortOrder(sortOrder);

    }

    @Override
    public boolean isArchived() throws DotStateException, DotDataException, DotSecurityException {
        return isDeleted();
    }


    /**
     * Returns the live.
     * @return boolean
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws DotStateException
     */
    @Override
    public boolean isLive() throws DotStateException, DotDataException, DotSecurityException {
        return APILocator.getVersionableAPI().isLive(this);
    }

    /**
     * Returns the locked.
     * @return boolean
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws DotStateException
     */
    @Override
    public boolean isLocked() throws DotStateException, DotDataException, DotSecurityException {
        return APILocator.getVersionableAPI().isLocked(this);
    }


    @Override
    public String getType(){
        return "dotasset";
    }

    @Override
    public String getExtension(){
        return UtilMethods.getFileExtension(getUnderlyingFileName());

    }

    @Override
    public Map<String, Object> getMap() throws DotRuntimeException {
        Map<String,Object> map = super.getMap();
        boolean live =  false;
        boolean working =  false;
        boolean deleted = false;
        boolean locked  = false;
        try{
            live =  isLive();
            working = isWorking();
            deleted = isDeleted();
            locked  = isLocked();
        }catch(Exception e){
            Logger.error(this, e.getMessage(), e);
        }
        map.put("extension", getExtension());
        map.put("live", live);
        map.put("working", working);
        map.put("deleted", deleted);
        map.put("locked", locked);
        map.put("isContent", true);
        map.put("dotAssetType", this.getStructureInode());
        map.put("mimeType", getMimeType());
        User modUser = null;
        try {
            modUser = APILocator.getUserAPI().loadUserById(this.getModUser(),APILocator.getUserAPI().getSystemUser(),false);
        } catch (NoSuchUserException | DotDataException e) {
            Logger.debug(this, e.getMessage());
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
        }
        if (UtilMethods.isSet(modUser) && UtilMethods.isSet(modUser.getUserId()) && !modUser.isNew())
            map.put("modUserName", modUser.getFullName());
        else
            map.put("modUserName", "unknown");

        map.put("type", this.getType());
        return map;
    }

    public String getURI() throws DotDataException {
        return UtilMethods.isSet(getIdentifier()) ?
                APILocator.getIdentifierAPI().find(getIdentifier()).getURI()
                : StringPool.BLANK;

    }

    @Override
    public String toString() {
        return this.getFileName();
    }
}
