package com.dotcms.rest.api.v1.categories;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityBulkResultView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.api.BulkResultView;
import com.dotcms.rest.api.FailedResultView;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.CloseUtils;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.CategoriesPaginator;
import com.dotcms.util.pagination.CategoryListDTOPaginator;
import com.dotcms.util.pagination.OrderDirection;
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
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Try;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.beanutils.BeanUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import javax.ws.rs.BeanParam;
import org.glassfish.jersey.server.JSONP;

/**
 * This resource provides all the different end-points associated to information and actions that
 * the front-end can perform on the Categories.
 */
@SwaggerCompliant(value = "Content management and workflow APIs", batch = 2)
@Path("/v1/categories")
@Tag(name = "Categories")
public class CategoriesResource {

    private final WebResource webResource;
    private final PaginationUtil paginationUtil;
    private final PaginationUtil extendedPaginationUtil;
    private final CategoryAPI categoryAPI;
    private final VersionableAPI versionableAPI;

    private final HostWebAPI hostWebAPI;
    private final PermissionAPI permissionAPI;
    private final CategoryHelper categoryHelper;

    public CategoriesResource() {
        this(new WebResource(), new PaginationUtil(new CategoriesPaginator()),
                new PaginationUtil(new CategoryListDTOPaginator()),
                APILocator.getCategoryAPI(),
                APILocator.getVersionableAPI(),
                WebAPILocator.getHostWebAPI(),
                APILocator.getPermissionAPI());
    }

    @VisibleForTesting
    public CategoriesResource(final WebResource webresource, final PaginationUtil paginationUtil,
            final PaginationUtil extendedPaginationUtil,
            final CategoryAPI categoryAPI, final VersionableAPI versionableAPI,
            final HostWebAPI hostWebAPI, final PermissionAPI permissionAPI) {
        this.webResource = webresource;
        this.paginationUtil = paginationUtil;
        this.extendedPaginationUtil = extendedPaginationUtil;
        this.categoryAPI = categoryAPI;
        this.versionableAPI = versionableAPI;
        this.hostWebAPI = hostWebAPI;
        this.permissionAPI = permissionAPI;
        this.categoryHelper = new CategoryHelper(categoryAPI);
    }

