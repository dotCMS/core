package com.dotcms.rest.api.v1.contenttype;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * Swagger-only schema view describing the request body of {@code POST /api/v1/contenttype}.
 *
 * <p>This class is never instantiated or deserialized; it exists purely to publish a typed OpenAPI
 * schema for the content-type object the endpoint accepts. The runtime request type is
 * {@code ContentTypeForm}, but that class has a custom {@code @JsonDeserialize} that reads the
 * <b>bare</b> {@code com.dotcms.contenttype.model.type.ContentType} object (or an array of them) at
 * the top level — it is NOT a {@code {"contentType": ...}} envelope. Introspecting the
 * {@code ContentTypeForm} bean would publish its internal {@code entries}/{@code requestJson} shape,
 * which bears no relation to the wire format. This view publishes the real shape instead.</p>
 *
 * @see ContentTypeResource#createType
 * @see ContentTypeFieldView
 */
@Schema(description = "A content-type object, posted directly (NOT wrapped in a 'contentType' envelope). "
        + "The endpoint also accepts an array of these objects to create several types in one call.")
public class ContentTypeRequestView {

    @Schema(
            requiredMode = Schema.RequiredMode.REQUIRED,
            description = "Fully-qualified class name of the content-type implementation (the 'Immutable' generated "
                    + "type). This is the discriminator that selects the base type.",
            allowableValues = {
                    "com.dotcms.contenttype.model.type.ImmutableSimpleContentType",
                    "com.dotcms.contenttype.model.type.ImmutableWidgetContentType",
                    "com.dotcms.contenttype.model.type.ImmutableFormContentType",
                    "com.dotcms.contenttype.model.type.ImmutableFileAssetContentType",
                    "com.dotcms.contenttype.model.type.ImmutablePageContentType",
                    "com.dotcms.contenttype.model.type.ImmutablePersonaContentType",
                    "com.dotcms.contenttype.model.type.ImmutableVanityUrlContentType",
                    "com.dotcms.contenttype.model.type.ImmutableKeyValueContentType",
                    "com.dotcms.contenttype.model.type.ImmutableDotAssetContentType"
            })
    private String clazz;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Display name of the content type.")
    private String name;

    @Schema(description = "Velocity variable name (unique, alphanumeric, starts with a letter; "
            + "auto-generated from 'name' if omitted).")
    private String variable;

    @Schema(description = "Site identifier UUID this content type lives on, or the literal 'SYSTEM_HOST' "
            + "(defaults to the default site).")
    private String host;

    @Schema(description = "Folder identifier UUID, or the literal 'SYSTEM_FOLDER' (the default).")
    private String folder;

    @Schema(description = "Description of the content type.")
    private String description;

    @Schema(description = "Whether this is the default content type.")
    private boolean defaultType;

    @Schema(description = "Whether the content type is fixed (system-managed).")
    private boolean fixed;

    @Schema(description = "Whether the content type is a system type.")
    private boolean system;

    @ArraySchema(
            arraySchema = @Schema(
                    description = "Workflow scheme identifiers to associate with the content type, e.g. "
                            + "[\"d61a59e1-a49c-46f2-a929-db2b4bfa88b2\"] for the System Workflow. "
                            + "NOTE: this key is 'workflow' (singular) in the REQUEST; GET responses return "
                            + "'workflows' (plural, array of objects) — rename the key when round-tripping."),
            schema = @Schema(type = "string"))
    private List<String> workflow;

    @ArraySchema(
            arraySchema = @Schema(description = "Fields that make up the content type, in order. Rows and columns are "
                    + "regular entries: 'ImmutableRowField' begins a row, 'ImmutableColumnField' begins a column, and "
                    + "subsequent content fields belong to the most-recent column."),
            schema = @Schema(implementation = ContentTypeFieldView.class))
    private List<ContentTypeFieldView> fields;

    @Schema(description = "Content-type metadata. Known keys: 'CONTENT_EDITOR2_ENABLED' (boolean), "
            + "'DOT_STYLE_EDITOR_SCHEMA' (JSON string).",
            type = "object")
    private Map<String, Object> metadata;

    @Schema(description = "Maps system actions (NEW, EDIT, PUBLISH, UNPUBLISH, ARCHIVE, UNARCHIVE, DELETE, DESTROY) "
            + "to workflow action identifiers.",
            type = "object")
    private Map<String, String> systemActionMappings;

    public String getClazz() {
        return clazz;
    }

    public String getName() {
        return name;
    }

    public String getVariable() {
        return variable;
    }

    public String getHost() {
        return host;
    }

    public String getFolder() {
        return folder;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDefaultType() {
        return defaultType;
    }

    public boolean isFixed() {
        return fixed;
    }

    public boolean isSystem() {
        return system;
    }

    public List<String> getWorkflow() {
        return workflow;
    }

    public List<ContentTypeFieldView> getFields() {
        return fields;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Map<String, String> getSystemActionMappings() {
        return systemActionMappings;
    }
}
