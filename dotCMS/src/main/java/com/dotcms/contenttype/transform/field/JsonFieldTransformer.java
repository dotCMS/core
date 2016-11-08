package com.dotcms.contenttype.transform.field;

import java.util.ArrayList;
import java.util.List;

import com.dotcms.contenttype.model.field.Field;

import com.dotcms.contenttype.transform.JsonHelper;
import com.dotcms.contenttype.transform.SerialWrapper;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonFieldTransformer implements FieldTransformer {
    final List<Field> list;
    ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


    public JsonFieldTransformer(Field field) {
        this.list = ImmutableList.of(field);
    }

    public JsonFieldTransformer(String json) {
        List<Field> fields;
        // are we an array?
        try {
            fields = fromJsonArrayStr(json);
        } catch (JSONException ex) {
            fields = ImmutableList.of(fromJsonStr(json));
        }
        this.list = ImmutableList.copyOf(fields);


    }

    public JsonFieldTransformer(List<Field> list) {
        this.list = ImmutableList.copyOf(list);
    }

    private List<Field> fromJsonArrayStr(String input) throws JSONException {
        List<Field> fields = new ArrayList<>();

        JSONArray jarr = new JSONArray(input);
        for (int i = 0; i < jarr.length(); i++) {
            JSONObject jo = jarr.getJSONObject(i);
            fields.add(fromJsonStr(jo.toString()));
        }


        return fields;
    }

    private String toJsonStr(Field field) throws DotStateException {
        SerialWrapper<Field> input = new SerialWrapper<>(field, field.getClass());
        try {
            return mapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            throw new DotStateException(e);
        }
    }

    private Field fromJsonStr(String input) throws DotStateException {

        try {
            return (Field) mapper.readValue(input, JsonHelper.resolveClass(input));
        } catch (Exception e) {
            throw new DotStateException(e);
        }
    }

    @Override
    public Field from() throws DotStateException {
        return this.list.get(0);
    }

    @Override
    public List<Field> asList() throws DotStateException {
        return this.list;
    }



    public String json() throws DotStateException, JsonProcessingException {
        List<SerialWrapper<Field>> wrapped = new ArrayList<SerialWrapper<Field>>();

        for (Field type : list) {
            wrapped.add(new SerialWrapper<>(type, type.getClass()));
        }
        return mapper.writeValueAsString(wrapped);

    }


}

