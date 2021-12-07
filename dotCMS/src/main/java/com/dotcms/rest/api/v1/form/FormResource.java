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
<<<<<<< HEAD
=======
import java.util.Optional;
>>>>>>> origin/master
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

<<<<<<< HEAD
=======
/**
 * Provides method to access information about form
 */
>>>>>>> origin/master
@Path("/v1/form")
public class FormResource {

    private String SUCCESS_CALLBACK_FUNCTION_TEMPLATE = "const formSuccessCallback_%s = function(contentlet){%s};";
    private final WebResource webResource;

    public FormResource () {
        this.webResource = new WebResource();
    }

<<<<<<< HEAD
=======
    /**
     * Response with the successCallback field value to the form with the ID or variable name equals to
     * <code>idOrVar</code>
     *
     * @param req
     * @param res
     * @param idOrVar form's Id or variable name
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
>>>>>>> origin/master
    @GET
    @Path("/{idOrVar}/successCallback")
    @NoCache
    @Produces({"application/javascript"})
    public final Response createType(@Context final HttpServletRequest req,
            @Context final HttpServletResponse res,
            @PathParam("idOrVar") final String idOrVar)
            throws DotDataException, DotSecurityException {

        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(idOrVar);

        if (BaseContentType.FORM  != contentType.baseType()){
            throw new NotFoundInDbException("The form not exists: " + idOrVar);
        }

<<<<<<< HEAD
        final List<Field> fields = contentType.fields().stream()
                .filter(field -> FORM_SUCCESS_CALLBACK.equals(field.variable()))
                .limit(1)
                .collect(Collectors.toList());

        if (fields.isEmpty()) {
            throw new BadRequestException(FORM_SUCCESS_CALLBACK + " field not exists in:" + idOrVar);
        }

        final String formSuccessCallback = fields.get(0).values();
=======
        final Optional<Field> fieldOptional = contentType.fields().stream()
                .filter(field -> FORM_SUCCESS_CALLBACK.equals(field.variable()))
                .findFirst();

        if (!fieldOptional.isPresent()) {
            throw new BadRequestException(FORM_SUCCESS_CALLBACK + " field not exists in:" + idOrVar);
        }

        final String formSuccessCallback = fieldOptional.get().values();
>>>>>>> origin/master
        final String functionSuccessCallback = String.format(SUCCESS_CALLBACK_FUNCTION_TEMPLATE, idOrVar,
                formSuccessCallback);
        return Response.ok(functionSuccessCallback).build();

    }
}
