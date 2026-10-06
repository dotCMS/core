package com.dotmarketing.portlets.containers.model;


import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Source;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UtilMethods;
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

    /**
     * A File Asset Container is not a database Container: it is the {@code container.vtl} file living
     * in a folder under {@code /application/containers/}, and that is why {@link #getPermissionType()}
     * makes it behave as a {@link Contentlet}. Since the very same asset id is also loaded as a plain
     * {@link Contentlet} -- whose parent Permissionable is the folder that holds it -- this Container
     * must inherit from that same Container folder.
     * <p>Falling back to the {@link Container} behavior (the Site) would make both views of the asset
     * disagree: the {@code permission_reference} row is keyed by asset id alone, so whichever view
     * resolved it last would overwrite the other one, and every permission granted on the Container
     * folder would be silently replaced by the ones inherited from the Site.</p>
     *
     * @return The Container {@link Folder}, or the default {@link Container} parent Permissionable
     * (the Site) when this instance does not hold enough information to resolve such a folder.
     *
     * @throws DotDataException The Container folder could not be looked up. This is deliberately
     * not swallowed: answering with the Site after a failed lookup would persist the very
     * {@code permission_reference} row this override exists to prevent, and it would do so
     * silently. Failing here leaves the reference unwritten so the next request resolves it again.
     */
    @JsonIgnore
    @Override
    public Permissionable getParentPermissionable() throws DotDataException {
        final Folder containerFolder = this.findContainerFolder();
        return null != containerFolder ? containerFolder : super.getParentPermissionable();
    }

    /**
     * Resolves the folder that makes up this File Asset Container. Both the Site and the path are set
     * when the Container is built from the file system, but a partially built instance -- such as the
     * one created when the {@code container.vtl} file is being deleted -- may have neither of them.
     * <p>{@code null} means the folder is genuinely not there, which is the only case the caller may
     * fall back to the Site for. A folder that cannot be <b>looked up</b> is a different thing
     * entirely and throws: {@code FolderAPI.findFolderByPath()} returns {@code null} or an
     * inode-less Folder when the path does not resolve, and only raises when the lookup itself
     * fails.</p>
     *
     * @return The Container {@link Folder}, or {@code null} when this instance cannot name one or the
     * named folder does not exist.
     *
     * @throws DotDataException The folder lookup failed.
     */
    @JsonIgnore
    private Folder findContainerFolder() throws DotDataException {
        if (null == this.host || !UtilMethods.isSet(this.path)) {
            return null;
        }
        final FileAssetContainerUtil fileAssetContainerUtil = FileAssetContainerUtil.getInstance();
        final String folderPath = fileAssetContainerUtil.isFullPath(this.path)
                ? fileAssetContainerUtil.getRelativePath(this.path)
                : this.path;
        final Folder containerFolder;
        try {
            containerFolder = APILocator.getFolderAPI()
                    .findFolderByPath(folderPath, this.host, APILocator.systemUser(), false);
        } catch (final DotSecurityException e) {
            // Unreachable for the System User, whose permission checks short-circuit. Rethrown rather
            // than ignored so it can never turn into a silent downgrade to the Site.
            throw new DotDataException(String.format(
                    "Unable to resolve Container folder '%s' on Site '%s': %s",
                    folderPath, this.host.getHostname(), e.getMessage()), e);
        }
        return null != containerFolder && UtilMethods.isSet(containerFolder.getInode())
                && !FolderAPI.SYSTEM_FOLDER.equals(containerFolder.getInode())
                ? containerFolder : null;
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

    @Override
    public String getHostId() {
        return host.getIdentifier();
    }

    @Override
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
