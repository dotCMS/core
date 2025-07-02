package com.dotcms.rest.api.v2.contenttype;


import static com.dotcms.util.CollectionsUtils.imap;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
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
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;
import io.swagger.v3.oas.annotations.tags.Tag;


@Path("/v2/contenttype/{typeIdOrVarName}/fields")
@Tag(name = "Content Type Field", description = "Content type field definitions and configuration")
public class FieldResource implements Serializable {
    private final WebResource webResource;
    private final FieldAPI fieldAPI;

    public FieldResource() {
        this(new WebResource(), APILocator.getContentTypeFieldAPI());
    }

    @VisibleForTesting
    protected FieldResource(final WebResource webresource, final FieldAPI fieldAPI) {
        this.fieldAPI = fieldAPI;
        this.webResource = webresource;
    }

    private static final long serialVersionUID = 1L;


    @PUT
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    /**
     * @deprecated {@link com.dotcms.rest.api.v3.contenttype.FieldResource#updateFields(String, String, HttpServletRequest)}
     * @since 5.2
     */
    @Deprecated()
    public Response updateFields(@PathParam("typeIdOrVarName") final String typeIdOrVarName, final String fieldsJson,
                                 @Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse) throws DotDataException, DotSecurityException {

        final InitDataObject initData = this.webResource.init(null, httpServletRequest, httpServletResponse, false, null);
        final User user = initData.getUser();

        Response response = null;

        try {
            final List<Field> fields = new JsonFieldTransformer(fieldsJson).asList();

            for (final Field field : fields) {
                fieldAPI.save(field, user);
            }
            final ContentType contentType = APILocator.getContentTypeAPI(user).find(typeIdOrVarName);
            final List<Field> contentTypeFields = fieldAPI.byContentTypeId(contentType.id());
            response = Response.ok(new ResponseEntityView<>(new JsonFieldTransformer(contentTypeFields).mapList())).build();
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
    public Response createContentTypeField(@PathParam("typeIdOrVarName") final String typeIdOrVarName, final String fieldJson,
            @Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse) throws DotDataException, DotSecurityException {

        final InitDataObject initData = this.webResource.init(null, httpServletRequest, httpServletResponse, false, null);
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
    /**
     * @deprecated {@link com.dotcms.rest.api.v3.contenttype.FieldResource#getContentTypeFields(String, String, HttpServletRequest)}
     * @since 5.2
     */
    @Deprecated()
    public final Response getContentTypeFields(@PathParam("typeIdOrVarName") final String typeIdOrVarName,
            @Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse) {

        final InitDataObject initData = this.webResource.init(null, httpServletRequest, httpServletResponse, true, null);
        final User user = initData.getUser();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user, true);

        Response response = null;

        try {
            //if we're dealing with a UUID we can use it right away. Otherwise Will attempt to resolve the ContentType out of a varName
            final String contentTypeId = UUIDUtil.isUUID(typeIdOrVarName) ? typeIdOrVarName
                    : APILocator.getContentTypeAPI(user).find(typeIdOrVarName).id();
            final List<Field> fields = fieldAPI.byContentTypeId(contentTypeAPI.find(contentTypeId).id());

            response = Response.ok(new ResponseEntityView<>(new JsonFieldTransformer(fields).mapList())).build();

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
    public Response getContentTypeFieldById(
            @PathParam("fieldId") final String fieldId, @Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse)
            throws DotDataException, DotSecurityException {

        this.webResource.init(null, httpServletRequest, httpServletResponse, false, null);

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
    public Response getContentTypeFieldByVar(@PathParam("typeIdOrVarName") final String typeIdOrVarName,
            @PathParam("fieldVar") final String fieldVar, @Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse)
            throws DotDataException, DotSecurityException {

        final InitDataObject initData = this.webResource.init(null, httpServletRequest, httpServletResponse, false, null);
        final User user = initData.getUser();
        Response response = null;
        try {
            //if we're dealing with a UUID we can use it right away. Otherwise Will attempt to resolve the ContentType out of a varName
            final String contentTypeId = UUIDUtil.isUUID(typeIdOrVarName) ? typeIdOrVarName
                    : APILocator.getContentTypeAPI(user).find(typeIdOrVarName).id();
            final Field field = fieldAPI.byContentTypeIdAndVar(contentTypeId, fieldVar);

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
    public Response updateContentTypeFieldById(@PathParam("fieldId") final String fieldId,
            final String fieldJson, @Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse) throws DotDataException, DotSecurityException {

        final InitDataObject initData = this.webResource.init(null, httpServletRequest, httpServletResponse, false, null);
        final User user = initData.getUser();

        Response response = null;

        try {
            Field field = new JsonFieldTransformer(fieldJson).from();
            if (!UtilMethods.isSet(field.id())) {

                response = ExceptionMapperUtil.createResponse(null, "Field 'id' should be set");

            } else {

                final Field currentField = fieldAPI.find(fieldId);

                if (!currentField.id().equals(field.id())) {

                    throw new DotDataValidationException("Field id '"+ field.id() +"' does not match a field with id '"+ currentField.id() +"'");

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
    public Response updateContentTypeFieldByVar(@PathParam("typeIdOrVarName") final String typeIdOrVarName, @PathParam("fieldVar") final String fieldVar,
            final String fieldJson, @Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse) throws DotDataException, DotSecurityException {

        final InitDataObject initData = this.webResource.init(null, httpServletRequest, httpServletResponse, false, null);
        final User user = initData.getUser();

        Response response = null;

        try {
            Field field = new JsonFieldTransformer(fieldJson).from();
            if (!UtilMethods.isSet(field.id())) {

                response = ExceptionMapperUtil.createResponse(null, "Field 'id' should be set");

            } else {
                final String contentTypeId = UUIDUtil.isUUID(typeIdOrVarName) ? typeIdOrVarName
                        : APILocator.getContentTypeAPI(user).find(typeIdOrVarName).id();
                final Field currentField = fieldAPI.byContentTypeIdAndVar(contentTypeId, fieldVar);

                if (!currentField.id().equals(field.id())) {

                    throw new DotDataValidationException("Field id '"+ field.id() +"' does not match a field with id '"+ currentField.id() +"'");

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
    /**
     * @deprecated {@link com.dotcms.rest.api.v3.contenttype.FieldResource#deleteFields(String, String[], HttpServletRequest)}
     * @since 5.2
     */
    @Deprecated()
    public Response deleteFields(@PathParam("typeIdOrVarName") final String typeIdOrVarName, final String[] fieldsID,
                                 @Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse)
            throws DotDataException, DotSecurityException {

        final InitDataObject initData = this.webResource.init(null, httpServletRequest, httpServletResponse, false, null);
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
            final String contentTypeId = UUIDUtil.isUUID(typeIdOrVarName) ? typeIdOrVarName
                    : APILocator.getContentTypeAPI(user).find(typeIdOrVarName).id();
            final List<Field> contentTypeFields = fieldAPI.byContentTypeId(contentTypeId);
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
    public Response deleteContentTypeFieldById(
            @PathParam("fieldId") final String fieldId,
            @Context final HttpServletRequest httpServletRequest,
            @Context final HttpServletResponse httpServletResponse)
            throws DotDataException, DotSecurityException {

        final InitDataObject initData = this.webResource.init(null, httpServletRequest, httpServletResponse, false, null);
        final User user = initData.getUser();

        Response response = null;
        try {

            final Field field = fieldAPI.find(fieldId);
            fieldAPI.delete(field, user);

            response = Response.ok(new ResponseEntityView<>((String)null)).build();

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
    public Response deleteContentTypeFieldByVar(@PathParam("typeIdOrVarName") final String typeIdOrVarName,
            @PathParam("fieldVar") final String fieldVar,
            @Context final HttpServletRequest httpServletRequest,
            @Context final HttpServletResponse httpServletResponse)
            throws DotDataException, DotSecurityException {

        final InitDataObject initData = this.webResource.init(null, httpServletRequest, httpServletResponse, false, null);
        final User user = initData.getUser();

        Response response = null;
        try {
            final String contentTypeId = UUIDUtil.isUUID(typeIdOrVarName) ? typeIdOrVarName
                    : APILocator.getContentTypeAPI(user).find(typeIdOrVarName).id();
            final Field field = fieldAPI.byContentTypeIdAndVar(contentTypeId, fieldVar);

            fieldAPI.delete(field, user);

            response = Response.ok(new ResponseEntityView<>((String)null)).build();

        } catch (Exception e) {

            response = ResponseUtil.mapExceptionResponse(e);
        }

        return response;
    }
}
