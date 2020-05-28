package com.dotmarketing.portlets.contentlet.transform;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import java.util.HashMap;
import java.util.Map;

/**
 * DBTransformer that converts DB objects into Contentlet instances
 */
public class FolderToMapTransformer implements FieldsToMapTransformer {
    final Map<String, Object> mapOfMaps;



    public FolderToMapTransformer(final Contentlet con, final User user) {
        if (con.getInode() == null || con.getIdentifier()==null) {
            throw new DotStateException("Contentlet needs an identifier to get properties");
        }

        final Map<String, Object> map = new HashMap<>();
        try {
            
            final Folder  folder= APILocator.getFolderAPI().find(con.getFolder(), user, true);
            map.put("folderId", folder.getIdentifier());
            map.put("folderFileMask", folder.getFilesMasks());
            map.put("folderSortOrder", folder.getSortOrder());
            map.put("folderName", folder.getName());
            map.put("folderPath", folder.getPath());
            map.put("folderTitle", folder.getTitle());
            map.put("folderDefaultFileType", folder.getDefaultFileType());
        } catch (DotSecurityException | DotDataException e) {
            throw new DotStateException(String.format("Unable to get the Identifier for given contentlet with id= %s", con.getIdentifier()), e);

        }
        final Map<String, Object> newMap = new HashMap<>();
        newMap.put("folder", con.getFolder());
        newMap.put("folderMap", map);

        this.mapOfMaps = newMap;
    }

    @Override
    public Map<String, Object> asMap() {
        return this.mapOfMaps;
    }



}

