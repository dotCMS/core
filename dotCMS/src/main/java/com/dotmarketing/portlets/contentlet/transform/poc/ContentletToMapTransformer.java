package com.dotmarketing.portlets.contentlet.transform.poc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

/**
 * DBTransformer that converts DB objects into Contentlet instances
 */
public class ContentletToMapTransformer  {
    
    final List<Map<String, Object>> list;

    final User user;

    public ContentletToMapTransformer(final List<Contentlet> initList, final User user){
        this.user=user;

        final List<Map<String, Object>> newList = new ArrayList<>();
        if (initList != null){
            for(final Contentlet c : initList){
                newList.add(transform(c, user));
            }
        }

        this.list = newList;
    }


    public List<Map<String, Object>> asList() {
        return this.list;
    }


    private  Map<String, Object> transform(final Contentlet con, final User user)  {
        
        if(con.getContentType()==null) {
            throw new DotStateException("Contentlet needs a ContentType to be serialized");
        }
        
        final Map<String, Object> map = new LinkedHashMap<>();
        map.putAll(new BinaryToMapTransformer(con).asMap());
        map.putAll(new CategoryToMapTransformer(con, user).asMap());
        map.putAll(new FolderToMapTransformer(con, user).asMap());
        map.putAll(new LanguageToMapTransformer(con).asMap());
        map.putAll(new IdentifierToMapTransformer(con).asMap());
        return map;
    }
}

