package com.dotcms.ai.v2.api.aitools;

import com.dotcms.ai.Marshaller;
import com.dotcms.ai.util.ContentToStringUtil;
import com.dotcms.ai.v2.api.dto.ContentletDTO;
import com.dotcms.ai.v2.api.dto.CreateContentletSpec;
import com.dotcms.ai.v2.api.dto.MinimalContentletDTO;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import dev.langchain4j.agent.tool.Tool;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @Tool(
            "Set the value of a field on an existing contentlet. " +
                    "Parameters: identifier (content identifier), languageId (optional, default 1), " +
                    "fieldVar (field variable name), value (string, can be large HTML), publish (optional, default false). " +
                    "Example: setValue(\"1234-...-abcd\", 1, \"body\", \"<p>HTML...</p>\", true)"
    )
    public ToolResult<String> setValue(final String identifier, final Long languageId,
                                       final String fieldVar, final String value, final Boolean publish) {

        try {
            final User iaUser = APILocator.systemUser();
            final String err = this.validate(identifier, fieldVar, value);
            if (err != null) {
                return ToolResult.invalid(err);
            }

            final long lang = (languageId == null || languageId <= 0) ? APILocator.getLanguageAPI().getDefaultLanguage().getId() : languageId;
            final boolean doPublish = publish != null && publish;
            final PageMode pageMode = PageMode.EDIT_MODE;
            final  boolean live = pageMode.showLive;
            final boolean respectFrontendRoles = pageMode.respectAnonPerms;

            final Contentlet contentlet = APILocator.getContentletAPI().findContentletByIdentifier(identifier, live,  lang,  iaUser,  respectFrontendRoles);
            if (contentlet == null) {
                return ToolResult.invalid("Content not found");
            }
            final ContentType contentType = contentlet.getContentType();
            if (contentType == null) {
                return ToolResult.invalid("Content type not found");
            }

            final Field finalField = contentType.fieldMap().get(fieldVar);
            if (finalField == null) {
                return ToolResult.invalid("Field not found");
            }

            final Contentlet checkoutContentlet = APILocator.getContentletAPI().checkout(contentlet.getInode(), iaUser, false);
            final String inode = checkoutContentlet.getInode();
            APILocator.getContentletAPI().copyProperties(checkoutContentlet, contentlet.getMap());
            checkoutContentlet.setInode(inode);
            if (Objects.nonNull(contentlet.getVariantId())) {
                checkoutContentlet.setVariantId(contentlet.getVariantId());
            }

            APILocator.getContentletAPI().setContentletProperty(checkoutContentlet, finalField, value);
            final Contentlet savedContentlet = APILocator.getContentletAPI().checkin(checkoutContentlet, iaUser, respectFrontendRoles);
            // todo: publish if true

            return ToolResult.success("Field updated: identifier=" + identifier + ", inode=" + savedContentlet.getInode() +
                    ", field=" + fieldVar + (doPublish ? " (published)" : "") );

        } catch (DotSecurityException e) {
            Logger.error(this, "Permission error updating field", e);
            return ToolResult.denied(e.getMessage());
        } catch (DotDataException e) {
            Logger.error(this, "Data error updating field", e);
            return ToolResult.fail("Persistence error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ToolResult.invalid(e.getMessage());
        } catch (Exception e) {
            Logger.error(this, "Unexpected error updating field", e);
            return ToolResult.fail("Unexpected error: " + e.getMessage());
        }
    }

    public String validate(final String identifier, final String fieldVar, final String value) {
        if (StringUtils.isBlank(identifier)) {
            return "'identifier' is required";
        }
        if (StringUtils.isBlank(fieldVar))   {
            return "'fieldVar' is required";
        }
        if (value == null)       {
            return "'value' must not be null (use empty string to clear)";
        }
        return null;
    }

    @Tool(
            "Create a new contentlet (checkin) in dotCMS from a JSON spec. " +
                    "The JSON MUST include: 'contentType' (content type varName), 'host' (e.g. SYSTEM_HOST), " +
                    "'languageId' (e.g. 1), optional 'folder' (e.g. SYSTEM_FOLDER) " +
                    "and 'fields' (a map of fieldVarName -> value). " +
                    "Example: {\"contentType\":\"pet_product\",\"host\":\"SYSTEM_HOST\",\"languageId\":1," +
                    "\"folder\":\"SYSTEM_FOLDER\",\"publish\":true,\"fields\":{\"title\":\"Dog Toy\",\"price\":9.99,\"tags\":\"dog,toy\"}}"
    )
    public ToolResult<String> createContentlet(final String specJson) {

        try {

            if (specJson == null || specJson.trim().isEmpty()) {
                return ToolResult.invalid("specJson is required");
            }

            final CreateContentletSpec spec = Marshaller.unmarshal(specJson, CreateContentletSpec.class);

            final String validationError = this.validateContentlet(spec);
            if (validationError != null) {
                return ToolResult.invalid(validationError);
            }

            final Contentlet result = this.createContentlet(spec);
            final String identifier = result.getIdentifier();
            final String inode = result.getInode();

            return ToolResult.success("Created contentlet: identifier=" + identifier + ", inode=" + inode + ", title=" + result.getTitle());
        } catch (DotSecurityException e) {
            Logger.error(this, "Security error creating contentlet", e);
            return ToolResult.denied( e.getMessage());
        } catch (DotDataException e) {
            Logger.error(this, "Data error creating contentlet", e);
            return ToolResult.fail("Persistence error: " + e.getMessage());
        } catch (Exception e) {
            Logger.error(this, "Unexpected error creating contentlet", e);
            return ToolResult.fail("Unexpected error: " + e.getMessage());
        }
    }


    // -------- stubs de PoC --------

    /**
     * Creates a contentlet.
     * NOTE: First slice supports non-binary fields only.
     */
    public Contentlet createContentlet(final CreateContentletSpec spec)
            throws DotDataException, DotSecurityException {

        final User user = APILocator.systemUser();
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();

        final ContentTypeAPI contentTypeAPI =  APILocator.getContentTypeAPI(user);
        final ContentType contentType = contentTypeAPI.find(spec.getContentType());
        if (null == contentType) {
            throw new IllegalArgumentException("Content type not found: " + spec.getContentType());
        }
        Contentlet contentlet = new Contentlet();
        contentlet.setContentType(contentType); // varName del type
        contentlet.setHost(spec.getHost());               // "SYSTEM_HOST" o hostId/hostName resoluble
        contentlet.setLanguageId(spec.getLanguageId());

        // Opcional: folder is apply
        if (!StringUtils.isBlank(spec.getFolder())) {
            contentlet.setFolder(spec.getFolder()); // dotCMS resuelve SYSTEM_FOLDER, path o UUID
        }

        // Set fields
        for (final Map.Entry<String, Object> fieldEntry : spec.getFields().entrySet()) {

            final String fieldVar = fieldEntry.getKey();
            final Object value = fieldEntry.getValue();
            if (value == null) {
                continue;
            }

            if (value instanceof Number || value instanceof Boolean) {
                contentlet.setProperty(fieldVar, value);
            } else {
                contentlet.setStringProperty(fieldVar, String.valueOf(value));
            }
        }

        contentlet = contentletAPI.checkin(contentlet, user, false);

        if (spec.isPublish()) {
            contentletAPI.publish(contentlet, user, false);
        }

        return contentlet;
    }

    /**
     * Validates minimal required fields.
     */
    private String validateContentlet(final CreateContentletSpec spec) {
        if (spec == null) {
            return "Spec is null";
        }
        if (StringUtils.isBlank(spec.getContentType())) {
            return "'contentType' (varName) is required";
        }
        if (StringUtils.isBlank(spec.getHost())) {
            return "'host' is required (use SYSTEM_HOST if applicable)";
        }
        if (spec.getLanguageId() == null || spec.getLanguageId() <= 0) {
            return "'languageId' must be > 0";
        }
        if (spec.getFields() == null || spec.getFields().isEmpty()) {
            return "'fields' is required and must not be empty";
        }

        return null;
    }

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
