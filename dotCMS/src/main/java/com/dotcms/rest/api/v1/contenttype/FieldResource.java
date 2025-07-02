package com.dotcms.rest.api.v1.contenttype;

import static com.dotcms.util.CollectionsUtils.imap;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
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

/**
 * @deprecated {@link com.dotcms.rest.api.v2.contenttype.FieldResource} should be used instead. Path:/v2/contenttype/{typeId}/fields
 */
@Deprecated
@Path("/v1/contenttype/{typeId}/fields")
@Tag(name = "Content Type Field", description = "Content type field management and configuration")
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
			response = Response.ok(new ResponseEntityView<>(new JsonFieldTransformer(contentTypeFields).mapList())).build();
		} catch (DotStateException e) {

			response = ExceptionMapperUtil.createResponse(null, "Field is not valid ("+ e.getMessage() +")");

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);
		} catch (DotSecurityException e) {

			throw new ForbiddenException(e);
		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
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
		final FieldAPI fapi = APILocator.getContentTypeFieldAPI();

		Response response = null;

		try {
			Field field = new JsonFieldTransformer(fieldJson).from();
			if (UtilMethods.isSet(field.id())) {

				response = ExceptionMapperUtil.createResponse(null, "Field 'id' should not be set");

			} else {

				field = fapi.save(field, user);

				response = Response.ok(new ResponseEntityView(new JsonFieldTransformer(field).mapObject())).build();
			}
		} catch (DotStateException e) {

			response = ExceptionMapperUtil.createResponse(null, "Field is not valid ("+ e.getMessage() +")");

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
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
		final ContentTypeAPI tapi = APILocator.getContentTypeAPI(user, true);
		final FieldAPI fapi = APILocator.getContentTypeFieldAPI();

		Response response = null;

		try {
			List<Field> fields = fapi.byContentTypeId(tapi.find(typeId).id());

			response = Response.ok(new ResponseEntityView<>(new JsonFieldTransformer(fields).mapList())).build();

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
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

		final InitDataObject initData = this.webResource.init(null, false, req, false, null);
		final FieldAPI fapi = APILocator.getContentTypeFieldAPI();

		Response response = null;
		try {

			Field field = fapi.find(fieldId);

			response = Response.ok(new ResponseEntityView(new JsonFieldTransformer(field).mapObject())).build();

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	@GET
	@Path("/var/{fieldVar}")
	@JSONP
	@NoCache
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public Response getContentTypeFieldByVar(@PathParam("typeId") final String typeId,
											 @PathParam("fieldVar") final String fieldVar, @Context final HttpServletRequest httpServletRequest, @Context final HttpServletResponse httpServletResponse)
			throws DotDataException, DotSecurityException {

		this.webResource.init(null, httpServletRequest, httpServletResponse, false, null);
		final FieldAPI typeFieldAPI = APILocator.getContentTypeFieldAPI();

		Response response = null;
		try {

			Field field = typeFieldAPI.byContentTypeIdAndVar(typeId, fieldVar);

			response = Response.ok(new ResponseEntityView(new JsonFieldTransformer(field).mapObject())).build();

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
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
		final FieldAPI fapi = APILocator.getContentTypeFieldAPI();

		Response response = null;
	
		try {
			Field field = new JsonFieldTransformer(fieldJson).from();
			if (!UtilMethods.isSet(field.id())) {

				response = ExceptionMapperUtil.createResponse(null, "Field 'id' should be set");

			} else {

				Field currentField = fapi.find(fieldId);

				if (!currentField.id().equals(field.id())) {

					response = ExceptionMapperUtil.createResponse(null, "Field id '"+ fieldId +"' does not match a field with id '"+ field.id() +"'");

				} else {

					field = fapi.save(field, user);

					response = Response.ok(new ResponseEntityView(new JsonFieldTransformer(field).mapObject())).build();
				}
			}
		} catch (DotStateException e) {

			response = ExceptionMapperUtil.createResponse(null, "Field is not valid ("+ e.getMessage() +")");

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
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
		final FieldAPI fapi = APILocator.getContentTypeFieldAPI();

		Response response = null;
	
		try {
			Field field = new JsonFieldTransformer(fieldJson).from();
			if (!UtilMethods.isSet(field.id())) {

				response = ExceptionMapperUtil.createResponse(null, "Field 'id' should be set");

			} else {

				Field currentField = fapi.byContentTypeIdAndVar(typeId, fieldVar);

				if (!currentField.id().equals(field.id())) {

					response = ExceptionMapperUtil.createResponse(null, "Field var '"+ fieldVar +"' does not match a field with id '"+ field.id() +"'");

				} else {

					field = fapi.save(field, user);

					response = Response.ok(new ResponseEntityView(new JsonFieldTransformer(field).mapObject())).build();
				}
			}
		} catch (DotStateException e) {

			response = ExceptionMapperUtil.createResponse(null, "Field is not valid ("+ e.getMessage() +")");

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
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

		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);

		}catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
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

			Field field = fieldAPI.find(fieldId);
			fieldAPI.delete(field, user);

			String responseString = null;
			response = Response.ok(new ResponseEntityView<>(responseString)).build();

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
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
		final FieldAPI fapi = APILocator.getContentTypeFieldAPI();

		Response response = null;
		try {

			Field field = fapi.byContentTypeIdAndVar(typeId, fieldVar);

			fapi.delete(field, user);

			response = Response.ok(new ResponseEntityView<>((String)null)).build();

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}
}
