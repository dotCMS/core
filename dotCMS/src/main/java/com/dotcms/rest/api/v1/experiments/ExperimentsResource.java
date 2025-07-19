package com.dotcms.rest.api.v1.experiments;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.experiments.business.ExperimentFilter;
import com.dotcms.experiments.business.ExperimentsAPI;
import com.dotcms.experiments.business.ExperimentsAPI.Health;
import com.dotcms.experiments.business.result.ExperimentResults;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.experiments.model.TargetingCondition;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.jitsu.EventLogRunnable;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.PATCH;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Try;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

import org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.annotation.SwaggerCompliant;

/**
 * REST API for {@link Experiment}s
 *
 * Includes all the CRUD operations
 */
@SwaggerCompliant(value = "Rules engine and business logic APIs", batch = 6)
@Path("/v1/experiments")
@Tag(name = "Experiments")
public class ExperimentsResource {

    private final WebResource webResource;
    private final ExperimentsAPI experimentsAPI;

    private static final String HEALTH_KEY = "health";

    public ExperimentsResource() {
        webResource =  new WebResource();
        experimentsAPI = APILocator.getExperimentsAPI();
    }

    /**
     * Creates a new Experiment with the information provided in JSON format and mapped to the {@link ExperimentForm}
     * <p>
     * An Experiment can be created with as minimum as a name and a description
     *
     * Returns the created Experiment.
     */
    @Operation(
        summary = "Create a new experiment",
        description = "Creates a new A/B test experiment with the provided configuration. An experiment can be created with minimal information such as name and description, and additional settings can be configured later."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Experiment created successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntitySingleExperimentView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid experiment data or missing required fields",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to create experiments",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseEntitySingleExperimentView create(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(description = "Experiment configuration form with name, description, and optional settings",
                        required = true,
                        content = @Content(schema = @Schema(implementation = ExperimentForm.class)))
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
    @Operation(
        summary = "Update an experiment",
        description = "Performs partial updates (PATCH) on an existing experiment. Only the provided fields will be updated, while other experiment properties remain unchanged."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Experiment updated successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntitySingleExperimentView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid experiment data or update parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to update experiment",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Experiment not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @PATCH
    @Path("/{experimentId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseEntitySingleExperimentView update(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Unique identifier of the experiment to update", required = true)
            @PathParam("experimentId") final String experimentId,
            @RequestBody(description = "Partial experiment update form with fields to modify",
                        required = true,
                        content = @Content(schema = @Schema(implementation = ExperimentForm.class)))
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
     * Archives an Experiment. Archiving operation applies for experiments to whom there's already
     * data collected and deletion is not wanted.
     *
     * Returns the archived version of the Experiment.
     */
    @Operation(
        summary = "Archive an experiment",
        description = "Archives an experiment to preserve collected data while removing it from active use. Ideal for experiments with valuable historical data that should not be deleted."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Experiment archived successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityExperimentView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid experiment ID or experiment cannot be archived",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to archive experiment",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Experiment not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path("/{experimentId}/_archive")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityExperimentView archive(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Unique identifier of the experiment to archive", required = true)
            @PathParam("experimentId") final String experimentId) throws DotDataException, DotSecurityException {
        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();
        final Experiment archivedExperiment =  experimentsAPI.archive(experimentId, user);
        return new ResponseEntityExperimentView(Collections.singletonList(archivedExperiment));
    }

    /**
     * Deletes an Experiment. Deletion can only be performed to Experiments in
     * {@link com.dotcms.experiments.model.AbstractExperiment.Status#DRAFT} state.
     */
    @Operation(
        summary = "Delete an experiment",
        description = "Permanently deletes an experiment. This operation can only be performed on experiments in DRAFT status. Use archive instead if you want to preserve experiment data."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Experiment deleted successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityExperimentOperationView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Experiment cannot be deleted (not in DRAFT status)",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to delete experiment",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Experiment not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @Path("/{experimentId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityExperimentOperationView delete(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Unique identifier of the experiment to delete", required = true)
            @PathParam("experimentId") final String experimentId) throws DotDataException, DotSecurityException {
        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();
        experimentsAPI.delete(experimentId, user);
        return new ResponseEntityExperimentOperationView("Experiment deleted");
    }

    /**
     * Returns an {@link Experiment}s by Id
     */
    @Operation(
        summary = "Get experiment by ID",
        description = "Retrieves a specific experiment by its unique identifier, including all configuration details, variants, and current status."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Experiment retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntitySingleExperimentView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to view experiment",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Experiment not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @NoCache
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntitySingleExperimentView get(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response, 
            @Parameter(description = "Unique identifier of the experiment to retrieve", required = true)
            @PathParam("id") String id
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
    @Operation(
        summary = "List experiments with optional filtering",
        description = "Retrieves a list of experiments with optional filtering by page ID, name, or status. Returns all experiments if no filters are provided."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Experiments list retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityExperimentView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid filter parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to list experiments",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityExperimentView list(
            @Parameter(description = "Filter experiments by page ID", required = false)
            final @QueryParam("pageId") String pageId,
            @Parameter(description = "Filter experiments by name", required = false)
            final @QueryParam("name") String name,
            @Parameter(description = "Filter experiments by status (DRAFT, RUNNING, ENDED, etc.)", required = false)
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
    @Operation(
        summary = "Delete experiment primary goal",
        description = "Removes the primary goal from an experiment. This will clear the goal configuration but preserve other experiment settings."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Primary goal deleted successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntitySingleExperimentView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid experiment ID or goal cannot be deleted",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to modify experiment",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Experiment not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @Path("/{experimentId}/goals/primary")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntitySingleExperimentView deleteGoal(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Unique identifier of the experiment to remove goal from", required = true)
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
     * Starts an {@link Experiment}. In order to start an Experiment it needs to:
     * <li>Have a {@link Status#DRAFT} status
     * <li>Have at least one Variant
     * <li>Have a primary goal set
     * <p>
     * The following considerations regarding {@link Experiment#scheduling()} are also taking place
     * when starting an Experiment:
     * <li>If no {@link Scheduling#startDate()} is provided, set it to now()
     * <li>If no {@link Scheduling#endDate()} is provided, set it to four weeks
     * <li>Unable to start if provided {@link Scheduling#startDate()} is in the past
     * <li>Unable to start if provided {@link Scheduling#endDate()} is in the past
     * <li>Unable to start if provided {@link Scheduling#endDate()} is not after provided {@link Scheduling#startDate()}
     * <li>Unable to start if difference {@link Scheduling#endDate()} is not after provided {@link Scheduling#startDate()}
     *
     */
    @Operation(
        summary = "Start an experiment",
        description = "Starts an experiment and changes its status to RUNNING. The experiment must be in DRAFT status, have at least one variant, and have a primary goal configured. Scheduling dates will be set automatically if not provided."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Experiment started successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntitySingleExperimentView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Experiment cannot be started (missing requirements or invalid scheduling)",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to start experiment",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Experiment not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @Path("/{experimentId}/_start")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntitySingleExperimentView start(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Unique identifier of the experiment to start", required = true)
            @PathParam("experimentId") final String experimentId) throws DotDataException, DotSecurityException {
        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();
        final Experiment startedExperiment = experimentsAPI.start(experimentId, user);
        return new ResponseEntitySingleExperimentView(startedExperiment);
    }

    /**
     * Ends an already started {@link Experiment}. The Experiment needs to be in
     * {@link Status#RUNNING} status to be able to end it.
     */
    @Operation(
        summary = "End a running experiment",
        description = "Ends a currently running experiment and changes its status to ENDED. The experiment must be in RUNNING status to be ended."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Experiment ended successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntitySingleExperimentView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Experiment cannot be ended (not in RUNNING status)",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to end experiment",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Experiment not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @Path("/{experimentId}/_end")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntitySingleExperimentView end(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Unique identifier of the experiment to end", required = true)
            @PathParam("experimentId") final String experimentId) throws DotDataException, DotSecurityException {
        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();
        final Experiment endedExperiment = experimentsAPI.end(experimentId, user);
        return new ResponseEntitySingleExperimentView(endedExperiment);
    }

    /**
     * Cancels the future execution of a Scheduled {@link Experiment} or the current execution of a Running
     * {@link Experiment}. The Experiment needs to be either in
     * {@link Status#SCHEDULED} or {@link Status#RUNNING} status to be able to cancel it.
     */
    @Operation(
        summary = "Cancel a scheduled or running experiment",
        description = "Cancels an experiment that is either scheduled for future execution or currently running. The experiment must be in SCHEDULED or RUNNING status to be cancelled."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Experiment cancelled successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntitySingleExperimentView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Experiment cannot be cancelled (not in SCHEDULED or RUNNING status)",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to cancel experiment",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Experiment not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @Path("/scheduled/{experimentId}/_cancel")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntitySingleExperimentView cancel(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Unique identifier of the experiment to cancel", required = true)
            @PathParam("experimentId") final String experimentId) throws DotDataException, DotSecurityException {
        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();
        final Experiment endedExperiment = experimentsAPI.cancel(experimentId, user);
        return new ResponseEntitySingleExperimentView(endedExperiment);
    }

    /**
     * Adds a new {@link com.dotcms.variant.model.Variant} to the {@link Experiment}
     *
     */
    @Operation(
        summary = "Add variant to experiment",
        description = "Adds a new variant to an existing experiment. Variants represent different versions of content to be tested against each other."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Variant added successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntitySingleExperimentView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid variant data or experiment cannot accept new variants",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to modify experiment",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Experiment not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @Path("/{experimentId}/variants")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntitySingleExperimentView addVariant(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Unique identifier of the experiment to add variant to", required = true)
            @PathParam("experimentId") final String experimentId,
            @RequestBody(description = "Variant configuration form with description and settings",
                        required = true,
                        content = @Content(schema = @Schema(implementation = AddVariantForm.class)))
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
    @Operation(
        summary = "Delete variant from experiment",
        description = "Removes a variant from an experiment. This will permanently delete the variant and any associated data."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Variant deleted successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntitySingleExperimentView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Variant cannot be deleted or does not exist",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to modify experiment",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Experiment or variant not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @Path("/{experimentId}/variants/{name}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntitySingleExperimentView deleteVariant(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Unique identifier of the experiment", required = true)
            @PathParam("experimentId") final String experimentId,
            @Parameter(description = "Name of the variant to delete", required = true)
            @PathParam("name") final String variantName) throws DotDataException, DotSecurityException {
        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();
        final Experiment updatedExperiment =  experimentsAPI.deleteVariant(experimentId, variantName, user);
        return new ResponseEntitySingleExperimentView(updatedExperiment);
    }

    /**
     * Updates an existing variant accepting partial updates. This means it is not needed to send
     * the entire Variant information but only what it is desired to update only. The rest
     * of the information will remain as previously persisted.
     *
     * Returns the updated version of the Experiment.
     */
    @Operation(
        summary = "Update experiment variant",
        description = "Updates the properties of an existing variant within an experiment. Supports partial updates - only provided fields will be modified."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Variant updated successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntitySingleExperimentView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid variant data or update parameters",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to modify experiment",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Experiment or variant not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path("/{experimentId}/variants/{name}")
    @JSONP
    @NoCache
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntitySingleExperimentView updateVariant(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Unique identifier of the experiment", required = true)
            @PathParam("experimentId") final String experimentId,
            @Parameter(description = "Name of the variant to update", required = true)
            @PathParam("name") final String variantName,
            @RequestBody(description = "Variant update form with properties to modify",
                        required = true,
                        content = @Content(schema = @Schema(implementation = ExperimentVariantForm.class)))
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
    @Operation(
        summary = "Promote variant to default",
        description = "Promotes a variant to become the default variant of the page. This makes the selected variant the primary version that will be used going forward."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Variant promoted successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntitySingleExperimentView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Variant cannot be promoted or invalid promotion data",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to promote variant",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Experiment or variant not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @PUT
    @Path("/{experimentId}/variants/{name}/_promote")
    @JSONP
    @NoCache
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntitySingleExperimentView promoteVariant(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Unique identifier of the experiment", required = true)
            @PathParam("experimentId") final String experimentId,
            @Parameter(description = "Name of the variant to promote to default", required = true)
            @PathParam("name") final String variantName,
            @RequestBody(description = "Variant promotion form with additional configuration",
                        required = true,
                        content = @Content(schema = @Schema(implementation = ExperimentVariantForm.class)))
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
    @Operation(
        summary = "Delete targeting condition",
        description = "Removes a targeting condition from an experiment. Targeting conditions define which users should be included in the experiment."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Targeting condition deleted successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntitySingleExperimentView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid targeting condition ID or condition cannot be deleted",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to modify experiment",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Experiment or targeting condition not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @DELETE
    @Path("/{experimentId}/targetingConditions/{id}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntitySingleExperimentView deleteTargetingCondition(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @Parameter(description = "Unique identifier of the experiment", required = true)
            @PathParam("experimentId") final String experimentId,
            @Parameter(description = "Unique identifier of the targeting condition to delete", required = true)
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
    @Operation(
        summary = "Check if user should be included in experiment",
        description = "Determines if the current user should be included in any running experiments based on targeting conditions, traffic allocation, and variant weighting. Supports excluding specific experiments from consideration."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "User inclusion status determined successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityExperimentSelectedView.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid request parameters or exclusion list",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @POST
    @NoCache
    @Path("/isUserIncluded")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ResponseEntityExperimentSelectedView isUserIncluded(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            @RequestBody(description = "Optional form containing list of experiment IDs to exclude from consideration",
                        required = false,
                        content = @Content(schema = @Schema(implementation = ExcludedExperimentListForm.class)))
            final ExcludedExperimentListForm excludedExperimentListForm
    ) throws DotDataException, DotSecurityException {

        return new ResponseEntityExperimentSelectedView(
                WebAPILocator.getExperimentWebAPI().isUserIncluded(request, response,
                        UtilMethods.isSet(excludedExperimentListForm) ? excludedExperimentListForm.getExclude()
                                : Collections.emptyList())
        );
    }

    /**
     * Returns the partial or total Result of a {@link Experiment}
     */
    @Operation(
        summary = "Get experiment results",
        description = "Retrieves the analytical results of an experiment, including performance metrics, conversion rates, and statistical significance data for all variants."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Experiment results retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityExperimentResults.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to view experiment results",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", 
                    description = "Experiment not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @NoCache
    @Path("/{id}/results")
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityExperimentResults getResult(@Context final HttpServletRequest request,
                                                     @Context final HttpServletResponse response,
                                                     @Parameter(description = "Unique identifier of the experiment to get results for", required = true)
                                                     @PathParam("id") String id)
        throws DotDataException, DotSecurityException {

        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();

        final Experiment experiment = experimentsAPI.find(id, user)
                .orElseThrow(
                        () -> new NotFoundException("Experiment with id: " + id + " not found."));

        final ExperimentResults experimentResults = APILocator.getExperimentsAPI().getResults(experiment, user);

        return new ResponseEntityExperimentResults(experimentResults);
    }

    /**
     * Healthcheck for the Experiments/Analytics configuration.
     *
     */
    @Operation(
        summary = "Check experiments system health",
        description = "Performs a health check of the experiments and analytics configuration to verify that the system is properly configured and can send test events."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Health check completed - see response body for status",
                    content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = ResponseEntityExperimentHealthView.class))),
        @ApiResponse(responseCode = "401", 
                    description = "Unauthorized - backend user authentication required",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", 
                    description = "Forbidden - insufficient permissions to check system health",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", 
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GET
    @NoCache
    @Path("/health")
    @Produces({MediaType.APPLICATION_JSON})
    public ResponseEntityExperimentHealthView healthcheck(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response)
            throws DotDataException, DotSecurityException, SystemException, PortalException {

        final InitDataObject initData = getInitData(request, response);
        final Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
        final AnalyticsApp analyticsApp = Try.of(()->AnalyticsHelper.get().appFromHost(host))
                .getOrNull();

        if(analyticsApp==null) {
            return new ResponseEntityExperimentHealthView(Map.of(HEALTH_KEY, Health.NOT_CONFIGURED));
        }

        try {
            final EventLogRunnable eventLogRunnable = new EventLogRunnable(host);
            Optional<CircuitBreakerUrl.Response<String>> responseOptional  = eventLogRunnable.sendTestEvent();

            return new ResponseEntityExperimentHealthView(Map.of(HEALTH_KEY, responseOptional.isPresent()
                    && UtilMethods.isSet(responseOptional.get().getResponse())
                    ? Health.OK:Health.CONFIGURATION_ERROR));
        } catch (IllegalStateException e) {
            return new ResponseEntityExperimentHealthView(Map.of(HEALTH_KEY, Health.CONFIGURATION_ERROR));
        }
    }

    private Experiment patchExperiment(final Experiment experimentToUpdate,
            final ExperimentForm experimentForm, final User user) {

        final Experiment.Builder builder = Experiment.builder().from(experimentToUpdate);

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
}
