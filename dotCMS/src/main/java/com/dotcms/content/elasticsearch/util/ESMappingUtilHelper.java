package com.dotcms.content.elasticsearch.util;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldFactory;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.RadioField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.SelectField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.TimeField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.jayway.jsonpath.JsonPath;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.util.StringPool;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Helper class responsible of setting Elasticsearch mapping for content type fields
 * @author nollymar
 */
public class ESMappingUtilHelper {

    private ContentTypeAPI contentTypeAPI;
    private ESMappingAPIImpl esMappingAPI;
    private RelationshipAPI relationshipAPI;

    private static class SingletonHolder {

        private static final ESMappingUtilHelper INSTANCE = new ESMappingUtilHelper();
    }

    public static ESMappingUtilHelper getInstance() {
        return ESMappingUtilHelper.SingletonHolder.INSTANCE;
    }

    private ESMappingUtilHelper() {
        contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        esMappingAPI = new ESMappingAPIImpl();
        relationshipAPI = APILocator.getRelationshipAPI();
    }

    @VisibleForTesting
    ESMappingUtilHelper(final ContentTypeAPI contentTypeAPI, final ESMappingAPIImpl esMappingAPI,
            final RelationshipAPI relationshipAPI) {
        this.contentTypeAPI = contentTypeAPI;
        this.esMappingAPI = esMappingAPI;
        this.relationshipAPI = relationshipAPI;
    }

    /**
     * Sets a custom index mapping for all fields in a full reindex (including relationship fields and
     * field variables that define the `esCustomMapping` key)
     *
     * @param indexName - Index where mapping will be applied
     */
    @CloseDBIfOpened
    public void addCustomMapping(final String indexName) {

        final Set<String> mappedFields = addCustomMappingFromFieldVariables(indexName);

        addCustomMappingForRelationships(indexName, mappedFields);

        addMappingForRemainingFields(indexName, mappedFields);
    }

    /**
     * Sets an ES mapping for a {@link Field} (it does not include field variables) on a specified index.
     * This method is used when a new field is created
     * @param indexName - Index where mapping will be applied
     * @param field
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws IOException
     * @throws JSONException
     */
    @CloseDBIfOpened
    public void addCustomMapping(final String indexName, Field field)
            throws DotSecurityException, DotDataException, IOException, JSONException {
        final ContentType contentType = contentTypeAPI.find(field.contentTypeId());
        if (field instanceof RelationshipField){
            final Relationship relationship = relationshipAPI.getRelationshipFromField(field, APILocator.systemUser());
            putRelationshipMapping(indexName, relationship.getRelationTypeValue().toLowerCase());
        } else {
            final String fieldVariableName = (contentType.variable() + StringPool.PERIOD + field.variable())
                    .toLowerCase();
            putMappingForField(indexName, contentType, field, getMappingForField(field, fieldVariableName));
        }
    }

    /**
     * Sets a mapping for all relationships except for those that contains its custom mapping using
     * field variables
     *
     * @param indexName - Index where mapping will be updated
     * @param mappedFields - Collection that contains the fields with a specific mapping until now.
     * </br> When a put mapping request is sent to Elasticsearch for each relationship (if needed),
     * a new entry is added to the <b>mappedFields</b> collection
     */
    private void addCustomMappingForRelationships(final String indexName,
            final Set<String> mappedFields) {
        final List<Relationship> relationships = relationshipAPI.dbAll();

        for (final Relationship relationship : relationships) {
            final String relationshipName = relationship.getRelationTypeValue().toLowerCase();
            if (!mappedFields.contains(relationshipName)) {

                try {
                    putRelationshipMapping(indexName, relationshipName);

                    //Adds to the set the mapped already set for this field
                    mappedFields.add(relationshipName);
                } catch (Exception e) {
                    handleInvalidCustomMappingError(indexName, relationshipName);
                    final String message =
                            "Error updating index mapping for relationship " + relationshipName
                                    + ". This custom mapping will be ignored for index: "
                                    + indexName;
                    Logger.warn(ESMappingUtilHelper.class, message, e);
                }
            }
        }
    }

