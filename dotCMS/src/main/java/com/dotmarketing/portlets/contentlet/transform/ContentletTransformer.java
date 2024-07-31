package com.dotmarketing.portlets.contentlet.transform;

import com.dotcms.content.business.json.ContentletJsonAPI;
import com.dotcms.contenttype.business.StoryBlockReferenceResult;
import com.dotcms.contenttype.model.field.LegacyFieldTypes;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.BinaryFileFilter;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;


/**
 * DBTransformer that converts DB objects into Contentlet instances
 *
 * @author Nollymar Longa
 * @since Jan 11th, 2018
 */
public class ContentletTransformer implements DBTransformer<Contentlet> {

    private static final String DISABLED_WYSIWYG = "disabled_wysiwyg";
    private static final String INODE = "inode";
    private static final String IDENTIFIER = "identifier";
    private static final String STRUCTURE_INODE = "structure_inode";
    private static final String SYSTEM_FIELD = "system_field";

    final List<Contentlet> list;

    private static Lazy<Boolean> IS_UNIQUE_PUBLISH_EXPIRE_DATE =
            Lazy.of(() -> Config.getBooleanProperty("uniquePublishExpireDate", false));

    public ContentletTransformer(final List<Map<String, Object>> initList){
        final List<Contentlet> newList = new ArrayList<>();
        if (initList != null){
            for(final Map<String, Object> map : initList){
                newList.add(transform(map));
            }
        }

        this.list = newList;
    }

    @Override
    public List<Contentlet> asList() {
        return this.list;
    }

