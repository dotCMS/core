package com.dotcms.contenttype.transform.contenttype;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.layout.FieldLayout;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.JsonTransformer;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonContentTypeTransformer implements ContentTypeTransformer, JsonTransformer {
  public static final String LAYOUT_PROPERTY_NAME = "layout";
  final List<ContentType> list;
  private ContentTypeInternationalization contentTypeInternationalization;

  public JsonContentTypeTransformer(
          final ContentType type,
          final ContentTypeInternationalization contentTypeInternationalization) {

    this.list = ImmutableList.of(type);
    this.contentTypeInternationalization = contentTypeInternationalization;
  }

  public JsonContentTypeTransformer(ContentType type) {
    this(type, null);
  }


  public JsonContentTypeTransformer(String json) {
    List<ContentType> types = new ArrayList<>();
    // are we an array?
    try {
      if (json != null && json.trim().startsWith("[")) {
        types = ImmutableList.copyOf(fromJsonArrayStr(json));
      } else {
        types = ImmutableList.of(fromJsonObject(new JSONObject(json)));
      }
    } catch (JSONException arrEx) {
      throw new DotStateException(arrEx);

    }
    this.list = ImmutableList.copyOf(types);
  }


  public JsonContentTypeTransformer(List<ContentType> list) {
    this.list = ImmutableList.copyOf(list);
  }

  @Override
  public JSONObject jsonObject() {
    ContentType type = from();
    JSONObject jsonObject;
    try {

      jsonObject =
          new JSONObject(mapper.writeValueAsString(type));


      jsonObject.remove("permissionType");
      jsonObject.remove("permissionId");
      jsonObject.remove("versionable");
      jsonObject.remove("multilingualable");

      jsonObject.put("fields", new JsonFieldTransformer(type.fields()).jsonArray());
      jsonObject.put(LAYOUT_PROPERTY_NAME, this.getLayoutAsJsonArray(type));

      return jsonObject;
    } catch (JSONException | JsonProcessingException e) {
      Logger.error(JsonContentTypeTransformer.class, String.format("Error with the content type %s: %s", type.name(), e.getMessage()));
      throw new DotStateException(e);
    }
  }

  private ContentType fromJsonObject(JSONObject jo) {
    try {
      
      if (jo.has("inode") && !jo.has("id")) {
        jo.put("id", jo.get("inode"));
      }

      jo.remove(LAYOUT_PROPERTY_NAME);
      ContentType type = (ContentType) mapper.readValue(jo.toString(), ContentType.class);


      if (jo.has("fields")) {
        List<Field> fields = new JsonFieldTransformer(jo.getJSONArray("fields").toString()).asList();
        type.constructWithFields(fields);
      }
      return type;
    } catch (Exception e) {
      throw new DotStateException(e);
    }
  }


  private List<ContentType> fromJsonArrayStr(String input) throws JSONException {
    List<ContentType> types = new ArrayList<>();
    JSONArray jarr = new JSONArray(input);
    for (int i = 0; i < jarr.length(); i++) {
      types.add(fromJsonObject(jarr.getJSONObject(i)));

    }
    return types;
  }


  @Override
  public ContentType from() throws DotStateException {
    return asList().get(0);
  }

  @Override
  public List<ContentType> asList() throws DotStateException {
    for (ContentType type : this.list) {
      for (Field f : type.fields()) {
        f.fieldVariables();
      }

    }
    return this.list;
  }



  @Override
  public JSONArray jsonArray() {
    JSONArray jarr = new JSONArray();
    for (ContentType type : asList()) {
      jarr.add(new JsonContentTypeTransformer(type).jsonObject());
    }
    return jarr;
  }

  public List<Map<String, Object>> mapList() {
    List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
    for (ContentType type : asList()) {
      list.add(new JsonContentTypeTransformer(type).mapObject());
    }
    return list;
  }

  public Map<String, Object> mapObject() {
    try {
      ContentType type = from();
      Map<String, Object> typeMap = mapper.convertValue(type, HashMap.class);
      typeMap.put("fields", this.getFields(type));
      typeMap.put(LAYOUT_PROPERTY_NAME, this.getLayout(type));
      typeMap.put("baseType", type.baseType());
      typeMap.remove("acceptedDataTypes");
      typeMap.remove("dbColumn");

      return typeMap;
    } catch (Exception e) {
      throw new DotStateException(e);
    }
  }

  private List<Map<String, Object>> getFields(final ContentType type) {
    final List<Map<String, Object>> fieldsMap = new JsonFieldTransformer(type.fields()).mapList();
    final List<Map<String, Object>> result = new ArrayList<>();

    if (contentTypeInternationalization != null) {
      for (final Map<String, Object> fieldMap : fieldsMap) {
        final Map<String, Object> fieldInternationalizationMap =
                APILocator.getContentTypeFieldAPI().getFieldInternationalization(type, contentTypeInternationalization, fieldMap);
        result.add(fieldInternationalizationMap);
      }
    } else {
      result.addAll(fieldsMap);
    }

    return result;
  }

  private FieldLayout getLayout(final ContentType type){
    final FieldLayout fieldLayout = new FieldLayout(type);
    fieldLayout.setContentTypeInternationalization(contentTypeInternationalization);
    return fieldLayout;
  }

  private JSONArray getLayoutAsJsonArray(final ContentType type) {
    return new JSONArray(getLayout(type).getRows());
  }

}


