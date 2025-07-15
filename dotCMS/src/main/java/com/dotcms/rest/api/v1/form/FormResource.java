package com.dotcms.rest.api.v1.form;

import static com.dotcms.contenttype.model.type.FormContentType.FORM_SUCCESS_CALLBACK;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.WebResource.InitBuilder;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.contenttype.ContentTypeForm;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dotcms.rest.annotation.SwaggerCompliant;

/**
 * Provides method to access information about form
 */
@SwaggerCompliant(value = "Rules engine and business logic APIs", batch = 6)
@Path("/v1/form")
@Tag(name = "Forms")
public class FormResource {

    private String SUCCESS_CALLBACK_FUNCTION_TEMPLATE = "const formSuccessCallback_%s = function(contentlet){%s};";
    private final WebResource webResource;

    public FormResource () {
        this.webResource = new WebResource();
    }

    @Operation(
        summary = "Get form success callback function",
        description = "Generates a JavaScript callback function for a form based on its formSuccessCallback field value. The function name includes the content type ID with dashes removed. Returns executable JavaScript code."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Success callback function generated successfully",
                    content = @Content(mediaType = "application/javascript")),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - formSuccessCallback field not found in form",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Form not found or not a form content type",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/{idOrVar}/successCallback")
    @NoCache
    @Produces({"application/javascript"})
    public final Response getSuccessCallbackFunction(@Context final HttpServletRequest req,
            @Context final HttpServletResponse res,
            @Parameter(description = "Form ID or variable name", required = true) @PathParam("idOrVar") final String idOrVar)
            throws DotDataException, DotSecurityException {

        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(idOrVar);

        if (BaseContentType.FORM  != contentType.baseType()){
            throw new NotFoundInDbException("The form not exists: " + idOrVar);
        }

        final Optional<Field> fieldOptional = contentType.fields().stream()
                .filter(field -> FORM_SUCCESS_CALLBACK.equals(field.variable()))
                .findFirst();

        if (fieldOptional.isEmpty()) {
            throw new BadRequestException(FORM_SUCCESS_CALLBACK + " field not exists in:" + idOrVar);
        }

        final String formSuccessCallback = fieldOptional.get().values();
        final String functionSuccessCallback = String.format(SUCCESS_CALLBACK_FUNCTION_TEMPLATE,
                contentType.id().replaceAll("-", ""),
                formSuccessCallback);
        return Response.ok(functionSuccessCallback).build();

    }
}