    @NotNull
    private static Contentlet transform(final Map<String, Object> map)  {

        final String inode = (String) map.get("inode");
        final String contentletId = (String) map.get(IDENTIFIER);
        final String contentTypeId = (String) map.get(STRUCTURE_INODE);

        ContentType type  = Try.of(()-> APILocator.getContentTypeAPI(APILocator.systemUser()).find(contentTypeId))
                .getOrElseThrow(e->new DotRuntimeException("Contentlet must have a valid content type.: " + e.getMessage(),e));

        final ContentletJsonAPI contentletJsonAPI = APILocator.getContentletJsonAPI();

        final Contentlet contentlet;
        final boolean hasJsonFields = (contentletJsonAPI.isPersistContentAsJson() && UtilMethods.isSet(map.get(ContentletJsonAPI.CONTENTLET_AS_JSON)));
        if(hasJsonFields){
          try {
              String json  = replaceBadContentTypes(map.get(ContentletJsonAPI.CONTENTLET_AS_JSON).toString(),type.id());
              json = UtilMethods.escapeHTMLCodeFromJSON(json);//Escape HTML chars from JSON
              contentlet = contentletJsonAPI.mapContentletFieldsFromJson(json);
          }catch (Exception e){
              final String errorMsg = String.format("Unable to populate contentlet from json for ID='%s', Inode='%s', Content-Type '%s': %s", contentletId, inode, contentTypeId, e.getMessage());
              Logger.error(ContentletTransformer.class, errorMsg, e);
              throw new DotRuntimeException(errorMsg, e);
          }
        } else {
            contentlet = new Contentlet();
        }

        contentlet.setInode(inode);
        contentlet.setIdentifier(contentletId);
        contentlet.setContentTypeId(contentTypeId);
        contentlet.setModDate((Date) map.get("mod_date"));
        contentlet.setModUser((String) map.get("mod_user"));
        contentlet.setOwner((String) map.get("owner"));
        contentlet.setSortOrder(ConversionUtils.toInt(map.get("sort_order"),0));
        contentlet.setLanguageId(ConversionUtils.toLong(map.get("language_id"), 0L));
        contentlet.setVariantId((String) map.get("variant_id"));

        try {
           if(!hasJsonFields) {
               populateFields(contentlet, map);
           }
            refreshStoryBlockReferences(contentlet);
            populateWysiwyg(map, contentlet);
            populateFolderAndHost(contentlet, contentletId, contentTypeId);
        } catch (final Exception e) {
            final String errorMsg = String
                    .format("Unable to populate contentlet from table columns for ID='%s', Inode='%s', Content-Type '%s': %s", contentletId, inode,
                            contentTypeId, e
                                    .getMessage());
            Logger.error(ContentletTransformer.class, errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
        }

        return contentlet;
    }

    private static Lazy<Boolean> replaceBadContentTypesInJson = Lazy.of(()->Config.getBooleanProperty("REPLACE_BAD_CONTENT_TYPES_IN_JSON", true));

    /**
     * This method will replace the contentType in the JSON string with the one passed as parameter, to insure that
     * the contentType in the JSON string is the same as the one in the Contentlet object.
     * @param jsonStringIn
     * @param contentType
     * @return
     */
    private static String replaceBadContentTypes(String jsonStringIn, String contentType){

        if(Boolean.TRUE.equals(replaceBadContentTypesInJson.get()) && !jsonStringIn.contains("\"contentType\": \"" + contentType+"\"")){

            JSONObject jsonObject = new JSONObject(jsonStringIn);
            jsonObject.put("contentType", contentType);
            return jsonObject.toString();
        }
        return jsonStringIn;



    }




    /**
     * Updates the values of the Contentlets that are being referenced in the Story Block field of the specified
     * Contentlet. This allows them to reflect the latest changes when they or other Users update such Contentlets from
     * the Content Search, so they show up as expected. In summary:
     * <ol>
     *     <li>Checks if the Contentlet has one or more Story Block Fields. If it doesn't no work is done.</li>
     *     <li>Compares the Inode of the Contentlet in the Story Block field with the original live Inode of the
     *     referenced Contentlet.</li>
     *     <li>If they're different, the properties -- i.e., field values -- in the referenced Contentlet will be
     *     updated with the properties from the latest version such a Contentlet.</li>
     * </ol>
     * Notice that the actual Inode being referenced in the Story Block field <b>WILL NOT BE UPDATED</b> to the latest
     * Inode until the "parent" Contentlet -- i.e., the one containing the Story Block field --  is published again.
     *
     * @param contentlet The {@link Contentlet} whose Story Block fields will be inspected.
     */
    private static void refreshStoryBlockReferences(final Contentlet contentlet) {
        final StoryBlockReferenceResult result = APILocator.getStoryBlockAPI().refreshReferences(contentlet);
        if (result.isRefreshed()) {
            Logger.debug(ContentletTransformer.class,
                    ()-> "Refreshed story block dependencies for the contentlet: " + contentlet.getIdentifier());
        }
    }

    private static void populateFolderAndHost(final Contentlet contentlet, final String contentletId,
            final String contentTypeId) throws DotDataException, DotSecurityException {
        if (UtilMethods.isSet(contentlet.getIdentifier())) {
            final Identifier identifier = APILocator.getIdentifierAPI().loadFromDb(contentletId);

            if (identifier == null) {
                throw new DotStateException(
                        "Contentlet's identifier not found in db. Contentlet's inode: " + contentlet
                                .getInode()
                                + ". Contentlet's identifier: " + contentlet.getIdentifier());
            }

            final Folder folder;
            if (!StringPool.FORWARD_SLASH.equals(identifier.getParentPath())) {
                folder = APILocator.getFolderAPI()
                        .findFolderByPath(identifier.getParentPath(), identifier.getHostId(),
                                APILocator.getUserAPI().getSystemUser(), false);
            } else {
                folder = APILocator.getFolderAPI().findSystemFolder();
            }

            if(folder==null) { 
                return;
            }

            contentlet.setHost(identifier.getHostId());
            contentlet.setFolder(folder.getInode());

            if (isUniquePublishExpireDatePerLanguages()) {
                setPublishExpireDateFromIdentifier(contentlet);
            }

        } else {
            if (contentlet.isSystemHost()) {
                // When we are saving a systemHost we cannot call
                // APILocator.getHostAPI().findSystemHost() method, because this
                // method will create a system host if not exist which cause
                // a infinite loop.
                contentlet.setHost(Host.SYSTEM_HOST);
            } else {
                contentlet.setHost(APILocator.getHostAPI().findSystemHost().getIdentifier());
            }
            contentlet.setFolder(APILocator.getFolderAPI().findSystemFolder().getInode());
        }
    }

    @VisibleForTesting
    public static void setUniquePublishExpireDatePerLanguages(final boolean newUniquePublishExpireDate) {
        IS_UNIQUE_PUBLISH_EXPIRE_DATE = Lazy.of(() -> newUniquePublishExpireDate);
    }

    public static boolean isUniquePublishExpireDatePerLanguages() {
        return IS_UNIQUE_PUBLISH_EXPIRE_DATE.get();
    }

    private static void setPublishExpireDateFromIdentifier(final Contentlet contentlet) throws DotSecurityException, DotDataException {

        final Identifier identifier = APILocator.getIdentifierAPI().find(contentlet);
        final String contentTypeId = contentlet.getContentTypeId();

        final ContentType contentType = APILocator.getContentTypeAPI(
                        APILocator.systemUser())
                .find(contentTypeId);

        if (UtilMethods.isSet(contentType.publishDateVar())) {
            contentlet.setDateProperty(contentType.publishDateVar(),
                    identifier.getSysPublishDate());
        }

        if (UtilMethods.isSet(contentType.expireDateVar())) {
            contentlet
                    .setDateProperty(contentType.expireDateVar(),
                            identifier.getSysExpireDate());
        }
    }

    private static void populateWysiwyg(final Map<String, Object> map, Contentlet contentlet) {
        final String wysiwyg = (String) map.get(DISABLED_WYSIWYG);
        if( UtilMethods.isSet(wysiwyg) ) {
            final List<String> wysiwygFields = new ArrayList<>();
            final StringTokenizer st = new StringTokenizer(wysiwyg,StringPool.COMMA);
            while( st.hasMoreTokens() ) wysiwygFields.add(st.nextToken().trim());
            contentlet.setDisabledWysiwyg(wysiwygFields);
        }
    }

    /**
     * Gets map of the contentlet properties based on the fields of the structure
     * The keys used in the map will be the velocity variables names
     * @param originalMap Map with the fields obtained from database
     */
    private static void populateFields(final Contentlet contentlet, final Map<String, Object> originalMap)
            throws DotDataException, DotSecurityException {

        final Map<String, Object> fieldsMap = new HashMap<>();
        final String inode = (String) originalMap.get(INODE);
        final String identifier = (String) originalMap.get(IDENTIFIER);
        final String contentTypeId = (String) originalMap.get(STRUCTURE_INODE);

        // Populate the title
        contentlet.setProperty(Contentlet.TITTLE_KEY, originalMap.get(Contentlet.TITTLE_KEY));

        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(contentTypeId);
        final List<Field> fields = new LegacyFieldTransformer(contentType.fields())
                .asOldFieldList();
        for (final Field field : fields) {
            // DO NOT map these types of fields
            if (!APILocator.getFieldAPI().valueSettable(field) ||
                    LegacyFieldTypes.HOST_OR_FOLDER.legacyValue().equals(field.getFieldType())
                    ||
                    LegacyFieldTypes.TAG.legacyValue().equals(field.getFieldType()) ||
                    (field.getFieldContentlet() != null && field.getFieldContentlet()
                            .startsWith(SYSTEM_FIELD) &&
                            !LegacyFieldTypes.BINARY.legacyValue()
                                    .equals(field.getFieldType()))) {
                continue;
            }
            Object value;
            if (APILocator.getFieldAPI().isElementConstant(field)) {
                value = field.getValues();
            } else {
                try {
                    if (UtilMethods.isSet(identifier)
                            && contentType instanceof FileAssetContentType
                            && FileAssetAPI.FILE_NAME_FIELD
                            .equals(field.getVelocityVarName())) {
                        value = APILocator.getIdentifierAPI().find(identifier).getAssetName();
                    } else if (UtilMethods.isSet(identifier)
                            && contentType instanceof FileAssetContentType
                            && FileAssetAPI.META_DATA_FIELD.equals(field.getVelocityVarName())) {
                        // We can ignore this metadata field, metadata will be generated directly from the asset
                        value = Collections.emptyMap();
                    } else {
                        if (LegacyFieldTypes.BINARY.legacyValue()
                                .equals(field.getFieldType())) {
                            java.io.File binaryFile = null;
                            final java.io.File binaryFilefolder = new java.io.File(
                                    APILocator.getFileAssetAPI().getRealAssetsRootPath()
                                            + java.io.File.separator
                                            + inode.charAt(0)
                                            + java.io.File.separator
                                            + inode.charAt(1)
                                            + java.io.File.separator
                                            + inode
                                            + java.io.File.separator
                                            + field.getVelocityVarName());
                            if (binaryFilefolder.exists()) {
                                java.io.File[] files = binaryFilefolder
                                        .listFiles(new BinaryFileFilter());
                                if (files.length > 0) {
                                    binaryFile = files[0];
                                }
                            }
                            value = binaryFile;
                        } else {
                            value = getObjectValue(originalMap, field);
                        }
                    }
                } catch (final Exception e) {
                    final String errorMsg = String
                            .format("Unable to obtain property value for field '%s' in " +
                                            "Contentlet with ID='%s', Inode='%s', Content Type '%s': %s",
                                    field
                                            .getFieldContentlet(), identifier, inode,
                                    contentTypeId, e
                                            .getMessage());
                    Logger.error(ContentletTransformer.class, errorMsg, e);
                    throw new DotRuntimeException(errorMsg, e);
                }
            }
            fieldsMap.put(field.getVelocityVarName(), value);
        }

        APILocator.getContentletAPI().copyProperties(contentlet, fieldsMap);
    }

    private static Object getObjectValue(final Map<String, Object> originalMap, final Field field) {
        Object value;
        if (field.getFieldContentlet().startsWith("float") && originalMap
                .get(field.getFieldContentlet()) instanceof Double) {
            value = ((Double) originalMap.get(field.getFieldContentlet()))
                    .floatValue();
        } else if (field.getFieldContentlet().startsWith("bool") && originalMap
                .get(field.getFieldContentlet()) instanceof Number) {
            value = ((Number)originalMap.get(field.getFieldContentlet())).intValue() == 1;
        } else {
            value = originalMap.get(field.getFieldContentlet());
        }

        //KeyValue Fields must be returned as Maps
        if (LegacyFieldTypes.KEY_VALUE.legacyValue().equals(field.getFieldType())
                && value instanceof String) {
            final String asString = value.toString();
            try {
                value = com.dotmarketing.portlets.structure.model.KeyValueFieldUtil
                        .JSONValueToHashMap(asString);
            } catch (Throwable e) {
                Logger.warn(ContentletTransformer.class, () -> String
                        .format("Failed to convert keyValue field `%s` to an actual json. An Empty will be returned instead. ",
                                field.getVelocityVarName()));
                value = Collections.emptyMap();
            }
        }

        return value;
    }

}
