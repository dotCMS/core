package com.dotmarketing.portlets.folders.business;

import com.dotmarketing.exception.DotSecurityException;

public class AddContentToFolderPermissionException extends DotSecurityException {

    private String userId;
    private String folderPath;

    AddContentToFolderPermissionException(String userId, String folderPath){
        super(String.format("User %s does not have permission to add to Folder %s", userId, folderPath));

        this.userId = userId;
        this.folderPath = folderPath;
    }

    public String getUserId() {
        return userId;
    }

    public String getFolderPath() {
        return folderPath;
    }
}
