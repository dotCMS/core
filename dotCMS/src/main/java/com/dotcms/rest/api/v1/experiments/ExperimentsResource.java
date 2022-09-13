package com.dotcms.rest.api.v1.experiments;

import com.dotcms.experiments.business.ExperimentFilter;
import com.dotcms.experiments.business.ExperimentsAPI;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.PATCH;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.NotFoundException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
    public ResponseEntityExperimentView create(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final ExperimentForm experimentForm) throws DotDataException, DotSecurityException {
        final InitDataObject initData = getInitData(request, response);
        final User user = initData.getUser();
        final Experiment experiment = createExperimentFromForm(experimentForm, user);
        final Experiment persistedExperiment = experimentsAPI.save(experiment, user);
        return new ResponseEntityExperimentView(Collections.singletonList(persistedExperiment));
    }

    private Experiment createExperimentFromForm(final ExperimentForm experimentForm,
            final User user) {
        return Experiment.builder().pageId(experimentForm.getPageId()).name(experimentForm.getName())
                .description(experimentForm.getDescription()).createdBy(user.getUserId())
                .lastModifiedBy(user.getUserId())
                .trafficAllocation(experimentForm.getTrafficAllocation()>-1
                        ? experimentForm.getTrafficAllocation()
                        : 100)
                .build();
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
    public ResponseEntityExperimentView update(@Context final HttpServletRequest request,
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
        return new ResponseEntityExperimentView(Collections.singletonList(persistedExperiment));
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
    public ResponseEntityExperimentView deleteGoal(@Context final HttpServletRequest request,
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
        return new ResponseEntityExperimentView(Collections.singletonList(savedExperiment));
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
