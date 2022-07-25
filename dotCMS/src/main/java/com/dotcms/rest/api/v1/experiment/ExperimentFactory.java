package com.dotcms.rest.api.v1.experiment;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.rest.api.v1.experiment.Experiment.ExperimentStatus;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.rules.model.Rule;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class ExperimentFactory {

    private static final String IS_ANY_RUNNING_EXPERIMENT_QUERY = "SELECT count(*) FROM experiment WHERE STATUS = 'RUNNING'";
    private static final String GET_RUNNING_EXPERIMENT_QUERY = "SELECT * FROM experiment WHERE STATUS = 'RUNNING'";
    private static final String GET_EXPERIMENT_BY_KEY_QUERY = "SELECT * FROM experiment WHERE key = ?";
    private static final String GET_RULES_BY_EXPERIMENT_QUERY = "SELECT rule_id FROM experiment_rules WHERE experiment_key = ?";
    private static final String GET_VARIANT_BY_EXPERIMENT_QUERY = "SELECT * FROM experiment_variant, variant WHERE experiment_variant.variant_key = variant.key AND experiment_variant.experiment_key = ?";
    private static final String GET_EVENTS_BY_EXPERIMENT_QUERY = "SELECT event_key, parameters FROM events_to_track WHERE experiment_key = ?";
    private static final String INSERT_EXPERIMENT_QUERY = "INSERT INTO experiment (name, key, status, uniquePerVisitor, lookBackWindowMinutes, pageInode) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String INSERT_EXPERIMENT_RULES_QUERY = "INSERT INTO experiment_rules (experiment_key, rule_id) VALUES (?, ?)";
    private static final String INSERT_EXPERIMENT_VARIANT_QUERY = "INSERT INTO experiment_variant (experiment_key, variant_key, traffic_percentage, original) VALUES (?, ?, ?, ?)";
    private static final String INSERT_VARIANT_QUERY = "INSERT INTO experiment_variant (name, key, domain) VALUES (?, ?, ?)";
    private static final String INSERT_EVENTS_TO_TRACK_QUERY = "INSERT INTO events_to_track (experiment_key, event_key, parameters) VALUES (?, ?, ?)";

    public static boolean isAnyExperimentRunning() {

        try {
            return new DotConnect()
                    .setSQL(IS_ANY_RUNNING_EXPERIMENT_QUERY)
                    .loadObjectResults()
                    .stream()
                    .map(result -> (Long) result.get("count"))
                    .findFirst().orElse(0l) > 0;
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Experiment> getRunningExperiments() {
        try {


            final List<Map<String, Object>> experimentsMap = new DotConnect()
                    .setSQL(GET_RUNNING_EXPERIMENT_QUERY)
                    .loadObjectResults();

            return experimentsMap.stream()
                    .map(experimentMap -> createExperiment(experimentMap))
                    .collect(Collectors.toList());
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private static Experiment createExperiment(Map<String, Object> experimentMap) {
        final Experiment experiment = new Experiment(
               experimentMap.get("key").toString(),
               experimentMap.get("name").toString(),
                ExperimentStatus.valueOf(experimentMap.get("status").toString()),
               Boolean.parseBoolean(
                       experimentMap.get("uniquePerVisitor").toString()),
               Integer.parseInt(
                       experimentMap.get("lookBackWindowMinutes").toString())
       );

        final List<Rule> rules = getRuleByExperiment(experiment);
        final VariantsCollection variantsCollection = getVariantByExperiment(experiment,  experimentMap.get("pageId").toString());
        final List<AnalyticEvent> events = getEventsByExperiment(experiment);

        experiment.setEvents(events);
        experiment.setRules(rules);
        experiment.setVariantsCollection(variantsCollection);

        return experiment;
    }

    private static List<AnalyticEvent> getEventsByExperiment(final Experiment experiment) {
        try{
            return new DotConnect()
                    .setSQL(GET_EVENTS_BY_EXPERIMENT_QUERY)
                    .addParam(experiment.getKey())
                    .loadObjectResults()
                    .stream()
                    .map(map -> {
                        try {
                            return new AnalyticEvent(
                                        AnalyticEventType.valueOf(map.get("event_key").toString()),
                                        JsonUtil.toMap(map.get("parameters").toString())
                                    );
                        } catch (IOException e) {
                            return null;
                        }
                    })
                    .collect(Collectors.toList());
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Rule> getRuleByExperiment(final Experiment experiment) {
        try {
            return new DotConnect()
                    .setSQL(GET_RULES_BY_EXPERIMENT_QUERY)
                    .addParam(experiment.getKey())
                    .loadObjectResults()
                    .stream()
                    .map(result -> result.get("rule_id").toString())
                    .map(ruleID -> {
                        try {
                            return APILocator.getRulesAPI()
                                    .getRuleById(ruleID, APILocator.systemUser(), false);
                        } catch (DotDataException | DotSecurityException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    private static VariantsCollection getVariantByExperiment(final Experiment experiment,
            final String pageInode) {
        try {
            final IHTMLPage page = APILocator.getHTMLPageAssetAPI()
                    .findPage(pageInode, APILocator.systemUser(), false);

            final List<ExperimentVariant> variants = new DotConnect()
                    .setSQL(GET_VARIANT_BY_EXPERIMENT_QUERY)
                    .addParam(experiment.getKey())
                    .loadObjectResults()
                    .stream()
                    .map(map -> new ExperimentVariant(
                            new DotCMSVariant(map.get("name").toString(),
                                    map.get("domain").toString(), map.get("key").toString()),
                            Integer.parseInt(map.get("trafficPercentage").toString()),
                            Boolean.valueOf(map.get("original").toString()))
                    )
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return new VariantsCollection(variants, page);
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public static Optional<Experiment> getExperiment(final String experimentValue) {
        try {
            return new DotConnect()
                    .setSQL(GET_EXPERIMENT_BY_KEY_QUERY)
                    .addParam(experimentValue)
                    .loadObjectResults()
                    .stream()
                    .map(experimentMap -> createExperiment(experimentMap))
                    .findFirst();
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    @WrapInTransaction
    public static void save(final ExperimentForm experimentForm) throws DotDataException {
        final DotConnect dotConnect = new DotConnect();

        dotConnect.setSQL(INSERT_EXPERIMENT_QUERY)
            .addParam(experimentForm.getName())
            .addParam(experimentForm.getKey())
            .addParam(ExperimentStatus.STOP)
            .addParam(experimentForm.isUniquePerVisitor())
            .addParam(experimentForm.getLookBackWindowMinutes())
            .addParam(experimentForm.getPageInode())
            .loadResult();

        final Collection<String> targeting = experimentForm.getTargeting();

        for (final String ruleId : targeting) {
            dotConnect.setSQL(INSERT_EXPERIMENT_RULES_QUERY)
                    .addParam(experimentForm.getKey())
                    .addParam(ruleId)
                    .loadResults();
        }

        final Collection<ExperimentVariant> variants = experimentForm.getVariants();

        for (final ExperimentVariant variant : variants) {

            dotConnect.setSQL(INSERT_EXPERIMENT_VARIANT_QUERY)
                    .addParam(experimentForm.getKey())
                    .addParam(variant.getVariant().getKey())
                    .addParam(variant.getTrafficPercentage())
                    .addParam(variant.isOriginal())
                    .loadResults();

            dotConnect.setSQL(INSERT_VARIANT_QUERY)
                    .addParam(variant.getVariant().getName())
                    .addParam(variant.getVariant().getKey())
                    .addParam(variant.getVariant().getDomain())
                    .loadResults();
        }

        final Collection<AnalyticEvent> events = experimentForm.getEvents();

        for (final AnalyticEvent event : events) {
            try {
                dotConnect.setSQL(INSERT_EVENTS_TO_TRACK_QUERY)
                        .addParam(experimentForm.getKey())
                        .addParam(event.getEventKey().toString())
                        .addParam(JsonUtil.toJson(event.getParameters()))
                        .loadResults();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
