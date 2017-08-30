package com.dotcms.contenttype.transform.field;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.transform.JsonTransformer;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class JsonFieldTransformer implements FieldTransformer, JsonTransformer {

  private static final String CATEGORIES_PROPERTY_NAME = "categories";

  final List<Field> list;

  public JsonFieldTransformer(Field field) {
    this.list = ImmutableList.of(field);
  }

  public JsonFieldTransformer(List<Field> list) {
    this.list = ImmutableList.copyOf(list);
  }

  public JsonFieldTransformer(String json) {
    List<Field> l = new ArrayList<>();
    // are we an array?
    try {
      JSONArray jarr = new JSONArray(json);
      if (jarr.size() > 0) {
        JSONObject jo = jarr.getJSONObject(0);
        if (jo.has("fields")) {
          l = fromJsonArray(jo.getJSONArray("fields"));
        } else {
          l = fromJsonArrayStr(json);
        }
      }
    } catch (Exception e) {
      try {
        final JSONObject fieldJsonObjec = new JSONObject(json);
        if (fieldJsonObjec.has("fields")) {
          l = fromJsonArray(fieldJsonObjec.getJSONArray("fields"));
        } else {
          l.add(fromJsonStr(json));
        }
      } catch (Exception ex) {
        throw new DotStateException(ex);
      }
    }
    this.list = ImmutableList.copyOf(l);
  }



  private List<Field> fromJsonArrayStr(String json)
      throws JSONException, JsonParseException, JsonMappingException, IOException {
    return fromJsonArray(new JSONArray(json));

  }

  private List<Field> fromJsonArray(JSONArray jarr)
      throws JSONException, JsonParseException, JsonMappingException, IOException {
    List<Field> fields = new ArrayList<>();
    for (int i = 0; i < jarr.length(); i++) {
      JSONObject jo = jarr.getJSONObject(i);
      jo.remove("acceptedDataTypes");
      Field f = fromJsonStr(jo.toString());
      if (jo.has("fieldVariables")) {
        String varStr = jo.getJSONArray("fieldVariables").toString();
        List<FieldVariable> vars = mapper.readValue(varStr,
            mapper.getTypeFactory().constructCollectionType(List.class, ImmutableFieldVariable.class));
        f.constructFieldVariables(vars);
      }
      fields.add(f);
    }


    return fields;
  }


  private Field fromJsonStr(String input) throws DotStateException {

    try {
      JSONObject jo = new JSONObject(input);

      if (jo.has(CATEGORIES_PROPERTY_NAME)){
        final Object categories = jo.get(CATEGORIES_PROPERTY_NAME);
        jo.put("values", categories);
      }

      return (Field) mapper.readValue(jo.toString(), Field.class);
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


  @Override
  public JSONObject jsonObject() {
    return new JSONObject(mapObject());
  }

  @Override
  public JSONArray jsonArray() {

    JSONArray jarr = new JSONArray();
    for (Field field : asList()) {
      jarr.add(new JsonFieldTransformer(field).jsonObject());
    }
    return jarr;
  }

  public List<Map<String, Object>> mapList() {
    List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
    for (Field field : asList()) {
      list.add(new JsonFieldTransformer(field).mapObject());
    }
    return list;
  }

  public Map<String, Object> mapObject() {
    try {
      Field f = from();
      Map<String, Object> field = mapper.convertValue(f, HashMap.class);
      field.put("fieldVariables", new JsonFieldVariableTransformer(f.fieldVariables()).mapList());
      field.remove("acceptedDataTypes");
      field.remove("dbColumn");

      return field;
    } catch (Exception e) {
      throw new DotStateException(e);
    }
  }
}