    /**
     * Returns a response of ResponseEntityView with PaginatedArrayList of categories
     * syntax:.
     * <p>
     * Url syntax:
     * api/v1/categories?filter=filter-string&page=page-number&per_page=per-page&orderby=order-field-name&direction=order-direction&showChildrenCount=true
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
     * Url example: /api/v1/categories?filter=&page=0&per_page=5&ordeby=category_name&direction=ASC&showChildrenCount=true
     *
     * @param httpRequest
     * @param httpResponse
     * @param filter
     * @param page
     * @param perPage
     * @param orderBy
     * @param direction
     * @param showChildrenCount
     * @return Response
     */
    @Operation(
        summary = "Get categories with pagination",
        description = "Retrieves a paginated list of categories with optional filtering, sorting, and children count information. Supports hierarchical category navigation and management."
    )
    @ApiResponse(responseCode = "200", description = "Categories retrieved successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityCategoryView.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - user authentication required")
    @ApiResponse(responseCode = "500", description = "Internal server error retrieving categories")
    @GET
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getCategories(@Context final HttpServletRequest httpRequest,
            @Context final HttpServletResponse httpResponse,
            @Parameter(description = "Filter text to search categories") @QueryParam(PaginationUtil.FILTER) final String filter,
            @Parameter(description = "Page number for pagination") @QueryParam(PaginationUtil.PAGE) final int page,
            @Parameter(description = "Number of items per page") @QueryParam(PaginationUtil.PER_PAGE) final int perPage,
            @Parameter(description = "Field to order results by") @DefaultValue("category_name") @QueryParam(PaginationUtil.ORDER_BY) final String orderBy,
            @Parameter(description = "Sort direction (ASC or DESC)") @DefaultValue("ASC") @QueryParam(PaginationUtil.DIRECTION) final String direction,
            @Parameter(description = "Whether to include children count for each category") @QueryParam("showChildrenCount") final boolean showChildrenCount) {

        final InitDataObject initData = webResource.init(null, httpRequest, httpResponse, true,
                null);

        Response response = null;
        final User user = initData.getUser();

        Logger.debug(this, () -> "Getting the List of categories. " + String.format(
                "Request query parameters are : {filter : %s, page : %s, perPage : %s}", filter,
                page, perPage));

        final Map<String, Object> extraParams = new HashMap<>();
        extraParams.put("childrenCategories", false);

        try {
           response = showChildrenCount == false ? this.paginationUtil.getPage(httpRequest, user, filter, page, perPage, orderBy,
                   direction.equals("ASC") == true ? OrderDirection.ASC : OrderDirection.DESC, extraParams)
                   : this.extendedPaginationUtil.getPage(httpRequest, user, filter, page, perPage, orderBy, direction);
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
     * Syntax:.
     *
     * <code>
     * {
     *   contentTypes: array of Category,
     *   total: total number of Categories
     * }
     * <code/>
     *
     * <p>
     * Url syntax:
     * api/v1/categories/children?filter=filter-string&page=page-number&per_page=per-page&orderby=order-field-name&direction=order-direction&inode=parentId
     * <p>
     *
     * Parameeters:
     *
     * - filter-string: Return categories whose names contain this pattern.
     * - page: The page number to return.
     * - per_page: The limit of items to return.
     * - orderby: The field to order by.
     * - direction: Sorting direction, asc for ascending and desc for descending.
     * - showChildrenCount: true to include the count of child categories, false to exclude it.
     * - allLevels: A Boolean value. If TRUE, the search will include categories at any level.
     * - inode: This represents a {@link Category}'s inode. The interpretation of this parameter depends on the value of
     *  the allLevels parameter. If allLevels is true and inode is set, the search will start from this {@link Category}
     *  and continue recursively through all its offspring. If allLevels is false, the search will be limited to
     *  the immediate children of this {@link Category}.
     * - parentList:"If this is true, an additional parameter called parentList is returned.
     * This list contains {@link Category} objects, starting with the direct parent and going up to the top-level {@link Category}.
     * <p>
     * Url example: v1/categories/children?filter=test&page=0&per_page=5&orderby=category_name
     *
     * @param httpRequest
     * @param httpResponse
     * @param filter
     * @param page
     * @param perPage
     * @param orderBy
     * @param direction
     * @param inode
     * @return Response
     */
    @Operation(
        summary = "Get category children",
        description = "Retrieves child categories of a specified parent category with pagination and filtering options. Can include all nested levels and parent hierarchy information."
    )
    @ApiResponse(responseCode = "200", description = "Child categories retrieved successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityCategoryView.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - user authentication required")
    @ApiResponse(responseCode = "404", description = "Parent category not found")
    @ApiResponse(responseCode = "500", description = "Internal server error retrieving child categories")
    @GET
    @Path(("/children"))
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getChildren(@Context final HttpServletRequest httpRequest,
            @Context final HttpServletResponse httpResponse,
            @Parameter(description = "Filter text to search child categories") @QueryParam(PaginationUtil.FILTER) final String filter,
            @Parameter(description = "Page number for pagination") @QueryParam(PaginationUtil.PAGE) final int page,
            @Parameter(description = "Number of items per page") @QueryParam(PaginationUtil.PER_PAGE) final int perPage,
            @Parameter(description = "Field to order results by") @DefaultValue("category_name") @QueryParam(PaginationUtil.ORDER_BY) final String orderBy,
            @Parameter(description = "Sort direction (ASC or DESC)") @DefaultValue("ASC") @QueryParam(PaginationUtil.DIRECTION) final String direction,
            @Parameter(description = "Parent category inode to get children for") @QueryParam("inode") final String inode,
            @Parameter(description = "Whether to include children count for each category") @QueryParam("showChildrenCount") final boolean showChildrenCount,
            @Parameter(description = "Whether to include all nested levels") @QueryParam("allLevels") final boolean allLevels,
            @Parameter(description = "Whether to include parent list hierarchy") @QueryParam("parentList") final boolean parentList) {

        final InitDataObject initData = webResource.init(null, httpRequest, httpResponse, true,
                null);

        final User user = initData.getUser();

        Logger.debug(this, () -> "Getting the List of children categories. " + String.format(
                "Request query parameters are : {filter : %s, page : %s, perPage : %s, orderBy : %s, direction : %s, inode : %s}",
                filter, page, perPage, orderBy, direction, inode));

        DotPreconditions.checkArgument(UtilMethods.isSet(inode),
                "The inode is required");

        final Map<String, Object> extraParams = new HashMap<>();
        extraParams.put("inode", inode);
        extraParams.put("searchInAllLevels", allLevels);
        extraParams.put("parentList", parentList);
        extraParams.put("showChildrenCount", showChildrenCount);

        try {
            return  this.paginationUtil.getPage(httpRequest, user, filter, page, perPage, orderBy,
                    direction.equals("ASC") ? OrderDirection.ASC : OrderDirection.DESC, extraParams);
        } catch (Exception e) {
            Logger.error(this, e.getMessage(), e);
            if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
                throw new ForbiddenException(e);
            }
            return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Response with the list of parents for a specific set of {@link Category}.
     *
     * This ned point receive a list the {@link Category}'s endpoint as follow:
     *
     * <code>
     *     {
     *         "keys": ["key_1", "key_2"]
     *     }
     * </code>
     *
     * The output is going to be something like:
     *
     * <code>
     *     {
     *         entity: [
     *              {
     *                  "inode": "1",
     *                  "key": "key_1",
     *                  "name": "Name_1",
     *                  "parentList": [
     *                       {
     *                          'name': 'Grand Parent Name',
     *                          'key': 'Grand Parent  Key',
     *                          'inode': 'Grand Parent  inode'
     *                      },
     *                       {
     *                          'name': 'Parent Name',
     *                          'key': 'Parent  Key',
     *                          'inode': 'Parent  inode'
     *                      }
     *                  ]
     *              },
     *              {
     *                  "inode": "2",
     *                  "key": "key_2",
     *                  "name": "Name_2",
     *                  "parentList": [
     *                       {
     *                          'name': 'Category name value',
     *                          'key': 'Key value',
     *                          'inode': 'inode value'
     *                      }
     *                  ]
     *              }
     *         ]
     *     }
     * </code>
     *
     *  parentList is the list of parents where the 0 is the more top level parent and the last one is the direct
     *  parent.
     *
     * @param httpRequest
     * @param httpResponse
     * @param form
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Operation(
        summary = "Get category hierarchy for multiple categories",
        description = "Retrieves the parent hierarchy for a set of categories specified by their keys. Returns parent lists for each category, starting from the top-level parent down to the direct parent. Categories that don't exist are ignored."
    )
    @ApiResponse(responseCode = "200", description = "Category hierarchies retrieved successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = HierarchyShortCategoriesResponseView.class)))
    @ApiResponse(responseCode = "400", description = "Bad request - invalid category keys form")
    @ApiResponse(responseCode = "401", description = "Unauthorized - user authentication required")
    @ApiResponse(responseCode = "500", description = "Internal server error retrieving hierarchies")
    @POST
    @Path(("/hierarchy"))
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public final HierarchyShortCategoriesResponseView getHierarchy(@Context final HttpServletRequest httpRequest,
                                       @Context final HttpServletResponse httpResponse,
                                       @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Category keys form containing array of category keys", required = true) final CategoryKeysForm form) throws DotDataException {

        Logger.debug(this, () -> "Getting the List of Parents for the follow categories: " +
                String.join(",", form.getKeys()));

        webResource.init(null, httpRequest, httpResponse, true, null);

        return new HierarchyShortCategoriesResponseView(categoryAPI.findHierarchy(form.getKeys()));
    }

    /**
     * Lookup operation. Categories can be retrieved by category id or key
     * @param httpRequest
     * @param httpResponse
     * @param idOrKey
     * @return CategoryView
     */

    @Operation(
        summary = "Get category by ID or key",
        description = "Retrieves a specific category by its unique identifier (inode) or key. Can optionally include child count information."
    )
    @ApiResponse(responseCode = "200", description = "Category retrieved successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityCategoryView.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized - user authentication required")
    @ApiResponse(responseCode = "404", description = "Category not found")
    @ApiResponse(responseCode = "500", description = "Internal server error retrieving category")
    @GET
    @JSONP
    @Path("/{idOrKey}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getCategoryByIdOrKey(@Context final HttpServletRequest httpRequest,
            @Context final HttpServletResponse httpResponse,
            @Parameter(description = "Category ID (inode) or key", required = true) @PathParam("idOrKey") final String idOrKey,
            @Parameter(description = "Whether to include children count") @QueryParam("showChildrenCount") final boolean showChildrenCount)
            throws DotSecurityException, DotDataException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user = initData.getUser();
        Logger.debug(this, () -> "Getting the category by id or key : " + idOrKey);

        final Host host = WebAPILocator.getHostWebAPI().getHost(httpRequest);
        final PageMode pageMode = PageMode.get(httpRequest);

        DotPreconditions.checkArgument(UtilMethods.isSet(idOrKey),
                "The idOrKey is required");

        Category category = Try.of(() -> this.categoryAPI.find(idOrKey, user, pageMode.respectAnonPerms))
                .getOrNull();
        if (category == null) {
            category = Try.of(
                            () -> this.categoryAPI.findByKey(idOrKey, user, pageMode.respectAnonPerms))
                    .getOrNull();
        }

        if (category == null) {
            Logger.error(this, "Category with idOrKey: " + idOrKey + " does not exist");
            throw new DoesNotExistException(
                    "Category with idOrKey: " + idOrKey + " does not exist");
        }

        return showChildrenCount ? Response.ok(new ResponseEntityCategoryWithChildCountView(this.categoryHelper.toCategoryWithChildCountView(category, user))).build() :
                Response.ok(new ResponseEntityCategoryView(this.categoryHelper.toCategoryView(category, user))).build();
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
    @Operation(
        summary = "Create new category",
        description = "Creates a new category with the specified properties. The category name is required, and optionally can be associated with a specific site."
    )
    @ApiResponse(responseCode = "200", description = "Category created successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityCategoryView.class)))
    @ApiResponse(responseCode = "400", description = "Bad request - missing category name or invalid form data")
    @ApiResponse(responseCode = "401", description = "Unauthorized - user authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions to create categories")
    @ApiResponse(responseCode = "500", description = "Internal server error creating category")
    @POST
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public final Response saveNew(@Context final HttpServletRequest httpRequest,
            @Context final HttpServletResponse httpResponse,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Category form with name and properties", required = true) final CategoryForm categoryForm)
            throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user = initData.getUser();
        final Host host = this.categoryHelper.getHost(categoryForm.getSiteId(),
                () -> this.hostWebAPI.getCurrentHostNoThrow(httpRequest));
        final PageMode pageMode = PageMode.get(httpRequest);

        Logger.debug(this, () -> "Saving category. Request payload is : " + getObjectToJsonString(
                categoryForm));

        DotPreconditions.checkArgument(UtilMethods.isSet(categoryForm.getCategoryName()),
                "The category name is required");

        try {
           return Response.ok(new ResponseEntityCategoryView(this.categoryHelper.toCategoryView(
                   this.fillAndSave(categoryForm, user, host, pageMode, new Category()), user))).build();
        } catch (InvocationTargetException | IllegalAccessException e) {
            Logger.error(this, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Update a working version of an existing category. The categoryForm must contain the inode of
     * the category.
     *
     * @param httpRequest  {@link HttpServletRequest}
     * @param httpResponse {@link HttpServletResponse}
     * @param categoryForm {@link CategoryForm}
     * @return CategoryView
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Operation(
        summary = "Update existing category",
        description = "Updates an existing category identified by its inode. All category properties can be modified including name, description, and hierarchy placement."
    )
    @ApiResponse(responseCode = "200", description = "Category updated successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityCategoryView.class)))
    @ApiResponse(responseCode = "400", description = "Bad request - missing inode or invalid form data")
    @ApiResponse(responseCode = "401", description = "Unauthorized - user authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions to update categories")
    @ApiResponse(responseCode = "404", description = "Category not found")
    @ApiResponse(responseCode = "500", description = "Internal server error updating category")
    @PUT
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public final Response save(@Context final HttpServletRequest httpRequest,
            @Context final HttpServletResponse httpResponse,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Category form with updated properties including inode", required = true) final CategoryForm categoryForm) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user = initData.getUser();
        final Host host = this.categoryHelper.getHost(categoryForm.getSiteId(),
                () -> this.hostWebAPI.getCurrentHostNoThrow(httpRequest));
        final PageMode pageMode = PageMode.get(httpRequest);

        Logger.debug(this, () -> "Saving category. Request payload is : " + getObjectToJsonString(
                categoryForm));

        DotPreconditions.checkArgument(UtilMethods.isSet(categoryForm.getInode()),
                "The inode is required");

        final Category oldCategory = this.categoryAPI.find(categoryForm.getInode(), user,
                pageMode.respectAnonPerms);

        if (null == oldCategory) {
            throw new DoesNotExistException(
                    "Category with inode: " + categoryForm.getInode() + " does not exist");
        }

        try {
           return Response.ok(new ResponseEntityCategoryView(this.categoryHelper.toCategoryView(
                    this.fillAndSave(categoryForm, user, host, pageMode, oldCategory,
                            new Category()), user))).build();
        } catch (InvocationTargetException | IllegalAccessException e) {
            Logger.error(this, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Update a working version of an existing category for sortOrder. The categoryEditDTO must
     * contain the inode and sortOrder of the category.
     *
     * @param httpRequest      {@link HttpServletRequest}
     * @param httpResponse     {@link HttpServletResponse}
     * @param categoryEditForm {@link CategoryForm}
     * @return CategoryView
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Operation(
        summary = "Update category sort order",
        description = "Updates the sort order of categories. The request must contain category inode and sortOrder pairs. Can update multiple categories at once within a parent category."
    )
    @ApiResponse(responseCode = "200", description = "Category sort order updated successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityCategoryView.class)))
    @ApiResponse(responseCode = "400", description = "Bad request - missing category data or invalid sort order")
    @ApiResponse(responseCode = "401", description = "Unauthorized - user authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions to update categories")
    @ApiResponse(responseCode = "404", description = "Parent category not found")
    @ApiResponse(responseCode = "500", description = "Internal server error updating sort order")
    @PUT
    @Path("/_sort")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public final Response save(@Context final HttpServletRequest httpRequest,
            @Context final HttpServletResponse httpResponse,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Category edit form with category data and sort order information", required = true) final CategoryEditForm categoryEditForm
    ) throws DotDataException, DotSecurityException {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user = initData.getUser();
        final Host host = this.categoryHelper.getHost(categoryEditForm.getSiteId(),
                () -> this.hostWebAPI.getCurrentHostNoThrow(httpRequest));
        final PageMode pageMode = PageMode.get(httpRequest);

        Logger.debug(this,
                () -> "Saving category sortOrder. Request payload is : " + getObjectToJsonString(
                        categoryEditForm));

        DotPreconditions.checkArgument(UtilMethods.isSet(categoryEditForm.getCategoryData()),
                "The body must send a collection of category inode and sortOrder");

        Category parentCategory = null;

        if (UtilMethods.isSet(categoryEditForm.getParentInode())) {
            parentCategory = this.categoryAPI.find(categoryEditForm.getParentInode(), user,
                    pageMode.respectAnonPerms);
        }

        updateSortOrder(categoryEditForm, user, host, pageMode, parentCategory);

        return parentCategory == null
                ? this.paginationUtil.getPage(httpRequest, user, categoryEditForm.getFilter(),
                categoryEditForm.getPage(), categoryEditForm.getPerPage())
                : this.getChildren(httpRequest, httpResponse, categoryEditForm.getFilter(),
                        categoryEditForm.getPage(),
                        categoryEditForm.getPerPage(), "", categoryEditForm.getDirection(),
                        categoryEditForm.getParentInode(), true, false, false);
    }

    /**
     * Deletes Categories.
     * <p>
     * This method receives a list of inodes and deletes all the children and the parent categories.
     * To delete a category successfully the user needs to have Edit Permissions over it.
     *
     * @param httpRequest        {@link HttpServletRequest}
     * @param httpResponse       {@link HttpServletResponse}
     * @param categoriesToDelete {@link String} category inode to look for and then delete it
     * @return Response
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Operation(
        summary = "Delete categories",
        description = "Deletes multiple categories by their inodes. Deletes both parent categories and their children. User needs Edit permissions on categories to delete them successfully."
    )
    @ApiResponse(responseCode = "200", description = "Categories deleted successfully (may include partial failures)",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityCategoryView.class)))
    @ApiResponse(responseCode = "400", description = "Bad request - missing category inodes")
    @ApiResponse(responseCode = "401", description = "Unauthorized - user authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions to delete categories")
    @ApiResponse(responseCode = "500", description = "Internal server error deleting categories")
    @DELETE
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public final Response delete(@Context final HttpServletRequest httpRequest,
            @Context final HttpServletResponse httpResponse,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "List of category inodes to delete", required = true) final List<String> categoriesToDelete) {

        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(httpRequest, httpResponse).rejectWhenNoUser(true).init();
        final User user = initData.getUser();
        final PageMode pageMode = PageMode.get(httpRequest);
        final List<FailedResultView> failedToDelete = new ArrayList<>();
        List<String> deletedIds = new ArrayList();

        DotPreconditions.checkArgument(UtilMethods.isSet(categoriesToDelete),
                "The body must send a collection of category inode such as: " +
                        "[\"dd60695c-9e0f-4a2e-9fd8-ce2a4ac5c27d\",\"cc59390c-9a0f-4e7a-9fd8-ca7e4ec0c77d\"]");

        try {
            HashMap<String, Category> undeletedCategoryList = this.categoryAPI.deleteCategoryAndChildren(
                    categoriesToDelete, user, pageMode.respectAnonPerms);
            List<String> undeletedIds = undeletedCategoryList.entrySet().stream()
                    .map(k -> k.getKey()).collect(
                            Collectors.toUnmodifiableList());
            deletedIds = new ArrayList<>(categoriesToDelete);
            deletedIds.removeAll(undeletedIds);

            ActivityLogger.logInfo(this.getClass(), "Delete Category Action", "User " +
                    user.getPrimaryKey() + " deleted category list: [" + String.join(",",
                    deletedIds) + "]");

            if (!undeletedCategoryList.isEmpty()) {
                for (final String categoryInode : undeletedIds) {
                    Logger.error(this, "Category with Id: " + categoryInode + " does not exist");
                    failedToDelete.add(new FailedResultView(categoryInode,
                            "Category does not exist or failed to remove child category"));
                }
            }
        } catch (Exception e) {
            Logger.debug(this, e.getMessage(), e);
        }

        return Response.ok(new ResponseEntityBulkResultView(
                        new BulkResultView(Long.valueOf(deletedIds.size()), 0L, failedToDelete)))
                .build();
    }

    private Category fillAndSave(final CategoryForm categoryForm,
            final User user,
            final Host host,
            final PageMode pageMode,
            final Category category)
            throws DotSecurityException, DotDataException, InvocationTargetException, IllegalAccessException {

        Category parentCategory = null;

        Logger.debug(this, () -> "Filling category entity");

        if (UtilMethods.isSet(categoryForm.getParent())) {
            parentCategory = this.categoryAPI.find(categoryForm.getParent(), user,
                    pageMode.respectAnonPerms);
        }

        BeanUtils.copyProperties(category, categoryForm);

        category.setModDate(new Date());

        Logger.debug(this, () -> "Saving category entity : " + getObjectToJsonString(category));
        this.categoryAPI.save(parentCategory, category, user, pageMode.respectAnonPerms);
        Logger.debug(this, () -> "Saved category entity : " + getObjectToJsonString(category));

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

        Logger.debug(this, () -> "Filling category entity");

        if (UtilMethods.isSet(categoryForm.getParent())) {
            parentCategory = this.categoryAPI.find(categoryForm.getParent(), user,
                    pageMode.respectAnonPerms);
        }

        BeanUtils.copyProperties(updatedCategory, oldCategory);

        updatedCategory.setCategoryName(categoryForm.getCategoryName());
        updatedCategory.setKey(categoryForm.getKey());
        updatedCategory.setKeywords(categoryForm.getKeywords());
        updatedCategory.setModDate(new Date());

        Logger.debug(this,
                () -> "Saving category entity : " + getObjectToJsonString(updatedCategory));
        this.categoryAPI.save(parentCategory, updatedCategory, user, pageMode.respectAnonPerms);
        Logger.debug(this,
                () -> "Saved category entity : " + getObjectToJsonString(updatedCategory));

        ActivityLogger.logInfo(this.getClass(), "Saved Category", "User " + user.getPrimaryKey()
                        + "Category: " + updatedCategory.getCategoryName(),
                host.getTitle() != null ? host.getTitle() : "default");

        return updatedCategory;
    }

    /**
     * Return a list of {@link com.dotmarketing.portlets.categories.model.Category}, entity response
     * syntax:
     * <code> { contentTypes: array of Category total: total number of Categories } <code/>
     * <p>
     * Url syntax: api/v1/categories/_export?contextInode=inode&filter=filter-string
     * <p>
     * where:
     *
     * <ul>
     * <li>filter-string: just return Category whose content this pattern into its name</li>
     * <li>contextInode: category inode</li>
     * </ul>
     * <p>
     * Url example: v1/categories/_export?contextInode=inode&filter=test
     *
     * @param httpRequest
     * @return
     */
    @Operation(
        summary = "Export categories to CSV",
        description = "Exports categories to a CSV file format. Can filter by category name pattern and specify a context category inode. Returns a downloadable CSV file."
    )
    @ApiResponse(responseCode = "200", description = "Categories exported successfully as CSV file",
                content = @Content(mediaType = "text/csv"))
    @ApiResponse(responseCode = "401", description = "Unauthorized - user authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions to export categories")
    @ApiResponse(responseCode = "404", description = "Context category not found")
    @ApiResponse(responseCode = "500", description = "Internal server error exporting categories")
    @GET
    @Path("/_export")
    @JSONP
    @NoCache
    @Produces({"text/csv"})
    public final void export(@Context final HttpServletRequest httpRequest,
            @Context final HttpServletResponse httpResponse,
            @Parameter(description = "Context category inode to export from") @QueryParam("contextInode") final String contextInode,
            @Parameter(description = "Filter pattern to match category names") @QueryParam(PaginationUtil.FILTER) final String filter)
            throws DotDataException, DotSecurityException, IOException {

        final InitDataObject initData = webResource.init(null, httpRequest, httpResponse, true,
                null);

        final User user = initData.getUser();
        final PageMode pageMode = PageMode.get(httpRequest);

        httpResponse.setCharacterEncoding("UTF-8");
        httpResponse.setContentType("application/octet-stream");
        httpResponse.setHeader("Content-Disposition",
                "attachment; filename=\"categories_" + UtilMethods.dateToHTMLDate(new Date(),
                        "M_d_yyyy") + ".csv\"");

        Logger.debug(this, () -> "Exporting the list of categories. " + String.format(
                "Request query parameters are : {contextInode : %s, filter : %s}", contextInode,
                filter));

        final PrintWriter output = httpResponse.getWriter();

        try {
            List<Category> categories =
                    UtilMethods.isSet(contextInode) ? this.categoryAPI.findChildren(user,
                            contextInode, false, filter) :
                            this.categoryAPI.findTopLevelCategories(user, false, filter);

            if (!categories.isEmpty()) {
                output.print("\"name\",\"key\",\"variable\",\"sort\"");
                output.print("\r\n");

                for (Category category : categories) {
                    String catName = category.getCategoryName();
                    String catKey = category.getKey();
                    String catVar = category.getCategoryVelocityVarName();
                    String catSort = Integer.toString(category.getSortOrder());
                    catName = catName == null ? "" : catName;
                    catKey = catKey == null ? "" : catKey;
                    catVar = catVar == null ? "" : catVar;

                    catName = "\"" + catName + "\"";
                    catKey = "\"" + catKey + "\"";
                    catVar = "\"" + catVar + "\"";

                    output.print(catName + "," + catKey + "," + catVar + "," + catSort);
                    output.print("\r\n");
                }

            } else {
                output.print("There are no Categories to show");
                output.print("\r\n");
            }
        } catch (Exception e) {
            Logger.error(this, "Error exporting categories", e);
        } finally {
            output.flush();
            output.close();
        }
    }

    /**
     * Imports categories from a CSV file.
     *
     * @param httpRequest HTTP request context
     * @param httpResponse HTTP response context
     * @param uploadedFile CSV file containing categories to import
     * @param fileDetail File metadata and disposition information
     * @param filter Filter pattern for categories
     * @param exportType Import type: 'replace' or 'append'
     * @param contextInode Context category inode to import into
     * @return Response indicating success/failure
     * @throws IOException if file reading fails
     */
    @Operation(
        summary = "Import categories from CSV file",
        description = "Imports categories from an uploaded CSV file. Supports 'replace' mode to replace existing categories or 'append' mode to add to existing categories. Can specify a context category and filter options."
    )
    @RequestBody(required = true,
            content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA,
                    schema = @Schema(implementation = CategoryImportData.class)))
    @ApiResponse(responseCode = "200", description = "Categories imported successfully",
                content = @Content(mediaType = "application/json",
                                  schema = @Schema(implementation = ResponseEntityCategoryView.class)))
    @ApiResponse(responseCode = "400", description = "Bad request - invalid file format or missing required parameters")
    @ApiResponse(responseCode = "401", description = "Unauthorized - user authentication required")
    @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions to import categories")
    @ApiResponse(responseCode = "500", description = "Internal server error importing categories")
    @POST
    @Path("/_import")
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    public Response importCategories(@Context final HttpServletRequest httpRequest,
            @Context final HttpServletResponse httpResponse,
            @Parameter(hidden = true) @BeanParam final CategoryImportData form) throws IOException {

        return processImport(httpRequest, httpResponse,
                form.getFileInputStream(), form.getFileDetail(),
                form.getFilter(), form.getExportType(), form.getContextInode());

    }

    

    @WrapInTransaction
    private Response processImport(final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse,
            final InputStream fileInputStream,
            final FormDataContentDisposition fileDetail,
            final String filter,
            final String exportType,
            final String contextInode) throws IOException {

        List<Category> unableToDeleteCats = null;
        final List<FailedResultView> failedToDelete = new ArrayList<>();

        BufferedReader bufferedReader = null;

        try {
            final InitDataObject initData = webResource.init(null, httpRequest, httpResponse, true,
                    null);

            final User user = initData.getUser();
            final PageMode pageMode = PageMode.get(httpRequest);

            Logger.debug(this, () -> "Importing the list of categories. " + String.format(
                    "Request payload is : {contextInode : %s, filter : %s, exportType : %s}",
                    contextInode,
                    filter, exportType));

            bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));

            if (exportType.equals("replace")) {
                Logger.debug(this, () -> "Replacing categories");
                if (UtilMethods.isSet(contextInode)) {
                    Category contextCat = this.categoryAPI.find(contextInode, user, false);
                    unableToDeleteCats = this.categoryAPI.removeAllChildren(contextCat, user,
                            false);
                    if (!unableToDeleteCats.isEmpty()) {
                        for (final Category category : unableToDeleteCats) {
                            Logger.error(this, "Category with Id: " + category.getInode()
                                    + " unable to delete");
                            failedToDelete.add(new FailedResultView(category.getInode(),
                                    "Category with id: " + category.getInode()
                                            + " unable to delete"));
                        }
                    }
                } else {
                    Logger.debug(this, () -> "Deleting all the categories");
                    categoryAPI.deleteAll(user, false);
                    Logger.debug(this, () -> "Deleted all the categories");
                }

                this.categoryHelper.addOrUpdateCategory(user, contextInode, bufferedReader, false);
            } else if (exportType.equals("merge")) {
                Logger.debug(this, () -> "Merging categories");
                this.categoryHelper.addOrUpdateCategory(user, contextInode, bufferedReader, true);
            }

        } catch (Exception e) {
            Logger.error(this, "Error importing categories", e);
        } finally {
            CloseUtils.closeQuietly(bufferedReader);
        }

        return Response.ok(new ResponseEntityBulkResultView(
                        new BulkResultView(Long.valueOf(UtilMethods.isSet(unableToDeleteCats) ? 1 : 0), 0L,
                                failedToDelete)))
                .build();
    }

    @WrapInTransaction
    private void updateSortOrder(final CategoryEditForm categoryEditForm, final User user,
            final Host host,
            final PageMode pageMode, final Category parentCategory)
            throws DotDataException, DotSecurityException {

        Iterator iterator = categoryEditForm.getCategoryData().entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String key = (String) entry.getKey();
            Integer value = (Integer) entry.getValue();

            final Category category = this.categoryAPI.find(key, user, pageMode.respectAnonPerms);

            if (null == category) {
                Logger.error(this, "Category with Id: " + key + " does not exist");
                throw new IllegalArgumentException("Category with Id: " + key + " does not exist");
            } else {

                category.setSortOrder(value);

                Logger.debug(this,
                        () -> "Saving category entity : " + getObjectToJsonString(category));
                this.categoryAPI.save(parentCategory, category, user,
                        pageMode.respectAnonPerms);
                Logger.debug(this,
                        () -> "Saved category entity : " + getObjectToJsonString(category));

                ActivityLogger.logInfo(this.getClass(), "Saved Category",
                        "User " + user.getPrimaryKey()
                                + "Category: " + category.getCategoryName(),
                        host.getTitle() != null ? host.getTitle() : "default");
            }
        }
    }

    private String getObjectToJsonString(final Object object) {
        ObjectMapper mapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
        try {
            final String json = mapper.writeValueAsString(object);
            return json;
        } catch (JsonProcessingException e) {
            Logger.error(this, e.getMessage(), e);
        }
        return StringPool.BLANK;
    }
}
