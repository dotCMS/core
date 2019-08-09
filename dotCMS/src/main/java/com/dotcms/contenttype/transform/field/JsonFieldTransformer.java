package com.dotcms.contenttype.transform.field;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.contenttype.model.field.ContentTypeFieldProperties;
import com.dotcms.contenttype.model.field.ImmutableRelationshipField;
import com.dotmarketing.util.UtilMethods;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.ImmutableCategoryField;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.transform.JsonTransformer;
import com.google.common.collect.ImmutableList;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.liferay.portal.language.LanguageUtil;

public class JsonFieldTransformer implements FieldTransformer, JsonTransformer {

  private static final String CATEGORIES_PROPERTY_NAME = "categories";
  private static final String VALUES = "values";

  final List<Field> list;

  public JsonFieldTransformer(final Field field) {
    this(ImmutableList.of(field));
  }

  public JsonFieldTransformer(final List<Field> list) {
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
        final JSONObject fieldJsonObject = new JSONObject(json);
        if (fieldJsonObject.has("fields")) {
          l = fromJsonArray(fieldJsonObject.getJSONArray("fields"));
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
      JSONObject fieldJsonObject = jarr.getJSONObject(i);
      fieldJsonObject.remove("acceptedDataTypes");
      Field f = fromJsonStr(fieldJsonObject.toString());
      if (fieldJsonObject.has("fieldVariables")) {
        String varStr = fieldJsonObject.getJSONArray("fieldVariables").toString();
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
          final JSONObject categories = (JSONObject) jo.get(CATEGORIES_PROPERTY_NAME);
          jo.put(VALUES, categories.get("inode"));
      } else if (jo.has(ContentTypeFieldProperties.RELATIONSHIPS.getName())) {
          final JSONObject relationship = (JSONObject) jo.get(ContentTypeFieldProperties.RELATIONSHIPS.getName());
          jo.put(VALUES, relationship.get("cardinality"));
          jo.put("relationType", relationship.get("velocityVar"));
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

  public Map<String, Object>  mapObject() {
    try {
      final Field field = from();
      final Map<String, Object> fieldMap = mapper.convertValue(field, HashMap.class);
      fieldMap.put("fieldVariables", new JsonFieldVariableTransformer(field.fieldVariables()).mapList());
      fieldMap.remove("acceptedDataTypes");
      fieldMap.remove("dbColumn");

      if (ImmutableCategoryField.class.getName().equals(fieldMap.get("clazz"))) {
        try {
          final Object values = fieldMap.get(VALUES);
          if (UtilMethods.isSet(values)) {
            final Category category = APILocator.getCategoryAPI()
                    .find(values.toString(), APILocator.getLoginServiceAPI().getLoggedInUser(),
                            false);
            if (null == category) {
              Logger.warn(JsonFieldTransformer.class, () -> String
                      .format("Unable to find category with id '%s' for field named %s. ", values.toString(), field.name()));
            } else {
              fieldMap.put(CATEGORIES_PROPERTY_NAME, category.getMap());
            }
          }
        } catch (final DotSecurityException e) {
          Logger.error(JsonFieldTransformer.class, e.getMessage());
        }
      } else if (ImmutableRelationshipField.class.getName().equals(fieldMap.get("clazz"))) {
        final String cardinality = fieldMap.remove(VALUES).toString();
        final String relationType = fieldMap.remove("relationType").toString();

        fieldMap.put(ContentTypeFieldProperties.RELATIONSHIPS.getName(), map(
            "cardinality", Integer.parseInt(cardinality), "velocityVar", relationType
        ));
      }


      fieldMap.put("fieldTypeLabel",
              LanguageUtil.get( APILocator.getLoginServiceAPI().getLoggedInUser(), field.getContentTypeFieldLabelKey() ));
      fieldMap.put("fieldType", field.getContentTypeFieldLabelKey());

      return fieldMap;
    } catch (Exception e) {
      throw new DotStateException(e);
    }
  }
}

