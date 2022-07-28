package com.dotcms.rest.api.v1.experiment;

import static com.dotmarketing.util.FileUtil.getFileContentFromResourceContext;

import com.dotcms.repackage.org.directwebremoting.json.parse.JsonParseException;
import com.dotcms.rest.RestClientBuilder;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.liferay.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.minidev.json.annotate.JsonIgnore;
import org.apache.commons.lang.RandomStringUtils;

@Path("/v1/experiment")
public class ExperimentResource {

    public static final String EXPERIMENT_COOKIE_NAME = "experiment";
    public static final String LOOK_BACK_WINDOW_COOKIE_NAME = "lookBackWindowCookie";

    private final String SCRIPT_INIT;
    private final String SCRIPT_TRAFFIC;
    private final String SCRIPT_METRICTS;

    public ExperimentResource(){
        try {
            SCRIPT_INIT = getFileContentFromResourceContext("/experiment/js/init_script.js");
            SCRIPT_TRAFFIC = getFileContentFromResourceContext("/experiment/js/init_traffic.js");;
            SCRIPT_METRICTS = getFileContentFromResourceContext("/experiment/js/init_metricts.js");;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public ExperimentSelected triggerExperiment(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response)
            throws DotSecurityException, DotDataException {

        final List<Experiment> runningExperiments = ExperimentFactory.getRunningExperiments();
        final Experiment experiment = runningExperiments.isEmpty() ? null : pickOneExperiment(runningExperiments);

        final ExperimentSelected experimentSelected = new ExperimentSelected();

        if (UtilMethods.isSet(experiment) && checkRules(experiment, request, response)) {

            final boolean shouldRunExperiment = shouldRunExperiment(experiment);
            setCookies(response, experiment, shouldRunExperiment);

            if (shouldRunExperiment) {
                final ExperimentVariant experimentVariant = pickUpVariant(experiment, response);
                experimentSelected.variant = new VariantSelected(
                        experimentVariant.getVariant().getName(), experiment.getVariantURL(experimentVariant));
                experimentSelected.url = experiment.getURL();
            } else {
                experimentSelected.name = "NONE";
            }
        } else {
            experimentSelected.name = "NONE";
        }

        return experimentSelected;
    }

    private ExperimentVariant pickUpVariant(final Experiment experiment,
            HttpServletResponse response) {
        final int randomValue = (int) (Math.random() * 100);
        double acum = 0;

        for (final ExperimentVariant variant : experiment.getVariants().getAll()) {
            acum += variant.getTrafficPercentage();

            if (randomValue < acum) {
                response.addCookie(
                        new Cookie("variant", variant.getVariant().getName()));
                return variant;
            }
        }

        throw new RuntimeException();
    }

    private void setCookies(final HttpServletResponse response, final Experiment experiment,
            final boolean shouldRunExperiment) {

        final Cookie experimentCookie = new Cookie(
                EXPERIMENT_COOKIE_NAME, shouldRunExperiment ? experiment.getName() : "NONE");
        experimentCookie.setMaxAge(experiment.getUniquePerVisitor() ? -1 : experiment.getLookBackWindowMinutes() * 60);
        response.addCookie(experimentCookie);

        if (shouldRunExperiment) {
            final Cookie lookBackWindowCookie = new Cookie(LOOK_BACK_WINDOW_COOKIE_NAME,
                    RandomStringUtils.randomAlphanumeric(20));
            lookBackWindowCookie.setMaxAge(experiment.getLookBackWindowMinutes() * 60);
            response.addCookie(lookBackWindowCookie);
        }
    }

    private boolean shouldRunExperiment(final Experiment experiment) {
        final int randomValue = (int) (Math.random() * experiment.getTraffic());
        return randomValue < experiment.getTraffic();
    }

    private boolean checkRules(final Experiment experiment,
            HttpServletRequest request, HttpServletResponse response) {
        final Collection<Rule> rules = experiment.getTargeting();

        for (final Rule rule : rules) {
            rule.checkValid();

            if (!rule.evaluate(request, response)) {
                return false;
            }
        }

        return true;
    }

    private Experiment pickOneExperiment(final List<Experiment> experiments) {
        final int randomValue = (int) (Math.random() * experiments.size());
        return experiments.get(randomValue);
    }

    @GET()
    @Path("/js/experiments.js")
    @Produces({"application/javascript"})
    public String loadJS(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {

        final Optional<Cookie> experimentCookie = getExperimentCookie(request);

        if (experimentCookie.isPresent()) {
            final Optional<Experiment> experiment = ExperimentFactory.getExperiment(experimentCookie.get().getValue());

            if (!experiment.isPresent()) {
                throw new NotFoundException();
            }

            final Collection<AnalyticEvent> events = experiment.get().getEvents();
            final StringBuffer metricsJsCode = new StringBuffer();

            for (final AnalyticEvent event : events) {
                final AnalyticEventTypeHandler analyticEventTypeHandler = AnalyticEventTypeHandlerManager.INSTANCE.get(
                        event.getEventKey());

                final String jsCode = analyticEventTypeHandler.getJSCode(event.getParameters());
                metricsJsCode.append(jsCode);
            }

            return SCRIPT_TRAFFIC +
                    SCRIPT_METRICTS.replaceAll("$\\{dinamycCodeHere}", metricsJsCode.toString()) +
                    SCRIPT_INIT;
        } else {
            throw new IllegalStateException();
        }
    }

    private Optional<Cookie> getExperimentCookie(HttpServletRequest request) {
        final Cookie[] cookies = request.getCookies();

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(EXPERIMENT_COOKIE_NAME)) {
                return Optional.of(cookie);
            }
        }

        return Optional.empty();
    }

    @GET
    @Path("/result/{key}")
    @Produces({MediaType.APPLICATION_JSON})
    public Map<String, ExperimentResult> result(@PathParam("key") final String experimentKey)
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
                + "                \"values\": [\""  + experimentKey + "\"]\n"
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

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void save(final ExperimentForm experimentForm) throws DotDataException {
        ExperimentFactory.save(experimentForm);
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

    @PUT
    @Path("/result/{key}/start")
    @Produces({MediaType.APPLICATION_JSON})
    public void start(@PathParam("key") final String experimentKey) {
        ExperimentFactory.start(experimentKey);
    }

    @PUT
    @Path("/result/{key}/stop")
    @Produces({MediaType.APPLICATION_JSON})
    public void stop(@PathParam("key") final String experimentKey) {
        ExperimentFactory.stop(experimentKey);
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

     private static class ExperimentSelected {
        String name;
        String url;
        VariantSelected variant;


    }

    private static class VariantSelected {
        private String name;
        private String url;

        public VariantSelected(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }
}
