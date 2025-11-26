package com.dotcms.rest.api.v1.browser;

import com.dotcms.browser.BrowserAPI;
import com.dotcms.browser.BrowserQuery;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.browsertree.BrowserTreeHelper;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static com.dotcms.rest.api.v1.browsertree.BrowserTreeHelper.ACTIVE_FOLDER_ID;

/**
 * Expose the Browser functionality such as get the contents in a folder
 * @author jsanca
 */
@Tag(name = "Browser Tree")
@Path("/v1/browser")
public class BrowserResource {

    public  final static String VERSION       = "1.0";
    private final BrowserAPI browserAPI;

    public BrowserResource() {

        this(APILocator.getBrowserAPI());
    }

    @VisibleForTesting
    public BrowserResource(final BrowserAPI browserAPI) {
        this.browserAPI = browserAPI;
    }

    /**
     * Get the select folder into the site browser
     * @param request  {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @return Response
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Path("/selectedfolder")
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getSelectFolder(@Context final HttpServletRequest request,
                                 @Context final HttpServletResponse response) throws DotSecurityException, DotDataException {

        final InitDataObject initDataObject = new WebResource.InitBuilder()
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init();

        final User user = initDataObject.getUser();

        final Optional<String> selectedPathOpt = Optional.ofNullable((String) request.getSession().getAttribute(ACTIVE_FOLDER_ID));
        return selectedPathOpt.isPresent()?
                Response.ok(new ResponseEntityView(APILocator.getFolderAPI().find(selectedPathOpt.get(), user, false))).build():
                Response.status(Response.Status.NOT_FOUND).build();
    }

    /**
     * Set the select folder into the site browser, next time the site browser is opened will expand to selected folder
     * @param request  {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @param openFolderForm {@link OpenFolderForm}
     * @return Response
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Path("/selectedfolder")
    @PUT
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response selectFolder(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                     final OpenFolderForm openFolderForm) throws DotSecurityException, DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder()
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init();

        final User user = initData.getUser();
        final boolean respectFrontendRoles = PageMode.get(request).respectAnonPerms;
        final String folderPath      = openFolderForm.getPath();
        final BrowserTreeHelper browserTreeHelper = BrowserTreeHelper.getInstance();

        Logger.debug(this, ()-> "Selecting the folder on the site browser: " + folderPath);
        browserTreeHelper.selectFolder(request, folderPath, user, respectFrontendRoles);

        return Response.ok(new ResponseEntityView(Boolean.TRUE)).build();
    }

    /**
     * Get the folder contents
     * Can get a host or specific folder, retrieve archive, working, include folders, pages, files and dotAsset
     * Can filter by extensions and mime type
     * @param request  {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @param browserQueryForm {@link BrowserQueryForm}
     * @return Response
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getFolderContent(@Context final HttpServletRequest request,
                                                @Context final HttpServletResponse response,
                                                final BrowserQueryForm browserQueryForm) throws DotSecurityException, DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder()
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true).init();

        final long languageId = browserQueryForm.getLanguageId() > 0?
                browserQueryForm.getLanguageId(): WebAPILocator.getLanguageWebAPI().getLanguage(request).getId();

        Logger.debug(this, "Getting folder contents, browser query form: " + browserQueryForm);

        return Response.ok(new ResponseEntityView(this.browserAPI.getFolderContent(
                BrowserQuery.builder()
                        .showDotAssets(browserQueryForm.isShowDotAssets())
                        .showLinks(browserQueryForm.isShowLinks())
                        .showExtensions(browserQueryForm.getExtensions())
                        .withFilter(browserQueryForm.getFilter())
                        .withHostOrFolderId(browserQueryForm.getHostFolderId())
                        .withLanguageId(languageId)
                        .offset(browserQueryForm.getOffset())
                        .showFiles(browserQueryForm.isShowFiles())
                        .showPages(browserQueryForm.isShowPages())
                        .showFolders(browserQueryForm.isShowFolders())
                        .showArchived(browserQueryForm.isShowArchived())
                        .showWorking(browserQueryForm.isShowWorking())
                        .showMimeTypes(browserQueryForm.getMimeTypes())
                        .maxResults(browserQueryForm.getMaxResults())
                        .sortBy(browserQueryForm.getSortBy())
                        .sortByDesc(browserQueryForm.isSortByDesc())
                        .withUser(initData.getUser())
                        .build()))
        ).build();
    }
}
