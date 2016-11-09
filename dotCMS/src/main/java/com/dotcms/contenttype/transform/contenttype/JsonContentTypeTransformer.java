package com.dotcms.contenttype.transform.contenttype;

import java.util.ArrayList;
import java.util.List;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.JsonHelper;
import com.dotcms.contenttype.transform.SerialWrapper;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonContentTypeTransformer implements ContentTypeTransformer {
    final List<ContentType> list;
    ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(Include.NON_NULL).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public JsonContentTypeTransformer(ContentType type) {
        this.list = ImmutableList.of(type);
    }

    
    public JsonContentTypeTransformer(String json) {
        List<ContentType> types = new ArrayList<>();
        // are we an array?
        try {
            types = ImmutableList.copyOf(fromJsonArrayStr(json));
        } catch (JSONException ex) {
            types = ImmutableList.of(fromJsonStr(json));
        }
        this.list = ImmutableList.copyOf(types);
    }

    
    public JsonContentTypeTransformer(List<ContentType> list) {
        this.list = ImmutableList.copyOf(list);
    }

    
    private JsonNode asNode(ContentType type) {
        ObjectNode typeNode = mapper.valueToTree(new SerialWrapper<>(type, type.getClass()));
        typeNode.put("implClass", type.getClass().getCanonicalName().replaceAll(".Immutable","."));

        ArrayNode fieldArray = mapper.createArrayNode();
        for(Field field : type.fields()){
           ObjectNode fieldNode = mapper.valueToTree(new SerialWrapper<>(field, field.getClass()));
           fieldNode.putArray("fieldVariables").addAll((ArrayNode) mapper.valueToTree(field.fieldVariables()));
           fieldArray.add(fieldNode);
        }
        typeNode.putArray("fields").addAll(fieldArray);
        return typeNode;
    }

    
    private ContentType fromJsonStr(String input) {
        try {
            return (ContentType) mapper.readValue(input, JsonHelper.resolveClass(input));
        } catch (Exception e) {
            throw new DotStateException(e);
        }
    }

    
    private List<ContentType> fromJsonArrayStr(String input) throws JSONException {
        List<ContentType> types = new ArrayList<>();
        JSONArray jarr = new JSONArray(input);
        for (int i = 0; i < jarr.length(); i++) {
            JSONObject jo = jarr.getJSONObject(i);
            types.add(fromJsonStr(jo.toString()));
        }
        return types;
    }

    
    @Override
    public ContentType from() throws DotStateException {
        return this.list.get(0);
    }

    @Override
    public List<ContentType> asList() throws DotStateException {
        return this.list;
    }

    public String json() throws DotStateException {
        ArrayNode outerArray = mapper.createArrayNode();
        for (ContentType type : list) {
            outerArray.add(asNode(type));
        }
        return outerArray.toString();
   
    }

}

