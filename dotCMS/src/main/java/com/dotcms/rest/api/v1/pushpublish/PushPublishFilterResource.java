package com.dotcms.rest.api.v1.pushpublish;

import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherAPI;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityListStringView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.annotation.SwaggerCompliant;
import com.dotcms.rest.api.MultiPartUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.Lazy;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This REST Endpoint provides developers useful methods to interact with Push Publishing Filters in dotCMS.
 * <p>You may create Push Publishing filters to control which content is pushed from your sending server to your
 * receiving server. The filters allow you to have fine-grained control over what content does and does not get pushed,
 * whether intentionally (when specifically selected) or by dependency.</p>
 * <p>You may create as many filters as you wish. You can specify permissions for the filters, allowing you to control
 * what content and objects different users and Roles may push. For example, you can allow users with a specific Role to
 * only push content of a specific Content Type, or only push content in a specific location.</p>
 *
 * @author Erick Gonzalez
 * @since Mar 6th, 2020
 */
@SwaggerCompliant(value = "Publishing and content distribution APIs", batch = 5)
@Tag(name = "Push Publishing")
@Path("/v1/pushpublish/filters")
public class PushPublishFilterResource {

    private final WebResource webResource;
    private final MultiPartUtils multiPartUtils = new MultiPartUtils();
    private final Lazy<PublisherAPI> publisherAPI = Lazy.of(() -> APILocator.getPublisherAPI());

    public PushPublishFilterResource(){
        this(new WebResource());
    }

    @VisibleForTesting
    public PushPublishFilterResource(final WebResource webResource) {
        this.webResource = webResource;
    }

