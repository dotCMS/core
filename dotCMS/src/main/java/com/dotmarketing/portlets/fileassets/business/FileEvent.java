package com.dotmarketing.portlets.fileassets.business;

import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;

import java.util.Date;

/**
 * Encapsulates the {@link com.dotmarketing.portlets.fileassets.business.FileAsset} that match with the listener criteria
 * @author jsanca
 */
public class FileEvent {

    private final String identifier;
    private final User   user;
    private final FileAsset fileAsset;
    private final Date   date;

    public FileEvent(final String id, final User user, final FileAsset fileAsset,
                     final Date date) {

        this.identifier = id;
        this.user       = user;
        this.fileAsset  = fileAsset;
        this.date       = date;
    }

    public String getIdentifier() {
        return identifier;
    }

    public FileAsset getFileAsset() {
        return fileAsset;
    }

    public Date getDate() {
        return date;
    }

    public User getUser() {
        return user;
    }
}
