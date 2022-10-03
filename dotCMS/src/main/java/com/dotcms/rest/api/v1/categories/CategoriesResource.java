package com.dotcms.rest.api.v1.categories;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.BulkResultView;
import com.dotcms.rest.api.FailedResultView;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
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
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.business.PaginatedCategories;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.beanutils.BeanUtils;
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

       Logger.debug(this, ()-> "Getting the List of categories. " + String.format("Request query parameters are : {filter : %s, page : %s, perPage : %s}", filter, page, perPage));

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
     * Return a list of {@link com.dotmarketing.portlets.categories.model.Category}, entity response
     * syntax:.
     *
     * <code> { contentTypes: array of Category total: total number of Categories } <code/>
     * <p>
     * Url syntax:
     * api/v1/categories/children?filter=filter-string&page=page-number&per_page=per-page&orderby=order-field-name&direction=order-direction&inode=parentId
     * <p>
     * where:
     *
     * <ul>
     * <li>filter-string: just return Category whose content this pattern into its name</li>
     * <li>page: page to return</li>
     * <li>per_page: limit of items to return</li>
     * <li>ordeby: field to order by</li>
     * <li>direction: asc for upward order and desc for downward order</li>
     * </ul>
     * <p>
     * Url example: v1/categories/children?filter=test&page=2&orderby=categoryName
     *
     * @param httpRequest
     * @return
     */
    @GET
    @Path(("/children"))
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getChildren(@Context final HttpServletRequest httpRequest,
            @Context final HttpServletResponse httpResponse,
            @QueryParam(PaginationUtil.FILTER) final String filter,
            @QueryParam(PaginationUtil.PAGE) final int page,
            @QueryParam(PaginationUtil.PER_PAGE) final int perPage,
            @DefaultValue("categoryName") @QueryParam(PaginationUtil.ORDER_BY) final String orderBy,
            @DefaultValue("ASC") @QueryParam(PaginationUtil.DIRECTION) final String direction,
            @QueryParam("inode") final String inode) throws DotDataException, DotSecurityException {

        final InitDataObject initData = webResource.init(null, httpRequest, httpResponse, true,
                null);

        Response response = null;
        final User user = initData.getUser();
        final PageMode pageMode = PageMode.get(httpRequest);

        Logger.debug(this, ()-> "Getting the List of children categories. " + String.format("Request query parameters are : {filter : %s, page : %s, perPage : %s, orderBy : %s, direction : %s, inode : %s}", filter, page, perPage, orderBy, direction, inode));

        DotPreconditions.checkArgument(UtilMethods.isSet(inode),
                "The inode is required");

        PaginatedCategories list = this.categoryAPI.findChildren(user, inode, pageMode.respectAnonPerms, page, perPage,
                filter, direction);

        return getPage(list.getCategories(), list.getTotalCount(), page, perPage);
    }

    /**
     * Saves a new working version of a category.
     *
     * @param httpRequest
     * @param httpResponse
     * @param categoryForm
     * @return CategoryView
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final CategoryView saveNew(@Context final HttpServletRequest httpRequest,
            @Context final HttpServletResponse httpResponse,
            final CategoryForm categoryForm)
            throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user = initData.getUser();
        final Host host = this.categoryHelper.getHost(categoryForm.getSiteId(),
                () -> this.hostWebAPI.getCurrentHostNoThrow(httpRequest));
        final PageMode pageMode = PageMode.get(httpRequest);

        Logger.debug(this, () -> "Saving category. Request payload is : " + getObjectToJsonString(categoryForm));

        DotPreconditions.checkArgument(UtilMethods.isSet(categoryForm.getCategoryName()),
                "The category name is required");

        try {
            return this.categoryHelper.toCategoryView(
                    this.fillAndSave(categoryForm, user, host, pageMode, new Category()), user);
        } catch (InvocationTargetException | IllegalAccessException e) {
            Logger.error(this, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Update a working version of an existing category. The categoryForm must contain the inode of the category.
     *
     * @param httpRequest       {@link HttpServletRequest}
     * @param httpResponse      {@link HttpServletResponse}
     * @param categoryForm  {@link CategoryForm}
     * @return CategoryView
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @PUT
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final CategoryView save(@Context final HttpServletRequest  httpRequest,
            @Context final HttpServletResponse httpResponse,
            final CategoryForm categoryForm) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final Host host = this.categoryHelper.getHost(categoryForm.getSiteId(),
                () -> this.hostWebAPI.getCurrentHostNoThrow(httpRequest));
        final PageMode pageMode = PageMode.get(httpRequest);

        Logger.debug(this, () -> "Saving category. Request payload is : " + getObjectToJsonString(categoryForm));

        DotPreconditions.checkArgument(UtilMethods.isSet(categoryForm.getInode()),
                "The inode is required");

        final Category oldCategory = this.categoryAPI.find(categoryForm.getInode(), user, pageMode.respectAnonPerms);

        if (null == oldCategory) {
            throw new DoesNotExistException("Category with inode: " + categoryForm.getInode() + " does not exist");
        }

        try {
           return this.categoryHelper.toCategoryView(
                    this.fillAndSave(categoryForm, user, host, pageMode, oldCategory, new Category()), user);
        } catch (InvocationTargetException | IllegalAccessException e) {
            Logger.error(this, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes Categories.
     *
     * This method receives a list of inodes and deletes all the children and the parent categories.
     * To delete a category successfully the user needs to have Edit Permissions over it.
     * @param httpRequest            {@link HttpServletRequest}
     * @param httpResponse           {@link HttpServletResponse}
     * @param categoriesToDelete     {@link String} category inode to look for and then delete it
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @DELETE
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response delete(@Context final HttpServletRequest  httpRequest,
            @Context final HttpServletResponse httpResponse,
            final List<String> categoriesToDelete) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user         = initData.getUser();
        final PageMode pageMode = PageMode.get(httpRequest);
        Long deletedCategoriesCount  = 0L;
        final List<FailedResultView> failedToDelete  = new ArrayList<>();

        DotPreconditions.checkArgument(UtilMethods.isSet(categoriesToDelete),
                "The body must send a collection of category inode such as: " +
                        "[\"dd60695c-9e0f-4a2e-9fd8-ce2a4ac5c27d\",\"cc59390c-9a0f-4e7a-9fd8-ca7e4ec0c77d\"]");

        for(final String categoryInode : categoriesToDelete){
            try{
                final Category category = this.categoryAPI.find(categoryInode, user, pageMode.respectAnonPerms);
                if (null != category && InodeUtils.isSet(category.getInode())){
                    List<Category> unableToDeleteChildren = this.categoryAPI.removeAllChildren(category, user, pageMode.respectAnonPerms);
                    this.categoryAPI.delete(category, user, pageMode.respectAnonPerms);
                    ActivityLogger.logInfo(this.getClass(), "Delete Category Action", "User " +
                            user.getPrimaryKey() + " deleted category: " + category.getInode());
                    deletedCategoriesCount++;
                } else {
                    Logger.error(this, "Category with Id: " + categoryInode + " does not exist");
                    failedToDelete.add(new FailedResultView(categoryInode,"Category does not exist"));
                }
            } catch(Exception e){
                Logger.debug(this,e.getMessage(),e);
                failedToDelete.add(new FailedResultView(categoryInode,e.getMessage()));
            }
        }

        return Response.ok(new ResponseEntityView(
                        new BulkResultView(deletedCategoriesCount,0L,failedToDelete)))
                .build();
    }

    private Category fillAndSave(final CategoryForm categoryForm,
            final User user,
            final Host host,
            final PageMode pageMode,
            final Category category)
            throws DotSecurityException, DotDataException, InvocationTargetException, IllegalAccessException {

        Category parentCategory = null;

        Logger.debug(this, ()-> "Filling category entity");

        if (UtilMethods.isSet(categoryForm.getParent())) {
            parentCategory = this.categoryAPI.find(categoryForm.getParent(), user, pageMode.respectAnonPerms);
        }

        BeanUtils.copyProperties(category, categoryForm);

        category.setModDate(new Date());

        Logger.debug(this, ()-> "Saving category entity : " + getObjectToJsonString(category));
        this.categoryAPI.save(parentCategory, category, user, pageMode.respectAnonPerms);
        Logger.debug(this, ()-> "Saved category entity : " + getObjectToJsonString(category));

        ActivityLogger.logInfo(this.getClass(), "Saved Category", "User " + user.getPrimaryKey()
                        + "Category: " + category.getCategoryName(),
                host.getTitle() != null ? host.getTitle() : "default");

        return category;
    }

    private Category fillAndSave(final CategoryForm categoryForm,
            final User user,
            final Host host,
            final PageMode pageMode,
            final Category oldCategory,
            final Category updatedCategory)
            throws DotSecurityException, DotDataException, InvocationTargetException, IllegalAccessException {

        Category parentCategory = null;

        Logger.debug(this, ()-> "Filling category entity");

        if (UtilMethods.isSet(categoryForm.getParent())) {
            parentCategory = this.categoryAPI.find(categoryForm.getParent(), user, pageMode.respectAnonPerms);
        }

        BeanUtils.copyProperties(updatedCategory, oldCategory);

        updatedCategory.setCategoryName(categoryForm.getCategoryName());
        updatedCategory.setKey(categoryForm.getKey());
        updatedCategory.setKeywords(categoryForm.getKeywords());
        updatedCategory.setModDate(new Date());

        Logger.debug(this, ()-> "Saving category entity : " + getObjectToJsonString(updatedCategory));
        this.categoryAPI.save(parentCategory, updatedCategory, user, pageMode.respectAnonPerms);
        Logger.debug(this, ()-> "Saved category entity : " + getObjectToJsonString(updatedCategory));

        ActivityLogger.logInfo(this.getClass(), "Saved Category", "User " + user.getPrimaryKey()
                        + "Category: " + updatedCategory.getCategoryName(),
                host.getTitle() != null ? host.getTitle() : "default");

        return updatedCategory;
    }

    private Response getPage(final List<Category> list, final int totalCount, final int page,
            final int perPage) {

        return Response.
                ok(new ResponseEntityView((Object) list))
                .header("X-Pagination-Per-Page", perPage)
                .header("X-Pagination-Current-Page", page)
                .header("X-Pagination-Total-Entries", totalCount)
                .build();
    }

    private String getObjectToJsonString(final Object object){
        ObjectMapper mapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
        try {
            final String json = mapper.writeValueAsString(object);
            return json;
        }
        catch (JsonProcessingException e){
            Logger.error(this, e.getMessage(), e);
        }
        return StringPool.BLANK;
    }
}
