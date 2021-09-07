package com.dotmarketing.portlets.contentlet.transform;

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
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.BinaryFileFilter;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import org.apache.commons.beanutils.PropertyUtils;
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
        final Contentlet contentlet = new Contentlet();
        final String inode = (String) map.get("inode");
        final String contentletId = (String) map.get(IDENTIFIER);
        final String contentTypeId = (String) map.get(STRUCTURE_INODE);

        if (!UtilMethods.isSet(contentTypeId)) {
            throw new DotRuntimeException("Contentlet must have a content type.");
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
            populateFields(contentlet, map);
            populateFolderAndHost(contentlet, contentletId, contentTypeId);
            populateWysiwyg(map, contentlet);
        } catch (final Exception e) {
            final String errorMsg = String
                    .format("Unable to populate contentlet with ID='%s', Inode='%s', Content Type '%s': %s", contentletId, inode,
                            contentTypeId, e
                                    .getMessage());
            Logger.error(ContentletTransformer.class, errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
        }

        return contentlet;
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
            contentlet.setHost(identifier.getHostId());
            contentlet.setFolder(folder.getInode());

            // lets check if we have publish/expire fields to set
            final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
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

    private static Object getObjectValue(Map<String, Object> originalMap, Field field) {
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
        return value;
    }
}

