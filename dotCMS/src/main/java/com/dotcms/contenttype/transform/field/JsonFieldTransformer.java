package com.dotcms.contenttype.transform.field;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.transform.JsonHelper;
import com.dotcms.contenttype.transform.JsonTransformer;
import com.dotcms.contenttype.transform.SerialWrapper;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class JsonFieldTransformer implements FieldTransformer, JsonTransformer {
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
      JSONObject jo = new JSONObject(json);
      if (jo.has("fields")) {
        l = fromJsonArray(jo.getJSONArray("fields"));
      }
      else{
        l.add(fromJsonStr(json));
      }
    } catch (Exception e) {
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
      return (Field) mapper.readValue(input, Field.class);
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
    try {
      JSONObject jo = new JSONObject(mapper.writeValueAsString(from()));
      jo.remove("acceptedDataTypes");
      //jo.remove("iDate");
      jo.remove("dbColumn");
      
      
      return jo;
      
      
      
      
    } catch (JSONException | JsonProcessingException e) {
      throw new DotStateException(e);
    }
  }

  @Override
  public JSONArray jsonArray() {

    JSONArray jarr = new JSONArray();
    for (Field field : asList()) {
      jarr.add(new JsonFieldTransformer(field).jsonObject());
    }
    return jarr;
  }

}

