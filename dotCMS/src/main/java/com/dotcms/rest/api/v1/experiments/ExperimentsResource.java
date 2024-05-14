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

import org.apache.commons.lang3.StringUtils;
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
     * Archives an Experiment. Archiving operation applies for experiments to whom there's already
     * data collected and deletion is not wanted.
     *
     * Returns the archived version of the Experiment.
     */
    @PUT
    @Path("/{experimentId}/_archive")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityExperimentView archive(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
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
    @POST
    @Path("/{experimentId}/_start")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntitySingleExperimentView start(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
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

    @POST
    @Path("/{experimentId}/_end")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntitySingleExperimentView end(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
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

    @POST
    @Path("/scheduled/{experimentId}/_cancel")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntitySingleExperimentView cancel(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
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
     * Returns the partial or total Result of a {@link Experiment}
     */
    @GET
    @NoCache
    @Path("/{id}/results")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityExperimentResults getResult(@Context final HttpServletRequest request,
                                                     @Context final HttpServletResponse response,
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
    @GET
    @NoCache
    @Path("/health")
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityView healthcheck(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response)
            throws DotDataException, DotSecurityException, SystemException, PortalException {

        final InitDataObject initData = getInitData(request, response);
        final Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
        final AnalyticsApp analyticsApp = Try.of(()->AnalyticsHelper.get().appFromHost(host))
                .getOrNull();

        if(analyticsApp==null) {
            return new ResponseEntityView<>(Map.of(HEALTH_KEY, Health.NOT_CONFIGURED));
        }

        try {
            final EventLogRunnable eventLogRunnable = new EventLogRunnable(host);
            Optional<CircuitBreakerUrl.Response<String>> responseOptional  = eventLogRunnable.sendTestEvent();

            return new ResponseEntityView<>(Map.of(HEALTH_KEY, responseOptional.isPresent()
                    && UtilMethods.isSet(responseOptional.get().getResponse())
                    ? Health.OK:Health.CONFIGURATION_ERROR));
        } catch (IllegalStateException e) {
            return new ResponseEntityView<>(Map.of(HEALTH_KEY, Health.CONFIGURATION_ERROR));
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
