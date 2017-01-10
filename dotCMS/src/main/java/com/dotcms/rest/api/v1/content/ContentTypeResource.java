package com.dotcms.rest.api.v1.content;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.directwebremoting.annotations.Param;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.portlets.structure.model.Structure;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.List;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;

/**
 * Gets the ContentLet types.
 * @author jsanca
 */
@SuppressWarnings("serial")
@Path("/v1/content")
public class ContentTypeResource implements Serializable {

    private final ContentTypeHelper contentTypeHelper;

    public ContentTypeResource() {
        this(ContentTypeHelper.getInstance());
    }

    @VisibleForTesting
    public ContentTypeResource(final ContentTypeHelper contentletHelper) {

        this.contentTypeHelper = contentletHelper;
    }

    /**
     * Get the ContentType with the follow json format:
     *
     * <pre>
     *    [
     *      {
     *          "structureName":"HTMLPAGE",
     *          "label": "..."
     *          "types":[
     *              {
     *                  "type":"HTMLPAGE",
     *                  "name":"...",
     *                  "inode":"...",
     *                  "action":"..."
     *              },
     *              {
     *                  "type":"HTMLPAGE",
     *                  "name":"...",
     *                  "inode":"...",
     *                  "action":"..."
     *              }
     *            ]
     *       },
     *      {
     *          "structureName":"CONTENT",
     *          "label": "..."
     *          "types":[
     *              {
     *                  "type":"CONTENT",
     *                  "name":"...",
     *                  "inode":"...",
     *                  "action":"..."
     *              }
     *            ]
     *       },
     *      {
     *          "structureName":"RECENT_CONTENT",
     *          "label": "..."
     *          "types":[
     *              {
     *                  "type":"CONTENT",
     *                  "name":"...",
     *                  "inode":"...",
     *                  "action":"..."
     *              }
     *            ]
     *       },
     *      {
     *          "structureName":"RECENT_WIDGET",
     *          "label": "..."
     *          "types":[
     *              {
     *                  "type":"WIDGET",
     *                  "name":"...",
     *                  "inode":"...",
     *                  "action":"..."
     *              }
     *            ]
     *       }
     *   ]
     * </pre>
     *
     * @param request
     * @return
     */
    @GET
    @Path("/types")
    @JSONP
    @InitRequestRequired
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getTypes(@Context final HttpServletRequest request) {

        Response response = null;

        try {

            List<BaseContentTypesView> types = contentTypeHelper.getTypes(request);
            response = Response.ok(new ResponseEntityView(types)).build();
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            response = ExceptionMapperUtil.createResponse(e,
                    Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // getTypes.

} // E:O:F:ContentTypeResource.
