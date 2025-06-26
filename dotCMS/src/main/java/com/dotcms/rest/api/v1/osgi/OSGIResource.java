package com.dotcms.rest.api.v1.osgi;

import com.dotcms.rest.ResponseEntityBooleanView;
import com.dotcms.rest.ResponseEntityListView;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.MultiPartUtils;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.util.FileUtil;
import com.liferay.util.StringPool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.Tuple2;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.felix.framework.OSGISystem;
import org.apache.felix.framework.OSGIUtil;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.server.JSONP;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a RESTful resource for OSGi related operations.
 * @author jsanca
 */
@Tag(name = "OSGi Plugins")
@Path ("/v1/osgi")
public class OSGIResource {

    public static final String DYNAMIC_PLUGINS = "dynamic-plugins";
    private final MultiPartUtils multiPartUtils = new MultiPartUtils();
    private final WebResource webResource    = new WebResource();

    /**
     * This method returns a dot system list of all bundles installed in the OSGi environment at the time of the call to this method.
     *
     * @param request
     * @param response
     * @param ignoreSystemBundles
     * @return ResponseEntityBundleListView
     */
    @GET
    @Path ("/dotsystem")
    @Produces (MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns the dot system list of all bundles installed",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityBundleListView.class)))
    })
    public ResponseEntityBundleListView getSystemInstalledBundles (@Context HttpServletRequest request,
                                               @Context final HttpServletResponse response,
                                               @QueryParam("ignoresystembundles") final boolean ignoreSystemBundles) {

        checkUserPermissions(request, response, DYNAMIC_PLUGINS);

        Logger.debug(this, ()-> "Getting dot system installed bundles");
        final List<BundleMap> bundlesArray =
                Stream.of(OSGISystem.getInstance().getBundles()).map(bundle -> getBundleMap(ignoreSystemBundles, bundle)).collect(Collectors.toList());

        return new ResponseEntityBundleListView(bundlesArray);
    }

    /**
     * This method returns a list of all bundles installed in the OSGi environment at the time of the call to this method.
     *
     * @param request
     * @param response
     * @param ignoreSystemBundles
     * @return ResponseEntityBundleListView
     */
    @GET
    @Produces (MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns a list of all bundles installed",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityBundleListView.class)))
            })
    public ResponseEntityBundleListView getInstalledBundles (@Context HttpServletRequest request,
                                               @Context final HttpServletResponse response,
                                               @QueryParam("ignoresystembundles") final boolean ignoreSystemBundles) {

        checkUserPermissions(request, response, DYNAMIC_PLUGINS);

        Logger.debug(this, ()-> "Getting installed bundles");
        final List<BundleMap> bundlesArray =
                Stream.of(OSGIUtil.getInstance().getBundles()).map(bundle -> getBundleMap(ignoreSystemBundles, bundle)).collect(Collectors.toList());

        return new ResponseEntityBundleListView(bundlesArray);
    }

    private void checkUserPermissions(final HttpServletRequest request,
                                      final HttpServletResponse response,
                                      final String... requiredPortlets) {
        new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .requiredPortlet(requiredPortlets)
                .rejectWhenNoUser(true)
                .init();
    }

    private BundleMap getBundleMap(final boolean ignoreSystemBundles,
                                                    final Bundle bundle) {

        final String bundleLocation = bundle.getLocation();
        final boolean isSystem = ignoreSystemBundles && (bundleLocation.contains("felix/bundle") || bundleLocation.contains("System Bundle"));

        // Getting the jar file name
        final String separator = bundle.getLocation().contains(StringPool.FORWARD_SLASH)?
                StringPool.FORWARD_SLASH: File.separator;
        final String jarFile = bundle.getLocation().contains(separator)
                ? bundle.getLocation().substring(bundle.getLocation().lastIndexOf(separator) + 1)
                : "System";

        // Build the version string
        final String version = bundle.getVersion().getMajor() + "." + bundle.getVersion().getMinor() + "."
                + bundle.getVersion().getMicro();

        return new BundleMap.Builder().bundleId(bundle.getBundleId())
                .symbolicName(bundle.getSymbolicName())
                .location(bundle.getLocation())
                .jarFile(jarFile)
                .state(bundle.getState())
                .version(version)
                .separator(separator)
                .isSystem(isSystem)
                .build();
    }

    /**
     * This method process exports
     *
     * @param request
     * @param response
     * @param bundle
     * @return ResponseEntityStringView
     */
    @GET
    @Path ("/_processExports/{bundle:.*}")
    @Produces (MediaType.APPLICATION_JSON)
    @Operation(summary = "Process exports",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityStringView.class)))
            })
    public ResponseEntityStringView processBundle (@Context final HttpServletRequest request,
                                                   @Context final HttpServletResponse response,
                                                   @PathParam ("bundle") final String bundle) {

        checkUserPermissions(request, response, DYNAMIC_PLUGINS);

        Logger.debug(this, ()-> "Processing exports for bundle: " + bundle);
        OSGIUtil.getInstance().processExports(bundle);
        return new ResponseEntityStringView("Exports processed");
    }

    private Bundle findBundleOrThrowNotExist(final String bundleId) {

        Bundle bundle = null;
        try {
            bundle = OSGIUtil.getInstance().getBundle(Long.parseLong(bundleId));
        } catch (NumberFormatException e) {
            bundle = OSGIUtil.getInstance().getBundle(bundleId);
        }

        if (null == bundle) {
            throw new DoesNotExistException("Bundle not found: " + bundleId);
        }

        return bundle;
    }

    /**
     * Does the undeploy of a jar
     *
     * @param request
     * @param response
     * @param jarName
     * @throws BundleException
     * @throws IOException
     * @return ResponseEntityBooleanView
     */
    @DELETE
    @Path("/jar/{jar}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(summary = "Undeploys bundle",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityBooleanView.class))),
                    @ApiResponse(responseCode = "404", description = "Bundle not found"),
                    @ApiResponse(responseCode = "400", description = "Error undeploying OSGI Bundle"),
                    @ApiResponse(responseCode = "400", description = "Can not stop system bundle")

            })
    public final ResponseEntityBooleanView undeploy(@Context final HttpServletRequest request,
                                                    @Context final HttpServletResponse response,
                                                    @PathParam ("jar") final String jarName) throws BundleException, IOException {

        checkUserPermissions(request, response, DYNAMIC_PLUGINS);
        Logger.debug(this, ()->"Undeploying OSGI jar " + jarName);

        final String sanitizeFileNameJarName = com.dotmarketing.util.FileUtil.sanitizeFileName(jarName);
        final String bundleId = findBundleByJar(sanitizeFileNameJarName);
        final Bundle bundle = findBundleOrThrowNotExist(bundleId);
        final String bundleLocation = bundle.getLocation();
        if (isSystemBundle(bundleLocation)) {
            throw new BadRequestException("Can undeploy system bundle: " + jarName);
        }
        bundle.uninstall();

        //Then move the bundle from the load folder to the undeployed folder
        final String loadPath = OSGIUtil.getInstance().getFelixDeployPath();
        final String undeployedPath = OSGIUtil.getInstance().getFelixUndeployPath();
        final File from = new File(loadPath + File.separator + sanitizeFileNameJarName);
        final File to   = new File(undeployedPath + File.separator + sanitizeFileNameJarName);

        if(to.getCanonicalPath().startsWith(undeployedPath) && to.exists()) {

            final boolean deleteOk = Files.deleteIfExists(to.toPath());
            Logger.info(this, String.format(" File [%s] successfully un-deployed [%s].",
                    to.getCanonicalPath(), BooleanUtils.toStringYesNo(deleteOk)));
        }

        final boolean success = FileUtil.move(from, to);
        if (success) {
            Logger.info(this, String.format("OSGI Bundle  %s Undeployed ", sanitizeFileNameJarName));
            removeReferences(); // removes portlets and actionlets references
            return new ResponseEntityBooleanView(true);
        }

        Logger.error(this, "Error undeploying OSGI Bundle " + sanitizeFileNameJarName);
        throw new BadRequestException("Error undeploying OSGI Bundle:    " + sanitizeFileNameJarName);
    }

    private String findBundleByJar(final String sanitizeFileNameJarName) {

        final Bundle[] bundles = OSGIUtil.getInstance().getBundles();
        for (final Bundle bundle : bundles) {
            final String bundleLocation = bundle.getLocation();
            final String separator = bundleLocation.contains(StringPool.FORWARD_SLASH)?
                    StringPool.FORWARD_SLASH: File.separator;
            final String jarFile = bundleLocation.contains(separator)
                    ? bundleLocation.substring(bundleLocation.lastIndexOf(separator) + 1)
                    : "System";

            if (jarFile.equals(sanitizeFileNameJarName)) {
                return String.valueOf(bundle.getBundleId());
            }
        }
        return null;
    }

    /**
     * Does the deploy of a bundle/jar
     *
     * @param request
     * @param response
     * @param jarName
     * @throws BundleException
     * @throws IOException
     * @return ResponseEntityStringView
     */
    @PUT
    @Path("/jar/{jar}/_deploy")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(summary = "deploys bundle",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityStringView.class))),
                    @ApiResponse(responseCode = "404", description = "Bundle not found"),
                    @ApiResponse(responseCode = "400", description = "Error loading OSGI Bundle"),
            })
    public final ResponseEntityStringView deploy(@Context final HttpServletRequest request,
                                   @Context final HttpServletResponse response,
                                   @PathParam ("jar") final String jarName) throws  IOException {

        checkUserPermissions(request, response, DYNAMIC_PLUGINS);

        Logger.debug(this, ()->"Deploying OSGI jar " + jarName);
        final String loadPath       = OSGIUtil.getInstance().getFelixDeployPath();
        final String undeployedPath = OSGIUtil.getInstance().getFelixUndeployPath();
        final String sanitizedJarName = com.dotmarketing.util.FileUtil.sanitizeFileName(jarName);
        final File from = new File(undeployedPath + File.separator + sanitizedJarName);
        final File to = new File(loadPath + File.separator + sanitizedJarName);

        if (to.getCanonicalPath().startsWith(loadPath) && from.getCanonicalPath().startsWith(undeployedPath) && from.exists()) {

            if (from.renameTo(to)) {
                final String responseText = String.format("OSGI Bundle  %s Loaded", jarName);
                Logger.info(this, responseText);
                return new ResponseEntityStringView(responseText);
            }
        }

        final String responseText = String.format("Error Loading OSGI Bundle  %s ", jarName);
        Logger.error(this, responseText);
        throw new BadRequestException(responseText);
    }

    /**
     * Does the stop of a bundle/jar
     *
     * @param request
     * @param response
     * @param jarName
     * @return ResponseEntityStringView
     * @throws BundleException
     * @throws IOException
     */
    @PUT
    @Path("/jar/{jar}/_stop")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(summary = "stops bundle",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityStringView.class))),
                    @ApiResponse(responseCode = "404", description = "Bundle not found"),
                    @ApiResponse(responseCode = "400", description = "Can not stop system bundle")
            })
    public final ResponseEntityStringView stop(@Context final HttpServletRequest request,
                                 @Context final HttpServletResponse response,
                                 @PathParam ("jar") final String jarName) throws BundleException {

        checkUserPermissions(request, response, DYNAMIC_PLUGINS);

        Logger.debug(this, ()->"Stopping OSGI jar " + jarName);
        final String sanitizeFileNameJarName = com.dotmarketing.util.FileUtil.sanitizeFileName(jarName);
        final String bundleId = findBundleByJar(sanitizeFileNameJarName);
        final Bundle bundle = findBundleOrThrowNotExist(bundleId);
        final String bundleLocation = bundle.getLocation();
        if (isSystemBundle(bundleLocation)) {
            throw new BadRequestException("Can not stop system bundle: " + jarName);
        }
        bundle.stop();
        final String responseText = String.format("OSGI Bundle %s Stopped", jarName);
        Logger.debug(this, responseText);
        removeReferences();
        return new ResponseEntityStringView(responseText);
    }

    private boolean isSystemBundle (final String bundleLocation) {
        return bundleLocation.contains("felix/bundle") || bundleLocation.contains("System Bundle");
    }

    /**
     * Does the Start of a bundle
     * @param request
     * @param response
     * @param jarName
     * @return ResponseEntityStringView
     * @throws BundleException
     * @throws IOException
     */
    @PUT
    @Path("/jar/{jar}/_start")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(summary = "starts bundle",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityStringView.class))),
                    @ApiResponse(responseCode = "404", description = "Bundle not found"),
                    @ApiResponse(responseCode = "400", description = "Can not stop system bundle")
            })
    public final ResponseEntityStringView start(@Context final HttpServletRequest request,
                               @Context final HttpServletResponse response,
                               @PathParam ("jar") final String jarName) throws BundleException {

        checkUserPermissions(request, response, DYNAMIC_PLUGINS);

        Logger.debug(this, ()->"Starting OSGI jar " + jarName);
        final String sanitizeFileNameJarName = com.dotmarketing.util.FileUtil.sanitizeFileName(jarName);
        final String bundleId = findBundleByJar(sanitizeFileNameJarName);
        final Bundle bundle = findBundleOrThrowNotExist(bundleId);
        final String bundleLocation = bundle.getLocation();
        if (isSystemBundle(bundleLocation)) {
            throw new BadRequestException("Can not stop system bundle: " + jarName);
        }
        bundle.start();
        final String responseText = String.format("OSGI Bundle %s started", jarName);
        Logger.debug(this, responseText);
        return new ResponseEntityStringView(responseText);
    }

    /**
     * Returns the packages inside the <strong>osgi-extra.conf</strong> file, those packages are the value
     * for the OSGI configuration property <strong>org.osgi.framework.system.packages.extra</strong>.
     *
     * @param request
     * @param response
     * @return ResponseEntityStringView
     */
    @GET
    @Path ("/extra-packages")
    @Produces (MediaType.APPLICATION_JSON)
    @Operation(summary = "get extra packages",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityStringView.class)))
            })
    public ResponseEntityStringView getExtraPackages (@Context HttpServletRequest request,
                                      @Context final HttpServletResponse response) {

        checkUserPermissions(request, response, DYNAMIC_PLUGINS);

        Logger.debug(this, ()-> "Getting extra packages");
        return new ResponseEntityStringView(OSGIUtil.getInstance().getExtraOSGIPackages());
    }

    /**
     * Overrides the content of the <strong>osgi-extra.conf</strong> file
     *
     * @param request
     * @param response
     * @param extraPackagesForm
     * @return ResponseEntityStringView
     */
    @PUT
    @Path("/extra-packages")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(summary = "modify extra packages",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityStringView.class)))
            })
    public final ResponseEntityStringView modifyExtraPackages(@Context final HttpServletRequest request,
                                @Context final HttpServletResponse response,
                                final ExtraPackagesForm extraPackagesForm)  {

        checkUserPermissions(request, response, DYNAMIC_PLUGINS);

        final String extraPackages = extraPackagesForm.getPackages();

        Logger.debug(this, ()->"Modifying the extra packages OSGI jar " + extraPackages);

        final boolean testDryRun = ConversionUtils.toBoolean(request.getParameter("testDryRun"), true);
        OSGIUtil.getInstance().writeOsgiExtras(extraPackages, testDryRun);
        return new ResponseEntityStringView("OSGI Extra Packages Saved");
    }


    /**
     * Restarts the OSGI framework
     *
     * @param request
     * @param response
     * @return ResponseEntityStringView
     * @throws BundleException
     * @throws IOException
     */
    @PUT
    @Path("/_restart")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(summary = "restarts the OSGI framework",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityStringView.class)))
            })
    public final ResponseEntityStringView restart(@Context final HttpServletRequest request,
                                              @Context final HttpServletResponse response) {

        checkUserPermissions(request, response, DYNAMIC_PLUGINS);

        Logger.debug(this, ()->"Restarting the framework");
        OSGIUtil.getInstance().restartOsgiClusterWide();
        return new ResponseEntityStringView("OSGI Framework Restarted");
    }

    /*
     * Removes portlets and actionlets references
     */
    private void removeReferences() {

        //Remove Portlets in the list
        OSGIUtil.getInstance().portletIDsStopped.forEach(p -> APILocator.getPortletAPI().deletePortlet(p));
        Logger.info( this, "Portlets Removed: " + OSGIUtil.getInstance().portletIDsStopped.toString());

        //Remove Actionlets in the list
        OSGIUtil.getInstance().actionletsStopped.forEach(p -> OSGIUtil.getInstance().getWorkflowOsgiService().removeActionlet(p));
        Logger.info( this, "Actionlets Removed: " + OSGIUtil.getInstance().actionletsStopped.toString());

        //Cleanup lists
        OSGIUtil.getInstance().portletIDsStopped.clear();
        OSGIUtil.getInstance().actionletsStopped.clear();
    }
    
    /**
     * This endpoint receives multiples jar files in order to upload to the osgi.
     *
     * @param request
     * @param response
     * @param multipart
     * @return Response
     * @throws IOException
     */
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Upload bundles to the OSGI framework",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityStringView.class))),
                    @ApiResponse(responseCode = "403", description = "Can not access the upload folder or invalid OSGI Upload request"),
            })
    public final Response uploadBundles(@Context final HttpServletRequest request,
                                              @Context final HttpServletResponse response,
                                              final FormDataMultiPart multipart) throws IOException {

        checkUserPermissions(request, response, PortletID.DYNAMIC_PLUGINS.toString());

        Logger.debug(this, "Uploading new bundles...");

        final List<File> files = new ArrayList<>();
        // the first part could be temp ids, the second one would be the files
        final Tuple2<Map<String,Object>, List<File>> multiPartContent =
                this.multiPartUtils.getBodyMapAndBinariesFromMultipart(multipart);

        final List<File> requestFiles = multiPartContent._2();
        files.addAll(requestFiles.stream()
                .filter(file -> file.getName().endsWith(".jar"))
                .collect(Collectors.toList()));

        final String felixUploadFolder = OSGIUtil.getInstance().getFelixUploadPath();
        final File felixFolder = new File(felixUploadFolder);
        if (!felixFolder.exists() || !felixFolder.canWrite()) {

            return Response.status(403).entity(new ResponseEntityView<>(
                    "Can not access the upload folder")).build();
        }

        for (final File uploadedBundleFile : files) {

            final File osgiJar = new File(felixUploadFolder + File.separator + uploadedBundleFile.getName());

            if (!osgiJar.getCanonicalPath().startsWith(felixFolder.getCanonicalPath())) {

                final String errorMsg = "Invalid OSGI Upload request:" +
                        osgiJar.getCanonicalPath() + " from:" +request.getRemoteHost();
                SecurityLogger.logInfo(this.getClass(),  errorMsg);
                return Response.status(403).entity(new ResponseEntityView<>(errorMsg)).build();
            }

            Logger.debug(this, "Coping the file: " + uploadedBundleFile.getName() +
                    " to the osgi upload folder.");

            try (final OutputStream out = Files.newOutputStream(osgiJar.toPath());
                    final InputStream in = Files.newInputStream(uploadedBundleFile.toPath())) {

                IOUtils.copyLarge(in, out);
            }
        }

        // since we already upload jar, we would like to try to run the upload folder when the
        // refresh strategy is running by schedule job
        OSGIUtil.getInstance().checkUploadFolder();

        return Response.ok(new ResponseEntityView<>(
                files.stream().map(File::getName).collect(Collectors.toSet())))
                .build();
    }

    /**
     * Returns available plugins to load
     *
     * @param request
     * @param response
     * @return ResponseEntityStringView
     */
    @GET
    @Path ("/available-plugins")
    @Produces (MediaType.APPLICATION_JSON)
    @Operation(summary = "get available plugins",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResponseEntityListView.class)))
            })
    public ResponseEntityListView<String> getAvailablePlugis (@Context HttpServletRequest request,
                                                      @Context final HttpServletResponse response) {

        checkUserPermissions(request, response, DYNAMIC_PLUGINS);

        Logger.debug(this, ()-> "Getting available plugins");

        final String path = OSGIUtil.getInstance().getFelixUndeployPath();
        final File undeployDirectory = new File(path);

        return new ResponseEntityListView<>(
                Arrays.stream(undeployDirectory.list()).filter(f -> f.toLowerCase().endsWith(".jar")).collect(Collectors.toList())
        );
    }

}
