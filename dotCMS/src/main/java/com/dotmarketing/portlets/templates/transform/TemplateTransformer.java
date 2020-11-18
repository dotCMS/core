package com.dotmarketing.portlets.templates.transform;

import com.dotcms.util.ConversionUtils;
import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.portlets.templates.model.Template;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * DBTransformer that converts DB objects into Template instances
 */
public class TemplateTransformer implements DBTransformer {
    final List<Template> list;


    public TemplateTransformer(List<Map<String, Object>> initList){
        List<Template> newList = new ArrayList<>();
        if (initList != null){
            for(Map<String, Object> map : initList){
                newList.add(transform(map));
            }
        }

        this.list = newList;
    }

    @Override
    public List<Template> asList() {

        return this.list;
    }

    @NotNull
    private static Template transform(Map<String, Object> map)  {
        final Template template;
        template = new Template();
        template.setInode(String.valueOf(map.get("inode")));
        template.setOwner(String.valueOf(map.get("owner")));
        template.setIDate((Date) map.get("create_date"));
        template.setShowOnMenu(ConversionUtils.toBooleanFromDb(map.getOrDefault("show_on_menu",false)));
        template.setTitle(String.valueOf(map.get("title")));
        template.setModDate((Date) map.get("mod_date"));
        template.setModUser(String.valueOf(map.get("mod_user")));
        template.setSortOrder(ConversionUtils.toInt(map.get("sort_order"),0));
        template.setFriendlyName(String.valueOf(map.get("friendly_name")));
        template.setBody(String.valueOf(map.get("body")));
        template.setHeader(String.valueOf(map.get("header")));
        template.setFooter(String.valueOf(map.get("footer")));
        template.setImage(String.valueOf(map.get("image")));
        template.setIdentifier(String.valueOf(map.get("identifier")));
        template.setDrawed(ConversionUtils.toBooleanFromDb(map.get("drawed")));
        template.setDrawedBody((String) map.get("drawed_body"));
        template.setCountAddContainer(ConversionUtils.toInt(map.get("add_container_links"),0));
        template.setCountContainers(ConversionUtils.toInt(map.get("containers_added"),0));
        template.setHeadCode((String) map.get("head_code"));
        template.setTheme((String) map.get("theme"));
        return template;
    }
}
