package com.dotcms.rest.api.v1.experiments;

import com.dotcms.experiments.business.ConfigExperimentUtil;
import com.dotcms.experiments.business.ExperimentFilter;
import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotcms.experiments.business.ExperimentsAPI;
import com.dotcms.experiments.business.ExperimentsAPI.Health;
import com.dotcms.experiments.business.result.ExperimentResults;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.experiments.model.TargetingCondition;
import com.dotcms.rest.api.v1.analytics.event.EventAnalyticsProxyHelper;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.PATCH;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

import com.google.common.annotations.VisibleForTesting;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

import org.glassfish.jersey.server.JSONP;

/**
 * REST API for {@link Experiment}s
 *
 * Includes all the CRUD operations
 */
@Path("/v1/experiments")
@Tag(name = "Experiment")
public class ExperimentsResource {

    private final WebResource webResource;
    private final ExperimentsAPI experimentsAPI;

    private static final String HEALTH_KEY = "health";
    private static final String FEATURE_DISABLED_CODE = "FEATURE_DISABLED";
    private static final String FEATURE_DISABLED_MSG =
            "The Experiments feature is currently disabled. Please contact dotCMS to enable it.";
    private static final String ANALYTICS_NOT_CONFIGURED_CODE = "ANALYTICS_NOT_CONFIGURED";
    private static final String ANALYTICS_NOT_CONFIGURED_MSG =
            "Analytics is not configured for this site. Please configure the Analytics App to use this feature.";

    public ExperimentsResource() {
        webResource =  new WebResource();
        experimentsAPI = APILocator.getExperimentsAPI();
    }

    @VisibleForTesting
    ExperimentsResource(final WebResource webResource, final ExperimentsAPI experimentsAPI) {
        this.webResource = webResource;
        this.experimentsAPI = experimentsAPI;
    }

    /**
     * Creates a new Experiment with the information provided in JSON format and mapped to the {@link ExperimentForm}
     * <p>
     * An Experiment can be created with as minimum as a name and a description
     *
     * Returns the created Experiment.
     */
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntitySingleExperimentView create(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final ExperimentForm experimentForm) throws DotDataException, DotSecurityException {
        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();
        final Experiment experiment = createExperimentFromForm(experimentForm, user);
        final Experiment persistedExperiment = experimentsAPI.save(experiment, user);
        return new ResponseEntitySingleExperimentView(persistedExperiment);
    }

    private Experiment createExperimentFromForm(final ExperimentForm experimentForm,
            final User user) {
        final Experiment.Builder builder = Experiment.builder();

        builder.pageId(experimentForm.getPageId()).name(experimentForm.getName())
                .createdBy(user.getUserId())
                .lastModifiedBy(user.getUserId())
                .trafficAllocation(experimentForm.getTrafficAllocation()>-1
                        ? experimentForm.getTrafficAllocation()
                        : 100);

        if(experimentForm.getDescription()!=null) {
            builder.description(experimentForm.getDescription());
        }

        if(experimentForm.getTrafficProportion()!=null) {
            builder.trafficProportion(experimentForm.getTrafficProportion());
        }

        if(experimentForm.getGoals()!=null) {
            builder.goals(experimentForm.getGoals());
        }

        if(experimentForm.getScheduling()!=null) {
            builder.scheduling(experimentForm.getScheduling());
        }

        return builder.build();
    }

    /**
     * Updates an existing experiment accepting partial updates (PATCH). This means it is not needed to send
     * the entire Experiment information but only what it is desired to update only. The rest
     * of the information will remain as previously persisted.
     *
     * Returns the updated version of the Experiment.
     */
    @PATCH
    @Path("/{experimentId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntitySingleExperimentView update(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("experimentId") final String experimentId,
            final ExperimentForm experimentForm) throws DotDataException, DotSecurityException {
        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();

        final Optional<Experiment> experimentToUpdate =  experimentsAPI.find(experimentId, user);

        if(experimentToUpdate.isEmpty()) {
            throw new NotFoundException("Experiment with id: " + experimentId + " not found.");
        }

        final Experiment patchedExperiment = patchExperiment(experimentToUpdate.get(), experimentForm,
                user);
        final Experiment persistedExperiment = experimentsAPI.save(patchedExperiment, user);
        return new ResponseEntitySingleExperimentView(persistedExperiment);
    }

