package com.dotcms.content.elasticsearch.util;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.content.index.IndexTag;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldFactory;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.MultiSelectField;
import com.dotcms.contenttype.model.field.RadioField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.SelectField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.TimeField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.JsonPath;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Helper class responsible of setting Elasticsearch mapping for content type fields
 * @author nollymar
 */
public class ESMappingUtilHelper implements MappingHelper {

    // -------------------------------------------------------------------------
    // Leaf put-mapping abstraction
    // -------------------------------------------------------------------------

    /**
     * Functional interface for the single {@code putMapping} call at the bottom of the
     * mapping cascade.
     *
     * <p>Injected at each public entry point so the six private helper methods are shared
     * between the phase-dispatch path
     * ({@link ESMappingAPIImpl#putMapping(List, String)}) and the targeted path
     * ({@link ESMappingAPIImpl#putMapping(List, String, IndexTag)}) without duplication.</p>
     */
    @FunctionalInterface
    interface PutMappingFn {
        void apply(List<String> indexes, String mappingJson) throws IOException;
    }

    // -------------------------------------------------------------------------
    // Fields & singleton
    // -------------------------------------------------------------------------

    private final ContentTypeAPI contentTypeAPI;
    private final ESMappingAPIImpl esMappingAPI;
    private final RelationshipAPI relationshipAPI;

    private static class SingletonHolder {

        private static final ESMappingUtilHelper INSTANCE = new ESMappingUtilHelper();
    }

    /**
     * Returns the singleton instance of this helper.
     *
     * @return the shared {@link ESMappingUtilHelper} instance
     */
    public static ESMappingUtilHelper getInstance() {
        return ESMappingUtilHelper.SingletonHolder.INSTANCE;
    }

