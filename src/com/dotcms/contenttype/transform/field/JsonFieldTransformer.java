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

