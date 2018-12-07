package com.dotcms.rest.api.v2.contenttype;


import com.dotcms.contenttype.business.ContentTypeAPI;
import static com.dotcms.util.CollectionsUtils.imap;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.Consumes;
import com.dotcms.repackage.javax.ws.rs.DELETE;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.POST;
import com.dotcms.repackage.javax.ws.rs.PUT;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDataValidationException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;


@Path("/v2/contenttype/{typeId}/fields")
public class FieldResource implements Serializable {
    private final WebResource webResource;
    private final FieldAPI fieldAPI;

    public FieldResource() {
        this(new WebResource(), APILocator.getContentTypeFieldAPI());
    }

    @VisibleForTesting
    public FieldResource(final WebResource webresource, final FieldAPI fieldAPI) {
        this.fieldAPI = fieldAPI;
        this.webResource = webresource;
    }

    private static final long serialVersionUID = 1L;


    @PUT
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    public Response updateFields(@PathParam("typeId") final String typeId, final String fieldsJson,
            @Context final HttpServletRequest req) throws DotDataException, DotSecurityException {

        final InitDataObject initData = this.webResource.init(null, false, req, false, null);
        final User user = initData.getUser();

        Response response = null;

        try {
            final List<Field> fields = new JsonFieldTransformer(fieldsJson).asList();

            for (final Field field : fields) {
                fieldAPI.save(field, user);
            }

            final List<Field> contentTypeFields = fieldAPI.byContentTypeId(typeId);
            response = Response.ok(new ResponseEntityView(new JsonFieldTransformer(contentTypeFields).mapList())).build();
        } catch (Exception e) {
            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    }

    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    public Response createContentTypeField(@PathParam("typeId") final String typeId, final String fieldJson,
            @Context final HttpServletRequest req) throws DotDataException, DotSecurityException {

        final InitDataObject initData = this.webResource.init(null, false, req, false, null);
        final User user = initData.getUser();

        Response response = null;

        try {
            Field field = new JsonFieldTransformer(fieldJson).from();
            if (UtilMethods.isSet(field.id())) {

                response = ExceptionMapperUtil.createResponse(null, "Field 'id' should not be set");

            } else {

                field = fieldAPI.save(field, user);

                response = Response.ok(new ResponseEntityView(new JsonFieldTransformer(field).mapObject())).build();
            }
        } catch (Exception e) {

            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    }

    @GET
    @JSONP
    @NoCache
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    public final Response getContentTypeFields(@PathParam("typeId") final String typeId,
            @Context final HttpServletRequest req) {

        final InitDataObject initData = this.webResource.init(null, true, req, true, null);
        final User user = initData.getUser();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user, true);

        Response response = null;

        try {
            final List<Field> fields = fieldAPI.byContentTypeId(contentTypeAPI.find(typeId).id());

            response = Response.ok(new ResponseEntityView(new JsonFieldTransformer(fields).mapList())).build();

        } catch (Exception e) {

            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    }


    @GET
    @Path("/id/{fieldId}")
    @JSONP
    @NoCache
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    public Response getContentTypeFieldById(@PathParam("typeId") final String typeId,
            @PathParam("fieldId") final String fieldId, @Context final HttpServletRequest req)
            throws DotDataException, DotSecurityException {

        this.webResource.init(null, false, req, false, null);

        Response response = null;
        try {

            final Field field = fieldAPI.find(fieldId);

            response = Response.ok(new ResponseEntityView(new JsonFieldTransformer(field).mapObject())).build();

        } catch (Exception e) {

            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    }

    @GET
    @Path("/var/{fieldVar}")
    @JSONP
    @NoCache
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    public Response getContentTypeFieldByVar(@PathParam("typeId") final String typeId,
            @PathParam("fieldVar") final String fieldVar, @Context final HttpServletRequest req)
            throws DotDataException, DotSecurityException {

        this.webResource.init(null, false, req, false, null);

        Response response = null;
        try {

            final Field field = fieldAPI.byContentTypeIdAndVar(typeId, fieldVar);

            response = Response.ok(new ResponseEntityView(new JsonFieldTransformer(field).mapObject())).build();

        } catch (Exception e) {

            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    }


    @PUT
    @Path("/id/{fieldId}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    public Response updateContentTypeFieldById(@PathParam("typeId") final String typeId, @PathParam("fieldId") final String fieldId,
            final String fieldJson, @Context final HttpServletRequest req) throws DotDataException, DotSecurityException {

        final InitDataObject initData = this.webResource.init(null, false, req, false, null);
        final User user = initData.getUser();

        Response response = null;

        try {
            Field field = new JsonFieldTransformer(fieldJson).from();
            if (!UtilMethods.isSet(field.id())) {

                response = ExceptionMapperUtil.createResponse(null, "Field 'id' should be set");

            } else {

                final Field currentField = fieldAPI.find(fieldId);

                if (!currentField.id().equals(field.id())) {

                    throw new DotDataValidationException("Field id '"+ fieldId +"' does not match a field with id '"+ field.id() +"'");

                } else {

                    field = fieldAPI.save(field, user);

                    response = Response.ok(new ResponseEntityView(new JsonFieldTransformer(field).mapObject())).build();
                }
            }
        } catch (Exception e) {

            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    }

    @PUT
    @Path("/var/{fieldVar}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    public Response updateContentTypeFieldByVar(@PathParam("typeId") final String typeId, @PathParam("fieldVar") final String fieldVar,
            final String fieldJson, @Context final HttpServletRequest req) throws DotDataException, DotSecurityException {

        final InitDataObject initData = this.webResource.init(null, false, req, false, null);
        final User user = initData.getUser();

        Response response = null;

        try {
            Field field = new JsonFieldTransformer(fieldJson).from();
            if (!UtilMethods.isSet(field.id())) {

                response = ExceptionMapperUtil.createResponse(null, "Field 'id' should be set");

            } else {

                final Field currentField = fieldAPI.byContentTypeIdAndVar(typeId, fieldVar);

                if (!currentField.id().equals(field.id())) {

                    throw new DotDataValidationException("Field var '"+ fieldVar +"' does not match a field with id '"+ field.id() +"'");

                } else {

                    field = fieldAPI.save(field, user);

                    response = Response.ok(new ResponseEntityView(new JsonFieldTransformer(field).mapObject())).build();
                }
            }
        } catch (Exception e) {

            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    }


    @DELETE
    @JSONP
    @NoCache
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    public Response deleteFields(@PathParam("typeId") final String typeId, final String[] fieldsID, @Context final HttpServletRequest req)
            throws DotDataException, DotSecurityException {

        final InitDataObject initData = this.webResource.init(null, false, req, false, null);
        final User user = initData.getUser();

        Response response = null;
        try {
            final List<String> deletedIds = new ArrayList<>();

            for (final String fieldId : fieldsID) {
                try {
                    final Field field = fieldAPI.find(fieldId);
                    fieldAPI.delete(field, user);
                    deletedIds.add(fieldId);
                } catch (NotFoundInDbException e) {
                    continue;
                }
            }

            final List<Field> contentTypeFields = fieldAPI.byContentTypeId(typeId);
            response = Response.ok(new ResponseEntityView(imap("deletedIds", deletedIds,
                    "fields", new JsonFieldTransformer(contentTypeFields).mapList()))).build();

        } catch (Exception e) {

            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    }

    @DELETE
    @Path("/id/{fieldId}")
    @JSONP
    @NoCache
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    public Response deleteContentTypeFieldById(@PathParam("typeId") final String typeId,
            @PathParam("fieldId") final String fieldId, @Context final HttpServletRequest req)
            throws DotDataException, DotSecurityException {

        final InitDataObject initData = this.webResource.init(null, false, req, false, null);
        final User user = initData.getUser();

        Response response = null;
        try {

            final Field field = fieldAPI.find(fieldId);
            fieldAPI.delete(field, user);

            response = Response.ok(new ResponseEntityView(null)).build();

        } catch (Exception e) {

            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    }

    @DELETE
    @Path("/var/{fieldVar}")
    @JSONP
    @NoCache
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    public Response deleteContentTypeFieldByVar(@PathParam("typeId") final String typeId,
            @PathParam("fieldVar") final String fieldVar, @Context final HttpServletRequest req)
            throws DotDataException, DotSecurityException {

        final InitDataObject initData = this.webResource.init(null, false, req, false, null);
        final User user = initData.getUser();

        Response response = null;
        try {

            final Field field = fieldAPI.byContentTypeIdAndVar(typeId, fieldVar);

            fieldAPI.delete(field, user);

            response = Response.ok(new ResponseEntityView(null)).build();

        } catch (Exception e) {

            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    }
}