    @Operation(
        summary = "Get push publishing filters",
        description = "Lists all Push Publishing filter descriptors that the user has access to, sorted alphabetically"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Filters retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityFilterDescriptorsView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getFilters(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response) throws DotDataException {
        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .init();
        final User user = initData.getUser();
        final List<FilterDescriptor> list = this.publisherAPI.get().getFiltersDescriptorsByRole(user);
        return Response.ok(new ResponseEntityFilterDescriptorsView(list)).build();
    }

    @Operation(
        summary = "Get push publishing filter by key",
        description = "Returns a specific Push Publishing filter descriptor by its key (if the user has access to it)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Filter retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityFilterDescriptorView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to access this filter",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Filter not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @Path("/{filterKey}")
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getFilter(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                     @Parameter(description = "Push Publishing filter key", required = true) @PathParam("filterKey") final String filterKey) throws DotDataException, DotSecurityException {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .init();
        final User user = initData.getUser();

        if (this.publisherAPI.get().existsFilterDescriptor(filterKey)) {

            final FilterDescriptor filterDescriptor = APILocator.getPublisherAPI().getFilterDescriptorByKey(filterKey);
            if (!user.isAdmin()) {
                final List<Role> roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId(), true);
                Logger.debug(this, ()->"User Roles: " + roles.toString());
                final String filterRoles = filterDescriptor.getRoles();
                Logger.debug(this, ()-> "File: " + filterDescriptor.getKey() + " Roles: " + filterRoles);
                final boolean allowed = roles.stream().anyMatch(role -> UtilMethods.isSet(role.getRoleKey()) && filterRoles.contains(role.getRoleKey()));
                if (!allowed) {
                    throw new DotSecurityException(
                            String.format("User '%s' does not have access to filter '%s'", user.getUserId(),
                                    filterKey));
                }
            }
            return Response.ok(new ResponseEntityFilterDescriptorView(filterDescriptor)).build();
        }
        final String errorMsg = String.format("Filter '%s' does not exist", filterKey);
        Logger.debug(this, ()-> errorMsg);
        throw new DoesNotExistException(errorMsg);
    }

    @Operation(
        summary = "Create push publishing filter from form",
        description = "Creates a new Push Publishing filter based on form data. Requires CMS Administrator role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Filter created successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityListStringView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid filter data or filter already exists",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "409", 
                    description = "Conflict - filter with this key already exists",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public final Response saveFromForm(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                           description = "Push Publishing filter form data", 
                                           required = true,
                                           content = @Content(schema = @Schema(implementation = FilterDescriptorForm.class))
                                       ) final FilterDescriptorForm filterDescriptorForm) throws DotDataException {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                        .init();
        final User user = initData.getUser();

        Logger.debug(this, ()-> "Adding PP filter: " + filterDescriptorForm);

        if (this.publisherAPI.get().existsFilterDescriptor(filterDescriptorForm.getKey())) {
            final String errorMsg = String.format("Filter '%s' cannot be added because it already exists",
                    filterDescriptorForm.getKey());
            Logger.debug(this, () -> errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        final List<String> filterNames = saveAndReloadFiltersFromForm(filterDescriptorForm, user);
        return Response.ok(new ResponseEntityListStringView(filterNames)).build();
    }

    @Operation(
        summary = "Create push publishing filter from YML file",
        description = "Creates a new Push Publishing filter by uploading a YML configuration file. Requires CMS Administrator role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Filter created successfully from file",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityListStringView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid file format or filter already exists",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "409", 
                    description = "Conflict - filter with this key already exists",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response saveFromFile(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                           description = "Multipart form data containing YML filter file(s)", 
                                           required = true,
                                           content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA)
                                       ) final FormDataMultiPart multipart) throws DotDataException, IOException {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                        .init();
        final User user = initData.getUser();

        Logger.debug(this, ()-> "Adding PP filter by file");
        final List<String> filterNames = saveAndReloadFiltersFromFile(multipart, user);
        return Response.ok(new ResponseEntityListStringView(filterNames)).build();
    }

    @Operation(
        summary = "Update push publishing filter from form",
        description = "Updates an existing Push Publishing filter based on form data. Requires CMS Administrator role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Filter updated successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityListStringView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid filter data",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Filter not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public final Response updateFromForm(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                           description = "Push Publishing filter form data with updates", 
                                           required = true,
                                           content = @Content(schema = @Schema(implementation = FilterDescriptorForm.class))
                                       ) final FilterDescriptorForm filterDescriptorForm) throws DotDataException {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                        .init();
        final User user = initData.getUser();

        Logger.debug(this, ()-> "Updating PP filter: " + filterDescriptorForm);

        if (!this.publisherAPI.get().existsFilterDescriptor(filterDescriptorForm.getKey())) {
            final String errorMsg = String.format("Filter '%s' does not exist", filterDescriptorForm.getKey());
            Logger.debug(this, ()-> errorMsg);
            throw new DoesNotExistException(errorMsg);
        }

        final List<String> filterNames = saveAndReloadFiltersFromForm(filterDescriptorForm, user);
        return Response.ok(new ResponseEntityListStringView(filterNames)).build();
    }

    @Operation(
        summary = "Update push publishing filter from YML file",
        description = "Updates an existing Push Publishing filter by uploading a YML configuration file. Requires CMS Administrator role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Filter updated successfully from file",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityListStringView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Bad request - invalid file format",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Filter not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response updateFromFile(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                           description = "Multipart form data containing YML filter file(s) with updates", 
                                           required = true,
                                           content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA)
                                       ) final FormDataMultiPart multipart) throws DotDataException, IOException {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                        .init();
        final User user = initData.getUser();

        Logger.debug(this, ()-> "Updating PP filter by file");
        final List<String> filterNames = updateAndReloadFiltersFromFile(multipart, user);
        return Response.ok(new ResponseEntityListStringView(filterNames)).build();
    }

