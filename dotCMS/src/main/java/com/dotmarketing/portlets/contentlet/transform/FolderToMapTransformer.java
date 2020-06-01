package com.dotmarketing.portlets.contentlet.transform;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import java.util.Map;

/**
 * DBTransformer that converts DB objects into Contentlet instances
 */
public class FolderToMapTransformer implements FieldsToMapTransformer {
    final Map<String, Object> mapOfMaps;



    public FolderToMapTransformer(final Contentlet con, final User user) {

        final DotMapViewTransformer transformer = new DotFolderTransformerBuilder().withFolders(con.getFolder()).build();
        mapOfMaps = transformer.toMaps().get(0);
  /*
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
   */

    }

    @Override
    public Map<String, Object> asMap() {
        return this.mapOfMaps;
    }



}

