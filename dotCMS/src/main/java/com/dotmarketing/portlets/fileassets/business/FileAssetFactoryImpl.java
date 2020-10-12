package com.dotmarketing.portlets.fileassets.business;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

/**
 * {@link FileAssetFactory} default implementation
 */
public class FileAssetFactoryImpl implements FileAssetFactory {
    private final PermissionAPI permissionAPI;

    private final String FIND_FILE_ASSETS_BY_FOLDER_QUERY =
            "SELECT contentlet.inode as mynode " +
            "FROM identifier, contentlet_version_info, contentlet, structure " +
            "WHERE identifier.parent_path = ? and " +
                "identifier.host_inode = ? and " +
                "contentlet_version_info.identifier = identifier.id and " +
                "contentlet_version_info.%s_inode = contentlet.inode and " +
                "contentlet.structure_inode = structure.inode and " +
                "structure.structuretype =4";

    public FileAssetFactoryImpl() {
        permissionAPI = APILocator.getPermissionAPI();
    }

    
    /**
     * takes a list of inodes and returns the list of contentlets - includes permissions check
     * 
     * @param identifiers
     * @param user
     * @param respectFrontendRoles
     * @return
     */

    @NotNull
    List<Contentlet> fromIdentifiers(
            final List<String> identifiers,
            final User user,
            final boolean respectFrontendRoles
            )  {

        final ImmutableList.Builder<Contentlet> builder = new ImmutableList.Builder<>();

        for (final String  fileInfo : identifiers) {
            final Contentlet file = Try.of(()-> APILocator.getContentletAPI().find(fileInfo, user, respectFrontendRoles)).getOrNull();
            if(file==null)continue;
            builder.add(file);
           
        }
        return builder.build();
    }



    @Override
    public List<Contentlet> findByDB(final FileAssetSearcher searcher)  {

        // check read permissions on parent folder
        if(!Try.of(()-> permissionAPI.doesUserHavePermission(searcher.folder, PermissionAPI.PERMISSION_READ, searcher.user, searcher.respectFrontendRoles)).getOrElse(false)) {
            throw new DotRuntimeException("user:" + searcher.user + " does not have permission to view the parent folder:"+searcher.folder);
        }
        



        final DotConnect dotConnect = new DotConnect()
            .setSQL(String.format(FIND_FILE_ASSETS_BY_FOLDER_QUERY, searcher.live ? "live" : "working"))
            .addParam(searcher.folder.getPath())
            .addParam(searcher.host.getIdentifier());

        final List<String> identifiers = Try.of(()->  dotConnect
                        .loadObjectResults())
                        .getOrElseThrow(e->new DotRuntimeException(e))
                        .stream()
                        .map(m->((String) m.get("mynode")))
                        .collect(Collectors.toList());

        return fromIdentifiers(identifiers, searcher.user, searcher.respectFrontendRoles);
    }
}
