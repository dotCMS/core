package com.dotcms.ai.v2.api.aitools;

import com.dotcms.ai.v2.api.aitools.dto.ContentTypeDTO;
import com.dotcms.ai.v2.api.aitools.dto.FieldDefinitionDTO;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.APILocator;
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