    private ESMappingUtilHelper() {
        contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        esMappingAPI = new ESMappingAPIImpl();
        relationshipAPI = APILocator.getRelationshipAPI();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Sets a custom index mapping for all fields in a full reindex (including relationship fields and
     * field variables that define the `esCustomMapping` key)
     * @param indexes where mapping will be applied
     */
    @CloseDBIfOpened
    public void addCustomMapping(final String... indexes) {
        final PutMappingFn putFn = (idx, json) -> esMappingAPI.putMapping(idx, json);
        final Set<String> mappedFields = addCustomMappingFromFieldVariables(putFn, indexes);
        addCustomMappingForRelationships(mappedFields, putFn, indexes);
        addMappingForRemainingFields(mappedFields, putFn, indexes);
    }

    /**
     * Sets an ES mapping for a {@link Field} (it does not include field variables) on the specified indexes
     * This method is used when a new field is created
     * @param field
     * @param indexes
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws IOException
     * @throws JSONException
     */
    @CloseDBIfOpened
    public void addCustomMapping(final Field field, final String... indexes)
            throws DotSecurityException, DotDataException, IOException, JSONException {
        final PutMappingFn putFn = (idx, json) -> esMappingAPI.putMapping(idx, json);
        applyFieldMapping(field, putFn, indexes);
    }

    /**
     * Targeted overload: applies the full reindex mapping exclusively to the provider
     * identified by {@code tag}, regardless of the current migration phase.
     *
     * <p>Use this when the caller knows that only one backend needs its mapping refreshed
     * (e.g. OS catchup during migration). The no-tag variant
     * {@link #addCustomMapping(String...)} continues to fan out to all write providers.</p>
     *
     * @param indexes plain (untagged) index names where mapping will be applied
     * @param tag     the target vendor ({@link IndexTag#ES} or {@link IndexTag#OS})
     */
    @CloseDBIfOpened
    public void addCustomMapping(final List<String> indexes, final IndexTag tag) {
        final String[] indexArray = indexes.toArray(String[]::new);
        final PutMappingFn putFn = (idx, json) -> esMappingAPI.putMapping(idx, json, tag);
        final Set<String> mappedFields = addCustomMappingFromFieldVariables(putFn, indexArray);
        addCustomMappingForRelationships(mappedFields, putFn, indexArray);
        addMappingForRemainingFields(mappedFields, putFn, indexArray);
    }

    /**
     * Targeted overload: applies the field-level mapping exclusively to the provider
     * identified by {@code tag}, regardless of the current migration phase.
     *
     * @param field   the field whose mapping will be set
     * @param indexes plain (untagged) index names where mapping will be applied
     * @param tag     the target vendor ({@link IndexTag#ES} or {@link IndexTag#OS})
     */
    @CloseDBIfOpened
    public void addCustomMapping(final Field field, final List<String> indexes, final IndexTag tag)
            throws DotSecurityException, DotDataException, IOException, JSONException {
        final String[] indexArray = indexes.toArray(String[]::new);
        final PutMappingFn putFn = (idx, json) -> esMappingAPI.putMapping(idx, json, tag);
        applyFieldMapping(field, putFn, indexArray);
    }

    // -------------------------------------------------------------------------
    // Private helpers — shared by both phase-dispatch and targeted paths
    // -------------------------------------------------------------------------

    /**
     * Applies the mapping for a single {@link Field} using the given put function.
     * Handles both relationship fields (keyword mapping) and regular fields (type mapping).
     */
    private void applyFieldMapping(final Field field, final PutMappingFn putFn, final String... indexes)
            throws DotSecurityException, DotDataException, IOException, JSONException {
        final ContentType contentType = contentTypeAPI.find(field.contentTypeId());
        if (field instanceof RelationshipField) {
            final Relationship relationship = relationshipAPI
                    .getRelationshipFromField(field, APILocator.systemUser());
            putRelationshipMapping(relationship.getRelationTypeValue().toLowerCase(), putFn, indexes);
        } else {
            final String fieldVariableName = (contentType.variable() + StringPool.PERIOD + field
                    .variable()).toLowerCase();
            final Optional<List<Tuple2<String, JSONObject>>> mapping = getMappingForField(field,
                    fieldVariableName);
            if (mapping.isPresent()) {
                putContentTypeMapping(contentType, mapping.get().stream()
                        .collect(Collectors.toMap(tuple -> tuple._1(), tuple -> tuple._2())),
                        putFn, indexes);
            }
        }
    }

    /**
     * Sets a mapping defined on field variables.
     *
     * @param putFn  leaf function that sends the assembled JSON to the target provider(s)
     * @param indexes where the mapping will be set
     * @return set of fully-qualified field names ({@code contentType.field}) that were mapped
     */
    private Set<String> addCustomMappingFromFieldVariables(final PutMappingFn putFn,
            final String... indexes) {
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

                    putContentTypeMapping(type, Map.of(field.variable().toLowerCase(),
                            new JSONObject(fieldVariable.value())), putFn, indexes);

                    //Adds to the set the mapped already set for this field
                    mappedFields.add((type.variable() + StringPool.PERIOD + field.variable())
                            .toLowerCase());

                } catch (Exception e) {
                    handleInvalidCustomMappingError("notification.reindexing.custom.mapping.error",
                            type != null ? type.variable() + "." + field.variable()
                                    : "[]", indexes);
                    final StringBuilder message = new StringBuilder("Error setting custom index mapping from field variable ");
                    message.append(fieldVariable.key());

                    if (field != null) {
                        message.append(". Field: ").append(field.name());
                    }

                    if (type != null) {
                        message.append(". Content Type: ").append(type.name());
                    }

                    message.append(
                            ". Custom mapping will be ignored for index(es).").append(Arrays.stream(indexes)
                                    .collect(Collectors.joining(",")));
                    Logger.warn(ESMappingUtilHelper.class, message.toString(), e);
                }
            }
        } catch (DotDataException e) {
            Logger.warn(ESMappingUtilHelper.class,
                    "Error setting custom index mapping for indexes", e);
        }
        return mappedFields;
    }

    /**
     * Sets a mapping for all relationships except for those that contains its custom mapping using
     * field variables.
     *
     * @param mappedFields - Collection that contains the fields with a specific mapping until now.
     * </br> When a put mapping request is sent to Elasticsearch for each relationship (if needed),
     * a new entry is added to the <b>mappedFields</b> collection
     * @param putFn        leaf function that sends the assembled JSON to the target provider(s)
     * @param indexes      where mapping will be applied
     */
    private void addCustomMappingForRelationships(final Set<String> mappedFields,
            final PutMappingFn putFn, final String... indexes) {
        final List<Relationship> relationships = relationshipAPI.dbAll();

        for (final Relationship relationship : relationships) {
            final String relationshipName = relationship.getRelationTypeValue().toLowerCase();
            if (!mappedFields.contains(relationshipName)) {

                try {
                    putRelationshipMapping(relationshipName, putFn, indexes);

                    //Adds to the set the mapped already set for this field
                    mappedFields.add(relationshipName);
                } catch (Exception e) {
                    handleInvalidCustomMappingError("notification.reindexing.custom.mapping.error",
                            relationshipName, indexes);

                    final String message =
                            "Error updating index mapping for relationship " + relationshipName
                                    + ". This custom mapping will be ignored for index(es) "
                                    + Arrays.stream(indexes).collect(Collectors.joining(","));
                    Logger.warn(ESMappingUtilHelper.class, message, e);
                }
            }
        }
    }

    /**
     * Sets mapping for all indexed fields in the system that do not contain a mapping.
     *
     * @param mappedFields Collection of fields already mapped in the index. This collection is used
     *                     to avoid duplicate mappings for fields, which could cause an explosion
     * @param putFn        leaf function that sends the assembled JSON to the target provider(s)
     * @param indexes      where the mapping will be applied
     */
    private void addMappingForRemainingFields(final Set<String> mappedFields,
            final PutMappingFn putFn, final String... indexes) {
        try {
            final List<ContentType> contentTypes = contentTypeAPI.findAll();
            contentTypes.forEach(
                    contentType -> addMappingForContentTypeIfNeeded(contentType, mappedFields,
                            putFn, indexes));
        } catch (DotDataException e) {
            Logger.warnAndDebug(ESMappingUtilHelper.class,
                    "It was not possible to get content types to map field types in Elasticsearch",
                    e);
        }
    }

    /**
     * Adds a mapping for all indexed fields in a given content type. Only fields that do not have a mapping set will be mapped.</b>
     * (relationship fields or fields whose mapping was set using field variables will be excluded)
     * @param contentType
     * @param mappedFields Collection of fields already mapped in the index. This collection is used
     *                     to avoid duplicate mappings for fields, which could cause an explosion
     * @param putFn        leaf function that sends the assembled JSON to the target provider(s)
     * @param indexes      where the mapping will be set
     */
    private void addMappingForContentTypeIfNeeded(final ContentType contentType,
            final Set<String> mappedFields, final PutMappingFn putFn, final String... indexes) {
        final Map<String, JSONObject> contentTypeMapping = new HashMap<>();
        try {
            contentType.fields().forEach(field-> {
                    try {
                        addMappingForFieldIfNeeded(contentType, field,
                                mappedFields, contentTypeMapping);
                    } catch (JSONException e) {
                        throw new DotRuntimeException(e);
                    }
                }
            );

            putContentTypeMapping(contentType, contentTypeMapping, putFn, indexes);
        } catch (Exception e) {
            handleInvalidCustomMappingError(
                    "notification.reindexing.content.type.mapping.error",
                    contentType.name(), indexes);
            final String message =
                    "Error updating index mapping for content type " + contentType.name()
                            + ". This custom mapping will be ignored for index(es) " +
                            Arrays.stream(indexes).collect(Collectors.joining(","));
            Logger.warn(ESMappingUtilHelper.class, message, e);
        }
    }

    /**
     * Defines an ES custom mapping for dates, numbers and text fields, excluding those that match the mapping defined in the `es-content.mapping.json` file
     * @param contentType Content type's whose fields will be mapped
     * @param field Field to be mapped
     * @param mappedFields Collection of fields already mapped in the index. This collection is used to avoid duplicate mappings for fields, which could cause an explosion
     * @param contentTypeMapping Collection where the field mapping will be appended
     */
    private void addMappingForFieldIfNeeded(
            final ContentType contentType, final Field field, final Set<String> mappedFields,
            final Map<String, JSONObject> contentTypeMapping)
            throws JSONException {
        if(!field.indexed()) {
            return;
        }
        final String fieldVariableName = (contentType.variable() + StringPool.PERIOD + field.variable())
                        .toLowerCase();
        if (!mappedFields.contains(fieldVariableName)) {
            final Optional<List<Tuple2<String, JSONObject>>> mappingForField = getMappingForField(field, fieldVariableName);
            if (mappingForField.isPresent()) {
                contentTypeMapping.putAll(mappingForField.get().stream()
                        .collect(Collectors.toMap(tuple -> tuple._1(), tuple -> tuple._2())));
                    //Adds to the set the mapped already set for this field
                    mappedFields.add(fieldVariableName);

            }
        }
    }

    /**
     * Given a {@link Field}, obtains its ES mapping according to the {@link Field}'s type
     * @param field
     * @param fieldVariableName
     * @return A map with just one element
     */
    private Optional<List<Tuple2<String, JSONObject>>> getMappingForField(final Field field, final String fieldVariableName)
            throws JSONException {
        final Map<DataTypes, String> dataTypesMap = ImmutableMap
                .of(DataTypes.BOOL, "boolean", DataTypes.FLOAT, "double", DataTypes.INTEGER,
                        "long");
        String mappingForField = null;

        if (!matchesExclusions(fieldVariableName)) {
            if (field instanceof DateField || field instanceof DateTimeField
                    || field instanceof TimeField) {

                mappingForField = "{\n\"type\":\"date\",\n";

                try {
                    final Map<String, Object> jsonFileContent = JsonUtil.getJsonFileContent(
                            "es-content-mapping.json");

                    mappingForField += String.format("\"format\": \"%s\"\n}",
                            ((List) jsonFileContent.get("dynamic_date_formats")).get(0));
                } catch (IOException e) {
                    Logger.error("Error getting es-content-mapping.json file: " + e.getMessage(), e);
                    throw new JSONException(e);
                }
            } else if (field instanceof TextField || field instanceof TextAreaField
                    || field instanceof WysiwygField || field instanceof RadioField
                    || field instanceof SelectField || field instanceof MultiSelectField
                    || field instanceof TagField || field instanceof StoryBlockField) {

                if (dataTypesMap.containsKey(field.dataType())) {
                    mappingForField = String.format(
                            "{\n\"type\":\"%s\"\n}",
                            dataTypesMap.get(field.dataType()));
                } else {
                    if (field.unique() || field instanceof TagField) {
                        mappingForField = "{\n\"type\":\"keyword\"\n}";
                    } else {
                        mappingForField = "{\n"
                                + ("\"type\":\"text\",\n")
                                + "\"analyzer\":\"my_analyzer\""
                                + "\n}";
                    }
                }
            }
        }

        if (mappingForField!= null){
            final List<Tuple2<String, JSONObject>> mappingList = new ArrayList<>();
            mappingList.add(Tuple.of(field.variable().toLowerCase(),
                    new JSONObject(mappingForField)));

            //Put mapping for _dotraw, _sha256 and _text fields if needed
            mappingList.add(Tuple.of(field.variable().toLowerCase() + "_dotraw",
                    new JSONObject("{\n"
                            + "\"type\":\"keyword\",\n"
                            + "\"ignore_above\": 8191"
                            + "\n}")));

            mappingList.add(Tuple.of(field.variable().toLowerCase() + ESUtils.SHA_256,
                    new JSONObject("{\n"
                            + "\"type\":\"keyword\",\n"
                            + "\"ignore_above\": 8191"
                            + "\n}")));

            if (Config
                    .getBooleanProperty("CREATE_TEXT_INDEX_FIELD_FOR_NON_TEXT_FIELDS", false)) {
                mappingList.add(Tuple.of(field.variable().toLowerCase() + ESMappingAPIImpl.TEXT,
                        new JSONObject("{\n\"type\":\"text\"\n}")));
            }

            return Optional.of(mappingList);
        }else{
            return Optional.empty();
        }
    }

    /**
     * Creates a json mapping for a relationship and sends it to the target provider(s) via
     * {@code putFn}.
     *
     * @param relationshipName lowercase relation type value used as the field name in the mapping
     * @param putFn            leaf function that sends the assembled JSON to the target provider(s)
     * @param indexes          where mapping will be applied
     * @throws JSONException if the mapping JSON cannot be constructed
     * @throws IOException   if the underlying REST call fails
     */
    private void putRelationshipMapping(final String relationshipName, final PutMappingFn putFn,
            final String... indexes) throws JSONException, IOException {
        final JSONObject properties = new JSONObject();
        properties.put("properties", new JSONObject()
                .put(relationshipName,
                        new JSONObject("{\n"
                                + "\"type\":  \"keyword\",\n"
                                + "\"ignore_above\": 8191\n"
                                + "}")));
        putFn.apply(CollectionsUtils.list(indexes), properties.toString());
    }

    /**
     * Generates a json mapping for a content type with the details set in {@code mappingForFields}
     * and sends it to the target provider(s) via {@code putFn}.
     *
     * @param contentType      the content type whose variable name scopes the mapping
     * @param mappingForFields mapping details to be added to a particular field
     * @param putFn            leaf function that sends the assembled JSON to the target provider(s)
     * @param indexes          where the mapping will be applied
     * @throws JSONException if the mapping JSON cannot be constructed
     * @throws IOException   if the underlying REST call fails
     */
    private void putContentTypeMapping(final ContentType contentType,
            final Map<String, JSONObject> mappingForFields, final PutMappingFn putFn,
            final String... indexes) throws JSONException, IOException {

        final JSONObject jsonObject = new JSONObject();
        final JSONObject properties = new JSONObject();

        jsonObject.put(contentType.variable().toLowerCase(),
                new JSONObject()
                        .put("properties", mappingForFields));

        properties.put("properties", jsonObject);
        putFn.apply(CollectionsUtils.list(indexes), properties.toString());
    }

    /**
     * Creates a system message event with an error in case a field mapping fails
     * @param messageKey - key in the Language.properties
     * @param field - Field whose map failed
     * @param indexes - Indexes where the map failed
     */
    private void handleInvalidCustomMappingError(final String messageKey, final String field, final String... indexes) {

        final SystemMessageEventUtil systemMessageEventUtil = SystemMessageEventUtil.getInstance();

        try {
            final String systemMessage = LanguageUtil.format(Locale.getDefault(),
                    messageKey,
                    new String[]{field, String.join(",", indexes)},
                    false);
            systemMessageEventUtil.pushMessage(
                    new SystemMessageBuilder()
                            .setMessage(systemMessage)
                            .setSeverity(MessageSeverity.ERROR)
                            .setType(MessageType.SIMPLE_MESSAGE)
                            .setLife(6000)
                            .create(), null);
        } catch (LanguageException languageException) {
            Logger.debug(this, "Error sending notification message ", languageException);
        }
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
