package com.dotcms.rest.api.v1.experiment;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.WebKeys;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v1/experiment")
public class ExperimentResource {

    List<Map> variants = list(
            map(
                "name", "Original",
                "url",
                "/blog?language_id=1&host_id=48190c8c-42c4-46af-8d1a-0cd5db894797&redirect=true",
                "traffic_percentage", 50,
                "traffic_percentage_sum", 50
            ),
            map(
                "name", "Red Title",
                "url",
                "/blog?language_id=4809665&host_id=48190c8c-42c4-46af-8d1a-0cd5db894797&redirect=true",
                "traffic_percentage", 25,
                "traffic_percentage_sum", 75
            ),
            map(
                "name", "Blue Title",
                "url",
                "/blog?language_id=4789229&host_id=48190c8c-42c4-46af-8d1a-0cd5db894797&redirect=true",
                "traffic_percentage", 25,
                "traffic_percentage_sum", 100
            )
    );

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response loadJson(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response)
            throws DotSecurityException, DotDataException {

        final Host currentHost = WebAPILocator.getHostWebAPI().getCurrentHost();
        final List<Rule> rules = APILocator.getRulesAPI().getAllRulesByParent(
                currentHost.getIdentifier(), APILocator.systemUser());

        final Optional<Rule> ruleExperiment = rules.stream()
                .filter(rule -> rule.getName().equals("Rule Experiment")).findFirst();

        final Map mapResponse = new HashMap<>();

        if (ruleExperiment.isPresent()) {
            ruleExperiment.get().checkValid();
            boolean evaluate = ruleExperiment.get().evaluate(request, response);

            if (evaluate) {

                mapResponse.put("metrics", map(
                        "click", list(
                                map(
                                        "page", "/blog",
                                        "query_selector", "a"
                                )
                        )
                ));

                final int random = (int) (Math.random() * 100);
                Map variantSelected = null;

                for (Map variant : variants) {
                    if (random < Integer.parseInt(
                            variant.get("traffic_percentage_sum").toString())) {
                        variantSelected = variant;
                        break;
                    }
                }

                mapResponse.put("experiment",
                        map("experiment_name", "POC_experiment", "variant", variantSelected));


            }
        }

        return Response.ok(mapResponse).build();
    }
}
