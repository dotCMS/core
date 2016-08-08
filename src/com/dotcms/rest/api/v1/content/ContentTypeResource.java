package com.dotcms.rest.api.v1.content;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.portlet.PortletURL;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.InitRequestRequired;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.business.StructureAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletURLUtil;
import com.liferay.portal.model.User;
import com.liferay.portlet.PortletURLImpl;
import com.liferay.util.LocaleUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;

/**
 * Gets the ContentLet types.
 * @author jsanca
 */
@SuppressWarnings("serial")
@Path("/v1/content")
public class ContentTypeResource implements Serializable {

    private final WebResource webResource;
    private final StructureAPI structureAPI;
    private final ContentTypeHelper contentTypeHelper;
    private final LayoutAPI layoutAPI;
    private final LanguageAPI languageAPI;

    public ContentTypeResource() {
        this(new WebResource(), ContentTypeHelper.INSTANCE,
                APILocator.getStructureAPI(), APILocator.getLayoutAPI(),
                APILocator.getLanguageAPI());
    }

    @VisibleForTesting
    public ContentTypeResource(final WebResource webResource,
                               final ContentTypeHelper contentletHelper,
                               final StructureAPI structureAPI,
                               final LayoutAPI layoutAPI,
                               final LanguageAPI languageAPI) {

        this.webResource       = webResource;
        this.contentTypeHelper = contentletHelper;
        this.structureAPI      = structureAPI;
        this.layoutAPI         = layoutAPI;
        this.languageAPI       = languageAPI;
    }

    @GET
    @Path("/types")
    @JSONP
    @InitRequestRequired
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
        final Map<String, List<ContentTypeView>> contentletTypeMap = map();

        try {

            structures =
                    this.structureAPI.find(user, false, true);

            if (null != structures) {


                contentletIdNameMapping = this.contentTypeHelper.getStrTypeNames(locale);

                // Init the type map
                contentletIdNameMapping.values().forEach(
                        name -> contentletTypeMap.put(name, list())
                );

                structures.stream().filter(structure -> contentletIdNameMapping.containsKey(structure.getStructureType()))
                        .forEach(structure -> {

                            String typeName = contentletIdNameMapping.get(structure.getStructureType());
                            contentletTypeMap.get(typeName).add
                                    (new ContentTypeView(typeName,
                                            structure.getName(), structure.getInode(),
                                            this.contentTypeHelper.getActionUrl(request, this.layoutAPI, this.languageAPI,
                                                    structure, user)));
                        });
            }


            response = Response.ok(new ResponseEntityView(contentletTypeMap)).build();
        } catch (Exception e) { // this is an unknown error, so we report as a 500.

            response = ExceptionMapperUtil.createResponse(e,
                    Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // getTypes.



} // E:O:F:ContentTypeResource.
