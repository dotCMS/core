package com.dotcms.rest;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.dotcms.rest.annotation.SwaggerCompliant;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

@SwaggerCompliant(value = "Legacy and utility APIs", batch = 8)
@Tag(name = "Widgets")
@Path("/widget")
public class WidgetResource {

    private final WebResource webResource = new WebResource();

    @Operation(
        summary = "Render widget",
        description = "Renders a custom widget by processing its Velocity templates. Supports widget lookup by ID or inode with language and live/working version options. Executes widget pre-execute code and main widget code."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Widget rendered successfully",
                    content = @Content(mediaType = "text/plain")),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - missing required ID or inode parameter",
                    content = @Content(mediaType = "text/plain")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication may be required for working version",
                    content = @Content(mediaType = "text/plain")),
        @ApiResponse(responseCode = "404", 
                    description = "Widget not found",
                    content = @Content(mediaType = "text/plain")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error during widget processing",
                    content = @Content(mediaType = "text/plain"))
    })
    @GET
	@Path("/{params:.*}")
	@Produces("text/plain; charset=UTF-8")
	public Response getWidget(@Context HttpServletRequest request, @Context HttpServletResponse response, 
		@Parameter(description = "URL parameters including id, inode, language, and live options", required = true) @PathParam("params") String params) throws ResourceNotFoundException, ParseErrorException, Exception {


        InitDataObject initData = webResource.init(params, request, response, false, null);

        //Creating an utility response object
        ResourceResponse responseResource = new ResourceResponse( initData.getParamsMap() );

		Map<String, String> paramsMap = initData.getParamsMap();
		User user = initData.getUser();

		String id = paramsMap.get(RESTParams.ID.getValue());
		long language = APILocator.getLanguageAPI().getDefaultLanguage().getId();

		if(paramsMap.get(RESTParams.LANGUAGE.getValue()) != null){
			try{
				language= Long.parseLong(paramsMap.get(RESTParams.LANGUAGE.getValue()))	;
			}
			catch(Exception e){
				Logger.error(this.getClass(), "Invald language passed in, defaulting to, well, the default");
			}
		}
		String inode = null;
		boolean live = true;

		if(user!=null){
			live=	(paramsMap.get(RESTParams.LIVE.getValue()) == null || ! "false".equals(paramsMap.get(RESTParams.LIVE.getValue())));
			inode = paramsMap.get(RESTParams.INODE.getValue());
		}

		if(!UtilMethods.isSet(id) && !UtilMethods.isSet(inode)) {
			response.getWriter().println("Please pass an id (or inode + user) in via the url");

			return null;
		}

		/* Fetching the widget using id passed */
		Contentlet widget = null;
		if(UtilMethods.isSet(inode)){
			widget = APILocator.getContentletAPI().find(inode, user, true);
		}
		else{
			widget = APILocator.getContentletAPI().findContentletByIdentifier(id, live, language, user, true);

		}

        return responseResource.response( parseWidget( request, response, widget ) );
    }

	public static String parseWidget(HttpServletRequest request, HttpServletResponse response, Contentlet widget) throws IOException {
		Structure contStructure = widget.getStructure();
		String result = "";

		if (contStructure.getStructureType() == Structure.STRUCTURE_TYPE_WIDGET) {
			StringWriter firstEval = new StringWriter();
			StringWriter secondEval = new StringWriter();
			StringBuilder widgetExecuteCode = new StringBuilder();

			org.apache.velocity.context.Context context = VelocityUtil.getBasicContext();
            for (String key : widget.getMap().keySet()) {
                context.put(key, widget.getMap().get(key).toString());
                context.put("widget" + key, widget.getMap().get(key).toString());
            }
            List<Field> fields = FieldsCache.getFieldsByStructureInode(contStructure.getInode());

            for (Field field : fields) {
                if (field.getFieldType().equals(Field.FieldType.HOST_OR_FOLDER.toString())) {
                    String host = widget.getHost();
                    String folder = widget.getFolder();
                    String fieldValue = UtilMethods.isSet(folder) && !folder.equals(FolderAPI.SYSTEM_FOLDER)?folder:host;
                    context.put(field.getVelocityVarName(), fieldValue);
                    context.put("widget" +field.getVelocityVarName(), fieldValue);
                }
            }
            // web context should overwrite everything
			context = VelocityUtil.getWebContext(context, request, response);

  			Field field = contStructure.getFieldVar("widgetPreexecute");

  			if(field!=null) {
    			String fval = field.getValues()!=null ? field.getValues().trim() : "";
    			widgetExecuteCode.append(fval + "\n");
  			}

			field = contStructure.getFieldVar("widgetCode");
			String fval = field.getValues()!=null ? field.getValues().trim() : "";
			widgetExecuteCode.append(fval + "\n");

			VelocityUtil.getEngine().evaluate(context, firstEval, "", widgetExecuteCode.toString());
			VelocityUtil.getEngine().evaluate(context, secondEval, "", firstEval.toString());
			result = secondEval.toString();

		}
		return result;
	}

}
