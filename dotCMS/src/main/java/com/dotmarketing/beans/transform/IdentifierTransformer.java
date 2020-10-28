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
        final Identifier identifier = new Identifier();
        identifier.setAssetName((String) map.get("asset_name"));
        identifier.setAssetType((String) map.get("asset_type"));
        identifier.setHostId((String) map.get("host_inode"));
        identifier.setId((String) map.get("id"));
        identifier.setParentPath((String) map.get("parent_path"));
        identifier.setSysPublishDate((Date) map.get("syspublish_date"));
        identifier.setSysExpireDate((Date) map.get("sysexpire_date"));
        identifier.setOwner((String) map.get("owner"));
        identifier.setCreateDate((Date) map.get("create_date"));
        identifier.setAssetSubType((String) map.get("asset_subtype"));
        return identifier;
    }
}
