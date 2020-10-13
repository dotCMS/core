package com.dotmarketing.beans.transform;

import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.beans.Identifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * DBTransformer that converts DB objects into Identifier instances
 */
public class IdentifierTransformer implements DBTransformer {
    final List<Identifier> list;


    public IdentifierTransformer(List<Map<String, Object>> initList){
        List<Identifier> newList = new ArrayList<>();
        if (initList != null){
            for(Map<String, Object> map : initList){
                newList.add(transform(map));
            }
        }

        this.list = newList;
    }

    @Override
    public List<Identifier> asList() {

        return this.list;
    }

    @NotNull
    private static Identifier transform(Map<String, Object> map)  {
        final Identifier i = new Identifier();
        i.setAssetName((String) map.get("asset_name"));
        i.setAssetType((String) map.get("asset_type"));
        i.setHostId((String) map.get("host_inode"));
        i.setId((String) map.get("id"));
        i.setParentPath((String) map.get("parent_path"));
        i.setSysPublishDate((Date) map.get("syspublish_date"));
        i.setSysExpireDate((Date) map.get("sysexpire_date"));
        i.setOwner((String) map.get("owner"));
        i.setCreateDate((Date) map.get("create_date"));
        i.setAssetSubType((String) map.get("asset_subtype"));
        return i;
    }
}
