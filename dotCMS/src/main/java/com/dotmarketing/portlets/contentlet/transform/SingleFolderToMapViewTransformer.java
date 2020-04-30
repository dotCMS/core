package com.dotmarketing.portlets.contentlet.transform;

import com.dotmarketing.beans.IconType;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transform a single folder to a map view
 * @author jsanca
 */
public class SingleFolderToMapViewTransformer implements FieldsToMapTransformer {

    private final Folder folder;
    private final List<Integer> permissions;

    public SingleFolderToMapViewTransformer(final Folder folder, final List<Integer> permissions) {
        this.folder = folder;
        this.permissions = permissions;
    }

    @Override
    public Map<String, Object> asMap() {

        final Map<String, Object> folderMap = new HashMap<>(Sneaky.sneaked(()->folder.getMap()).get());
        folderMap.put("permissions", permissions);
        folderMap.put("parent", folder.getInode());
        folderMap.put("mimeType", "");
        folderMap.put("name", folder.getName());
        folderMap.put("title", folder.getName());
        folderMap.put("description", folder.getTitle());
        folderMap.put("extension", "folder");
        folderMap.put("hasTitleImage", StringPool.BLANK);
        folderMap.put("__icon__", IconType.FOLDER.iconName());
        return folderMap;
    }
}
