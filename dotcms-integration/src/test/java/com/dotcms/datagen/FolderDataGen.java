package com.dotcms.datagen;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class FolderDataGen extends AbstractDataGen<Folder> {
    private long currentTime = System.currentTimeMillis();
    private String name = "testName" + currentTime;
    private String title = "testTitle" + currentTime;
    private boolean showOnMenu;
    private int sortOrder = 0;
    private String fileMasks = "";
    private Folder parent;
    private Host site = host;
    private User owner;
    private String defaultFileType = CacheLocator.getContentTypeCache()
                    .getStructureByVelocityVarName(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME)
                    .getInode();
    
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
    public FolderDataGen defaultFileType(String defaultFileType) {
        this.defaultFileType = defaultFileType;
        return this;
    }
    
    
    @SuppressWarnings("unused")
    public FolderDataGen sortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    @SuppressWarnings("unused")
    public FolderDataGen fileMasks(String fileMasks) {
        this.fileMasks = fileMasks;
        return this;
    }

    @SuppressWarnings("unused")
    public FolderDataGen site(Host site) {
        this.site = site;
        this.parent = null;
        return this;
    }

    @SuppressWarnings("unused")
    public FolderDataGen owner(User owner) {
        this.owner = owner;
        return this;
    }


    @SuppressWarnings("unused")
    public FolderDataGen parent(Folder parent) {
        this.parent = parent;
        try {
            this.site = APILocator.getHostAPI()
                    .find(parent.getHostId(), APILocator.systemUser(), true);
        } catch (DotDataException e) {
            Logger.error(this,"ERROR GETTING HOST WITH ID: " + parent.getHostId());
        } catch (DotSecurityException e) {
            Logger.error(this,"USER DO NOT HAVE PERMISSION TO HOST WITH ID " + parent.getHostId());
        }
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
        f.setHostId(site.getIdentifier());
        f.setDefaultFileType(defaultFileType);
        if(null!= owner ) {
            f.setOwner(owner.getUserId());
        } else {
            f.setOwner(user.getUserId());
        }
        return f;
    }

    @WrapInTransaction
    @Override
    public Folder persist(Folder folder) {
        try {
            Identifier newIdentifier;
            if (!UtilMethods.isSet(parent)) {
                newIdentifier = APILocator.getIdentifierAPI().createNew(folder, site);
            } else {
                newIdentifier = APILocator.getIdentifierAPI().createNew(folder, parent);
            }

            folder.setIdentifier(newIdentifier.getId());
            APILocator.getFolderAPI().save(folder, user, false);

            return APILocator.getFolderAPI().find(newIdentifier.getId(), APILocator.systemUser(), false);
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException("Unable to persist folder.", e);
        }

    }

    /**
     * Creates a new {@link Folder} instance and persists it in DB
     *
     * @return A new Folder instance persisted in DB
     */
    @Override
    public Folder nextPersisted() {
        return persist(next());
    }

    @WrapInTransaction
    public static void remove(Folder folder) {
        try {
            APILocator.getFolderAPI().delete(folder, user, false);
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException("Unable to remove folder.", e);
        }
    }

}