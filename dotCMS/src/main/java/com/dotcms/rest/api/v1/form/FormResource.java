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
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Provides method to access information about form
 */
@Path("/v1/form")
@Tag(name = "Forms", description = "Form management and processing")
public class FormResource {

    private String SUCCESS_CALLBACK_FUNCTION_TEMPLATE = "const formSuccessCallback_%s = function(contentlet){%s};";
    private final WebResource webResource;

    public FormResource () {
        this.webResource = new WebResource();
    }

    /**
     * Response with the <b>successCallback function</b> for a FORM with the ID or variable name equals to
     * <code>idOrVar</code>.
     *
     * The <b>successCallback function</b> has the follow sintax:
     * <code>
     *     const formSuccessCallback_[Content Type ID] = function(contentlet){
     *         [Content Type formSuccessCallback field value]
     *     };
     * </code>
     *
     * If we have a form with the follow value in the formSuccessCallback field:
     *
     * <code>
     *     window.location='/contact-us/thank-you?id=' + contentlet.identifier
     * </code>
     *
     * ... and with id equals to:  897cf4a9171a4204accbc1b498c813fe
     * Then this end point is going to response with the follow code:
     *
     * <code>
     *     const formSuccessCallback_897cf4a9171a4204accbc1b498c813fe = function(contentlet){
     *         window.location='/contact-us/thank-you?id=' + contentlet.identifier
     *     };
     * </code>
     *
     * If the Content type's id include '-' character like: 897cf4a9-171a-4204-accb-c1b498c813fe, then
     * the '-' are remove for the <b>successCallback function</b> name.
     *
     *
     * @param req
     * @param res
     * @param idOrVar form's Id or variable name
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @GET
    @Path("/{idOrVar}/successCallback")
    @NoCache
    @Produces({"application/javascript"})
    public final Response getSuccessCallbackFunction(@Context final HttpServletRequest req,
            @Context final HttpServletResponse res,
            @PathParam("idOrVar") final String idOrVar)
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
