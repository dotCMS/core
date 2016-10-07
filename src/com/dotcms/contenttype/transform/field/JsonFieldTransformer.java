package com.dotcms.contenttype.transform.field;

import java.util.Date;
import java.util.List;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.transform.JsonHelper;
import com.dotcms.contenttype.transform.JsonWrapper;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.org.python.modules.newmodule;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonFieldTransformer implements FieldTransformer {
    final List<Field> list;


    public JsonFieldTransformer(Field field) {
        this.list = ImmutableList.of(field);
    }

    public JsonFieldTransformer(String json) {
        this(fromJsonStr(json));
    }

    public JsonFieldTransformer(List<Field> list) {
        this.list = ImmutableList.copyOf(list);
    }


    private static String toJsonStr(Field field) throws DotStateException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_EMPTY);

        JsonWrapper input = new JsonWrapper<>(field, field.getClass());
        try {
            return mapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            throw new DotStateException(e);
        }
    }

    private static Field fromJsonStr(String input) throws DotStateException {

        return (Field) JsonHelper.fromJson(input);

        /*
        
        JSONObject jo;
        try {
           



            String clazz = jo.getString("implClass");
            FieldBuilder builder = FieldBuilder.builder(Class.forName(clazz));
            builder.contentTypeId(jo.getString("contentTypeId"));
          
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

            if (UtilMethods.isSet(jo.optString("urlMapPattern")))
                builder.urlMapPattern(jo.optString("urlMapPattern"));
            if (UtilMethods.isSet(jo.optString("publishDateVar")))
                builder.publishDateVar(jo.optString("publishDateVar"));
            if (UtilMethods.isSet(jo.optString("expireDateVar")))
                builder.expireDateVar(jo.optString("expireDateVar"));
            if (UtilMethods.isSet(jo.optString("detailPage")))
                builder.detailPage(jo.optString("detailPage"));
            if (UtilMethods.isSet(jo.optString("owner")))
                builder.owner(jo.optString("owner"));
            if (UtilMethods.isSet(jo.optString("description")))
                builder.description(jo.optString("description"));
                
            return builder.build();
        } catch (JSONException | ClassNotFoundException e) {

            throw new DotStateException(e + " : json=" + input);
        }
        */
    }

    @Override
    public Field from() throws DotStateException {
        return this.list.get(0);
    }

    @Override
    public List<Field> asList() throws DotStateException {
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

