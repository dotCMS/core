package com.dotcms.contenttype.transform.contenttype;

import java.io.IOException;
import java.util.List;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonContentTypeTransformer implements ContentTypeTransformer {
    final List<ContentType> list;


    public JsonContentTypeTransformer(ContentType type) {
        this.list = ImmutableList.of(type);
    }

    public JsonContentTypeTransformer(String json) {
        this(fromJsonStr(json));
    }

    public JsonContentTypeTransformer(List<ContentType> list) {
        this.list = ImmutableList.copyOf(list);
    }


    private static String toJsonStr(ContentType type) throws DotStateException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(type);
        } catch (JsonProcessingException e) {
            throw new DotStateException(e);
        }
    }

    private static ContentType fromJsonStr(String type) throws DotStateException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(type, ContentType.class);
        } catch (IOException e) {
            throw new DotStateException(e);
        }
    }

    @Override
    public ContentType from() throws DotStateException {
        return this.list.get(0);
    }
    
    @Override
    public List<ContentType> asList() throws DotStateException {
        return this.list;
    }

    public String asJson() throws DotStateException {
        return toJsonStr(this.list.get(0));
    }


    public String asJsonList() throws DotStateException {
        StringBuilder sb = new StringBuilder('[');
        for (int i = 0; i < list.size(); i++) {
            sb.append(toJsonStr(list.get(i)));
            if (i != list.size()) {
                sb.append(',');
            }
        }
        sb.append(']');

        return sb.toString();
    }
}

/**
 * Fields in the db inode owner idate type inode name description default_structure page_detail
 * structuretype system fixed velocity_var_name url_map_pattern host folder expire_date_var
 * publish_date_var mod_date
 **/
