package com.dotcms.content.business.json;

import com.dotcms.content.model.Contentlet;
import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.ImmutableContentlet;
import com.dotcms.content.model.ImmutableContentlet.Builder;
import com.dotcms.content.model.annotation.HydrateWith;
import com.dotcms.content.model.annotation.Hydration;
import com.dotcms.content.model.hydration.HydrationDelegate;
import com.dotcms.content.model.type.system.CategoryFieldType;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.ColumnField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.HiddenField;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.LineDividerField;
import com.dotcms.contenttype.model.field.PermissionTabField;
import com.dotcms.contenttype.model.field.RelationshipsTabField;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.field.TabDividerField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.BinaryFileFilter;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;

import java.io.File;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.dotmarketing.portlets.contentlet.model.Contentlet.DISABLED_WYSIWYG_KEY;
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

        final com.dotmarketing.portlets.contentlet.model.Contentlet copy = new com.dotmarketing.portlets.contentlet.model.Contentlet(contentlet);
        return ContentletJsonHelper.INSTANCE.get().writeAsString(toImmutable(copy));
    }

    /**
     * Takes a regular contentlet and builds an ImmutableContentlet
     * @param contentlet {@link com.dotmarketing.portlets.contentlet.model.Contentlet}
     * @return {@link ImmutableContentlet}
     */
    @Override
    public ImmutableContentlet toImmutable(
            final com.dotmarketing.portlets.contentlet.model.Contentlet contentlet) {

        final Builder builder = ImmutableContentlet.builder();
        builder.baseType(contentlet.getBaseType().orElseGet(() -> BaseContentType.ANY).toString());
        builder.contentType(Try.of(()->contentlet.getContentType().id()).getOrElse("unknown"));
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

        //These two are definitively mandatory but..
        //internalCheckIn calls "save" twice and the first time it is called these two aren't already set
        //At that moment we have to fake it to make it. Second save should provide the actual identifiers.
        //We'll have to use empty strings to prevent breaking the execution.
        builder.identifier(UtilMethods.isNotSet(contentlet.getIdentifier()) ? StringPool.BLANK : contentlet.getIdentifier() );
        builder.inode( UtilMethods.isNotSet(contentlet.getInode()) ? StringPool.BLANK : contentlet.getInode() );

        final List<Field> fields = contentlet.getContentType().fields();
        for (final Field field : fields) {
            if (isNotMappable(field)) {
                Logger.debug(ContentletJsonAPIImpl.class, String.format("Field `%s` is getting skipped.", field.name()));
                continue;
            }

            final String variable = field.variable();
            if(null == variable){
                Logger.debug(ContentletJsonAPIImpl.class, String.format("Field `%s` lacks variable unique name.", field.name()));
                continue;
            }
            final Object value = contentlet.get(field.variable());
            if (null != value) {
                final Optional<FieldValue<?>> fieldValue = hydrateThenGetFieldValue(value, field, contentlet);
                if (fieldValue.isEmpty()) {
                    Logger.debug(ContentletJsonAPIImpl.class,
                            String.format("Unable to set field `%s` with the given value %s.",
                                    field.name(), value));
                } else {
                    builder.putFields(variable, fieldValue.get());
                }
            } else {
                if(DataTypes.SYSTEM == field.dataType()){
                    //There is still the possibility that we're looking at a system field that isn't in the contentlet.
                    //So we need to fetch the values from an external source
                    final Optional<FieldValue<?>> externalFieldValue = Try
                            .of(() -> loadSystemFieldValue(field, contentlet, APILocator.systemUser())).get();
                    if(externalFieldValue.isPresent()){
                        builder.putFields(variable, externalFieldValue.get());
                        continue;
                    }
                }
                //finally if the it wasn't included in the contentlet still might have a default value
                final Optional<FieldValueBuilder> fieldDefaultValue = field.fieldValue(null);
                //Therefore if a default is present it must be included.
                if(fieldDefaultValue.isPresent()){
                    builder.putFields(variable, fieldDefaultValue.get().build());
                } else {
                    Logger.debug(ContentletJsonAPIImpl.class, String.format("Unable to set field `%s` as it wasn't set on the source contentlet", field.name()));
                }
            }
        }
        return builder.build();
    }

    /**
     * Public entry point when going from the json representation to a regular "mutable" contentlet
     * @param json
     * @return
     * @throws JsonProcessingException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Override
    public com.dotmarketing.portlets.contentlet.model.Contentlet mapContentletFieldsFromJson(final String json)
            throws JsonProcessingException, DotDataException, DotSecurityException{
        final Map<String, Object> map = mapFieldsFromJson(json);
        return new com.dotmarketing.portlets.contentlet.model.Contentlet(map);
    }

    /**
     * Takes an {@link ImmutableContentlet} and builds a mutable(legacy) {@link com.dotmarketing.portlets.contentlet.model.Contentlet}
     * @param immutableContent
     * @return {@link Contentlet} immutableContent
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Override
    public com.dotmarketing.portlets.contentlet.model.Contentlet toMutableContentlet(
            final Contentlet immutableContent)
            throws DotDataException, DotSecurityException{
        return new com.dotmarketing.portlets.contentlet.model.Contentlet(getContentletMapFromImmutable(immutableContent));
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
        return getContentletMapFromImmutable(immutableContentlet);
    }

    /**
     * Given a {@link Contentlet}, it returns a {@link Map} with its fields
     * @param immutableContentlet
     * @return {@link Map<String, Object>}
     * @throws DotSecurityException
     * @throws DotDataException
     */
    private Map<String, Object> getContentletMapFromImmutable(final Contentlet immutableContentlet)
            throws DotSecurityException, DotDataException {
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
        map.put(DISABLED_WYSIWYG_KEY, immutableContentlet.disabledWysiwyg());

        final ContentType contentType = contentTypeAPI.find(contentTypeId);
        final Map<String, Field> fieldsByVarName = contentType.fields().stream()
                .collect(Collectors.toMap(Field::variable, Function.identity()));

        final Map<String, FieldValue<?>> contentletFields = immutableContentlet.fields();

        for (final Entry<String, Field> entry : fieldsByVarName.entrySet()) {

            final Field field = entry.getValue();
            if (isNotMappable(field)) {
                continue;
            }

            Object value;

            if (isSet(identifier) && isFileAsset(contentType, field)) {
                value = identifierAPI.find(identifier).getAssetName();
            } else {
                if (field instanceof BinaryField) {
                    value = getBinary(field, inode).orElse(null);
                } else {
                    value = getValue(contentletFields, field);
                }
            }
            //Backwards compatibility for Date Fields, should be Timestamp
            if(value instanceof Date){
                value = new Timestamp(((Date) value).getTime());
            }
            if (field instanceof StoryBlockField) {
                map.put(field.variable() + "_raw", value);
            }
            map.put(field.variable(), value);
        }
        return map;
    }

    /**
     * These basically tells what system fields are accepted in the generate contentlet-json
     * @param field
     * @return
     */
    private boolean isAllowedSystemField(final Field field){
        return (field instanceof BinaryField || field instanceof HiddenField);
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
                !(isAllowedSystemField(field))
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
        return (!isSettable(field) ||
                (field instanceof HostFolderField) ||
                isNoneMappableSystemField(field) ||
                isMetadataField(field));
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
     * @param contentlet
     * @return
     */
    private Optional<FieldValue<?>> hydrateThenGetFieldValue(final Object value, final Field field,
            final com.dotmarketing.portlets.contentlet.model.Contentlet contentlet) {

        final Optional<FieldValueBuilder> fieldValueBuilder = field.fieldValue(value);
        if (fieldValueBuilder.isPresent()) {
            FieldValueBuilder builder = fieldValueBuilder.get();
            final List<Tuple2<HydrationDelegate,String>> delegateAndFields = getHydrationDelegatesFromAnnotations(builder.getClass());
            for (Tuple2<HydrationDelegate,String> delegateAndField : delegateAndFields) {
                final HydrationDelegate delegate = delegateAndField._1();
                final String propertyName = delegateAndField._2();
                try {
                    builder = delegate.hydrate(builder, field, contentlet, propertyName);
                } catch (Exception e) {
                    Logger.error(ContentletJsonAPIImpl.class,
                            String.format(
                                    "An error occurred while hydrating FieldValue with Builder: %s, field: %s, contentlet: %s , propertyName %s ",
                                    builder, field, contentlet.getIdentifier(), propertyName), e);
                }
            }
            return Optional.of(builder.build());
        }
        return Optional.empty();
    }

    /**
     * FieldValue descendant's Builders are annotated so the builder class is instructed on how the properties need to be fetched and set into the Builder
     * This method takes the
     * @param builderClass
     * @return
     */
    private List<Tuple2<HydrationDelegate,String>> getHydrationDelegatesFromAnnotations(final Class<? extends FieldValueBuilder> builderClass){

        final ImmutableList.Builder<Hydration> annotations = new ImmutableList.Builder<>();
        Optional<Hydration> hydrateWith = annotations
                .add(builderClass.getAnnotationsByType(Hydration.class))
                .add(builderClass.getSuperclass().getAnnotationsByType(Hydration.class))
                .build().stream().findFirst();

        final ImmutableList.Builder<Tuple2<HydrationDelegate,String>> delegates = new ImmutableList.Builder<>();
        if(hydrateWith.isPresent()) {
            final HydrateWith [] hydrateWiths = hydrateWith.get().properties();
            for (final HydrateWith with : hydrateWiths) {
                final HydrationDelegate instance = ReflectionUtils.newInstance(with.delegate());
                if(null == instance){
                    Logger.error(ContentletJsonAPIImpl.class,String.format("Unable to instantiate hydration delegate %s.",with.delegate()));
                    continue;
                }
                if(UtilMethods.isNotSet(with.propertyName())){
                    Logger.error(ContentletJsonAPIImpl.class,String.format("HydrateWith Annotation is missing required param 'field' %s.",with.propertyName()));
                    continue;
                }
                delegates.add(Tuple.of(instance, with.propertyName()));
            }
        }


        return delegates.build();
    }

    Optional<FieldValue<?>> loadSystemFieldValue(final Field field, final com.dotmarketing.portlets.contentlet.model.Contentlet contentlet, final User user)
            throws DotDataException, DotSecurityException {
        if(field instanceof CategoryField){
            final List<String> selectedCategories = findSelectedCategories((CategoryField) field, contentlet.getContentType(), user);
            if(!selectedCategories.isEmpty()){
                return Optional.of(CategoryFieldType.builder().value(selectedCategories).build());
            }
        }
        return Optional.empty();
    }

    List<String> findSelectedCategories(final CategoryField categoryField, final ContentType contentType, final User user)
            throws DotDataException, DotSecurityException {
        final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
        final List<Category> categories = categoryAPI.findCategories(contentType, user);
        final ImmutableList.Builder<String> builder = ImmutableList.builder();
        final Category parent = Try.of(()->categoryAPI.find(categoryField.values(), user, false)).getOrNull();
        if(null != parent){
            for(final Category child:categories){
                if(categoryAPI.isParent(child, parent, user)){
                    builder.add(child.getCategoryVelocityVarName());
                }
            }
        }
        return builder.build();
    }

    /**
     * Json read to immutable
     * @param json
     * @return
     * @throws JsonProcessingException
     */
    public Contentlet immutableFromJson(final String json) throws JsonProcessingException {
        return ContentletJsonHelper.INSTANCE.get().immutableFromJson(json);
    }

}
