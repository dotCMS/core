package com.dotcms.rest.api.v1.experiment;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.repackage.org.directwebremoting.json.parse.JsonParseException;
import com.dotcms.rest.RestClientBuilder;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.minidev.json.annotate.JsonIgnore;
import org.apache.commons.lang.RandomStringUtils;

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
                "name", "Red_Title",
                "url",
                "/blog?language_id=4809665&host_id=48190c8c-42c4-46af-8d1a-0cd5db894797&redirect=true",
                "traffic_percentage", 25,
                "traffic_percentage_sum", 75
            ),
            map(
                "name", "Blue_Title",
                "url",
                "/blog?language_id=4789229&host_id=48190c8c-42c4-46af-8d1a-0cd5db894797&redirect=true",
                "traffic_percentage", 25,
                "traffic_percentage_sum", 100
            )
    );

    private boolean uniquePerVisitor = true;
    private int lookBackWindowMinutes = 10;

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
                final String experimentName = "POC_experiment";

                final Cookie experimentCookie = new Cookie("experiment", experimentName);
                experimentCookie.setMaxAge(uniquePerVisitor ? -1 : lookBackWindowMinutes * 60);
                response.addCookie(experimentCookie);

                final Cookie lookBackWindowCookie = new Cookie("lookBackWindowCookie", RandomStringUtils.randomAlphanumeric(20));
                lookBackWindowCookie.setMaxAge(lookBackWindowMinutes * 60);
                response.addCookie(lookBackWindowCookie);

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
                        response.addCookie(new Cookie("variant", variant.get("name").toString()));
                        break;
                    }
                }

                mapResponse.put("experiment",
                        map(
                            "experiment_name", experimentName,
                            "url", "/blog",
                            "variant", variantSelected
                        )
                );


            }
        }

        return Response.ok(mapResponse).build();
    }


    private final String scriptInit = "var experiment_data = localStorage.getItem('experiment_data');\n"
            + "console.log('experiment_data', experiment_data);\n"
            + "var isInExperiment = document.cookie.includes('lookBackWindowCookie');"
            + "\n"
            + "if (isInExperiment && !!experiment_data && window.location.href.includes(JSON.parse(experiment_data).experiment.url)){\n"
            + "        console.log('Trigger init event...');\n"
            + "        const event = new CustomEvent('init_custom', { detail: JSON.parse(experiment_data) });\n"
            + "        window.dispatchEvent(event);\n"
            + "} else if (!experiment_data) {\n"
            + "        console.log('Getting init data...');\n"
            + "        fetch('http://localhost:8080/api/v1/experiment')\n"
            + "          .then(response => response.json())     \n"
            + "          .then(data => {\n"
            + "             console.log('data', data);\n"
            + "              if (data.experiment) {\n"
            + "                localStorage.setItem('experiment_data', JSON.stringify(data));\n"
            + "                \n"
            + "                console.log('Trigger init event...');\n"
            + "                const event = new CustomEvent('init_custom', { detail: data });\n"
            + "                window.dispatchEvent(event);\n"
            + "              } \n"
            + "           });\n"
            + "}\n";

    private final String scriptTraffic = "console.log('traffic_scripts');\n"
            + "\n"
            + "window.addEventListener(\"init_custom\", function (event) {\n"
            + "    console.log('traffic init_custom', event.detail.experiment.variant);\n"
            + "    \n"
            + "    if (!window.location.href.includes('redirect=true')) {\n"
            + "        window.location = event.detail.experiment.variant.url;\n"
            + "    }\n"
            + "});";

    private final String scriptMetricts = "var initEvent = new Promise(function(resolve) {\n"
            + "    window.addEventListener(\"init_custom\",resolve,false);\n"
            + "});\n"
            + "var loadEvent = new Promise(function(resolve) {\n"
            + "    window.addEventListener(\"load\", resolve, false);\n"
            + "});\n"
            + "\n"
            + "Promise.all([initEvent, loadEvent]).then(function(data) {\n"
            + "    console.log('metrics_enabled', data[0].detail.metrics.click);\n"
            + "    \n"
            + "    var click_config = data[0].detail.metrics.click;\n"
            + "    var page_click_config;\n"
            + "    \n"
            + "    console.log('click_config', click_config);\n"
            + "    \n"
            + "    for (var i = 0; i < click_config.length; i++){\n"
            + "        console.log('window.location.href', window.location.href);\n"
            + "        console.log('click_config[i].page', click_config[i].page);\n"
            + "        if (window.location.href.includes(click_config[i].page)){\n"
            + "            page_click_config = click_config[i];\n"
            + "        }\n"
            + "    }\n"
            + "    console.log('page_click_config', page_click_config);\n"
            + "    if (page_click_config) {\n"
            + "        console.log('Listener click events in', page_click_config.query_selector);\n"
            + "        var as = document.querySelectorAll(page_click_config.query_selector);\n"
            + "        \n"
            + "        var experiment = localStorage.getItem('experiment');\n"
            + "        var variant = localStorage.getItem('variant');\n"
            + "        console.log(\"variant\", variant);\n"
            + "        for (var i = 0; i < as.length; i++){\n"
            + "            console.log(\"a link\", as[i]);\n"
            + "            as[i].addEventListener('click', (event) => {\n"
            + "                var target = event.target;\n"
            + "                jitsu('track', 'click', {\n"
            + "                  target: {\n"
            + "                    name: event.target.name,\n"
            + "                    class: event.target.classList,\n"
            + "                    id: event.target.id,\n"
            + "                    tag: event.target.tagName,\n"
            + "                  },\n"
            + "                });\n"
            + "            });\n"
            + "        }\n"
            + "    }\n"
            + "});";

    @GET()
    @Path("/js/experiments.js")
    @Produces({"application/javascript"})
    public Response loadJS(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response)
            throws DotSecurityException, DotDataException {
        return Response.ok( scriptTraffic + scriptMetricts + scriptInit).build();

    }

    @GET
    @Path("/result/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Map<String, ExperimentResult> result(@PathParam("id") final String experimentID)
            throws DotSecurityException, DotDataException, JsonParseException, IOException {
        final String numberOfVisitors = " {\n"
                + "        \"query\": {\n"
                + "            \"measures\":[\"Events.count\"],\n"
                + "            \"dimensions\": [\"Events.userAnonymousId\"]\n"
                + "        }\n"
                + "}";

        final ArrayList<Map<String, String>> visitors = requestToCubeJS(numberOfVisitors);

        final String query = " {\n"
                + "    \"query\": {\n"
                + "        \"measures\":[\"Events.count\"],\n"
                + "        \"dimensions\": [\"Events.eventType\", \"Events.userAnonymousId\", \"Events.experimentVariantName\", \"Events.docPath\"],\n"
                + "        \"filters\": [\n"
                + "            {\n"
                + "                \"member\": \"Events.eventType\",\n"
                + "                \"operator\": \"equals\",\n"
                + "                \"values\": [\"pageview\"]\n"
                + "            },\n"
                + "            {\n"
                + "                \"member\": \"Events.experimentName\",\n"
                + "                \"operator\": \"equals\",\n"
                + "                \"values\": [\""  + experimentID + "\"]\n"
                + "            }    \n"
                + "        ]\n"
                + "    }\n"
                + "}";

        final ArrayList<Map<String, String>> data = requestToCubeJS(query);

        Map<String, ExperimentResult> variants = new HashMap();

        for (Map<String, String> datum : data) {
            final String variantName = datum.get("Events.experimentVariantName");

            final ExperimentResult experimentResult = UtilMethods.isSet(variants.get(variantName)) ?
                    variants.get(variantName) : new ExperimentResult(visitors.size());

            experimentResult.addConversions(Integer.parseInt(datum.get("Events.count")));
            experimentResult.addUniqueConversions(datum.get("Events.userAnonymousId"), datum.get("Events.docPath"));
            variants.put(variantName, experimentResult);
        }

        return variants;

    }

    private ArrayList<Map<String, String>> requestToCubeJS(String query) throws IOException {
        final Response post = RestClientBuilder.newClient()
                .target("https://sound-yak.aws-us-west-2.cubecloudapp.dev/cubejs-api/v1/load")
                .request()
                .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpYXQiOjE2NTU4MzU2NjR9.6PFE4IbfWCVINS7KD2mgjhoLzCUB-X6D4gO7J72zK5k")
                .post(Entity.entity(query, MediaType.APPLICATION_JSON));

        final String response = post.readEntity(String.class);

        final Map<String, Object> map = JsonUtil.toMap(response);
        final ArrayList<Map<String, String>> data = (ArrayList<Map<String, String>>) map.get("data");
        return data;
    }

    public static class ExperimentResult {
        @JsonIgnore
        private int totalConversions = 0;
        @JsonIgnore
        private Map<String, Boolean> uniqueConversions = new HashMap<>();
        @JsonIgnore
        private int numberOfVisitors;

        public ExperimentResult(int numberOfVisitors) {
            this.numberOfVisitors = numberOfVisitors;
        }

        public void addConversions(int newConversions) {
            totalConversions += newConversions;
        }

        public void addUniqueConversions(String... keys) {
            final String key = String.join("", keys);
            uniqueConversions.put(key, true);
        }

        @JsonProperty
        public int getNotDedupedConversions(){
            return totalConversions;
        }

        @JsonProperty
        public int getNotDedupedConversionsRate(){
            return (getNotDedupedConversions() / numberOfVisitors) * 100;
        }

        @JsonProperty
        public int getDedupedConversions(){
            return uniqueConversions.size();
        }

        @JsonProperty
        public int getDedupedConversionsRate(){
            return (getDedupedConversions() / numberOfVisitors) * 100;
        }
    }
}
