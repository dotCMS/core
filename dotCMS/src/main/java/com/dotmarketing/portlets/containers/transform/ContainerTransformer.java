package com.dotmarketing.portlets.containers.transform;

import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.containers.model.Container;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * DBTransformer that converts DB objects into Container instances
 */
public class ContainerTransformer implements DBTransformer {
    final List<Container> list;


    public ContainerTransformer(List<Map<String, Object>> initList){
        List<Container> newList = new ArrayList<>();
        if (initList != null){
            for(Map<String, Object> map : initList){
                newList.add(transform(map));
            }
        }

        this.list = newList;
    }

    @Override
    public List<Container> asList() throws DotStateException {

        return this.list;
    }

    @NotNull
    private static Container transform(Map<String, Object> map)  {
        final Container container;
        container = new Container();
        container.setInode((String) map.get("inode"));
        container.setOwner((String) map.get("owner"));
        container.setIDate((Date) map.get("idate"));
        container.setCode((String) map.get("code"));
        container.setPreLoop((String) map.get("pre_loop"));
        container.setPostLoop((String) map.get("post_loop"));
        container.setShowOnMenu((Boolean) map.getOrDefault("show_on_menu",false));
        container.setTitle((String) map.get("title"));
        container.setModDate((Date) map.get("mod_date"));
        container.setModUser((String) map.get("mod_user"));
        container.setSortOrder((Integer) map.getOrDefault("sort_order",0));
        container.setFriendlyName((String) map.get("friendly_name"));
        container.setMaxContentlets((Integer) map.getOrDefault("max_contentlets",0));
        container.setUseDiv((Boolean) map.getOrDefault("use_div",false));
        container.setStaticify((Boolean) map.getOrDefault("staticify",false));
        container.setSortContentletsBy((String) map.get("sort_contentlets_by"));
        container.setLuceneQuery((String) map.get("lucene_query"));
        container.setNotes((String) map.get("notes"));
        container.setIdentifier((String) map.get("identifier"));
        return container;
    }
}
