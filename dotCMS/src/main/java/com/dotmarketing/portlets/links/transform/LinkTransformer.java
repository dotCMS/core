package com.dotmarketing.portlets.links.transform;

import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.portlets.links.model.Link;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * DBTransformer that converts DB objects into Link instances
 */
public class LinkTransformer implements DBTransformer {
    final List<Link> list;


    public LinkTransformer(List<Map<String, Object>> initList){
        List<Link> newList = new ArrayList<>();
        if (initList != null){
            for(Map<String, Object> map : initList){
                newList.add(transform(map));
            }
        }

        this.list = newList;
    }

    @Override
    public List<Link> asList()  {

        return this.list;
    }

    @NotNull
    private static Link transform(Map<String, Object> map)  {
        final Link link = new Link();
        link.setInode((String) map.get("inode"));
        link.setOwner((String) map.get("owner"));
        link.setIDate((Date) map.get("idate"));
        link.setShowOnMenu((Boolean) map.getOrDefault("show_on_menu",false));
        link.setTitle((String) map.get("title"));
        link.setModDate((Date) map.get("mod_date"));
        link.setModUser((String) map.get("mod_user"));
        link.setSortOrder((Integer) map.getOrDefault("sort_order",0));
        link.setFriendlyName((String) map.get("friendly_name"));
        link.setIdentifier((String) map.get("identifier"));
        link.setProtocal((String) map.get("protocal"));
        link.setUrl((String) map.get("url"));
        link.setTarget((String) map.get("target"));
        link.setInternalLinkIdentifier((String) map.get("internal_link_identifier"));
        link.setLinkType((String) map.get("link_type"));
        link.setLinkCode((String) map.get("link_code"));
        return link;
    }
}
