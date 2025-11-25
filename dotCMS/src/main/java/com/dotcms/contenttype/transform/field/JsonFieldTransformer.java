package com.dotcms.contenttype.transform.field;

import com.dotcms.contenttype.model.field.ContentTypeFieldProperties;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.ImmutableCategoryField;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.field.ImmutableRelationshipField;
import com.dotcms.contenttype.transform.JsonTransformer;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonFieldTransformer implements FieldTransformer, JsonTransformer {

    private static final String CATEGORIES_PROPERTY_NAME = "categories";
    private static final String CARDINALITY_PROPERTY_NAME = "cardinality";
    private static final String VELOCITY_VARIABLE_PROPERTY_NAME = "velocityVar";
    private static final String FIELDS_VARIABLES_PROPERTY_NAME = "fieldVariables";
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
        if (fieldJsonObject.has(FIELDS_VARIABLES_PROPERTY_NAME)) {
            String varStr = fieldJsonObject.getJSONArray(FIELDS_VARIABLES_PROPERTY_NAME).toString();
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
          jo.put(VALUES, relationship.get(CARDINALITY_PROPERTY_NAME));
          jo.put("relationType", relationship.get(VELOCITY_VARIABLE_PROPERTY_NAME));
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
    List<Map<String, Object>> list = new ArrayList<>();
    for (Field field : asList()) {
      list.add(new JsonFieldTransformer(field).mapObject());
    }
    return list;
  }

    /**
     * Transforms a Field object into a Map representation. This method handles the conversion of
     * various field types, including special processing for category and relationship fields.
     *
     * @return A Map containing the field's properties and additional processed information.
     * @throws DotStateException if an error occurs during the transformation process.
     */
    public Map<String, Object> mapObject() {

        try {
            final Field field = from();
            final Map<String, Object> fieldMap = createBaseFieldMap(field);

            processSpecialFieldTypes(field, fieldMap);

            addFieldTypeLabels(field, fieldMap);

            return fieldMap;
        } catch (Exception e) {
            throw new DotStateException(e);
        }
    }

    /**
     * Creates a base map of field properties.
     *
     * @param field The Field object to convert.
     * @return A Map containing the basic field properties.
     */
    private Map<String, Object> createBaseFieldMap(Field field) {

        Map<String, Object> fieldMap = mapper.convertValue(field, HashMap.class);
        fieldMap.put(FIELDS_VARIABLES_PROPERTY_NAME,
                new JsonFieldVariableTransformer(field.fieldVariables()).mapList());
        fieldMap.remove("acceptedDataTypes");
        fieldMap.remove("dbColumn");
        return fieldMap;
    }

    /**
     * Processes special field types (Category and Relationship fields).
     *
     * @param field    The Field object to process.
     * @param fieldMap The map to update with processed information.
     */
    private void processSpecialFieldTypes(Field field, Map<String, Object> fieldMap)
            throws DotDataException, DotSecurityException {

        String className = (String) fieldMap.get("clazz");
        if (ImmutableCategoryField.class.getName().equals(className)) {
            processCategoryField(field, fieldMap);
        } else if (ImmutableRelationshipField.class.getName().equals(className)) {
            processRelationshipField(field, fieldMap);
        }
    }

    /**
     * Processes a Category field, adding category information to the field map.
     *
     * @param field    The Category Field object.
     * @param fieldMap The map to update with category information.
     */
    private void processCategoryField(Field field, Map<String, Object> fieldMap)
            throws DotDataException {

        try {

            Object values = fieldMap.get(VALUES);
            if (UtilMethods.isSet(values)) {
                Category category = APILocator.getCategoryAPI().find(
                        values.toString(),
                        APILocator.getLoginServiceAPI().getLoggedInUser(),
                        false
                );
                if (null == category) {
                    Logger.warn(JsonFieldTransformer.class, () -> String.format(
                            "Unable to find category with id '%s' for field named %s. ",
                            values.toString(), field.name())
                    );
                } else {
                    fieldMap.put(CATEGORIES_PROPERTY_NAME, category.getMap());
                }
            }
        } catch (final DotSecurityException e) {
            Logger.error(JsonFieldTransformer.class, e.getMessage());
        }
    }

    /**
     * Processes a Relationship field, adding relationship information to the field map.
     *
     * @param field    The Relationship Field object.
     * @param fieldMap The map to update with relationship information.
     */
    private void processRelationshipField(Field field, Map<String, Object> fieldMap)
            throws DotDataException, DotSecurityException {

        String cardinality = fieldMap.remove(VALUES).toString();
        String relationType = fieldMap.remove("relationType").toString();

        Map<String, Object> relationshipMap = new HashMap<>();
        relationshipMap.put(CARDINALITY_PROPERTY_NAME, Integer.parseInt(cardinality));
        relationshipMap.put(VELOCITY_VARIABLE_PROPERTY_NAME, relationType);

        if (UtilMethods.isSet(field.contentTypeId())) {
            addRelationshipInfo(field, relationshipMap);
        }

        fieldMap.put(ContentTypeFieldProperties.RELATIONSHIPS.getName(), relationshipMap);
    }

    /**
     * Adds additional relationship information to the relationship map.
     *
     * @param field           The Relationship Field object.
     * @param relationshipMap The map to update with additional relationship information.
     */
    private void addRelationshipInfo(Field field, Map<String, Object> relationshipMap)
            throws DotDataException, DotSecurityException {

        final var relationship = APILocator.getRelationshipAPI().getRelationshipFromField(
                field, APILocator.getLoginServiceAPI().getLoggedInUser()
        );
        if (null != relationship) {
            relationshipMap.put("isParentField",
                    relationship.getParentStructureInode().equals(field.contentTypeId()));
        }
    }

    /**
     * Adds field type labels to the field map.
     *
     * @param field    The Field object.
     * @param fieldMap The map to update with field type labels.
     */
    private void addFieldTypeLabels(Field field, Map<String, Object> fieldMap)
            throws LanguageException {

        fieldMap.put("fieldTypeLabel", LanguageUtil.get(
                APILocator.getLoginServiceAPI().getLoggedInUser(),
                field.getContentTypeFieldLabelKey())
        );
        fieldMap.put("fieldType", field.getContentTypeFieldLabelKey());
    }

}