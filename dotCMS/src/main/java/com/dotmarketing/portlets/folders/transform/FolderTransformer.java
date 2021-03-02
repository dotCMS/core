package com.dotmarketing.portlets.folders.transform;

import com.dotcms.util.ConversionUtils;
import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.portlets.folders.model.Folder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * DBTransformer that converts DB objects into Folder instances
 */
public class FolderTransformer implements DBTransformer {
    final List<Folder> list;


    public FolderTransformer(List<Map<String, Object>> initList){
        List<Folder> newList = new ArrayList<>();
        if (initList != null){
            for(Map<String, Object> map : initList){
                newList.add(transform(map));
            }
        }

        this.list = newList;
    }

    @Override
    public List<Folder> asList() {
        return this.list;
    }

    @NotNull
    private static Folder transform(Map<String, Object> map)  {
        final Folder folder;
        folder = new Folder();
        folder.setInode((String) map.get("inode"));
        folder.setOwner((String) map.get("owner"));
        folder.setIDate((Date) map.get("idate"));
        folder.setName((String) map.get("name"));
        folder.setTitle((String) map.get("title"));
        folder.setShowOnMenu(ConversionUtils.toBooleanFromDb(map.getOrDefault("show_on_menu",false)));
        folder.setSortOrder(ConversionUtils.toInt(map.get("sort_order"),0));
        folder.setFilesMasks((String) map.get("files_masks"));
        folder.setIdentifier((String) map.get("identifier"));
        folder.setDefaultFileType((String) map.get("default_file_type"));
        folder.setModDate((Date) map.get("mod_date"));
        return folder;
    }
}
