package com.dotcms.rest.api.v2.contenttype;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.JsonContentTypeFieldLayoutTransformer;
import com.dotcms.contenttype.transform.contenttype.JsonContentTypeTransformer;
import com.dotcms.repackage.javax.ws.rs.GET;
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
import com.dotcms.rest.annotation.PermissionsUtil;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Path("/v2/contenttype")
public class ContentTypeResource {

    private final WebResource webResource;
    private final WorkflowHelper workflowHelper;

    public ContentTypeResource() {
        this.webResource = new WebResource();
        this.workflowHelper = WorkflowHelper.getInstance();
    }

    @GET
    @Path("/id/{idOrVar}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getType(@PathParam("idOrVar") final String idOrVar, @Context final HttpServletRequest req)
            throws DotDataException {

        final InitDataObject initData = this.webResource.init(null, false, req, false, null);
        final User user = initData.getUser();

        final ContentTypeAPI tapi = APILocator.getContentTypeAPI(user, true);

        try {

            Logger.debug(this, ()-> "Getting the Type: " + idOrVar);

            final ContentType type = tapi.find(idOrVar);

            final HttpSession session = req.getSession(false);

            if(null != session && null != type){
                session.setAttribute(
                        com.dotcms.rest.api.v1.contenttype.ContentTypeResource.SELECTED_STRUCTURE_KEY, type.inode());
            }

            final Map<String, Object> resultMap = new HashMap<>();
            resultMap.putAll(new JsonContentTypeFieldLayoutTransformer(type).mapObject());
            resultMap.put("workflows", this.workflowHelper.findSchemesByContentType(type.id(), initData.getUser()));

            return ("true".equalsIgnoreCase(req.getParameter("include_permissions")))?
                    Response.ok(new ResponseEntityView(resultMap, PermissionsUtil.getInstance().getPermissionsArray(type, initData.getUser()))).build():
                    Response.ok(new ResponseEntityView(resultMap)).build();
        } catch (DotSecurityException e) {
            throw new ForbiddenException(e);
        } catch (NotFoundInDbException nfdb2) {
            return Response.status(404).build();
        }
    }
}
