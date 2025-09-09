package com.dotcms.ai.v2.api.aitools;

import com.dotcms.ai.util.ContentToStringUtil;
import com.dotcms.ai.v2.api.dto.ContentletDTO;
import com.dotcms.ai.v2.api.dto.MinimalContentletDTO;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import dev.langchain4j.agent.tool.Tool;
import io.vavr.control.Try;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class AiContentTools {


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
                return ToolResult.fail(
                        "Something is wrong with the content, and the Contentlet cannot be embedded:\n" +
                                "Found fields from guessing:" + fields.stream().map(Field::variable));
            }

            final ContentletDTO dto = new ContentletDTO.Builder()
                    .contentId(contentId).languageId(content.getLanguageId())
                    .title(content.getTitle())
                    .body(safeBody(contentTxt.get()))
                    .meta(Map.of("variable", content.getContentType().variable()))
                    .build();

            return ToolResult.success(dto);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            return ToolResult.fail("temporary failure");
        }
    }

    @Tool("Fetch a list of contentlets based on the content type with the variableName.\n" +
           "Parameters:\n" +
                   "- limit:  int (optional). Number of items to return (default 20, min 1, max 100).\n" +
                   "- offset: int (optional). Starting index (default 0). Use 0 unless you are paginating a next page.\n" +
                   "Guidance:\n" +
                   "- If you specify offset, you MUST also specify limit.\n" +
                   "- For first page, use limit=20, offset=0.\n" +
                   "Examples:\n" +
                   "- First page of all types: variableName=Blog, limit=20, offset=0\n" +
                   "- Next page: variableName=Blog, limit=20, offset=20\n" +
             "Returns NOT_FOUND if missing.")
    public ToolResult<List<MinimalContentletDTO>> findContentletsByContentTypeVarName(final String variableName, final Integer limit, final Integer offset) {
        try {
            if (variableName == null || variableName.isBlank()) {
                return ToolResult.invalid("variableName is required");
            }

            final User user = APILocator.systemUser(); // todo: we need to recover the current user
            final ContentType contentType = APILocator.getContentTypeAPI(user).find(variableName);

            if (contentType == null) {
                return ToolResult.invalid("Invalid variableName: " + variableName);
            }

            final String structureInode = contentType.inode();
            final boolean respectFrontendRoles = false; // todo: we need to figured out this
            final List<Contentlet> contentlets = APILocator.getContentletAPI().findByStructure(structureInode, user, respectFrontendRoles, limit, offset);
            if (contentlets == null) {
                return ToolResult.notFound("Contentlets not found");
            }

            final List<MinimalContentletDTO> minimalContentletDTOS = new ArrayList<>();
            for (final Contentlet content : contentlets) {

                minimalContentletDTOS.add(new MinimalContentletDTO.Builder()
                        .contentId(content.getIdentifier()).languageId(content.getLanguageId())
                        .title(content.getTitle()).build());
            }

            return ToolResult.success(minimalContentletDTOS);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            return ToolResult.fail("temporary failure");
        }
    }

    // -------- stubs de PoC --------
    private Contentlet fetchFromDotCMS(final String identifier, final String lang) {

        final boolean live = true;
        final long languageId = 1;
        final User user = APILocator.systemUser();
        final boolean respectFrontendRoles = true;
        // todo: we need to retrieve the PageMode, the current user, etc
        return Try.of(() -> APILocator.getContentletAPI().findContentletByIdentifier(identifier, live, languageId, user, respectFrontendRoles)).getOrNull();
    }


    private String safeBody(String b) {
        return b != null && b.length() > 4000 ? b.substring(0, 4000) + "..." : b;
    }
}