    /**
     * Creates a json mapping for a relationship and saves it into the specified index
     * @param indexName - Index where the mapping will be saved (Full index name including cluster id)
     * @param relationshipName - Relationship to be indexed
     * @throws JSONException
     * @throws IOException
     */
    private void putRelationshipMapping(final String indexName, final String relationshipName)
            throws JSONException, IOException {
        final JSONObject properties = new JSONObject();
        properties.put("properties", new JSONObject()
                .put(relationshipName,
                        new JSONObject("{\n"
                                + "\"type\":  \"keyword\",\n"
                                + "\"ignore_above\": 8191\n"
                                + "}")));
        esMappingAPI.putMapping(indexName, properties.toString());
    }

    /**
     * Creates a system message event with an error in case a field mapping fails
     * @param indexName - Index where the mapping is trying to be applied
     * @param fieldName - Field whose mapping is trying to be applied to
     */
    private void handleInvalidCustomMappingError(final String indexName,
            final String fieldName) {

        final SystemMessageEventUtil systemMessageEventUtil = SystemMessageEventUtil.getInstance();

        try {
            systemMessageEventUtil.pushMessage(
                    new SystemMessageBuilder()
                            .setMessage(LanguageUtil.format(Locale.getDefault(),
                                    "notification.reindexing.custom.mapping.error",
                                    new String[]{fieldName, indexName}, false))
                            .setSeverity(MessageSeverity.ERROR)
                            .setType(MessageType.SIMPLE_MESSAGE)
                            .setLife(6000)
                            .create(), null);
        } catch (LanguageException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Sets a mapping defined on field variables
     *
     * @param indexName - Index where mapping will be updated
     * @return Collection of fields names whose mapping was set
     */
    private Set<String> addCustomMappingFromFieldVariables(final String indexName) {
        final FieldFactory fieldFactory = FactoryLocator.getFieldFactory();
        final Set<String> mappedFields = new HashSet<>();

        try {
            //Find field variables
            final List<FieldVariable> fieldVariables = fieldFactory
                    .byFieldVariableKey(FieldVariable.ES_CUSTOM_MAPPING_KEY);

            for (final FieldVariable fieldVariable : fieldVariables) {
                Field field = null;
                ContentType type = null;
                try {

                    field = fieldFactory.byId(fieldVariable.fieldId());
                    type = contentTypeAPI.find(field.contentTypeId());

                    putMappingForField(indexName, type, field, fieldVariable.value());


                    //Adds to the set the mapped already set for this field
                    mappedFields.add((type.variable() + StringPool.PERIOD + field.variable())
                            .toLowerCase());

                } catch (Exception e) {
                    handleInvalidCustomMappingError(indexName,
                            type != null ? type.variable() + "." + field.variable() : "[]");
                    String message = "Error setting custom index mapping from field variable "
                            + fieldVariable.key();

                    if (field != null) {
                        message += ". Field: " + field.name();
                    }

                    if (type != null) {
                        message += ". Content Type: " + type.name();
                    }

                    message += ". Custom mapping will be ignored for index: " + indexName;
                    Logger.warn(ESMappingUtilHelper.class, message, e);
                }
            }
        } catch (DotDataException e) {
            Logger.warn(ESMappingUtilHelper.class,
                    "Error setting custom index mapping for index " + indexName, e);
        }
        return mappedFields;
    }

    /**
     *
     * @param indexName Index where the mapping will be applied
     * @param mappedFields Collection of fields already mapped in the index. This collection is used to avoid duplicate mappings for fields, which could cause an explosion
     */
    private void addMappingForRemainingFields(final String indexName,
            final Set<String> mappedFields) {
        try {
            final List<ContentType> contentTypes = contentTypeAPI.findAll();
            contentTypes.forEach(contentType -> contentType.fields().forEach(
                    field -> addMappingForFieldIfNeeded(indexName, contentType, field,
                            mappedFields)));
        } catch (DotDataException e) {
            Logger.warnAndDebug(ESMappingUtilHelper.class,
                    "It was not possible to get content types to map field types in Elasticsearch"
                            + indexName, e);
        }
    }

    /**
     * Defines an ES custom mapping for dates, numbers and text fields, excluding those that match the mapping defined in the `es-content.mapping.json` file
     * @param indexName Index where the mapping will be applied
     * @param contentType Content type's whose field will be mapped
     * @param field Field to be mapped
     * @param mappedFields Collection of fields already mapped in the index. This collection is used to avoid duplicate mappings for fields, which could cause an explosion
     */
    private void addMappingForFieldIfNeeded(final String indexName,
            final ContentType contentType, final Field field, final Set<String> mappedFields) {
        final String fieldVariableName = (contentType.variable() + StringPool.PERIOD + field.variable())
                        .toLowerCase();
        if (!mappedFields.contains(fieldVariableName)) {
            String mappingForField = getMappingForField(field, fieldVariableName);
            if (mappingForField != null) {
                try {
                    putMappingForField(indexName, contentType, field, mappingForField);
                    //Adds to the set the mapped already set for this field
                    mappedFields.add(fieldVariableName);
                } catch (Exception e) {
                    handleInvalidCustomMappingError(indexName, fieldVariableName);
                    final String message =
                            "Error updating index mapping for field " + fieldVariableName
                                    + ". This custom mapping will be ignored for index: "
                                    + indexName;
                    Logger.warn(ESMappingUtilHelper.class, message, e);
                }
            }
        }
    }

    /**
     * Given a {@link Field}, obtains the ES mapping according to the {@link Field} type
     * @param field
     * @param fieldVariableName
     * @return
     */
    private String getMappingForField(final Field field, final String fieldVariableName) {
        String mappingForField = null;
        if (field instanceof DateField || field instanceof DateTimeField
                || field instanceof TimeField) {
            mappingForField = "{\n\"type\":\"date\",\n";
            mappingForField += "\"format\": \"yyyy-MM-dd't'HH:mm:ss||MMM d, yyyy h:mm:ss a||yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis\"\n}";
        } else if (field instanceof TextField || field instanceof TextAreaField
                || field instanceof WysiwygField || field instanceof RadioField
                || field instanceof SelectField) {
            if (field.dataType() == DataTypes.BOOL) {
                mappingForField = "{\n\"type\":\"boolean\"\n}";
            } else if (field.dataType() == DataTypes.FLOAT) {
                mappingForField = "{\n\"type\":\"float\"\n}";
            } else if (field.dataType() == DataTypes.INTEGER) {
                mappingForField = "{\n\"type\":\"integer\"\n}";
            } else if (!matchesExclusions(fieldVariableName)){
                mappingForField = "{\n\""
                        + "type\":\"text\","
                        + "analyzer\":\"my_analyzer\","
                        + "store\":\"false\""
                        + "\n}";
            }
        }
        return mappingForField;
    }

    /**
     * Generates a json mapping for a field with the details set in `mappingForField`.
     * This mapping will be applied into the specified index
     * @param indexName - Index where the mapping will be applied (Full index name including cluster id)
     * @param contentType
     * @param field
     * @param mappingForField - Mapping details to be added
     * @throws JSONException
     * @throws IOException
     */
    private void putMappingForField(final String indexName, final ContentType contentType, final Field field,
            final String mappingForField) throws JSONException, IOException {

        final JSONObject jsonObject = new JSONObject();
        final JSONObject properties = new JSONObject();

        jsonObject.put(contentType.variable().toLowerCase(),
                new JSONObject()
                        .put("properties", new JSONObject()
                                .put(field.variable().toLowerCase(),
                                        new JSONObject(mappingForField))));
        properties.put("properties", jsonObject);
        esMappingAPI.putMapping(indexName, properties.toString());
    }



    /**
     * Verifies if a field variable name is part of the exclusions defined in the `es-content-mapping.json` </p>
     * Those exclusions will be handled directly by Elasticsearch using dynamic mappings
     * @param fieldVarName field variable name that will be evaluated
     * @return
     */
    private boolean matchesExclusions(final String fieldVarName) {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final URL url = classLoader.getResource("es-content-mapping.json");
        final String defaultSettings;
        try {
            defaultSettings = new String(
                    com.liferay.util.FileUtil.getBytes(new File(url.getPath())));

            final List<String> matches = JsonPath.read(defaultSettings, "$..match");
            matches.addAll(JsonPath.read(defaultSettings, "$..path_match"));

            final Pattern pattern = Pattern
                    .compile(matches.stream().map(match -> match.replaceAll("\\.", "\\\\.")
                            .replaceAll("\\*", "\\.*")).collect(
                            Collectors.joining("|")));

            return pattern.matcher(fieldVarName).matches();
        } catch (IOException e) {
            Logger.warnAndDebug(ESMappingUtilHelper.class,
                    "cannot load es-content-mapping.json file, skipping", e);
        }

        return false;
    }
}
