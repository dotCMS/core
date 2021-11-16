package com.dotcms.content.business;

import com.dotcms.content.model.Contentlet;
import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.ImmutableContentlet;
import com.dotcms.content.model.ImmutableContentlet.Builder;
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
import com.dotcms.contenttype.model.field.TabDividerField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.util.List;
import java.util.Optional;

/**
 * Complementary class that centralize ImmutableContentlet to json and vice-versa conversion
 * NO API Are used here since this class is intended to be accessible from Upgrade Task and Factories
 */
public class ImmutableContentletHelper {


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
    boolean isNotMappable(final Field field) {
        return (!isSettable(field) || (field instanceof HostFolderField)
                || (field instanceof TagField) || isNoneMappableSystemField(field) || isMetadataField(field)
        );
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
     * internal method Makes a regular contentlet and makes an ImmutableContentlet which later will be translated into a json
     * @param contentlet
     * @return
     */
    public ImmutableContentlet toImmutable(
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
                    Logger.warn(ContentletJsonAPIImpl.class,
                            String.format("Unable to set field `%s` with the given value %s.",
                                    field.name(), value));
                } else {
                    builder.putFields(field.variable(), fieldValue.get());
                }
            } else {
                Logger.debug(ContentletJsonAPIImpl.class,
                        String.format("Unable to set field `%s` as it wasn't set on the source contentlet", field.name()));
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
        return objectMapper.get().readValue(json, Contentlet.class);
    }

    /**
     * Public entry point. Going from json to regular contentlet
     * @param contentlet regular "mutable" contentlet
     * @return String json representation
     * @throws JsonProcessingException
     * @throws DotDataException
     */
    String toJson(final com.dotmarketing.portlets.contentlet.model.Contentlet contentlet)
            throws JsonProcessingException, DotDataException {
        return objectMapper.get().writeValueAsString(toImmutable(contentlet));
    }


    /**
     * Jackson mapper configuration and lazy initialized instance.
     */
    final Lazy<ObjectMapper> objectMapper = Lazy.of(() -> {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new GuavaModule());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return objectMapper;
    });

}