    @Operation(
        summary = "Delete push publishing filter",
        description = "Deletes a Push Publishing filter by its unique key. Requires CMS Administrator role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Filter deleted successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityListStringView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - CMS Administrator role required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Filter not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "417", 
                    description = "Expectation failed - filter cannot be deleted",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @Path("/{filterKey}")
    @JSONP
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response deleteFilter(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                     @Parameter(description = "Push Publishing filter key to delete", required = true) @PathParam("filterKey") final String filterKey) throws DotDataException {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                        .init();
        final User user = initData.getUser();
        final boolean deleted = this.publisherAPI.get().deleteFilterDescriptor(filterKey);
        if (deleted) {
            final List<String> filterNames =
                    this.publisherAPI.get().getFiltersDescriptorsByRole(user).stream().map(FilterDescriptor::getKey).collect(Collectors.toList());
            return Response.ok(new ResponseEntityListStringView(filterNames)).build();
        }
        return Response.status(Response.Status.EXPECTATION_FAILED).
                entity(new ResponseEntityFilterErrorView(List.of(
                        new ErrorEntity(Integer.valueOf(Response.Status.EXPECTATION_FAILED.getStatusCode()).toString(),
                                String.format("Filter '%s' cannot be deleted", filterKey))
                        )
                    )
                ).build();
    }

    /**
     * Saves or updates the Push Publishing Filter submitted via the REST Form, and re-initializes the complete list of
     * Filters.
     *
     * @param filterDescriptorForm The {@link FilterDescriptorForm} containing the information from the Push
     *                             Publishing Filter.
     * @param user                 The {@link User} performing this action.
     *
     * @return The list containing the keys from all Push Publishing Filters.
     *
     * @throws DotDataException An error occurred when interacting with the data source.
     */
    protected List<String> saveAndReloadFiltersFromForm(final FilterDescriptorForm filterDescriptorForm,
                                                        final User user) throws DotDataException {
        final FilterDescriptor filterDescriptor = new FilterDescriptor(filterDescriptorForm.getKey(),
                filterDescriptorForm.getTitle(), filterDescriptorForm.getSort(), filterDescriptorForm.getFilters(),
                filterDescriptorForm.isDefaultFilter(), filterDescriptorForm.getRoles());
        this.publisherAPI.get().upsertFilterDescriptor(filterDescriptor);
        return this.publisherAPI.get().getFiltersDescriptorsByRole(user).stream().map(FilterDescriptor::getKey).collect(Collectors.toList());
    }

    /**
     * Saves the Push Publishing Filter submitted as YML files, and re-initializes the complete list of Filters.
     *
     * @param multipart The {@link FormDataMultiPart} object containing the binary YML file(s).
     * @param user      The {@link User} performing this action.
     *
     * @return The list containing the keys from all Push Publishing Filters.
     *
     * @throws DotDataException         An error occurred when interacting with the data source.
     * @throws IOException              An error occurred when copying the YML files.
     * @throws IllegalArgumentException At least one of the specified Filters already exist.
     */
    protected List<String> saveAndReloadFiltersFromFile(final FormDataMultiPart multipart, final User user) throws DotDataException, IOException {
        final List<File> filterFiles = this.multiPartUtils.getBinariesFromMultipart(multipart);
        for (final File file : filterFiles) {
            if (APILocator.getPublisherAPI().existsFilterDescriptor(file.getName())) {
                final String errorMsg =
                        String.format("Filter '%s' cannot be added because it already exists", file.getName());
                Logger.warn(this, () -> errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }
        }
        this.publisherAPI.get().saveFilterDescriptors(filterFiles);
        return this.publisherAPI.get().getFiltersDescriptorsByRole(user).stream().map(FilterDescriptor::getKey).collect(Collectors.toList());
    }

    /**
     * Updates the Push Publishing Filter submitted as YML files, and re-initializes the complete list of Filters.
     *
     * @param multipart The {@link FormDataMultiPart} object containing the binary YML file(s).
     * @param user      The {@link User} performing this action.
     *
     * @return The list containing the keys from all Push Publishing Filters.
     *
     * @throws DotDataException         An error occurred when interacting with the data source.
     * @throws IOException              An error occurred when copying the YML files.
     * @throws IllegalArgumentException At least one of the specified Filters does not exist.
     */
    protected List<String> updateAndReloadFiltersFromFile(final FormDataMultiPart multipart, final User user) throws DotDataException, IOException {
        final List<File> filterFiles = this.multiPartUtils.getBinariesFromMultipart(multipart);
        for (final File file : filterFiles) {
            if (!APILocator.getPublisherAPI().existsFilterDescriptor(file.getName())) {
                final String errorMsg =
                        String.format("Filter '%s' does not exist", file.getName());
                Logger.warn(this, () -> errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }
        }
        this.publisherAPI.get().saveFilterDescriptors(filterFiles);
        return this.publisherAPI.get().getFiltersDescriptorsByRole(user).stream().map(FilterDescriptor::getKey).collect(Collectors.toList());
    }

}
