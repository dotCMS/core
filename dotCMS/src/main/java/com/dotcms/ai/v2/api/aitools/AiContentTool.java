package com.dotcms.ai.v2.api.aitools;

import com.dotcms.ai.util.ContentToStringUtil;
import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import dev.langchain4j.agent.tool.Tool;
import io.vavr.control.Try;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class AiContentTool {

    public static class ContentletDTO {
        public String contentId;
        public long languageId;
        public String title;
        public String body;
        public Map<String,Object> meta;
    }

    public static class ToolResult<T> {
        public boolean ok;
        public String errorCode;  // NOT_FOUND | INVALID_ARG | PERMISSION_DENIED | TIMEOUT | INTERNAL
        public String message;
        public T data;
        public static <T> ToolResult<T> ok(T d){ var r=new ToolResult<T>(); r.ok=true; r.data=d; return r; }
        public static <T> ToolResult<T> notFound(String m){ var r=new ToolResult<T>(); r.ok=false; r.errorCode="NOT_FOUND"; r.message=m; return r; }
        public static <T> ToolResult<T> invalid(String m){ var r=new ToolResult<T>(); r.ok=false; r.errorCode="INVALID_ARG"; r.message=m; return r; }
        public static <T> ToolResult<T> fail(String c,String m){ var r=new ToolResult<T>(); r.ok=false; r.errorCode=c; r.message=m; return r; }
    }

    @Tool("Fetch a contentlet by contentId and languageId, but language is optional could be null and the default one will be used. Returns NOT_FOUND if missing.")
    public ToolResult<ContentletDTO> getContentlet(final String contentId, final String languageId) {
        try {
            if (contentId == null || contentId.isBlank()) {
                return ToolResult.invalid("contentId is required");
            }

            final Contentlet content = fetchFromDotCMS(contentId, languageId); // <- reemplaza este stub
            if (content == null) {
                return ToolResult.notFound("Contentlet not found");
            }

            final List<Field> fields = ContentToStringUtil.impl.get().guessWhatFieldsToIndex(content);

            if (fields.isEmpty()) {
                return ToolResult.notFound(String.format("No valid fields to embed for Contentlet ID '%s' of type " +
                                "'%s' with title '%s'", content.getIdentifier(),
                        content.getContentType().variable(), content.getTitle()));
            }

            final Optional<String> contentTxt = ContentToStringUtil.impl.get().parseFields(content, fields);

            if (contentTxt.isEmpty() || UtilMethods.isEmpty(contentTxt.get())) {
                return ToolResult.fail("400", // not sure about this
                        "Something is wrong with the content, and the Contentlet cannot be embedded:\n" +
                        "Found fields from guessing:" + fields.stream().map(Field::variable));
            }

            final ContentletDTO dto = new ContentletDTO();
            dto.contentId  = contentId;
            dto.languageId = content.getLanguageId();
            dto.title      = content.getTitle();
            dto.body       = safeBody(contentTxt.get());
            dto.meta       = Map.of("variable", content.getContentType().variable());
            return ToolResult.ok(dto);

        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            return ToolResult.fail("INTERNAL", "temporary failure");
        }
    }

    // -------- stubs de PoC --------
    private Contentlet fetchFromDotCMS(final String identifier, final String lang) {

        final boolean live = true;
        final long languageId = 1;
        final User user = APILocator.systemUser();
        final boolean respectFrontendRoles=true;
        // todo: we need to retrieve the PageMode, the current user, etc
        return Try.of(()->APILocator.getContentletAPI().findContentletByIdentifier(identifier, live, languageId, user, respectFrontendRoles)).getOrNull();
    }


    private String safeBody(String b){ return b != null && b.length() > 4000 ? b.substring(0,4000)+"..." : b; }
}
