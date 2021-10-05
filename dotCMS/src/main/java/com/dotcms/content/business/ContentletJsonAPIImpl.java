package com.dotcms.content.business;

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
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.LineDividerField;
import com.dotcms.contenttype.model.field.PermissionTabField;
import com.dotcms.contenttype.model.field.RelationshipsTabField;
import com.dotcms.contenttype.model.field.TabDividerField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.FileAssetContentType;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.BinaryFileFilter;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
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

public class ContentletJsonAPIImpl implements ContentletJsonAPI {

    final IdentifierAPI identifierAPI;
    final ContentTypeAPI contentTypeAPI;
    final FileAssetAPI fileAssetAPI;
    final ContentletAPI contentletAPI;
    final FolderAPI folderAPI;
    final HostAPI hostAPI;

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

    public ContentletJsonAPIImpl() {
        this(APILocator.getIdentifierAPI(), APILocator.getContentTypeAPI(APILocator.systemUser()),
             APILocator.getFileAssetAPI(), APILocator.getContentletAPI(),
             APILocator.getFolderAPI(), APILocator.getHostAPI());
    }

    public String toJson(final com.dotmarketing.portlets.contentlet.model.Contentlet contentlet)
            throws JsonProcessingException, DotDataException {
        return objectMapper.get().writeValueAsString(toImmutable(contentlet));
    }

    ImmutableContentlet toImmutable(
            final com.dotmarketing.portlets.contentlet.model.Contentlet contentlet)
            throws DotDataException {

        contentlet.setTags();

        final Builder builder = ImmutableContentlet.builder();
        builder.baseType(contentlet.getBaseType().orElseGet(() -> BaseContentType.ANY).toString());
        builder.contentType(contentlet.getContentType().id());
        builder.title(contentlet.getTitle());
        builder.languageId(contentlet.getLanguageId());
        builder.friendlyName(contentlet.getStringProperty("friendlyName"));
        builder.showOnMenu(contentlet.getBoolProperty("showOnMenu"));
        builder.owner(contentlet.getOwner());//comes from inode
        builder.sortOrder(contentlet.getSortOrder());
        builder.disabledWysiwyg(contentlet.getDisabledWysiwyg());
        builder.modUser(contentlet.getModUser());
        builder.modDate(Try.of(() -> contentlet.getModDate().toInstant()).getOrNull());
        builder.identifier(contentlet.getIdentifier());
        builder.iNode(contentlet.getInode());
        builder.host(contentlet.getHost());
        builder.folder(contentlet.getFolder());

        final List<Field> fields = contentlet.getContentType().fields();
        for (final Field field : fields) {
            if (null != contentlet.get(field.variable())) {
                final Object value = contentlet.get(field.variable());
                final Optional<FieldValue<?>> fieldValue = getFieldValue(value, field);
                if (!fieldValue.isPresent()) {
                    Logger.warn(ContentletJsonAPIImpl.class,
                            String.format("Unable to set field %s with the given value %s.",
                                    field.name(), value));
                } else {
                    builder.putFields(field.variable(), fieldValue.get());
                }
            }
        }
        return builder.build();
    }


    public com.dotmarketing.portlets.contentlet.model.Contentlet mapContentletFromJson(final String json)
            throws JsonProcessingException, DotDataException, DotSecurityException{
        final Map<String, Object> map = mapFieldsFromJson(json);
        final com.dotmarketing.portlets.contentlet.model.Contentlet contentlet = new com.dotmarketing.portlets.contentlet.model.Contentlet(map);
        populateFolderAndHost(contentlet);
        //TODO: Add the Wysiwyg props here tooo
        return contentlet;
    }

