package com.dotcms.rest.api.v1.categories;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.CategoriesPaginator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;

/**
 * This resource provides all the different end-points associated to information and actions that
 * the front-end can perform on the Categories.
 */
@Path("/v1/categories")
public class CategoriesResource {

    private final WebResource webResource;
    private final PaginationUtil paginationUtil;

    private final CategoryAPI categoryAPI;
    private final VersionableAPI versionableAPI;

    private final HostWebAPI hostWebAPI;
    private final PermissionAPI permissionAPI;
    private final CategoryHelper categoryHelper;


    public CategoriesResource() {
        this(new WebResource(), new PaginationUtil(new CategoriesPaginator()),
                APILocator.getCategoryAPI(),
                APILocator.getVersionableAPI(),
                WebAPILocator.getHostWebAPI(),
                APILocator.getPermissionAPI(),
                new CategoryHelper(APILocator.getCategoryAPI()));
    }

    @VisibleForTesting
    public CategoriesResource(final WebResource webresource, final PaginationUtil paginationUtil,
            final CategoryAPI categoryAPI, final VersionableAPI versionableAPI,
            final HostWebAPI hostWebAPI, final PermissionAPI permissionAPI,
            final CategoryHelper categoryHelper) {
        this.webResource = webresource;
        this.paginationUtil = paginationUtil;
        this.categoryAPI = categoryAPI;
        this.versionableAPI = versionableAPI;
        this.hostWebAPI = hostWebAPI;
        this.permissionAPI = permissionAPI;
        this.categoryHelper = new CategoryHelper(categoryAPI);
    }

    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getCategories(@Context final HttpServletRequest httpRequest,
            @Context final HttpServletResponse httpResponse,
            @QueryParam(PaginationUtil.FILTER) final String filter,
            @QueryParam(PaginationUtil.PAGE) final int page,
            @QueryParam(PaginationUtil.PER_PAGE) final int perPage) {

        final InitDataObject initData = webResource.init(null, httpRequest, httpResponse, true,
                null);

        Response response = null;
        final User user = initData.getUser();

        try {
            response = this.paginationUtil.getPage(httpRequest, user, filter, page, perPage);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
                throw new ForbiddenException(e);
            }
            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    /**
     * Saves a new working version of a category.
     *
     * @param request
     * @param response
     * @param categoryForm
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response saveNew(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final CategoryForm categoryForm) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response).rejectWhenNoUser(true).init();
        final User user = initData.getUser();
        final Host host = this.categoryHelper.getHost(categoryForm.getSiteId(),
                () -> this.hostWebAPI.getCurrentHostNoThrow(request));
        final PageMode pageMode = PageMode.get(request);

        DotPreconditions.checkArgument(UtilMethods.isSet(categoryForm.getCategoryName()),
                "The category name is required");

        return Response.ok(new ResponseEntityView(this.categoryHelper.toCategoryView(
                        this.fillAndSave(categoryForm, user, host, pageMode, new Category()), user)))
                .build();
    }

    @WrapInTransaction
    private Category fillAndSave(final CategoryForm categoryForm,
            final User user,
            final Host host,
            final PageMode pageMode,
            final Category category) throws DotSecurityException, DotDataException {

        Category parentCategory = null;

        if (UtilMethods.isSet(categoryForm.getParent())) {
            parentCategory = categoryAPI.find(categoryForm.getParent(), user, true);
        }

        category.setInode(categoryForm.getInode());
        category.setDescription(categoryForm.getDescription());
        category.setKeywords(categoryForm.getKeywords());
        category.setKey(categoryForm.getKey());
        category.setCategoryName(categoryForm.getCategoryName());
        category.setActive(categoryForm.isActive());
        category.setSortOrder(categoryForm.getSortOrder());
        category.setCategoryVelocityVarName(categoryForm.getCategoryVelocityVarName());
        category.setModDate(new Date());

        this.categoryAPI.save(parentCategory, category, user, pageMode.respectAnonPerms);

        ActivityLogger.logInfo(this.getClass(), "Saved Category", "User " + user.getPrimaryKey()
                        + "Category: " + category.getCategoryName(),
                host.getTitle() != null ? host.getTitle() : "default");

        return category;
    }
}
