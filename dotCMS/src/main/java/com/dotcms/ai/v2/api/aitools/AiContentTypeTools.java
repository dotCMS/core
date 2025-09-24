package com.dotcms.ai.v2.api.aitools;

import com.dotcms.ai.v2.api.dto.ContentTypeDTO;
import com.dotcms.ai.v2.api.dto.FieldDefinitionDTO;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.JsonContentTypeTransformer;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import dev.langchain4j.agent.tool.Tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AiContentTypeTools {


    /**
     * Read a content type schema by varName.
     * Optional 'onlyFields' limits which fields are returned (reduce tokens).
     */
    @Tool("Get content type schema by varName. Use onlyFields to restrict returned fields.")
    public ToolResult<ContentTypeDTO> getContentType(final String varName, final String[] onlyFields) {

        try {
            if (varName == null || varName.trim().isEmpty()) {
                return ToolResult.invalid("varName is required");
            }

            final User user = APILocator.systemUser(); // todo: this should be trying to retrieve the current user to use the right permissions schema
            final ContentType contentType = APILocator.getContentTypeAPI(user).find(varName);

            if (contentType == null) {
                return ToolResult.invalid("ContentType: " + varName + " not found");
            }

            final List<FieldDefinitionDTO> all = new ArrayList<>();
            for (final Field field : contentType.fields()) {
                all.add(new FieldDefinitionDTO.Builder()
                        .name(field.variable()).label(field.name()).type(field.typeName())
                        .required(field.required()).indexed(field.indexed()).i18n(true).maxLength(200)
                        .build());
            }

            final List<FieldDefinitionDTO> filtered = null != onlyFields? filterFields(all, Stream.of(onlyFields)
                    .filter(UtilMethods::isSet).map(String::trim).collect(Collectors.toList())):
                    all;

            final ContentTypeDTO dto = new ContentTypeDTO.Builder()
                    .varName(contentType.variable())
                    .name(contentType.name())
                    .description(contentType.description())
                    .icon(contentType.icon())
                    .workflowEnabled(true) // todo: check about this
                    .fields(filtered)
                    .build();

            return ToolResult.success(dto);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            return ToolResult.internal("temporary failure");
        }
    }

    /**
     * Lightweight listing of content types (varName + name). Useful for discovery.
     */
    @Tool(
            "List content types by optional prefix.\n" +
                    "Parameters:\n" +
                    "- prefix: string (optional). Filter by varName prefix. Use empty or null for all.\n" +
                    "- limit:  int (optional). Number of items to return (default 20, min 1, max 100).\n" +
                    "- offset: int (optional). Starting index (default 0). Use 0 unless you are paginating a next page.\n" +
                    "Guidance:\n" +
                    "- If you specify offset, you MUST also specify limit.\n" +
                    "- For first page, use limit=20, offset=0.\n" +
                    "Examples:\n" +
                    "- First page of all types: prefix=null, limit=20, offset=0\n" +
                    "- Next page: prefix=null, limit=20, offset=20\n" +
                    "- Filtered: prefix=\"Blo\", limit=20, offset=0"
    )
    public ToolResult<List<ContentTypeDescriptor>> listContentTypes(final String prefix, final Integer limit, final Integer offset) {
        try {

            final User user = APILocator.systemUser(); // todo: this should be trying to retrieve the current user to use the right permissions schema
            final List<ContentType> contentTypes = APILocator.getContentTypeAPI(user).findAll();
            final List<ContentTypeDescriptor> all = new ArrayList<>();
            for (final ContentType contentType : contentTypes) {

                all.add(new ContentTypeDescriptor(contentType.variable(), contentType.name()));
            }

            final List<ContentTypeDescriptor> filtered = new ArrayList<>();
            for (ContentTypeDescriptor contentTypeDescriptor : all) {
                if (prefix == null || prefix.isEmpty() || contentTypeDescriptor.varName.toLowerCase().startsWith(prefix.toLowerCase())) {
                    filtered.add(contentTypeDescriptor);
                }
            }

            final int start = offset == null ? 0 : Math.max(0, offset);
            final int end = limit == null ? filtered.size() : Math.min(filtered.size(), start + Math.max(0, limit));
            final List<ContentTypeDescriptor> page = start >= filtered.size() ? Collections.<ContentTypeDescriptor>emptyList() : filtered.subList(start, end);
            return ToolResult.success(page);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            return ToolResult.internal("temporary failure");
        }
    }


    @Tool(
            "Create a dotCMS Content Type from a dotCMS-native JSON spec. " +
                    "ALWAYS include the top-level 'clazz' of the content type. " +
                    "Allowed content type classes:\n" +
                    " - com.dotcms.contenttype.model.type.ImmutableSimpleContentType\n" +
                    " - com.dotcms.contenttype.model.type.ImmutableDotAssetContentType\n" +
                    " - com.dotcms.contenttype.model.type.ImmutableFileAssetContentType\n" +
                    " - com.dotcms.contenttype.model.type.ImmutableFormContentType\n" +
                    " - com.dotcms.contenttype.model.type.ImmutableKeyValueContentType\n" +
                    " - com.dotcms.contenttype.model.type.ImmutablePageContentType\n" +
                    " - com.dotcms.contenttype.model.type.ImmutablePersonaContentType\n" +
                    " - com.dotcms.contenttype.model.type.ImmutableVanityUrlContentType\n" +
                    " - com.dotcms.contenttype.model.type.ImmutableWidgetContentType\n" +
                    "\n" +
                    "Required top-level keys: 'clazz', 'name', 'variable', 'host', 'folder', 'fields'.\n" +
                    "Optional: 'description', 'defaultType', 'system', 'fixed', 'icon', 'sortOrder', 'workflow' (array of UUIDs), 'systemActionMappings'.\n" +
                    "\n" +
                    "Each entry in 'fields' MUST include its own 'clazz' and recommended keys:\n" +
                    " - Field 'clazz' examples:\n" +
                    "   com.dotcms.contenttype.model.field.TextField\n" +
                    "   com.dotcms.contenttype.model.field.ImmutableTagField\n" +
                    "   com.dotcms.contenttype.model.field.ImmutableStoryBlockField\n" +
                    "   com.dotcms.contenttype.model.field.ImageField\n" +
                    "   com.dotcms.contenttype.model.field.BinaryField\n" +
                    "   com.dotcms.contenttype.model.field.WysiwygField\n" +
                    "   com.dotcms.contenttype.model.field.DateField\n" +
                    "   com.dotcms.contenttype.model.field.HostFolderField (or ImmutableHostFolderField)\n" +
                    " - Common field keys: 'dataType', 'variable', 'name', 'required', 'indexed', 'listed', 'searchable', 'readOnly', 'unique', 'sortOrder', 'dbColumn', 'defaultValue', 'hint'.\n" +
                    "\n" +
                    "The input may be a SINGLE object or an ARRAY of objects. The JSON MUST be valid for JsonContentTypeTransformer.\n" +
                    "Do NOT invent non-dotCMS keys. Prefer SYSTEM_HOST and SYSTEM_FOLDER when targeting the system site/folder.\n" +
                    "\n" +
                    "Minimal valid example:\n" +
                    "{\n" +
                    "  \"clazz\": \"com.dotcms.contenttype.model.type.ImmutableSimpleContentType\",\n" +
                    "  \"name\": \"Test Content\",\n" +
                    "  \"variable\": \"test_content\",\n" +
                    "  \"host\": \"SYSTEM_HOST\",\n" +
                    "  \"folder\": \"SYSTEM_FOLDER\",\n" +
                    "  \"fields\": [\n" +
                    "    {\n" +
                    "      \"clazz\": \"com.dotcms.contenttype.model.field.TextField\",\n" +
                    "      \"dataType\": \"TEXT\",\n" +
                    "      \"variable\": \"title\",\n" +
                    "      \"name\": \"Title\",\n" +
                    "      \"required\": true,\n" +
                    "      \"indexed\": true,\n" +
                    "      \"listed\": true,\n" +
                    "      \"searchable\": true,\n" +
                    "      \"sortOrder\": 1\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"workflow\": [\"d61a59e1-a49c-46f2-a929-db2b4bfa88b2\"]\n" +
                    "}\n"
    )
    public ToolResult<String> createContentType(final String specJson) {
        try {
            if (specJson == null || specJson.trim().isEmpty()) {
                return ToolResult.invalid("specJson is required");
            }

            final List<ContentType> typesToSave = new JsonContentTypeTransformer(specJson).asList();
            if (typesToSave == null || typesToSave.isEmpty()) {
                return ToolResult.invalid("No Content Types found in specJson");
            }

            final String validationError = this.validateDotCmsContentTypes(typesToSave);
            if (validationError != null) {
                return ToolResult.invalid(validationError);
            }

            final String createdSummary = this.saveDotCmsContentTypes(typesToSave);

            return ToolResult.success(createdSummary);
        } catch (DotDataException e) {
            Logger.error(this, "Data error creating content type(s)", e);
            return ToolResult.fail( "Persistence error: " + e.getMessage());
        } catch (Exception e) {
            Logger.error(this, "Unexpected error creating content type(s)", e);
            return ToolResult.fail("Unexpected error: " + e.getMessage());
        }
    }


    private String validateDotCmsContentTypes(final List<ContentType> types) throws DotDataException {

        for (final ContentType contentType : types) {
            final String variable = contentType.variable();
            final String name     = contentType.name();

            if (variable == null || variable.trim().isEmpty()) {
                return "Each content type must include a non-empty 'variable'.";
            }
            if (name == null || name.trim().isEmpty()) {
                return "Each content type must include a non-empty 'name'.";
            }

            // Campos
            final List<Field> fields = contentType.fields();
            if (fields == null || fields.isEmpty()) {
                return "Content type '" + variable + "' must include at least one field.";
            }
            for (Field field : fields) {
                if (field == null) {
                    return "Null field found in content type '" + variable + "'.";
                }
                if (field.variable() == null || field.variable().trim().isEmpty()) {
                    return "A field without 'variable' was found in content type '" + variable + "'.";
                }
                if (field.name() == null || field.name().trim().isEmpty()) {
                    return "Field '" + field.variable() + "' in content type '" + variable + "' must have a 'name'.";
                }
            }
        }
        return null;
    }

    private String saveDotCmsContentTypes(final List<ContentType> typesToSave)
            throws DotDataException, DotSecurityException {

        final User systemUser = APILocator.systemUser();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(systemUser);

        final List<String> results = new ArrayList<>();

        for (ContentType contentType : typesToSave) {
            final String variable = contentType.variable();

            if (existsByVariable(contentTypeAPI, variable)) {
                results.add("Skipped (already exists): " + variable);
                continue;
            }

            final List<Field> fields = contentType.fields() != null ? contentType.fields() : Collections.emptyList();

            contentTypeAPI.save(contentType, fields);

            results.add("Created: " + variable);
        }

        return String.join(" | ", results);
    }

    private boolean existsByVariable(final ContentTypeAPI api, final String variable) {
        try {
            final ContentType found = api.find(variable);
            return found != null;
        } catch (Exception ignore) {
            return false;
        }
    }


    // --- DTO small listing ---
    public static class ContentTypeDescriptor {
        public final String varName;
        public final String name;
        public ContentTypeDescriptor(String varName, String name) {
            this.varName = varName;
            this.name = name;
        }
    }

    private List<FieldDefinitionDTO> filterFields(List<FieldDefinitionDTO> all, List<String> onlyFields) {
        if (onlyFields == null || onlyFields.isEmpty()) {
            return all;
        }

        final List<FieldDefinitionDTO> out = new ArrayList<>();
        for (FieldDefinitionDTO fieldDefinitionDTO : all) {
            for (String k : onlyFields) {
                if (fieldDefinitionDTO.getName().equalsIgnoreCase(k)) {
                    out.add(fieldDefinitionDTO);
                    break;
                }
            }
        }
        return out;
    }
}
