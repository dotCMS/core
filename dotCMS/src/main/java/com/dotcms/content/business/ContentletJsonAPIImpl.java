package com.dotcms.content.business;

import static com.dotmarketing.portlets.contentlet.model.Contentlet.DISABLED_WYSIWYG_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.FOLDER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.HOST_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.IDENTIFIER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.INODE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.LANGUAGEID_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.MOD_DATE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.MOD_USER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.OWNER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.SORT_ORDER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.STRUCTURE_INODE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.TITTLE_KEY;
import static com.dotmarketing.util.UtilMethods.isSet;

import com.dotcms.content.model.Contentlet;
import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.ImmutableContentlet;
import com.dotcms.content.model.ImmutableContentlet.Builder;
import com.dotcms.content.model.type.text.FloatTextFieldType;
import com.dotcms.content.model.type.text.LongTextFieldType;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.HiddenField;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.LineDividerField;
import com.dotcms.contenttype.model.field.PermissionTabField;
import com.dotcms.contenttype.model.field.RelationshipsTabField;
import com.dotcms.contenttype.model.field.TabDividerField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.BinaryFileFilter;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.io.File;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This is class takes care of translating a contentlet from it's regular mutable representation
 * given by @see com.dotmarketing.portlets.contentlet.model.Contentlet to json For that purpose we
 * use an intermediate representation generated via Immutables @see com.dotcms.content.model.Contentlet
 * This is based on the original logic located in @See com.dotmarketing.portlets.contentlet.transform.ContentletTransformer
 * which reads the contentlet from the columns located in the contentlet table.
 * This is meant to deal with a json representation of contentlet stored in only one column.
 */
public class ContentletJsonAPIImpl implements ContentletJsonAPI {

    private static final BinaryFileFilter binaryFileFilter = new BinaryFileFilter();

    final IdentifierAPI identifierAPI;
    final ContentTypeAPI contentTypeAPI;
    final FileAssetAPI fileAssetAPI;
    final ContentletAPI contentletAPI;
    final FolderAPI folderAPI;
    final HostAPI hostAPI;

    /**
     * API-Parametrized constructor
     * @param identifierAPI
     * @param contentTypeAPI
     * @param fileAssetAPI
     * @param contentletAPI
     * @param folderAPI
     * @param hostAPI
     */
    @VisibleForTesting
    ContentletJsonAPIImpl(final IdentifierAPI identifierAPI,
            final ContentTypeAPI contentTypeAPI,
            final FileAssetAPI fileAssetAPI,
            final ContentletAPI contentletAPI,
            final FolderAPI folderAPI,
            final HostAPI hostAPI) {
        this.identifierAPI = identifierAPI;
        this.contentTypeAPI = contentTypeAPI;
        this.fileAssetAPI = fileAssetAPI;
        this.contentletAPI = contentletAPI;
        this.folderAPI = folderAPI;
        this.hostAPI = hostAPI;
    }

    /**
     * Default Constructor
     */
    public ContentletJsonAPIImpl() {
        this(APILocator.getIdentifierAPI(), APILocator.getContentTypeAPI(APILocator.systemUser()),
             APILocator.getFileAssetAPI(), APILocator.getContentletAPI(),
             APILocator.getFolderAPI(), APILocator.getHostAPI());
    }

    /**
     * Public entry point. Going from json to regular contentlet
     * @param contentlet regular "mutable" contentlet
     * @return String json representation
     * @throws JsonProcessingException
     * @throws DotDataException
     */
    public String toJson(final com.dotmarketing.portlets.contentlet.model.Contentlet contentlet)
            throws JsonProcessingException, DotDataException {
        return objectMapper.get().writeValueAsString(toImmutable(contentlet));
    }

