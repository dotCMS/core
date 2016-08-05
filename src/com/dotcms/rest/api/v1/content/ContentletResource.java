package com.dotcms.rest.api.v1.content;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
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
import com.dotmarketing.portlets.structure.business.StructureAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;
import com.liferay.util.LocaleUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;

/**
 * Resource for Contentlet stuff
 * - Gets the ContentLet types.
 * @author jsanca
 */
@SuppressWarnings("serial")
@Path("/v1/contentlet")
public class ContentletResource implements Serializable {

    private final WebResource webResource;
    private final StructureAPI structureAPI;
    private final ContentletHelper contentletHelper;

    public ContentletResource() {
        this(new WebResource(), ContentletHelper.INSTANCE, APILocator.getStructureAPI());
    }

    @VisibleForTesting
    public ContentletResource(final WebResource webResource,
                              final ContentletHelper contentletHelper,
                              final StructureAPI structureAPI) {

        this.webResource = webResource;
        this.contentletHelper  = contentletHelper;
        this.structureAPI = structureAPI;
    }

    @GET
    @Path("/types")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getTypes(@Context final HttpServletRequest request) {

        Response response = null;
        final InitDataObject initData = this.webResource.init(null, true, request, true, null); // should logged in
        final User user = initData.getUser();
        Locale locale = LocaleUtil.getLocale(request);
        locale = (null != user && null == locale)? user.getLocale():locale;
        final List<Structure> structures;
        final Map<Integer, String> contentletIdNameMapping;
        final Map<String, List<ContentletTypeView>> contentletTypeMap = map();

        try {

            structures =
                    this.structureAPI.find(user, false, true);

            if (null != structures) {


                contentletIdNameMapping = this.contentletHelper.getStrTypeNames(locale);

                // Init the type map
                contentletIdNameMapping.values().forEach(
                        name -> contentletTypeMap.put(name, list())
                );

                structures.stream().filter(structure -> contentletIdNameMapping.containsKey(structure.getStructureType()))
                        .forEach(structure -> {

                            String typeName = contentletIdNameMapping.get(structure.getStructureType());
                            contentletTypeMap.get(typeName).add
                                    (new ContentletTypeView(typeName,
                                            structure.getName(), structure.getInode()));
                        });
            }


            response = Response.ok(new ResponseEntityView(contentletTypeMap)).build();
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            response = ExceptionMapperUtil.createResponse(e,
                    Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // getTypes.

} // E:O:F:ContentletResource.
