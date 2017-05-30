package com.dotcms.rest.api.v1.contenttype;

import java.io.Serializable;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.transform.field.JsonFieldVariableTransformer;
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
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

@Path("/v1/contenttype/{typeId}/fields")
public class FieldVariableResource implements Serializable {
	private final WebResource webResource;

	public FieldVariableResource() {
		this(new WebResource());
	}

	@VisibleForTesting
	public FieldVariableResource(final WebResource webresource) {
		this.webResource = webresource;
	}

	private static final long serialVersionUID = 1L;


	@POST
	@Path("/id/{fieldId}/variables")
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public Response createFieldVariableByFieldId(@PathParam("typeId") final String typeId, @PathParam("fieldId") final String fieldId,
			final String fieldVariableJson, @Context final HttpServletRequest req) throws DotDataException, DotSecurityException {

		final InitDataObject initData = this.webResource.init(null, false, req, false, null);
		final User user = initData.getUser();
		final FieldAPI fapi = APILocator.getContentTypeFieldAPI();
		
		Response response = null;
		
		try {
			Field field = fapi.find(fieldId);

			FieldVariable fieldVariable = new JsonFieldVariableTransformer(fieldVariableJson).from();

			if (UtilMethods.isSet(fieldVariable.id())) {

				response = ExceptionMapperUtil.createResponse(null, "Field 'id' should not be set");

			} else if (!UtilMethods.isSet(fieldVariable.fieldId()) || !fieldVariable.fieldId().equals(field.id())) {

				response = ExceptionMapperUtil.createResponse(null, "Field fieldId '"+ fieldVariable.fieldId() +"' does not match a field with id '"+ field.id() +"'");

			} else {

				fieldVariable = fapi.save(fieldVariable, user);

				response = Response.ok(new ResponseEntityView(new JsonFieldVariableTransformer(fieldVariable).mapObject())).build();
			}
		} catch (DotStateException e) {

			response = ExceptionMapperUtil.createResponse(null, "Field variable is not valid ("+ e.getMessage() +")");

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	@POST
	@Path("/var/{fieldVar}/variables")
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public Response createFieldVariableByFieldVar(@PathParam("typeId") final String typeId, @PathParam("fieldVar") final String fieldVar,
			final String fieldVariableJson, @Context final HttpServletRequest req) throws DotDataException, DotSecurityException {

		final InitDataObject initData = this.webResource.init(null, false, req, false, null);
		final User user = initData.getUser();
		final FieldAPI fapi = APILocator.getContentTypeFieldAPI();
		
		Response response = null;
		
		try {
			Field field = fapi.byContentTypeIdAndVar(typeId, fieldVar);

			FieldVariable fieldVariable = new JsonFieldVariableTransformer(fieldVariableJson).from();

			if (UtilMethods.isSet(fieldVariable.id())) {

				response = ExceptionMapperUtil.createResponse(null, "Field 'id' should not be set");

			} else if (!UtilMethods.isSet(fieldVariable.fieldId()) || !fieldVariable.fieldId().equals(field.id())) {

				response = ExceptionMapperUtil.createResponse(null, "Field fieldId '"+ fieldVariable.fieldId() +"' does not match a field with id '"+ field.id() +"'");

			} else {

				fieldVariable = fapi.save(fieldVariable, user);

				response = Response.ok(new ResponseEntityView(new JsonFieldVariableTransformer(fieldVariable).mapObject())).build();
			}
		} catch (DotStateException e) {

			response = ExceptionMapperUtil.createResponse(null, "Field variable is not valid ("+ e.getMessage() +")");

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}


	@GET
	@Path("/id/{fieldId}/variables")
	@JSONP
	@NoCache
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public final Response getFieldVariablesByFieldId(@PathParam("typeId") final String typeId,
			@PathParam("fieldId") final String fieldId, @Context final HttpServletRequest req) {

		final InitDataObject initData = this.webResource.init(null, true, req, true, null);
		final User user = initData.getUser();
		final ContentTypeAPI tapi = APILocator.getContentTypeAPI(user, true);
		final FieldAPI fapi = APILocator.getContentTypeFieldAPI();

		Response response = null;

		try {
			Field field = fapi.find(fieldId);

			List<FieldVariable> fieldVariables = field.fieldVariables();

			response = Response.ok(new ResponseEntityView(new JsonFieldVariableTransformer(fieldVariables).mapList())).build();

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	@GET
	@Path("/var/{fieldVar}/variables")
	@JSONP
	@NoCache
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public final Response getFieldVariablesByFieldVar(@PathParam("typeId") final String typeId,
			@PathParam("fieldVar") final String fieldVar, @Context final HttpServletRequest req) {

		final InitDataObject initData = this.webResource.init(null, true, req, true, null);
		final User user = initData.getUser();
		final ContentTypeAPI tapi = APILocator.getContentTypeAPI(user, true);
		final FieldAPI fapi = APILocator.getContentTypeFieldAPI();

		Response response = null;

		try {
			Field field = fapi.byContentTypeIdAndVar(typeId, fieldVar);

			List<FieldVariable> fieldVariables = field.fieldVariables();

			response = Response.ok(new ResponseEntityView(new JsonFieldVariableTransformer(fieldVariables).mapList())).build();

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}


	@GET
	@Path("/id/{fieldId}/variables/id/{fieldVarId}")
	@JSONP
	@NoCache
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public Response getFieldVariableByFieldId(@PathParam("typeId") final String typeId,
			@PathParam("fieldId") final String fieldId, @PathParam("fieldVarId") final String fieldVarId,
			@Context final HttpServletRequest req) throws DotDataException, DotSecurityException {

		final InitDataObject initData = this.webResource.init(null, false, req, false, null);
		final FieldAPI fapi = APILocator.getContentTypeFieldAPI();

		Response response = null;
		try {

			Field field = fapi.find(fieldId);

			FieldVariable fieldVariable = getFieldVariable(field, fieldVarId);

			if (!field.id().equals(fieldVariable.fieldId())) {

				response = ExceptionMapperUtil.createResponse(null, "Field fieldVarId '"+ fieldVarId +"' does not match a field with id '"+ field.id() +"'");

			} else {

				response = Response.ok(new ResponseEntityView(new JsonFieldVariableTransformer(fieldVariable).mapObject())).build();				
			}

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	@GET
	@Path("/var/{fieldVar}/variables/id/{fieldVarId}")
	@JSONP
	@NoCache
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public Response getFieldVariableByFieldVar(@PathParam("typeId") final String typeId,
			@PathParam("fieldVar") final String fieldVar, @PathParam("fieldVarId") final String fieldVarId,
			@Context final HttpServletRequest req) throws DotDataException, DotSecurityException {

		final InitDataObject initData = this.webResource.init(null, false, req, false, null);
		final FieldAPI fapi = APILocator.getContentTypeFieldAPI();

		Response response = null;
		try {

			Field field = fapi.byContentTypeIdAndVar(typeId, fieldVar);

			FieldVariable fieldVariable = getFieldVariable(field, fieldVarId);

			if (!field.id().equals(fieldVariable.fieldId())) {

				response = ExceptionMapperUtil.createResponse(null, "Field fieldVarId '"+ fieldVarId +"' does not match a field with id '"+ field.id() +"'");

			} else {

				response = Response.ok(new ResponseEntityView(new JsonFieldVariableTransformer(fieldVariable).mapObject())).build();				
			}

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}


	@PUT
	@Path("/id/{fieldId}/variables/id/{fieldVarId}")
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public Response updateFieldVariableByFieldId(@PathParam("typeId") final String typeId, @PathParam("fieldId") final String fieldId,
			@PathParam("fieldVarId") final String fieldVarId, final String fieldVariableJson, @Context final HttpServletRequest req
	) throws DotDataException, DotSecurityException {

		final InitDataObject initData = this.webResource.init(null, false, req, false, null);
		final User user = initData.getUser();
		final FieldAPI fapi = APILocator.getContentTypeFieldAPI();

		Response response = null;

		try {
			Field field = fapi.find(fieldId);

			FieldVariable fieldVariable = new JsonFieldVariableTransformer(fieldVariableJson).from();

			if (!UtilMethods.isSet(fieldVariable.id())) {

				response = ExceptionMapperUtil.createResponse(null, "Field 'id' should be set");

			} else if (!UtilMethods.isSet(fieldVariable.fieldId()) || !fieldVariable.fieldId().equals(field.id())) {

				response = ExceptionMapperUtil.createResponse(null, "Field fieldId '"+ fieldVariable.fieldId() +"' does not match a field with id '"+ field.id() +"'");

			} else {

				FieldVariable currentFieldVariable = getFieldVariable(field, fieldVarId);

				if (!currentFieldVariable.id().equals(fieldVariable.id())) {

					response = ExceptionMapperUtil.createResponse(null, "Field variable id '"+ fieldVarId +"' does not match a field variable with id '"+ fieldVariable.id() +"'");

				} else {

					fieldVariable = fapi.save(fieldVariable, user);

					response = Response.ok(new ResponseEntityView(new JsonFieldVariableTransformer(fieldVariable).mapObject())).build();
				}
			}
		} catch (DotStateException e) {

			response = ExceptionMapperUtil.createResponse(null, "Field variable is not valid ("+ e.getMessage() +")");

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	@PUT
	@Path("/var/{fieldVar}/variables/id/{fieldVarId}")
	@JSONP
	@NoCache
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public Response updateFieldVariableByFieldVar(@PathParam("typeId") final String typeId, @PathParam("fieldVar") final String fieldVar,
			@PathParam("fieldVarId") final String fieldVarId, final String fieldVariableJson, @Context final HttpServletRequest req
	) throws DotDataException, DotSecurityException {

		final InitDataObject initData = this.webResource.init(null, false, req, false, null);
		final User user = initData.getUser();
		final FieldAPI fapi = APILocator.getContentTypeFieldAPI();

		Response response = null;

		try {
			Field field = fapi.byContentTypeIdAndVar(typeId, fieldVar);

			FieldVariable fieldVariable = new JsonFieldVariableTransformer(fieldVariableJson).from();

			if (!UtilMethods.isSet(fieldVariable.id())) {

				response = ExceptionMapperUtil.createResponse(null, "Field 'id' should be set");

			} else if (!UtilMethods.isSet(fieldVariable.fieldId()) || !fieldVariable.fieldId().equals(field.id())) {

				response = ExceptionMapperUtil.createResponse(null, "Field fieldId '"+ fieldVariable.fieldId() +"' does not match a field with id '"+ field.id() +"'");

			} else {

				FieldVariable currentFieldVariable = getFieldVariable(field, fieldVarId);

				if (!currentFieldVariable.id().equals(fieldVariable.id())) {

					response = ExceptionMapperUtil.createResponse(null, "Field variable id '"+ fieldVarId +"' does not match a field variable with id '"+ fieldVariable.id() +"'");

				} else {

					fieldVariable = fapi.save(fieldVariable, user);

					response = Response.ok(new ResponseEntityView(new JsonFieldVariableTransformer(fieldVariable).mapObject())).build();
				}
			}
		} catch (DotStateException e) {

			response = ExceptionMapperUtil.createResponse(null, "Field variable is not valid ("+ e.getMessage() +")");

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}


	@DELETE
	@Path("/id/{fieldId}/variables/id/{fieldVarId}")
	@JSONP
	@NoCache
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public Response deleteFieldVariableByFieldId(@PathParam("typeId") final String typeId,
			@PathParam("fieldId") final String fieldId, @PathParam("fieldVarId") final String fieldVarId,
			@Context final HttpServletRequest req) throws DotDataException, DotSecurityException {

		final InitDataObject initData = this.webResource.init(null, false, req, false, null);
		final User user = initData.getUser();
		final FieldAPI fapi = APILocator.getContentTypeFieldAPI();

		Response response = null;
		try {

			Field field = fapi.find(fieldId);

			FieldVariable fieldVariable = getFieldVariable(field, fieldVarId);

			if (!fieldVariable.fieldId().equals(field.id())) {

				response = ExceptionMapperUtil.createResponse(null, "Field fieldId '"+ fieldVariable.fieldId() +"' does not match a field with id '"+ field.id() +"'");

			} else {

				fapi.delete(fieldVariable);

				response = Response.ok(new ResponseEntityView(null)).build();
			}

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	@DELETE
	@Path("/var/{fieldVar}/variables/id/{fieldVarId}")
	@JSONP
	@NoCache
	@Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
	public Response deleteFieldVariableByFieldVar(@PathParam("typeId") final String typeId,
			@PathParam("fieldVar") final String fieldVar, @PathParam("fieldVarId") final String fieldVarId,
			@Context final HttpServletRequest req) throws DotDataException, DotSecurityException {

		final InitDataObject initData = this.webResource.init(null, false, req, false, null);
		final User user = initData.getUser();
		final FieldAPI fapi = APILocator.getContentTypeFieldAPI();

		Response response = null;
		try {

			Field field = fapi.byContentTypeIdAndVar(typeId, fieldVar);

			FieldVariable fieldVariable = getFieldVariable(field, fieldVarId);

			if (!fieldVariable.fieldId().equals(field.id())) {

				response = ExceptionMapperUtil.createResponse(null, "Field fieldId '"+ fieldVariable.fieldId() +"' does not match a field with id '"+ field.id() +"'");

			} else {

				fapi.delete(fieldVariable);

				response = Response.ok(new ResponseEntityView(null)).build();
			}

		} catch (NotFoundInDbException e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.NOT_FOUND);

		} catch (Exception e) {

			response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	private FieldVariable getFieldVariable(Field field, String fieldVarId) throws NotFoundInDbException {
		FieldVariable result = field.fieldVariablesMap().get(fieldVarId);
		if (result == null) {
			throw new NotFoundInDbException("Field variable with id:" + fieldVarId + " not found");
		}
		return result;
	}
}
