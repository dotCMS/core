package com.dotcms.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Jonathan Gamba 6/1/18
 */
public class LazyFileAssetAPIWrapper implements FileAssetAPI {

    private FileAssetAPI fileAssetAPI = null;

    private FileAssetAPI getFileAssetAPI() {

        if (null == this.fileAssetAPI) {
            synchronized (this) {
                if (null == this.fileAssetAPI) {
                    this.fileAssetAPI = APILocator.getFileAssetAPI();
                }
            }
        }

        return fileAssetAPI;
    }

    @Override
    public void createBaseFileAssetFields(Structure structure)
            throws DotDataException, DotStateException {
        this.getFileAssetAPI().createBaseFileAssetFields(structure);
    }

    @Override
    public FileAsset fromContentlet(Contentlet con) throws DotStateException {
        return this.getFileAssetAPI().fromContentlet(con);
    }

    @Override
    public List<FileAsset> fromContentlets(List<Contentlet> cons) throws DotStateException {
        return this.getFileAssetAPI().fromContentlets(cons);
    }

    @Override
    public List<IFileAsset> fromContentletsI(List<Contentlet> cons) throws DotStateException {
        return this.getFileAssetAPI().fromContentletsI(cons);
    }

    @Override
    public boolean isFileAsset(Contentlet c) {
        return this.getFileAssetAPI().isFileAsset(c);
    }

    @Override
    public Map<String, String> getMetaDataMap(Contentlet contentlet, File binFile)
            throws DotDataException {
        return this.getFileAssetAPI().getMetaDataMap(contentlet, binFile);
    }

    @Override
    public boolean fileNameExists(Host host, Folder folder, String fileName, String identifier)
            throws DotDataException {
        return this.getFileAssetAPI().fileNameExists(host, folder, fileName, identifier);
    }

    @Override
    public boolean fileNameExists(Host host, Folder folder, String fileName, String identifier,
            long languageId) throws DotDataException {
        return this.getFileAssetAPI()
                .fileNameExists(host, folder, fileName, identifier, languageId);
    }

    @Override
    public String getRelativeAssetPath(FileAsset fa) {
        return this.getFileAssetAPI().getRelativeAssetPath(fa);
    }

    @Override
    public List<FileAsset> findFileAssetsByFolder(Folder parentFolder, User user,
            boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        return this.getFileAssetAPI()
                .findFileAssetsByFolder(parentFolder, user, respectFrontendRoles);
    }

    @Override
    public List<FileAsset> findFileAssetsByHost(Host parentHost, User user,
            boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        return this.getFileAssetAPI().findFileAssetsByHost(parentHost, user, respectFrontendRoles);
    }

    @Override
    public List<FileAsset> findFileAssetsByHost(Host parentHost, User user, boolean live,
            boolean working, boolean archived, boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {
        return this.getFileAssetAPI()
                .findFileAssetsByHost(parentHost, user, live, working, archived,
                        respectFrontendRoles);
    }

    @Override
    public List<FileAsset> findFileAssetsByFolder(Folder parentFolder, String sortBy, boolean live,
            User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
        return this.getFileAssetAPI()
                .findFileAssetsByFolder(parentFolder, sortBy, live, user, respectFrontendRoles);
    }

    @Override
    public List<FileAsset> findFileAssetsByFolder(Folder parentFolder, String sortBy, boolean live,
            boolean working, User user, boolean respectFrontendRoles)
            throws DotDataException, DotSecurityException {
        return this.getFileAssetAPI()
                .findFileAssetsByFolder(parentFolder, sortBy, live, working, user,
                        respectFrontendRoles);
    }

    @Override
    public boolean renameFile(Contentlet fileAssetCont, String newName, User user,
            boolean respectFrontendRoles)
            throws DotStateException, DotDataException, DotSecurityException, IOException {
        return this.getFileAssetAPI()
                .renameFile(fileAssetCont, newName, user, respectFrontendRoles);
    }

    @Override
    public boolean moveFile(Contentlet fileAssetCont, Folder parent, User user,
            boolean respectFrontendRoles)
            throws DotStateException, DotDataException, DotSecurityException {
        return this.getFileAssetAPI().moveFile(fileAssetCont, parent, user, respectFrontendRoles);
    }

    @Override
    public boolean moveFile(Contentlet fileAssetCont, Host host, User user,
            boolean respectFrontendRoles)
            throws DotStateException, DotDataException, DotSecurityException {
        return this.getFileAssetAPI().moveFile(fileAssetCont, host, user, respectFrontendRoles);
    }

    @Override
    public String getRealAssetPath(String inode, String fileName, String ext) {
        return this.getFileAssetAPI().getRealAssetPath(inode, fileName, ext);
    }

    @Override
    public String getRealAssetPath(String inode) {
        return this.getFileAssetAPI().getRealAssetPath(inode);
    }

    @Override
    public String getRealAssetPath(String inode, String fileName) {
        return this.getFileAssetAPI().getRealAssetPath(inode, fileName);
    }

    @Override
    public String getRelativeAssetsRootPath() {
        return this.getFileAssetAPI().getRelativeAssetsRootPath();
    }

    @Override
    public String getRealAssetsRootPath() {
        return this.getFileAssetAPI().getRealAssetsRootPath();
    }

    @Override
    public File getContentMetadataFile(String inode) {
        return this.getFileAssetAPI().getContentMetadataFile(inode);
    }

    @Override
    public String getContentMetadataAsString(File metadataFile) throws Exception {
        return this.getFileAssetAPI().getContentMetadataAsString(metadataFile);
    }

    @Override
    public void cleanThumbnailsFromContentlet(Contentlet contentlet) {
        this.getFileAssetAPI().cleanThumbnailsFromContentlet(contentlet);
    }

    @Override
    public void cleanThumbnailsFromFileAsset(IFileAsset fileAsset) {
        this.getFileAssetAPI().cleanThumbnailsFromFileAsset(fileAsset);
    }

    @Override
    public String getMimeType(String filename) {
        return this.getFileAssetAPI().getMimeType(filename);
    }

    @Override
    public String getRealAssetPathTmpBinary() {
        return this.getFileAssetAPI().getRealAssetPathTmpBinary();
    }

}