package com.dotcms.rest;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.User;

@Path("/widget")
public class WidgetResource extends WebResource {


	@GET
	@Path("/{path:.*}")
	@Produces(MediaType.TEXT_PLAIN)
	public String getWidget(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("path") String path) throws DotDataException, DotSecurityException, IOException {

		Map<String, String> params = parsePath(path);
		String id = params.get(ID);
		String username = params.get(USER);
		String password = params.get(PASSWORD);
		User user = null;
		String result = null;

		/* Authenticate the User if passed */

		user = authenticateUser(username, password);

		/* Fetching the widget using id passed */

		if(!UtilMethods.isSet(id)) return null;

		Contentlet widget = APILocator.getContentletAPI().find(id, user, true);

		Structure contStructure = widget.getStructure();

		if (contStructure.getStructureType() == Structure.STRUCTURE_TYPE_WIDGET) {
			StringWriter writer = new StringWriter();
			StringBuilder widgetExecuteCode = new StringBuilder();


			org.apache.velocity.context.Context context = VelocityUtil.getWebContext(request, response);

			for(String key : widget.getMap().keySet()){
				context.put(key, widget.getMap().get(key));
			}

			Field field = contStructure.getFieldVar("widgetPreexecute");
			widgetExecuteCode.append(field.getValues().trim() + "\n");

			field = contStructure.getFieldVar("widgetCode");
			widgetExecuteCode.append(field.getValues().trim() + "\n");

			VelocityUtil.getEngine().evaluate(context, writer, "", widgetExecuteCode.toString());
			result = writer.toString();

		}

		return result;

	}

}