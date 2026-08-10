package com.dotmarketing.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;

/**
 * The shape PermissionBitFactoryImpl#resolvePermissionType could take if the hierarchy were sealed.
 */
public final class PermissionResolver {

    private PermissionResolver() {
    }

    /**
     * The same switch as the real resolver — with no `default`.
     *
     * <p>Host precedes Contentlet because Host extends it; the compiler rejects the other order with
     * "this case label is dominated by a preceding case label".</p>
     */
    public static String resolve(final Permissionable permissionable) {

        return switch (permissionable) {

            case Host _ -> "Host";

            case Contentlet _ -> "Contentlet";

            case Folder _ -> "Folder";

            case Identifier _ -> "Identifier";

            case Inode _ -> "Inode";
        };
    }
}
