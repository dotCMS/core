package com.dotcms.contenttype.transform.contenttype;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
        mapper.setSerializationInclusion(Include.NON_NULL);
        
        JsonWrapper input = new JsonWrapper<>(type, type.baseType().immutableClass());
        try {
            return mapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            throw new DotStateException(e);
        }
    }

    private static ContentType fromJsonStr(String input) throws DotStateException {
        
        JSONObject jo;
        try {
            jo = new JSONObject(input);

            
            
            String clazz = jo.getString("implClass");
            ContentTypeBuilder builder = ContentTypeBuilder.builder(Class.forName(clazz));
            builder.defaultType(jo.optBoolean("defaultType", false));

            builder.fixed(jo.optBoolean("fixed")); 
            builder.folder(jo.getString("folder")); 
            builder.host(jo.getString("host")); 
            builder.iDate(new Date(jo.getLong("iDate"))); 
            builder.modDate(new Date(jo.getLong("modDate"))); 
            builder.multilingualable(jo.getBoolean("multilingualable")); 
            builder.name(jo.optString("name")); 
            builder.system(jo.getBoolean("system")); 
            builder.versionable(jo.getBoolean("versionable"));   
            builder.variable(jo.getString("variable")); 
            builder.inode(jo.getString("inode"));

            if(UtilMethods.isSet(jo.optString("urlMapPattern"))) builder.urlMapPattern(jo.optString("urlMapPattern")); 
            if(UtilMethods.isSet(jo.optString("publishDateVar")))  builder.publishDateVar(jo.optString("publishDateVar")); 
            if(UtilMethods.isSet(jo.optString("expireDateVar"))) builder.expireDateVar(jo.optString("expireDateVar"));
            if(UtilMethods.isSet(jo.optString("detailPage"))) builder.detailPage(jo.optString("detailPage"));
            if(UtilMethods.isSet(jo.optString("owner"))) builder.owner(jo.optString("owner"));
            if(UtilMethods.isSet(jo.optString("description"))) builder.description(jo.optString("description"));
            return builder.build();
        } catch (JSONException | ClassNotFoundException e) {

           throw new DotStateException(e + " : json=" + input);
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
    static class JsonWrapper<T> {
        @JsonUnwrapped
        final private T inner;
        final private Class implClass;

        public JsonWrapper(T inner, Class field) {
            this.inner = inner;
            this.implClass = field;
        }

        public T getInner() {
            return inner;
        }

        public Class getImplClass() {
            return implClass;
        }
    }
}

