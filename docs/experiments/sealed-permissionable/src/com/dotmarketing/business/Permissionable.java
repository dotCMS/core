package com.dotmarketing.business;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;

/**
 * Sealed, with its permitted subtypes in OTHER packages — mirroring the real dotCMS layout, where
 * fourteen implementors are spread across nine packages.
 */
public sealed interface Permissionable permits Contentlet, Folder, Identifier, Inode {

    String getPermissionType();
}
