package com.dotmarketing.portlets.contentlet.transform;

import java.util.HashMap;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;

/**
 * DBTransformer that converts DB objects into Contentlet instances
 */
public class FolderToMapTransformer implements FieldsToMapTransformer {
    final Map<String, Object> mapOfMaps;



    public FolderToMapTransformer(final Contentlet con, final User user) {
        if (con.getInode() == null || con.getIdentifier()==null) {
            throw new DotStateException("Contentlet needs an identifier to get properties");
        }

        final Map<String, Object> newMap = new HashMap<>();
        Map<String, Object> map = new HashMap<>();
        try {
            
            Folder  folder= APILocator.getFolderAPI().find(con.getFolder(), user, true);
            map.put("id", folder.getIdentifier());
            map.put("fileMask", folder.getFilesMasks());
            map.put("sortOrder", folder.getSortOrder());
            map.put("name", folder.getName());
            map.put("path", folder.getPath());
            map.put("title", folder.getTitle());
            map.put("defaultFileType", folder.getDefaultFileType());
        } catch (Exception e) {
            throw new DotStateException(String.format("Unable to get the Identifier for given contentlet with id= %s", con.getIdentifier()), e);

        }
        newMap.put("folder", con.getFolder());
        newMap.put("folderMap", map);
        

        this.mapOfMaps = newMap;
    }

    @Override
    public Map<String, Object> asMap() {
        return this.mapOfMaps;
    }



}