    /**
     * internal method Makes a regular contentlet and makes an ImmutableContentlet which later will be translated into a json
     * @param contentlet
     * @return
     */
    ImmutableContentlet toImmutable(
            final com.dotmarketing.portlets.contentlet.model.Contentlet contentlet) {

        final Builder builder = ImmutableContentlet.builder();
        builder.baseType(contentlet.getBaseType().orElseGet(() -> BaseContentType.ANY).toString());
        builder.contentType(contentlet.getContentType().id());
        // Don't use the title getter method here.
        // Title is a nullable field and the getter is meant to always calculate something.
        // it's ok if it's null sometimes. We need to mirror the old columns behavior.
        builder.title((String)contentlet.get("title"));
        builder.languageId(contentlet.getLanguageId());
        builder.friendlyName(contentlet.getStringProperty("friendlyName"));
        builder.showOnMenu(contentlet.getBoolProperty("showOnMenu"));
        builder.owner(contentlet.getOwner());//comes from inode
        builder.sortOrder(contentlet.getSortOrder());
        builder.disabledWysiwyg(contentlet.getDisabledWysiwyg());
        builder.modUser(contentlet.getModUser());
        builder.modDate(Try.of(() -> contentlet.getModDate().toInstant()).getOrNull());
        builder.host(contentlet.getHost());
        builder.folder(contentlet.getFolder());

        //These two are definitively mandatory but..
        //intenralCheckIn calls "save" twice and the first time it is called these two aren't already set
        //At that moment we have to fake it to make it. Second save should provide the actual identifiers.
        //We'll have to use empty strings to prevent breaking the execution.
        builder.identifier(UtilMethods.isNotSet(contentlet.getIdentifier()) ? StringPool.BLANK : contentlet.getIdentifier() );
        builder.inode( UtilMethods.isNotSet(contentlet.getInode()) ? StringPool.BLANK : contentlet.getInode() );

        final List<Field> fields = contentlet.getContentType().fields();
        for (final Field field : fields) {
            if (isNotMappable(field)) {
                continue;
            }
            final Object value = contentlet.get(field.variable());
            if (null != value) {
                final Optional<FieldValue<?>> fieldValue = getFieldValue(value, field);
                if (!fieldValue.isPresent()) {
                    Logger.debug(ContentletJsonAPIImpl.class,()->
                            String.format("Unable to set field `%s` with the given value %s.",
                                    field.name(), value));
                } else {
                    builder.putFields(field.variable(), fieldValue.get());
                }
            } else {
                Logger.debug(ContentletJsonAPIImpl.class,()->
                        String.format("Unable to set field `%s` as it wasn't set on the source contentlet", field.name()));

                if(Config.getBooleanProperty(JSON_NUMERIC_FIELD_DEFAULT_TO_ZERO, true)) {
                    final FieldValueInitializer<?> fieldValueInitializer = initializeWithValue
                            .get(Tuple.of(field.getClass(),field.dataType()));
                    if (null != fieldValueInitializer) {
                        builder.putFields(field.variable(), fieldValueInitializer.init());
                        Logger.debug(ContentletJsonAPIImpl.class,
                                String.format("Field `%s` was set to a default.", field.name()));
                    }
                }
            }
        }
        return builder.build();
    }

    interface FieldValueInitializer<T>{
        FieldValue <T> init();
    }

    final Map<Tuple2<Class<?>,DataTypes>, FieldValueInitializer<?>> initializeWithValue = ImmutableMap.of(
            Tuple.of(ImmutableTextField.class, DataTypes.INTEGER), (FieldValueInitializer<Long>) () -> LongTextFieldType.of(0L),
            Tuple.of(ImmutableTextField.class, DataTypes.FLOAT), (FieldValueInitializer<Float>) () -> FloatTextFieldType.of(0F)
    );

    /**
     * Public entry point when going from the json representation to a regular "mutable" contentlet
     * @param json
     * @return
     * @throws JsonProcessingException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public com.dotmarketing.portlets.contentlet.model.Contentlet mapContentletFieldsFromJson(final String json)
            throws JsonProcessingException, DotDataException, DotSecurityException{
        final Map<String, Object> map = mapFieldsFromJson(json);
        return new com.dotmarketing.portlets.contentlet.model.Contentlet(map);
    }

    /**
     * Internal method, takes the json a makes a map that later is used to build a regular mutable contentlet
     * @param json
     * @return
     * @throws JsonProcessingException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Map<String, Object> mapFieldsFromJson(final String json)
            throws JsonProcessingException, DotDataException, DotSecurityException {

        final Contentlet immutableContentlet = immutableFromJson(json);
        final Map<String, Object> map = new HashMap<>();
        final String inode = immutableContentlet.inode();
        final String identifier = immutableContentlet.identifier();
        final String contentTypeId = immutableContentlet.contentType();

        map.put(INODE_KEY, inode);
        map.put(IDENTIFIER_KEY, identifier);
        map.put(STRUCTURE_INODE_KEY, contentTypeId);
        map.put(MOD_DATE_KEY, Date.from(immutableContentlet.modDate()));
        map.put(MOD_USER_KEY, immutableContentlet.modUser());
        map.put(OWNER_KEY, immutableContentlet.owner());
        map.put(TITTLE_KEY, immutableContentlet.title());
        map.put(SORT_ORDER_KEY, immutableContentlet.sortOrder());
        map.put(LANGUAGEID_KEY, immutableContentlet.languageId());
        map.put(HOST_KEY,immutableContentlet.host());
        map.put(FOLDER_KEY,immutableContentlet.folder());
        map.put(DISABLED_WYSIWYG_KEY,immutableContentlet.disabledWysiwyg());

        final ContentType contentType = contentTypeAPI.find(contentTypeId);
        final Map<String, Field> fieldsByVarName = contentType.fields().stream()
                .collect(Collectors.toMap(Field::variable, Function.identity()));

        final Map<String, FieldValue<?>> contentletFields = immutableContentlet.fields();

        for (final Entry<String, Field> entry : fieldsByVarName.entrySet()) {

            final Field field = entry.getValue();
            if (isNotMappable(field)) {
                continue;
            }

            final Object value;
            if (field instanceof ConstantField) {
                value = field.values();
            } else {
                if (isSet(identifier) && isFileAsset(contentType, field)) {
                    value = identifierAPI.find(identifier).getAssetName();
                } else {
                    if (field instanceof BinaryField) {
                        value = getBinary(field, inode).orElse(null);
                    } else {
                        value = getValue(contentletFields, field);
                    }
                }
            }
            map.put(field.variable(), value);
        }

        return map;
    }

    /**
     * Determine if we're looking at File-Asset.
     * @param contentType
     * @param field
     * @return
     */
    private boolean isFileAsset(final ContentType contentType, final Field field) {
        return (contentType instanceof FileAssetContentType && FileAssetAPI.FILE_NAME_FIELD
                .equals(field.variable()));
    }

