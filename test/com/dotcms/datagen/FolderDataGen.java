package com.dotcms.datagen;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UtilMethods;

public class FolderDataGen extends AbstractDataGen<Folder> {
    private long currentTime = System.currentTimeMillis();
    private String name = "test-folder-name-" + currentTime;
    private String title = "test-folder-title-" + currentTime;
    private boolean showOnMenu;
    private int sortOrder = 0;
    private String fileMasks = "";

    public FolderDataGen() {
        this.folder = null;
    }

    @SuppressWarnings("unused")
    public FolderDataGen name(String name) {
        this.name = name;
        return this;
    }

    @SuppressWarnings("unused")
    public FolderDataGen title(String title) {
        this.title = title;
        return this;
    }

    @SuppressWarnings("unused")
    public FolderDataGen showOnMenu(boolean showOnMenu) {
        this.showOnMenu = showOnMenu;
        return this;
    }

    @SuppressWarnings("unused")
    public FolderDataGen showOnMenu(int sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    @SuppressWarnings("unused")
    public FolderDataGen showOnMenu(String fileMasks) {
        this.fileMasks = fileMasks;
        return this;
    }

    @SuppressWarnings("unused")
    public FolderDataGen host(Host host) {
        this.host = host;
        this.folder = null;
        return this;
    }

    @SuppressWarnings("unused")
    public FolderDataGen folder(Folder folder) {
        this.folder = folder;
        this.host = null;
        return this;
    }


    @Override
    public Folder next() {
        Folder f = new Folder();
        f.setName(name);
        f.setTitle(title);
        f.setShowOnMenu(showOnMenu);
        f.setSortOrder(sortOrder);
        f.setFilesMasks(fileMasks);
        f.setHostId(host.getIdentifier());
        f.setDefaultFileType(CacheLocator.getContentTypeCache()
            .getStructureByVelocityVarName(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME)
            .getInode());
        return f;
    }

    @Override
    public Folder persist(Folder folder) {
        try {
            Identifier newIdentifier;
            if (!UtilMethods.isSet(this.folder)) {
                newIdentifier = APILocator.getIdentifierAPI().createNew(folder, host);
            } else {
                newIdentifier = APILocator.getIdentifierAPI().createNew(folder, this.folder);
            }

            folder.setIdentifier(newIdentifier.getId());
            APILocator.getFolderAPI().save(folder, user, false);
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException("Unable to persist folder.", e);
        }

        return folder;

    }

    public static void remove(Folder folder) {
        try {
            APILocator.getFolderAPI().delete(folder, user, false);
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException("Unable to remove folder.", e);
        }
    }
}
