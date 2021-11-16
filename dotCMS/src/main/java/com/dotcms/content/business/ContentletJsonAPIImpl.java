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
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.BinaryFileFilter;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.annotations.VisibleForTesting;
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

    /**
     * API-Parametrized constructor
     * @param identifierAPI
     * @param contentTypeAPI
     * @param fileAssetAPI
     */
    @VisibleForTesting
    ContentletJsonAPIImpl(final IdentifierAPI identifierAPI,
            final ContentTypeAPI contentTypeAPI,
            final FileAssetAPI fileAssetAPI) {
        this.identifierAPI = identifierAPI;
        this.contentTypeAPI = contentTypeAPI;
        this.fileAssetAPI = fileAssetAPI;
    }

    /**
     * Default Constructor
     */
    public ContentletJsonAPIImpl() {
        this(APILocator.getIdentifierAPI(), APILocator.getContentTypeAPI(APILocator.systemUser()),
             APILocator.getFileAssetAPI());
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
        return ImmutableContentletHelper.toJson(contentlet);
    }


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

        final Contentlet immutableContentlet = ImmutableContentletHelper.immutableFromJson(json);
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
            if (ImmutableContentletHelper.isNotMappable(field)) {
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

}
