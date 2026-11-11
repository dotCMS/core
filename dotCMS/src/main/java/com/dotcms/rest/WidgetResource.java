package com.dotcms.rest;

import com.dotcms.rendering.velocity.WidgetRenderer;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
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

@Tag(name = "Widgets")
@Path("/widget")
public class WidgetResource {

    /**
     * Response header set when the widget's field values were returned as text.
     */
    public static final String WIDGET_RENDER_HEADER = "X-DotCMS-Widget-Render";

    /**
     * Value of {@link #WIDGET_RENDER_HEADER} when widget field scripting did not run.
     */
    public static final String FIELD_SCRIPTING_OFF = "field-scripting-off";

    private final WebResource webResource = new WebResource();

    @GET
	@Path("/{params:.*}")
	@Produces("text/plain; charset=UTF-8")
	public Response getWidget(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("params") String params) throws ResourceNotFoundException, ParseErrorException, Exception {


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

        final WidgetRenderer.Rendering rendering = WidgetRenderer.getInstance()
                .renderWithDetails(request, response, widget);
        if (rendering.fieldScriptingSkipped()) {
            // The body is the widget text itself, so the note travels as a header
            response.setHeader(WIDGET_RENDER_HEADER, FIELD_SCRIPTING_OFF);
        }
        return responseResource.response(rendering.text());
    }

	/**
	 * Renders a widget contentlet.
	 *
	 * @deprecated use {@link WidgetRenderer#render(HttpServletRequest, HttpServletResponse, Contentlet)}
	 * @param request  the current request
	 * @param response the current response
	 * @param widget   the widget contentlet
	 * @return the rendered text
	 * @throws IOException kept for binary compatibility with previous callers
	 */
	@Deprecated
	public static String parseWidget(HttpServletRequest request, HttpServletResponse response, Contentlet widget) throws IOException {
		return WidgetRenderer.getInstance().render(request, response, widget);
	}

}
