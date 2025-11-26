package com.dotcms.rest.api.v1.accessibility;

import com.dotcms.enterprise.achecker.ACheckerResponse;
import com.dotcms.enterprise.achecker.model.GuideLineBean;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.google.common.annotations.VisibleForTesting;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static com.dotcms.enterprise.achecker.ACheckerAPI.CONTENT;
import static com.dotcms.enterprise.achecker.ACheckerAPI.GUIDELINES;
import static com.dotcms.util.DotPreconditions.checkNotEmpty;

/**
 * This REST Endpoint provides the ability to validate content against the Accessibility Checker
 * service.
 *
 * @author Jose Castro
 * @since Sep 3rd, 2024
 */
@Path("/v1/achecker")
@Tag(name = "Accessibility Checker",
        description = "Endpoints that perform operations related to validating accessibility in content.")
public class ACheckerResource {

    private final WebResource webResource;

    @SuppressWarnings("unused")
    public ACheckerResource() {
        this(new WebResource());
    }

    @VisibleForTesting
    public ACheckerResource(final WebResource webResource) {
        this.webResource = webResource;
    }

    /**
     * Returns the list of Accessibility Guidelines available in the system.
     *
     * @return The list of Accessibility Guidelines available in the system.
     *
     * @throws Exception If the guidelines can't be retrieved.
     */
    @GET
    @Path("/guidelines")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(
            operationId = "getAccessibilityGuidelines",
            summary = "Retrieves Accessibility Guidelines",
            description = "Returns the list of available Accessibility Guidelines that are used to validate content.",
            tags = {"Accessibility Checker"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Guideline list retrieved successfully",
                            content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    public final Response getGuidelines(@Context final HttpServletRequest request,
                                        @Context final HttpServletResponse response) throws Exception {
        new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requireLicense(true)
                .init();
        final List<GuideLineBean> guidelines = APILocator.getACheckerAPI().getAccessibilityGuidelineList();
        return Response.ok(new ResponseEntityView<>(guidelines)).build();
    }

    /**
     * Validates the given content against the given guidelines. This endpoint is only available to
     * users with a valid Enterprise license. The request body must contain the following
     * parameters:
     * <ul>
     * <li>{@code content}: the content to validate (required)</li>
     * <li>{@code guidelines}: the guidelines to validate against (required)</li>
     * </ul>
     * <p>The response will contain the validation result as a
     * {@link ACheckerResponse} object.</p>
     *
     * @param request           the HTTP request
     * @param response          the HTTP response
     * @param accessibilityForm the request body object containing the content and guidelines to
     *                          validate
     *
     * @return the {@link ACheckerResponse} object with the list of issues found during the
     * validation, if any.
     */
    @POST
    @Path("/_validate")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes({MediaType.APPLICATION_JSON})
    @Operation(
            operationId = "postValidateContent",
            summary = "Validates content",
            description = "Validates the given content against the one or more Accessibility Guidelines.",
            tags = {"Accessibility Checker"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Content validated successfully",
                            content = @Content(mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    value = "{\n" +
                                                            "    \"entity\": {\n" +
                                                            "        \"errors\": [\n" +
                                                            "            {\n" +
                                                            "                \"check\": {\n" +
                                                            "                    \"check_id\": 98,\n" +
                                                            "                    \"confidence\": 2,\n" +
                                                            "                    \"confidenceEnum\": \"POTENTIAL\",\n" +
                                                            "                    \"decision_fail\": \"Not all abbreviations are marked with the <code>abbr</code> element.\",\n" +
                                                            "                    \"decision_pass\": \"All abbreviations are marked with the <code>abbr</code> element.\",\n" +
                                                            "                    \"description\": \"If <code>body</code> element content is greater than 10 characters (English) this error will be generated.\",\n" +
                                                            "                    \"err\": \"Abbreviations may not be marked with <code>abbr</code> element.\",\n" +
                                                            "                    \"func\": \"return (BasicFunctions::getPlainTextLength() <= 10);\",\n" +
                                                            "                    \"how_to_repair\": \"\",\n" +
                                                            "                    \"html_tag\": \"body\",\n" +
                                                            "                    \"lang\": \"en\",\n" +
                                                            "                    \"long_description\": null,\n" +
                                                            "                    \"name\": \"Abbreviations must be marked with <code>abbr</code> element.\",\n" +
                                                            "                    \"note\": \"\",\n" +
                                                            "                    \"open_to_public\": 1,\n" +
                                                            "                    \"question\": \"Are there any abbreviations in the document that are not marked with the <code>abbr</code> element?\",\n" +
                                                            "                    \"rationale\": \"_RATIONALE_98\",\n" +
                                                            "                    \"repair_example\": \"\",\n" +
                                                            "                    \"search_str\": null,\n" +
                                                            "                    \"test_expected_result\": \"1. All abbreviations are expected to be marked with a valid <code>abbr</code> element.\",\n" +
                                                            "                    \"test_failed_result\": \"1. Mark all abbreviations with a valid <code>abbr</code> element.\",\n" +
                                                            "                    \"test_procedure\": \"1. Check the document for any text abbreviations.\\\\n2. If an abbreviation is found, check if it is properly marked with the <code>abbr</code> element.\",\n" +
                                                            "                    \"user_id\": 0\n" +
                                                            "                },\n" +
                                                            "                \"col_number\": 110,\n" +
                                                            "                \"cssCode\": null,\n" +
                                                            "                \"htmlCode\": \"<body> Adventure travel done right  Wherever you want to go, whatever you want to get into, we’ve go...\",\n" +
                                                            "                \"image\": null,\n" +
                                                            "                \"imageAlt\": null,\n" +
                                                            "                \"line_number\": 0,\n" +
                                                            "                \"success\": false\n" +
                                                            "            },\n" +
                                                            "            ...\n" +
                                                            "            ...\n" +
                                                            "        ],\n" +
                                                            "        \"lang\": \"en\",\n" +
                                                            "        \"results\": [\n" +
                                                            "            {\n" +
                                                            "                \"check\": {\n" +
                                                            "                    \"check_id\": 29,\n" +
                                                            "                    \"confidence\": 0,\n" +
                                                            "                    \"confidenceEnum\": \"KNOWN\",\n" +
                                                            "                    \"decision_fail\": \"\",\n" +
                                                            "                    \"decision_pass\": \"\",\n" +
                                                            "                    \"description\": \"Each document must contain a valid <code>doctype</code> declaration.\",\n" +
                                                            "                    \"err\": \"<code>doctype</code> declaration missing.\",\n" +
                                                            "                    \"func\": \"return (BasicFunctions::getNumOfTagInWholeContent(\\\\\\\"doctype\\\\\\\") > 0);\",\n" +
                                                            "                    \"how_to_repair\": \"Add a valid <code>doctype</code> declaration to the document.\",\n" +
                                                            "                    \"html_tag\": \"html\",\n" +
                                                            "                    \"lang\": \"en\",\n" +
                                                            "                    \"long_description\": null,\n" +
                                                            "                    \"name\": \"HTML content has a valid <code>doctype</code> declaration.\",\n" +
                                                            "                    \"note\": \"\",\n" +
                                                            "                    \"open_to_public\": 1,\n" +
                                                            "                    \"question\": \"\",\n" +
                                                            "                    \"rationale\": \"\",\n" +
                                                            "                    \"repair_example\": \"\",\n" +
                                                            "                    \"search_str\": null,\n" +
                                                            "                    \"test_expected_result\": \"1. HTML content has a valid <code>doctype</code> declaration.\",\n" +
                                                            "                    \"test_failed_result\": \"1. Add a valid <code>doctype</code> declaration to the document.\",\n" +
                                                            "                    \"test_procedure\": \"1. Check for the presence of a <code>doctype</code> declaration at the start of the document.\\\\n2. Check the content of the <code>doctype</code> declaration.\",\n" +
                                                            "                    \"user_id\": 0\n" +
                                                            "                },\n" +
                                                            "                \"col_number\": 110,\n" +
                                                            "                \"cssCode\": null,\n" +
                                                            "                \"htmlCode\": \"<html lang=\\\"en\\\" xml:lang=\\\"en\\\">     <body> Adventure travel done right  Wherever you want to go, what...\",\n" +
                                                            "                \"image\": null,\n" +
                                                            "                \"imageAlt\": null,\n" +
                                                            "                \"line_number\": 0,\n" +
                                                            "                \"success\": true\n" +
                                                            "            },\n" +
                                                            "            ...\n" +
                                                            "            ...\n" +
                                                            "        ]\n" +
                                                            "    },\n" +
                                                            "    \"errors\": [],\n" +
                                                            "    \"i18nMessagesMap\": {},\n" +
                                                            "    \"messages\": [],\n" +
                                                            "    \"pagination\": null,\n" +
                                                            "    \"permissions\": []\n" +
                                                            "}"
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Bad Request"),
                    @ApiResponse(responseCode = "415", description = "Unsupported Media Type"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    public final Response validate(@Context final HttpServletRequest request,
                                   @Context final HttpServletResponse response,
                                   @NotNull final AccessibilityForm accessibilityForm) throws Exception {
        new WebResource.InitBuilder(this.webResource)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .requiredBackendUser(true)
                .requireLicense(true)
                .init();
        checkNotEmpty(accessibilityForm.get(CONTENT), IllegalArgumentException.class,
                String.format("'%s' parameter cannot be empty", CONTENT));
        checkNotEmpty(accessibilityForm.get(GUIDELINES), IllegalArgumentException.class,
                String.format("'%s' parameter cannot be empty", GUIDELINES));
        final ACheckerResponse aCheckerResponse = APILocator.getACheckerAPI().validate(accessibilityForm);
        return Response.ok(new ResponseACheckerEntityView(aCheckerResponse)).build();
    }

}
