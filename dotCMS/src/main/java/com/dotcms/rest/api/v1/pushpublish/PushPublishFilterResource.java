package com.dotcms.rest.api.v1.pushpublish;

import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PushPublishFiltersInitializer;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.MultiPartUtils;
import com.dotcms.util.YamlUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This Resource is for the push publishing filters
 */
@Path("/v1/pushpublish/filters")
public class PushPublishFilterResource {

    private final WebResource webResource;

    private final MultiPartUtils multiPartUtils = new MultiPartUtils();

    public PushPublishFilterResource(){
        this(new WebResource());
    }

    @VisibleForTesting
    public PushPublishFilterResource(final WebResource webResource) {
        this.webResource = webResource;
    }

    /**
     * Lists all filters descriptors that the user role has access to.
     */
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
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

            final List<FilterDescriptor> list = APILocator.getPublisherAPI().getFiltersDescriptorsByRole(user);

        return Response.ok(new ResponseEntityView(list)).build();
    }

    /**
     * Get a filter descriptors by filter key (if the user has access to it)
     */
    @GET
    @Path("/{filterKey}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getFilter(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                     @PathParam("filterKey") final String filterKey) throws DotDataException, DotSecurityException {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .init();
        final User user = initData.getUser();

        if (APILocator.getPublisherAPI().existsFilterDescriptor(filterKey)) {

            final FilterDescriptor filterDescriptor = APILocator.getPublisherAPI().getFilterDescriptorByKey(filterKey);
            if (!user.isAdmin()) {
                final List<Role> roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId(), true);
                Logger.debug(this, ()->"User Roles: " + roles.toString());
                final String filterRoles = filterDescriptor.getRoles();
                Logger.debug(this, ()-> "File: " + filterDescriptor.getKey() + " Roles: " + filterRoles);
                final boolean allowed = roles.stream().anyMatch(role -> UtilMethods.isSet(role.getRoleKey()) && filterRoles.contains(role.getRoleKey()));
                if (!allowed) {

                    throw new DotSecurityException("Has not access to the fitler: " + filterKey);
                }
            }
            return Response.ok(new ResponseEntityView(filterDescriptor)).build();
        }

        Logger.debug(this, ()-> "The filter: " + filterKey + " does not exists");
        throw new DoesNotExistException("The Filter: " + filterKey + " does not exists");
    }

    /**
     * Creates a new Filter based on a bean form
     */
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response saveFromForm(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       final FilterDescriptorForm filterDescriptorForm) throws DotDataException {

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

        if (APILocator.getPublisherAPI().existsFilterDescriptor(filterDescriptorForm.getKey())) {

            Logger.debug(this, ()-> "The filter: " + filterDescriptorForm.getKey() +
                    " can not be added, because it already exists");
            throw new IllegalArgumentException("The Filter: " + filterDescriptorForm.getKey()
                    + " can not be added, because it already exists");
        }

        final FilterDescriptor filterDescriptor = new FilterDescriptor(filterDescriptorForm.getKey(), filterDescriptorForm.getTitle(),
                filterDescriptorForm.getFilters(), filterDescriptorForm.isDefaultFilter(), filterDescriptorForm.getRoles());
        final File filterPathFile = new File(new File(APILocator.getFileAssetAPI().getRealAssetsRootPath() + File.separator + "server"
                + File.separator + "publishing-filters" + File.separator), filterDescriptor.getKey());
        YamlUtil.write(filterPathFile, filterDescriptor);
        new PushPublishFiltersInitializer().init();
        final List<String> filterNames = APILocator.getPublisherAPI()
                .getFiltersDescriptorsByRole(user).stream().map(filter -> filter.getKey()).collect(Collectors.toList());
        return Response.ok(new ResponseEntityView(filterNames)).build();
    }

    /**
     * Creates a new Filter based on a file
     */
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response saveFromFile(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       final FormDataMultiPart multipart) throws DotDataException, IOException {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                        .init();
        final User user = initData.getUser();

        Logger.debug(this, ()-> "Adding PP filter by file ");

        final List<File> filterFiles = this.multiPartUtils.getBinariesFromMultipart(multipart);

        for (final File file : filterFiles) {
            if (APILocator.getPublisherAPI().existsFilterDescriptor(file.getName())) {

                Logger.debug(this, () -> "The filter: " + file.getName() +
                        " can not be added, because it already exists");
                throw new IllegalArgumentException("The Filter: " + file.getName() +
                        " can not be added, because it already exists");
            }
        }

        for (final File file : filterFiles) {
            final File filterPathFile = new File(new File(APILocator.getFileAssetAPI().getRealAssetsRootPath() + File.separator + "server"
                    + File.separator + "publishing-filters" + File.separator), file.getName());
            FileUtils.copyFile(file, filterPathFile);
        }
        new PushPublishFiltersInitializer().init();
        final List<String> filterNames = APILocator.getPublisherAPI()
                .getFiltersDescriptorsByRole(user).stream().map(filter -> filter.getKey()).collect(Collectors.toList());

        return Response.ok(new ResponseEntityView(filterNames)).build();
    }

    /**
     * Updates an existing Filter based on a bean form
     */
    @PUT
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response updateFromForm(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       final FilterDescriptorForm filterDescriptorForm) throws DotDataException {

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

        if (!APILocator.getPublisherAPI().existsFilterDescriptor(filterDescriptorForm.getKey())) {

            Logger.debug(this, ()-> "The filter: " + filterDescriptorForm.getKey() + " does not exists");
            throw new DoesNotExistException("The Filter: " + filterDescriptorForm.getKey() + " does not exists");
        }

        final FilterDescriptor filterDescriptor = new FilterDescriptor(filterDescriptorForm.getKey(), filterDescriptorForm.getTitle(),
                filterDescriptorForm.getFilters(), filterDescriptorForm.isDefaultFilter(), filterDescriptorForm.getRoles());
        final File filterPathFile = new File(new File(APILocator.getFileAssetAPI().getRealAssetsRootPath() + File.separator + "server"
                + File.separator + "publishing-filters" + File.separator), filterDescriptor.getKey());
        YamlUtil.write(filterPathFile, filterDescriptor);
        new PushPublishFiltersInitializer().init();
        final List<String> filterNames = APILocator.getPublisherAPI()
                .getFiltersDescriptorsByRole(user).stream().map(filter -> filter.getKey()).collect(Collectors.toList());
        return Response.ok(new ResponseEntityView(filterNames)).build();
    }

    /**
     * Updates an existing Filter based on a file
     */
    @PUT
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response updateFromFile(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       final FormDataMultiPart multipart) throws DotDataException, IOException {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                        .init();
        final User user = initData.getUser();

        Logger.debug(this, ()-> "Updating PP filter by file ");

        final List<File> filterFiles = this.multiPartUtils.getBinariesFromMultipart(multipart);

        for (final File file : filterFiles) {
            if (!APILocator.getPublisherAPI().existsFilterDescriptor(file.getName())) {

                Logger.debug(this, ()-> "The filter: " + file.getName() + " does not exists");
                throw new IllegalArgumentException("The Filter: " + file.getName() + " does not exists");
            }
        }

        for (final File file : filterFiles) {
            final File filterPathFile = new File(new File(APILocator.getFileAssetAPI().getRealAssetsRootPath() + File.separator + "server"
                    + File.separator + "publishing-filters" + File.separator), file.getName());
            FileUtils.copyFile(file, filterPathFile);
        }
        new PushPublishFiltersInitializer().init();
        final List<String> filterNames = APILocator.getPublisherAPI()
                .getFiltersDescriptorsByRole(user).stream().map(filter -> filter.getKey()).collect(Collectors.toList());

        return Response.ok(new ResponseEntityView(filterNames)).build();
    }

    /**
     * Deletes a filter by filter key
     */
    @DELETE
    @Path("/{filterKey}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response deleteFilter(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                     @PathParam("filterKey") final String filterKey) throws DotDataException, DotSecurityException {

        final InitDataObject initData =
                new WebResource.InitBuilder(webResource)
                        .requiredBackendUser(true)
                        .requiredFrontendUser(false)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .requiredRoles(Role.CMS_ADMINISTRATOR_ROLE)
                        .init();
        final User user = initData.getUser();

        if (!APILocator.getPublisherAPI().existsFilterDescriptor(filterKey)) {

            Logger.debug(this, ()-> "The filter: " + filterKey + " does not exists");
            throw new DoesNotExistException("The Filter: " + filterKey + " does not exists");
        }

        final File filterPathFile = new File(new File(APILocator.getFileAssetAPI().getRealAssetsRootPath() + File.separator + "server"
                + File.separator + "publishing-filters" + File.separator), filterKey);
        if (FileUtils.deleteQuietly(filterPathFile)) {
            new PushPublishFiltersInitializer().init();
            final List<String> filterNames = APILocator.getPublisherAPI()
                    .getFiltersDescriptorsByRole(user).stream().map(filter -> filter.getKey()).collect(Collectors.toList());

            return Response.ok(new ResponseEntityView(filterNames)).build();
        }

        return Response.status(Response.Status.EXPECTATION_FAILED).
                entity(new ResponseEntityView(Arrays.asList(
                        new ErrorEntity(
                                new Integer(Response.Status.EXPECTATION_FAILED.getStatusCode()).toString(),
                                "The filter:  " + filterKey + "can not be deleted")
                        )
                    )
                ).build();
    }
}
