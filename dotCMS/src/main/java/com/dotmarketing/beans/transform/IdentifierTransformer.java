package com.dotmarketing.beans.transform;

import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.beans.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.dotmarketing.business.IdentifierFactory.ASSET_NAME;
import static com.dotmarketing.business.IdentifierFactory.ASSET_SUBTYPE;
import static com.dotmarketing.business.IdentifierFactory.ASSET_TYPE;
import static com.dotmarketing.business.IdentifierFactory.CREATE_DATE;
import static com.dotmarketing.business.IdentifierFactory.HOST_INODE;
import static com.dotmarketing.business.IdentifierFactory.ID;
import static com.dotmarketing.business.IdentifierFactory.OWNER;
import static com.dotmarketing.business.IdentifierFactory.PARENT_PATH;
import static com.dotmarketing.business.IdentifierFactory.SYS_EXPIRE_DATE;
import static com.dotmarketing.business.IdentifierFactory.SYS_PUBLISH_DATE;

/**
 * This implementation of the {@link DBTransformer} class converts DB objects into Identifier
 * instances.
 *
 * @author Will Ezell
 * @since Dec 14th, 2017
 */
public class IdentifierTransformer implements DBTransformer<Identifier> {
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
    private static Identifier transform(final Map<String, Object> map)  {
        final Identifier identifier = new Identifier();
        identifier.setAssetName((String) map.get(ASSET_NAME));
        identifier.setAssetType((String) map.get(ASSET_TYPE));
        identifier.setHostId((String) map.get(HOST_INODE));
        identifier.setId((String) map.get(ID));
        identifier.setParentPath((String) map.get(PARENT_PATH));
        identifier.setSysPublishDate((Date) map.get(SYS_PUBLISH_DATE));
        identifier.setSysExpireDate((Date) map.get(SYS_EXPIRE_DATE));
        identifier.setOwner((String) map.get(OWNER));
        identifier.setCreateDate((Date) map.get(CREATE_DATE));
        identifier.setAssetSubType((String) map.get(ASSET_SUBTYPE));
        return identifier;
    }

}