    /**
     * Used to determine if we're looking at readOnly field.
     * @param field
     * @return
     */
    private boolean isSettable(final Field field) {
        return !(
                field instanceof LineDividerField ||
                        field instanceof TabDividerField ||
                        field instanceof ColumnField ||
                        field instanceof CategoryField ||
                        field instanceof PermissionTabField ||
                        field instanceof RelationshipsTabField
        );
    }

    /**
     * Used to determine if we're looking at a system field excluding BinaryFields, HiddenField which must make it into the json.
     * @param field
     * @return
     */
    private boolean isNoneMappableSystemField(final Field field) {
        return (field.dataType() == DataTypes.SYSTEM &&
                  !(field instanceof BinaryField) && !(field instanceof HiddenField)
        );
    }

    /**
     * Metadata must be skipped. Even though its KeyValue it should never make it into the final json
     * @param field
     * @return
     */
    private boolean isMetadataField(final Field field){
        return (field instanceof KeyValueField && FileAssetAPI.META_DATA_FIELD.equals(field.variable()));
    }

    /**
     * This method basically tells whether or not the field must be processed.
     * @param field
     * @return
     */
    private boolean isNotMappable(final Field field) {
        return (!isSettable(field) || (field instanceof HostFolderField)
                || (field instanceof TagField) || isNoneMappableSystemField(field) || isMetadataField(field)
        );
    }

    /**
     * Once a BinaryField is found this will rebuild it.
     * @param field
     * @param inode
     * @return
     */
    private Optional<File> getBinary(final Field field, final String inode) {
        // This validation is here to prevent an exception.
        // Cause the json gets saved twice by internalCheckin and the first time it does it no inode is set yet

        final java.io.File binaryFileFolder = new java.io.File(
                fileAssetAPI.getRealAssetsRootPath()
                        + java.io.File.separator
                        + inode.charAt(0)
                        + java.io.File.separator
                        + inode.charAt(1)
                        + java.io.File.separator
                        + inode
                        + java.io.File.separator
                        + field.variable());
        if (binaryFileFolder.exists()) {
            final java.io.File[] files = binaryFileFolder.listFiles(binaryFileFilter);
            if (files != null && files.length > 0) {
                return Optional.of(files[0]);
            }
        }

        return Optional.empty();
    }

    /**
     * Given the map of fieldValues indexed by VarName this applies any additional conversion logic that might be required
     * @param fields
     * @param field
     * @return
     */
    private Object getValue(final Map<String, FieldValue<?>> fields, final Field field){
       final Object value = Try.of(()->fields.get(field.variable()).value()).getOrNull();
       if(null == value){
          //defined in the CT but not present on the instance
          return null;
       }
       if(field instanceof KeyValueField){
         //KeyValues are stored as List to preserve the order of their elements so some additional logic is required here
         List<com.dotcms.content.model.type.keyvalue.Entry<?>> asList = (List<com.dotcms.content.model.type.keyvalue.Entry<?>>)value;
         return KeyValueField.asMap(asList);
       }
       //We store Dates as Instants in our json so a bit of extra conversion is required for backwards compatibility
       return value instanceof Instant ? Date.from((Instant)value) : value;
    }

    /**
     * This is a pretty straight forward method that simply takes a field an transform its value into the respective FieldValue Representation
     * Meaning this converts the field to a json representation
     * @param value
     * @param field
     * @return
     */
    private Optional<FieldValue<?>> getFieldValue(final Object value, final Field field) {
        return field.fieldValue(value);
    }

    /**
     * Json read to immutable
     * @param json
     * @return
     * @throws JsonProcessingException
     */
    Contentlet immutableFromJson(final String json) throws JsonProcessingException {
        return objectMapper.get().readValue(json, Contentlet.class);
    }

    /**
     * Jackson mapper configuration and lazy initialized instance.
     */
    private final Lazy<ObjectMapper> objectMapper = Lazy.of(() -> {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new GuavaModule());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return objectMapper;
    });

}
