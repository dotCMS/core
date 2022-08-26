package com.dotmarketing.portlets.contentlet.transform;

import com.dotcms.content.business.json.ContentletJsonAPI;
import com.dotcms.content.business.json.ContentletJsonHelper;
import com.dotcms.contenttype.model.field.LegacyFieldTypes;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.transform.DBTransformer;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;

/**
 * DBTransformer that converts DB objects into Contentlet instances
 */
public class ContentletTransformer implements DBTransformer {

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

        if (!UtilMethods.isSet(contentTypeId)) {
            throw new DotRuntimeException("Contentlet must have a content type.");
        }

        final ContentletJsonAPI contentletJsonAPI = APILocator.getContentletJsonAPI();

        final Contentlet contentlet;
        final boolean hasJsonFields = (contentletJsonAPI.isPersistContentAsJson() && UtilMethods.isSet(map.get(ContentletJsonAPI.CONTENTLET_AS_JSON)));
        if(hasJsonFields){
          try {
              final String json = map.get(ContentletJsonAPI.CONTENTLET_AS_JSON).toString();
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
        contentlet.setProperty(Contentlet.TITTLE_KEY, map.get(Contentlet.TITTLE_KEY));
        contentlet.setSortOrder(ConversionUtils.toInt(map.get("sort_order"),0));

        contentlet.setLanguageId(ConversionUtils.toLong(map.get("language_id"), 0L));

        try {
           if(!hasJsonFields) {
               populateFields(contentlet, map);
           }

            refreshBlockEditorReferences(contentlet);
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

    /**
     * In the case the content type has block editor fields, the dotContentlet references must be merge with the new references
     * @param contentlet
     */
    private static void refreshBlockEditorReferences(final Contentlet contentlet) {

        final List<com.dotcms.contenttype.model.field.Field> fields = contentlet.getContentType().fields();
        for (final com.dotcms.contenttype.model.field.Field field : fields) {

            if (field instanceof StoryBlockField) {

                final Object blockEditorValue = contentlet.get(field.variable());
                final Tuple2<Boolean, Object> resultOfRefresh = refreshBlockEditorValueReferences(blockEditorValue);
                if (resultOfRefresh._1()) { // the block editor value has changed and has to be override

                    contentlet.setProperty(field.variable(), resultOfRefresh._2());
                }
            }
        }
    }

    private static Tuple2<Boolean, Object> refreshBlockEditorValueReferences(final Object blockEditorValue) {

        boolean refreshed = false;
        try {

            final LinkedHashMap blockEditorMap = ContentletJsonHelper.INSTANCE.get().objectMapper()
                    .readValue(Try.of(()->blockEditorValue.toString())
                            .getOrElse(""), LinkedHashMap.class);
            final List contentsMap = (List) blockEditorMap.get("content");

            for (final Object contentMapObject : contentsMap) {

                final Map contentMap = (Map) contentMapObject;
                if (null != contentMap) {

                    if ("dotContent".equals(contentMap.get("type"))) {

                        final Map attrsMap = (Map) contentMap.get("attrs");
                        if (null != attrsMap) {

                            final Map dataMap = (Map) attrsMap.get("data");
                            if (null != dataMap) {

                                final String identifier = (String) dataMap.get("identifier");
                                final String inode = (String) dataMap.get("inode");
                                if (null != identifier && null != inode) {

                                    final VersionInfo versionInfo = APILocator.getVersionableAPI().getVersionInfo(identifier);
                                    if (null != versionInfo &&
                                            !(inode.equals(versionInfo.getWorkingInode()) || inode.equals(versionInfo.getLiveInode()))) {

                                        refreshed = true;
                                        // the inode stored on the json does not match with any top inode, so the information stored is old and need refresh
                                        refreshBlockEditorDataMap(dataMap, versionInfo, Collections.emptySet());
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (refreshed) {

                final String blockEditJsonValueString = ContentletJsonHelper.INSTANCE.get().objectMapper()
                        .writeValueAsString(blockEditorMap);

                return Tuple.of(true, blockEditJsonValueString); // has changed and the now json is returned
            }
        } catch (final Exception e) {
            Logger.debug(ContentletTransformer.class, e.getMessage());
        }

        return Tuple.of(false, blockEditorValue); // return the original value and value didn't change
    }

    private static void refreshBlockEditorDataMap(final Map dataMap, final VersionInfo versionInfo, final Set<String> skipFieldSet) throws DotDataException, DotSecurityException {
        // todo: not sure which inode should use to refresh the reference
        final Contentlet contentlet = APILocator.getContentletAPI().find(
                versionInfo.getWorkingInode(), APILocator.systemUser(), false); /// todo: find contentlet by working or live
        final Set contenteFieldNames = dataMap.keySet();
        for (Object contentFieldName : contenteFieldNames) {

            if (!skipFieldSet.contains(contentFieldName)) {  // if it is not a field already edit by the client

                final Object value = contentlet.get(contentFieldName.toString());
                if (null != value) {

                    dataMap.put(contentFieldName, value);
                }
            }
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
            final List<String> wysiwygFields = new ArrayList<String>();
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

