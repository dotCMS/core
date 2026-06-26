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

    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            description = "Fully-qualified class name of the field implementation (the 'Immutable' generated type). "
                    + "Determines the kind of field.",
            allowableValues = {
                    "com.dotcms.contenttype.model.field.ImmutableTextField",
                    "com.dotcms.contenttype.model.field.ImmutableTextAreaField",
                    "com.dotcms.contenttype.model.field.ImmutableStoryBlockField",
                    "com.dotcms.contenttype.model.field.ImmutableWysiwygField",
                    "com.dotcms.contenttype.model.field.ImmutableConstantField",
                    "com.dotcms.contenttype.model.field.ImmutableHiddenField",
                    "com.dotcms.contenttype.model.field.ImmutableCustomField",
                    "com.dotcms.contenttype.model.field.ImmutableJSONField",
                    "com.dotcms.contenttype.model.field.ImmutableBinaryField",
                    "com.dotcms.contenttype.model.field.ImmutableImageField",
                    "com.dotcms.contenttype.model.field.ImmutableFileField",
                    "com.dotcms.contenttype.model.field.ImmutableTagField",
                    "com.dotcms.contenttype.model.field.ImmutableCategoryField",
                    "com.dotcms.contenttype.model.field.ImmutableCheckboxField",
                    "com.dotcms.contenttype.model.field.ImmutableRadioField",
                    "com.dotcms.contenttype.model.field.ImmutableSelectField",
                    "com.dotcms.contenttype.model.field.ImmutableMultiSelectField",
                    "com.dotcms.contenttype.model.field.ImmutableDateField",
                    "com.dotcms.contenttype.model.field.ImmutableTimeField",
                    "com.dotcms.contenttype.model.field.ImmutableDateTimeField",
                    "com.dotcms.contenttype.model.field.ImmutableKeyValueField",
                    "com.dotcms.contenttype.model.field.ImmutableHostFolderField",
                    "com.dotcms.contenttype.model.field.ImmutableRelationshipField",
                    "com.dotcms.contenttype.model.field.ImmutableRelationshipsTabField",
                    "com.dotcms.contenttype.model.field.ImmutablePermissionTabField",
                    "com.dotcms.contenttype.model.field.ImmutableLineDividerField",
                    "com.dotcms.contenttype.model.field.ImmutableTabDividerField",
                    "com.dotcms.contenttype.model.field.ImmutableRowField",
                    "com.dotcms.contenttype.model.field.ImmutableColumnField"
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