    Map<String, Object> mapFieldsFromJson(final String json)
            throws JsonProcessingException, DotDataException, DotSecurityException {

        final Contentlet immutableContentlet = immutableFromJson(json);
        final Map<String, Object> map = new HashMap<>();
        final String inode = immutableContentlet.iNode();
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

        final ContentType contentType = contentTypeAPI.find(contentTypeId);
        final Map<String, Field> fieldsByVarName = contentType.fields().stream()
                .collect(Collectors.toMap(Field::variable, Function.identity()));

        final Map<String, FieldValue<?>> fields = immutableContentlet.fields();

        for (final Entry<String, FieldValue<?>> entry : fields.entrySet()) {

            final Field field = fieldsByVarName.get(entry.getKey());
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
                        value = getValue(fields, field);
                    }
                }
            }
            map.put(field.variable(), value);
        }

        return map;
    }

    private boolean isFileAsset(final ContentType contentType, final Field field) {
        return (contentType instanceof FileAssetContentType && FileAssetAPI.FILE_NAME_FIELD
                .equals(field.variable()));
    }

    private boolean isSettable(final Field field) {
        return !(field instanceof LineDividerField || field instanceof TabDividerField
                || field instanceof CategoryField || field instanceof PermissionTabField
                || field instanceof RelationshipsTabField);
    }

    private boolean isSystemField(final Field field) {
        return (field.dataType() == DataTypes.SYSTEM && !(field instanceof BinaryField));
    }

    private boolean isNotMappable(final Field field) {
        return (!isSettable(field) || (field instanceof HostFolderField)
                || (field instanceof TagField) || isSystemField(field));
    }

    private Optional<File> getBinary(final Field field, final String inode) {
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
            java.io.File[] files = binaryFileFolder.listFiles(new BinaryFileFilter());
            if (files != null && files.length > 0) {
                return Optional.of(files[0]);
            }
        }
        return Optional.empty();
    }

    private Object getValue(final Map<String, FieldValue<?>> fields, final Field field){
       final Object value = Try.of(()->fields.get(field.variable()).value()).getOrNull();
       return value instanceof Instant ? Date.from((Instant)value) : value;
    }

    private void populateFolderAndHost(final com.dotmarketing.portlets.contentlet.model.Contentlet contentlet) throws DotDataException, DotSecurityException {
        if (UtilMethods.isSet(contentlet.getIdentifier())) {
            final Identifier identifier = identifierAPI.loadFromDb(contentlet.getIdentifier());
            if (identifier == null) {
                throw new DotStateException(
                        String.format("Unable to find contentlet identifier in db. inode: %s identifier: %s ", contentlet.getInode(), contentlet.getIdentifier()));
            }

            final Folder folder;
            if (!StringPool.FORWARD_SLASH.equals(identifier.getParentPath())) {
                folder = folderAPI
                        .findFolderByPath(identifier.getParentPath(), identifier.getHostId(),
                                APILocator.systemUser(), false);
            } else {
                folder = folderAPI.findSystemFolder();
            }

            if(folder == null) {
                return;
            }

            contentlet.setHost(identifier.getHostId());
            contentlet.setFolder(folder.getInode());

            final String contentTypeId = contentlet.getContentTypeId();
            if(UtilMethods.isNotSet(contentTypeId)){
                throw new DotStateException(
                        String.format("ContentType Id isn't set for contentlet with inode: %s and identifier: %s ", contentlet.getInode(), contentlet.getIdentifier()));
            }

            // lets check if we have publish/expire fields to set
            final ContentType contentType = contentTypeAPI.find(contentTypeId);

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
                contentlet.setHost(hostAPI.findSystemHost().getIdentifier());
            }
            contentlet.setFolder(folderAPI.findSystemFolder().getInode());
        }
    }


    Contentlet immutableFromJson(final String json) throws JsonProcessingException {
        return objectMapper.get().readValue(json, Contentlet.class);
    }

    private final Lazy<ObjectMapper> objectMapper = Lazy.of(() -> {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new GuavaModule());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return objectMapper;
    });

    private Optional<FieldValue<?>> getFieldValue(final Object value, final Field field) {
        return field.fieldValue(value);
    }

    public enum INSTANCE {
        INSTANCE;
        private final ContentletJsonAPIImpl provider = new ContentletJsonAPIImpl();

        public static ContentletJsonAPI get() {
            return INSTANCE.provider;
        }
    }

}
