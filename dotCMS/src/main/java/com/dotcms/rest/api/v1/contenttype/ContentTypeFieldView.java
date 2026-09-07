package com.dotcms.rest.api.v1.contenttype;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Swagger-only schema view describing a single entry in a content type's {@code fields[]} array.
 *
 * <p>This class is never instantiated or deserialized; it exists purely to publish a typed OpenAPI
 * schema for the polymorphic field DTO accepted by {@code POST /contenttype} (and the field
 * endpoints). The actual runtime model is the polymorphic {@code com.dotcms.contenttype.model.field.Field}
 * hierarchy, which the {@code clazz} discriminator selects.</p>
 *
 * @see ContentTypeResource#createType
 */
@Schema(description = "A single field within a content type's 'fields[]' array. The 'clazz' property is the "
        + "discriminator that selects the concrete field type; the remaining properties apply across field types.")
public class ContentTypeFieldView {

    @Schema(description = "Field identifier. Preserve this when updating an existing field.")
    private String id;

    @Schema(description = "Identifier of the content type that owns this field.")
    private String contentTypeId;

    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            description = "Field type, as a case-insensitive short field-type name (the discriminator that "
                    + "selects the concrete field). The fully-qualified 'Immutable*' class name and the bare "
                    + "simple class name (e.g. 'TextField') are also still accepted, but the short names below "
                    + "are the preferred form. Example: \"TEXT\".",
            example = "TEXT",
            allowableValues = {
                    "TEXT",
                    "TEXT_AREA",
                    "STORY_BLOCK_FIELD",
                    "WYSIWYG",
                    "CONSTANT",
                    "HIDDEN",
                    "CUSTOM_FIELD",
                    "JSON_FIELD",
                    "BINARY",
                    "IMAGE",
                    "FILE",
                    "TAG",
                    "CATEGORY",
                    "CHECKBOX",
                    "RADIO",
                    "SELECT",
                    "MULTI_SELECT",
                    "DATE",
                    "TIME",
                    "DATE_TIME",
                    "KEY_VALUE",
                    "HOST_OR_FOLDER",
                    "RELATIONSHIP",
                    "RELATIONSHIPS_TAB",
                    "PERMISSIONS_TAB",
                    "LINE_DIVIDER",
                    "TAB_DIVIDER",
                    "ROW_FIELD",
                    "COLUMN_FIELD"
            })
    private String clazz;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Display name of the field.")
    private String name;

    @Schema(description = "Velocity variable name of the field (unique within the content type; "
            + "auto-generated from 'name' if omitted).")
    private String variable;

    @Schema(
            description = "Storage/column data type backing the field. This is the **storage** type, which often "
                    + "differs from the field's UI class. In particular, fields that store their payload elsewhere — "
                    + "such as 'ImmutableImageField', 'ImmutableFileField', and 'ImmutableBinaryField' — use "
                    + "dataType 'TEXT' (they keep an asset reference in a text column), **not** 'SYSTEM'. "
                    + "Reserve 'SYSTEM' for true layout/tab/relationship system fields. Use 'LONG_TEXT' for "
                    + "text-area/story-block/WYSIWYG content.",
            allowableValues = {"TEXT", "LONG_TEXT", "SYSTEM", "BOOL", "INTEGER", "FLOAT", "DATE"})
    private String dataType;

    @Schema(description = "Whether a value is required to save content.")
    private boolean required;

    @Schema(description = "Whether the field is added to the search index.")
    private boolean indexed;

    @Schema(description = "Whether the field appears in content list/table views.")
    private boolean listed;

    @Schema(description = "Whether the field is unique across content of this type.")
    private boolean unique;

    @Schema(description = "Position of the field within the 'fields[]' array (also drives row/column layout order).")
    private int sortOrder;

    @Schema(description = "Options for Radio/Select/Checkbox/Multi-Select fields: newline-separated 'Display|value' "
            + "pairs. For a boolean choice use ImmutableRadioField + dataType 'BOOL' + "
            + "values 'True|true\\r\\nFalse|false' (there is no dedicated boolean field class).")
    private String values;

    @Schema(description = "Default value applied when content is created.")
    private String defaultValue;

    @Schema(description = "Help text shown beneath the field in the editor.")
    private String hint;

    @Schema(description = "Regular expression used to validate the field value.")
    private String regexCheck;

    public String getClazz() {
        return clazz;
    }

    public String getId() {
        return id;
    }

    public String getContentTypeId() {
        return contentTypeId;
    }

    public String getName() {
        return name;
    }

    public String getVariable() {
        return variable;
    }

    public String getDataType() {
        return dataType;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isIndexed() {
        return indexed;
    }

    public boolean isListed() {
        return listed;
    }

    public boolean isUnique() {
        return unique;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getValues() {
        return values;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getHint() {
        return hint;
    }

    public String getRegexCheck() {
        return regexCheck;
    }
}