    /**
     * Archives an Experiment. Gated on {@code FEATURE_FLAG_EXPERIMENTS}: when {@code false},
     * returns {@code 403 FEATURE_DISABLED}. Archiving applies for experiments with already
     * collected data where deletion is not wanted.
     */
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Experiment archived successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityExperimentView.class))),
            @ApiResponse(responseCode = "403", description = "Feature disabled (FEATURE_DISABLED) — flag=false",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path("/{experimentId}/_archive")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response archive(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("experimentId") final String experimentId) throws DotDataException, DotSecurityException {
        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();
        if (!ConfigExperimentUtil.INSTANCE.isExperimentEnabled()) {
            return featureDisabledResponse();
        }
        final Experiment archivedExperiment = experimentsAPI.archive(experimentId, user);
        return Response.ok(
                new ResponseEntityExperimentView(Collections.singletonList(archivedExperiment)))
                .build();
    }

    /**
     * Deletes an Experiment. Deletion can only be performed to Experiments in
     * {@link com.dotcms.experiments.model.AbstractExperiment.Status#DRAFT} state.
     */
    @DELETE
    @Path("/{experimentId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityView<String> delete(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("experimentId") final String experimentId) throws DotDataException, DotSecurityException {
        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();
        experimentsAPI.delete(experimentId, user);
        return new ResponseEntityView<>("Experiment deleted");
    }

    /**
     * Returns an {@link Experiment}s by Id
     */
    @GET
    @NoCache
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntitySingleExperimentView get(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response, @PathParam("id") String id
    ) throws DotDataException, DotSecurityException {
        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();

        return experimentsAPI.find(id, user)
                .map(experiment -> new ResponseEntitySingleExperimentView(experiment))
                .orElseThrow(() -> new NotFoundException("Experiment with id: " + id + " not found."));
    }

    /**
     * Returns a list of {@link Experiment}s optionally filtered by pageId, name or status.
     */
    @GET
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityExperimentView list(final @QueryParam("pageId") String pageId,
            final @QueryParam("name") String name,
            final @QueryParam("status") Set<Status> statuses,
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response
            ) throws DotDataException, DotSecurityException {
        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();
        final ExperimentFilter.Builder filterBuilder = ExperimentFilter.builder();

        if(UtilMethods.isSet(pageId)) {
            filterBuilder.pageId(pageId);
        }

        if(UtilMethods.isSet(name)) {
            filterBuilder.name(name);
        }

        if(UtilMethods.isSet(statuses)) {
            filterBuilder.statuses(statuses);
        }

        final List<Experiment> experiments = experimentsAPI.list(filterBuilder.build(), user);
        return new ResponseEntityExperimentView(experiments);
    }

    /**
     * Deletes the primary Goal.
     */
    @DELETE
    @Path("/{experimentId}/goals/primary")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntitySingleExperimentView deleteGoal(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("experimentId") final String experimentId) throws DotDataException, DotSecurityException {
        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();
        final Optional<Experiment> existingExperiment =  experimentsAPI.find(experimentId, user);

        if(existingExperiment.isEmpty()) {
            throw new NotFoundException("Experiment with id: " + experimentId + " not found.");
        }

        final Experiment experimentNoGoal = existingExperiment.get().withGoals(Optional.empty());

        final Experiment savedExperiment = experimentsAPI.save(experimentNoGoal, user);
        return new ResponseEntitySingleExperimentView(savedExperiment);
    }

    /**
     * Starts an {@link Experiment}. In addition to the standard validation, this endpoint applies
     * feature-gate checks when {@code FEATURE_FLAG_EXPERIMENTS=false} (limited mode):
     * <ul>
     *   <li>The Analytics App must be configured for the site ({@code 503} otherwise).
     *   <li>Scheduling with a future {@code startDate} is not allowed ({@code 403 FEATURE_DISABLED}).
     *   <li>The free experiment slot must be available ({@code 403 FEATURE_DISABLED} if occupied).
     *   <li>Duration is capped at {@link ExperimentLimitedModeGate#MAX_DAYS} days ({@code 400} if exceeded).
     *   <li>When no {@code endDate} is set, it is automatically set to
     *       {@link ExperimentLimitedModeGate#MAX_DAYS} days from now.
     * </ul>
     * When {@code FEATURE_FLAG_EXPERIMENTS=true} only the App-configuration check applies.
     */
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Experiment started successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntitySingleExperimentView.class))),
            @ApiResponse(responseCode = "400", description = "Duration exceeds the 10-day limited-mode cap",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Feature disabled (FEATURE_DISABLED) — flag=false and slot occupied or scheduled start",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "503", description = "Analytics App not configured for this site (ANALYTICS_NOT_CONFIGURED)",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @Path("/{experimentId}/_start")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response start(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("experimentId") final String experimentId)
            throws DotDataException, DotSecurityException, SystemException, PortalException {

        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();
        final Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);

        // Gate 1: Analytics App must be configured
        if (!ContentAnalyticsUtil.isAppConfigured(host)) {
            return analyticsNotConfiguredResponse();
        }

        // Gate 2: limited-mode checks (flag=false)
        if (!ConfigExperimentUtil.INSTANCE.isExperimentEnabled()) {
            final Optional<ExperimentLimitedModeGate.Rejection> rejection =
                    ExperimentLimitedModeGate.evaluate(experimentId, user);
            if (rejection.isPresent()) {
                return switch (rejection.get()) {
                    case FEATURE_DISABLED -> featureDisabledResponse();
                    case DURATION_EXCEEDED -> durationExceededResponse();
                };
            }
        }

        final Experiment startedExperiment = experimentsAPI.start(experimentId, user);
        return Response.ok(new ResponseEntitySingleExperimentView(startedExperiment)).build();
    }

    /**
     * Ends an already started {@link Experiment}. Gated on {@code FEATURE_FLAG_EXPERIMENTS}:
     * when {@code false}, returns {@code 403 FEATURE_DISABLED}. App configuration has no effect
     * on this endpoint because {@code _end} does not interact with the analytics backend.
     */
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Experiment ended successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntitySingleExperimentView.class))),
            @ApiResponse(responseCode = "403", description = "Feature disabled (FEATURE_DISABLED) — flag=false",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @Path("/{experimentId}/_end")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response end(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("experimentId") final String experimentId) throws DotDataException, DotSecurityException {
        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();
        if (!ConfigExperimentUtil.INSTANCE.isExperimentEnabled()) {
            return featureDisabledResponse();
        }
        final Experiment endedExperiment = experimentsAPI.end(experimentId, user);
        return Response.ok(new ResponseEntitySingleExperimentView(endedExperiment)).build();
    }

    /**
     * Cancels the future execution of a Scheduled {@link Experiment} or the current execution of a
     * Running {@link Experiment}. Gated on {@code FEATURE_FLAG_EXPERIMENTS}: when {@code false},
     * returns {@code 403 FEATURE_DISABLED}.
     */
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Experiment cancelled successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntitySingleExperimentView.class))),
            @ApiResponse(responseCode = "403", description = "Feature disabled (FEATURE_DISABLED) — flag=false",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @Path("/scheduled/{experimentId}/_cancel")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response cancel(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("experimentId") final String experimentId) throws DotDataException, DotSecurityException {
        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();
        if (!ConfigExperimentUtil.INSTANCE.isExperimentEnabled()) {
            return featureDisabledResponse();
        }
        final Experiment cancelledExperiment = experimentsAPI.cancel(experimentId, user);
        return Response.ok(new ResponseEntitySingleExperimentView(cancelledExperiment)).build();
    }

    /**
     * Adds a new {@link com.dotcms.variant.model.Variant} to the {@link Experiment}
     *
     */
    @POST
    @Path("/{experimentId}/variants")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntitySingleExperimentView addVariant(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("experimentId") final String experimentId,
            AddVariantForm addVariantForm) throws DotDataException, DotSecurityException {

        DotPreconditions.isTrue(addVariantForm!=null, ()->"Missing Variant name",
                IllegalArgumentException.class);

        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();
        final Experiment updatedExperiment =  experimentsAPI.addVariant(experimentId,
                addVariantForm.getDescription(), user);
        return new ResponseEntitySingleExperimentView(updatedExperiment);
    }

    /**
     * Deletes a new {@link com.dotcms.variant.model.Variant} from the {@link Experiment}
     *
     */
    @DELETE
    @Path("/{experimentId}/variants/{name}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntitySingleExperimentView deleteVariant(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("experimentId") final String experimentId,
            @PathParam("name") final String variantName) throws DotDataException, DotSecurityException {
        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();
        final Experiment updatedExperiment =  experimentsAPI.deleteVariant(experimentId, variantName, user);
        return new ResponseEntitySingleExperimentView(updatedExperiment);
    }

    /**
     * Updates an existing experiment accepting partial updates (PATCH). This means it is not needed to send
     * the entire Experiment information but only what it is desired to update only. The rest
     * of the information will remain as previously persisted.
     *
     * Returns the updated version of the Experiment.
     */
    @PUT
    @Path("/{experimentId}/variants/{name}")
    @JSONP
    @NoCache
    @Consumes({MediaType.APPLICATION_JSON, "application/javascript"})
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntitySingleExperimentView updateVariant(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("experimentId") final String experimentId,
            @PathParam("name") final String variantName,
            ExperimentVariantForm experimentVariantForm) throws DotDataException, DotSecurityException {
        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();

        final Optional<Experiment> experimentToUpdate =  experimentsAPI.find(experimentId, user);

        if(experimentToUpdate.isEmpty()) {
            throw new NotFoundException("Experiment with id: " + experimentId + " not found.");
        }

        final Experiment persistedExperiment = experimentsAPI.editVariantDescription(experimentId,
                variantName, experimentVariantForm.getDescription(), user);
        return new ResponseEntitySingleExperimentView(persistedExperiment);
    }

    /**
     * Promotes a Variant to become the DEFAULT variant of the Page of the Experiment
     * Returns the updated version of the Experiment.
     */
    @PUT
    @Path("/{experimentId}/variants/{name}/_promote")
    @JSONP
    @NoCache
    @Consumes({MediaType.APPLICATION_JSON, "application/javascript"})
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntitySingleExperimentView promoteVariant(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("experimentId") final String experimentId,
            @PathParam("name") final String variantName,
            ExperimentVariantForm experimentVariantForm) throws DotDataException, DotSecurityException {
        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();

        final Optional<Experiment> experimentToUpdate =  experimentsAPI.find(experimentId, user);

        if(experimentToUpdate.isEmpty()) {
            throw new NotFoundException("Experiment with id: " + experimentId + " not found.");
        }

        final Experiment persistedExperiment = experimentsAPI.promoteVariant(experimentId,
                variantName, user);
        return new ResponseEntitySingleExperimentView(persistedExperiment);
    }

    /**
     * Deletes the {@link TargetingCondition} with the given id from the {@link Experiment} with the given experimentId
     *
     */
    @DELETE
    @Path("/{experimentId}/targetingConditions/{id}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntitySingleExperimentView deleteTargetingCondition(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @PathParam("experimentId") final String experimentId,
            @PathParam("id") final String conditionId) throws DotDataException, DotSecurityException {
        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();
        final Experiment updatedExperiment =  experimentsAPI
                .deleteTargetingCondition(experimentId, conditionId, user);
        return new ResponseEntitySingleExperimentView(updatedExperiment);
    }

    /**
     * Return if the current user should be included into a RUNNING {@link Experiment}:
     *
     * - First it checks it the {@link Experiment#targetingConditions()} is valid for the user or current
     * {@link HttpServletRequest}.
     * - Then it use the {@link Experiment#trafficAllocation()} to know if the user should go into the
     * {@link Experiment}.
     * - Finally it assign a {@link com.dotcms.experiments.model.ExperimentVariant} according to
     * {@link com.dotcms.experiments.model.ExperimentVariant#weight()}
     *
     * If exists more than one {@link Experiment} RUNNING it try to get the user into any of them
     * one by one if finally the user is not going into any experiment then it returned a
     * {@link com.dotcms.experiments.business.web.ExperimentWebAPI#NONE_EXPERIMENT}
     *
     * Also, you can include a list of excluded Experiments's id on the request payload as follows:
     *
     * <code>
     * {
     *     "exclude": ["1234", "5678"]
     * }
     * </code>
     *
     * it means that the Experiments '1234' and '5678' are not going to be taken account so they are going to be
     * excluded from the Running Experiment list to check.
     *
     * Also on the response a list of excludedExperimentIdsEnded it is a List of the Experiment excluded that already
     * are ended, so in the before Example if '1234' is ended then the response is going to include:
     *
     * {
     *     ...
     *     excludedExperimentIdsEnded: ['1234']
     * }
     *
     * @see com.dotcms.experiments.business.web.ExperimentWebAPI#isUserIncluded(HttpServletRequest, HttpServletResponse, List)
     */
    @POST
    @NoCache
    @Path("/isUserIncluded")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public ResponseEntityExperimentSelectedView isUserIncluded(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final ExcludedExperimentListForm excludedExperimentListForm
    ) throws DotDataException, DotSecurityException {

        return new ResponseEntityExperimentSelectedView(
                WebAPILocator.getExperimentWebAPI().isUserIncluded(request, response,
                        UtilMethods.isSet(excludedExperimentListForm) ? excludedExperimentListForm.getExclude()
                                : Collections.emptyList())
        );
    }

    /**
     * Returns the partial or total results of a {@link Experiment}.
     *
     * <p>Gated on Analytics App configuration: results depend on the analytics backend and
     * cannot be retrieved when the App is absent for the site. Returns
     * {@code 503 ANALYTICS_NOT_CONFIGURED} when the App is not configured.
     */
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Experiment results retrieved",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityExperimentResults.class))),
            @ApiResponse(responseCode = "503", description = "Analytics App not configured for this site",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @NoCache
    @Path("/{id}/results")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public Response getResult(@Context final HttpServletRequest request,
                              @Context final HttpServletResponse response,
                              @PathParam("id") String id)
        throws DotDataException, DotSecurityException, SystemException, PortalException {

        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();
        final Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);

        if (!ContentAnalyticsUtil.isAppConfigured(host)) {
            return analyticsNotConfiguredResponse();
        }

        final Experiment experiment = experimentsAPI.find(id, user)
                .orElseThrow(
                        () -> new NotFoundException("Experiment with id: " + id + " not found."));

        final ExperimentResults experimentResults = APILocator.getExperimentsAPI().getResults(experiment, user);

        return Response.ok(new ResponseEntityExperimentResults(experimentResults)).build();
    }

    /**
     * Healthcheck for the Experiments / Analytics configuration.
     *
     * <p>Returns an {@link ExperimentsHealthView} with:
     * <ul>
     *   <li>{@code health} — evaluated by {@link ContentAnalyticsUtil#resolveAnalyticsHealth}.
     *   <li>{@link ExperimentsHealthView.Tier} — {@code FULL} when {@code FEATURE_FLAG_EXPERIMENTS=true},
     *       {@code LIMITED} when {@code false}. Reflects flag state only; App configuration
     *       does not affect this field.
     *   <li>{@code freeExperimentUsed} — present only when {@code tier=LIMITED}: {@code true}
     *       if any experiment is in {@code {RUNNING, SCHEDULED, ENDED}}.
     *   <li>{@code warning} — {@code "ANALYTICS_DISABLED"} when the App is not configured;
     *       absent otherwise.
     * </ul>
     */
    @GET
    @NoCache
    @Path("/health")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityView<ExperimentsHealthView> healthcheck(
            @Context final HttpServletRequest request,
            @Context final HttpServletResponse response)
            throws DotDataException, DotSecurityException, SystemException, PortalException {

        getInitData(request, response);
        final Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);

        final boolean fullMode = ConfigExperimentUtil.INSTANCE.isExperimentEnabled();
        final ExperimentsHealthView.Tier tier =
                fullMode ? ExperimentsHealthView.Tier.FULL : ExperimentsHealthView.Tier.LIMITED;
        final Health health = ContentAnalyticsUtil.resolveAnalyticsHealth(host);
        final boolean appConfigured = ContentAnalyticsUtil.isAppConfigured(host);
        final ExperimentsHealthView.Warning warning =
                appConfigured ? null : ExperimentsHealthView.Warning.ANALYTICS_DISABLED;
        final Boolean freeExperimentUsed = fullMode ? null : experimentsAPI.isFreeSlotUsed();

        return new ResponseEntityView<>(ExperimentsHealthView.builder()
                .health(health)
                .tier(tier)
                .freeExperimentUsed(freeExperimentUsed)
                .warning(warning)
                .build());
    }

    private Experiment patchExperiment(final Experiment experimentToUpdate,
            final ExperimentForm experimentForm, final User user) {

        final Experiment.Builder builder = Experiment.builder().from(experimentToUpdate);

        if(experimentForm.getPageId()!=null) {
            builder.pageId(experimentForm.getPageId());
        }

        if(experimentForm.getName()!=null) {
            builder.name(experimentForm.getName());
        }

        if(experimentForm.getDescription()!=null) {
            builder.description(experimentForm.getDescription());
        }

        if(experimentForm.getTrafficAllocation()>0) {
            builder.trafficAllocation(experimentForm.getTrafficAllocation());
        }

        if(experimentForm.getTrafficProportion()!=null) {
            builder.trafficProportion(experimentForm.getTrafficProportion());
        }

        if(experimentForm.getScheduling()!=null) {
            builder.scheduling(experimentForm.getScheduling());
        }

        if(experimentForm.getGoals()!=null) {
            builder.goals(experimentForm.getGoals());
        }

        if(experimentForm.getTargetingConditions()!=null) {
            builder.targetingConditions(experimentForm.getTargetingConditions());
        }

        if(experimentForm.getLookbackWindow()>-1) {
            builder.lookBackWindowExpireTime(experimentForm.getLookbackWindow());
        }

        return builder.build();
    }

    private InitDataObject getInitData(@Context HttpServletRequest request,
            @Context HttpServletResponse response) {
        return new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .init();
    }

    /**
     * Builds a {@code 403 FEATURE_DISABLED} gate response used when {@code FEATURE_FLAG_EXPERIMENTS=false}
     * prevents an operation.
     */
    private static Response featureDisabledResponse() {
        return Response.status(Response.Status.FORBIDDEN)
                .entity(new ResponseEntityView<>(
                        List.of(new com.dotcms.rest.ErrorEntity(
                                FEATURE_DISABLED_CODE, FEATURE_DISABLED_MSG))))
                .build();
    }

    /**
     * Builds a {@code 400 Bad Request} response used when the experiment duration exceeds
     * the limited-mode cap defined in {@link ExperimentLimitedModeGate#MAX_DAYS}.
     */
    private static Response durationExceededResponse() {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ResponseEntityView<>(
                        List.of(new com.dotcms.rest.ErrorEntity(
                                "DURATION_EXCEEDED",
                                "Limited mode allows experiments of at most "
                                        + ExperimentLimitedModeGate.MAX_DAYS
                                        + " days. Please shorten the duration."))))
                .build();
    }

    /**
     * Builds a {@code 503 Service Unavailable} gate response used when the Analytics App is
     * not configured for the site.
     */
    private static Response analyticsNotConfiguredResponse() {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(new ResponseEntityView<>(
                        List.of(new com.dotcms.rest.ErrorEntity(
                                ANALYTICS_NOT_CONFIGURED_CODE, ANALYTICS_NOT_CONFIGURED_MSG))))
                .build();
    }

}
