package com.dotcms.rest.api.v1.experiments;

import com.dotcms.experiments.business.ExperimentsAPI;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.server.JSONP;

@Path("/v1/experiments")
@Tag(name = "Experiment")
public class ExperimentsResource {

    private final WebResource webResource;
    private final ExperimentsAPI experimentsAPI;

    public ExperimentsResource() {
        webResource =  new WebResource();
        experimentsAPI = APILocator.getExperimentsAPI();
    }

    @POST
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ResponseEntityExperimentView create(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response,
            final ExperimentForm experimentForm) {
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requestAndResponse(request, response)
                .requiredBackendUser(true)
                .rejectWhenNoUser(true)
                .init();

        final User user = initData.getUser();
        // test
        final Experiment experiment = new Experiment("test", "test");

        final Experiment persistedExperiment = experimentsAPI.save(experiment, user);

        return new ResponseEntityExperimentView(Collections.singletonList(persistedExperiment));
    }

}
