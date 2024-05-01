package com.dotcms.experiments.business.web;

import com.dotcms.analytics.metrics.Metric;
import com.dotcms.experiments.business.ConfigExperimentUtil;
import com.dotcms.experiments.business.ExperimentUrlPatternCalculator;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.ExperimentVariant;
import com.dotcms.experiments.model.TrafficProportion;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.util.StringPool;
import net.bytebuddy.utility.RandomString;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotmarketing.util.FileUtil.getFileContentFromResourceContext;


/**
 * Default implementation of {@link ExperimentWebAPI}
 */
public class ExperimentWebAPIImpl implements ExperimentWebAPI {


    @Override
    public SelectedExperiments isUserIncluded(final HttpServletRequest request,
            final HttpServletResponse response, final List<String> idsToExclude)
            throws DotDataException, DotSecurityException {

        final Host currentHost = getCurrentHost(request).orElseThrow();

        final List<Experiment> experimentsRunning = APILocator.getExperimentsAPI()
                .getRunningExperiments(currentHost);

        final List<String> experimentsRunningId = experimentsRunning.stream()
                .map(experiment -> experiment.id().get())
                .collect(Collectors.toList());

        final List<Experiment> experimentFiltered = UtilMethods.isSet(idsToExclude) ?
                experimentsRunning.stream()
                    .filter(experiment -> !idsToExclude.contains(experiment.id().get()))
                    .collect(Collectors.toList()) : experimentsRunning;

        final List<String> excludedExperimentIdsEnded = idsToExclude != null ? idsToExclude.stream()
                .filter(experimentId -> !experimentsRunningId.contains(experimentId))
                .collect(Collectors.toList()) : Collections.emptyList();

        if (experimentFiltered.isEmpty()) {
            return new SelectedExperiments.Builder()
                    .experiments(Collections.emptyList())
                    .included(Collections.emptyList())
                    .excluded(UtilMethods.isSet(idsToExclude) ? new ArrayList<>(idsToExclude) : Collections.emptyList())
                    .excludedExperimentIdsEnded(excludedExperimentIdsEnded)
                    .build();
        }

        final List<Experiment> experimentsSelected = pickExperiments(experimentFiltered, request, response);

        final List<SelectedExperiment> selectedExperiments = !experimentsSelected.isEmpty() ?
                getSelectedExperimentsResult(experimentsSelected) : getNoneExperimentListResult();

        return new SelectedExperiments.Builder()
                .experiments(selectedExperiments)
                .included(experimentFiltered.stream().map(experiment -> experiment.id().get()).collect(Collectors.toList()))
                .excluded(UtilMethods.isSet(idsToExclude) ? new ArrayList(idsToExclude) : Collections.EMPTY_LIST)
                .excludedExperimentIdsEnded(excludedExperimentIdsEnded)
                .build();
    }

    private static Optional<Host> getCurrentHost(HttpServletRequest request)  {
        try {
            return Optional.ofNullable(WebAPILocator.getHostWebAPI().getCurrentHost(request));
        } catch (DotDataException| DotSecurityException| PortalException| SystemException e) {
            return Optional.empty();
        }

    }

    private List<SelectedExperiment> getSelectedExperimentsResult(final List<Experiment> experiments) {
        return experiments.stream()
                    .map(experiment -> createSelectedExperiment (experiment))
                    .collect(Collectors.toList());
    }

    private List<SelectedExperiment> getNoneExperimentListResult() {
        return list(NONE_EXPERIMENT);
    }

    private String nextLookBackWindow(){
        return new RandomString(20).nextString();
    }

    private SelectedExperiment createSelectedExperiment(final Experiment experiment) {
        final SelectedVariant variantSelected = pickOneVariant(experiment);
        return getExperimentSelected(experiment, variantSelected);
    }

    private SelectedVariant pickOneVariant(final Experiment experiment) {
        final int randomValue = nextRandomNumber();

        final TrafficProportion trafficProportion = experiment.trafficProportion();
        float totalWeight = 0;

        for (ExperimentVariant variant : trafficProportion.variants()) {
            totalWeight += variant.weight();

            if (randomValue < totalWeight) {
                return new SelectedVariant(variant.id(), variant.url().orElse(StringPool.BLANK));
            }
        }

        throw new IllegalStateException(
                String.format("Should return one variant for the Experiment: %s - %s", experiment.id(),
                        experiment.name()));
    }

    private int nextRandomNumber() {
        return (int) (Math.random() * 100);
    }

    private SelectedExperiment getExperimentSelected(final Experiment experiment,
            final SelectedVariant variantSelected) {

        final HTMLPageAsset htmlPageAsset = getHtmlPageAsset(experiment);

        if (!UtilMethods.isSet(htmlPageAsset)) {
            throw new RuntimeException("Page not found: " + experiment.pageId());
        }

        try {
            final String currentRunningId = experiment.runningIds().getCurrent()
                    .map(runningId -> runningId.id())
                    .orElse(null);

            final Metric metric = experiment.goals().orElseThrow().primary().getMetric();

            final SelectedExperiment.Builder builder = new SelectedExperiment.Builder()
                    .id(experiment.id().orElse(StringPool.BLANK))
                    .name(experiment.name())
                    .pageUrl(htmlPageAsset.getURI())
                    .variant(variantSelected)
                    .lookBackWindow(nextLookBackWindow())
                    .expireTime(experiment.lookBackWindowExpireTime())
                    .runningId(currentRunningId)
                    .experimentPagePattern(ExperimentUrlPatternCalculator.INSTANCE
                            .calculatePageUrlRegexPattern(experiment));

            final Optional<String> targetPageUrlPattern = ExperimentUrlPatternCalculator.INSTANCE
                    .calculateTargetPageUrlPattern(htmlPageAsset, metric);

            if (targetPageUrlPattern.isPresent()) {
                builder.targetPagePattern(targetPageUrlPattern.get());
            }

            return builder.build();
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }
    
    private List<Experiment> pickExperiments(final List<Experiment> runningExperiments,
            final HttpServletRequest request, final HttpServletResponse response)
            throws DotDataException, DotSecurityException {

        final List<Experiment> experiments = new ArrayList<>();

        for (final Experiment experiment : runningExperiments) {
            final int randomValue = nextRandomNumber();

            if (randomValue < experiment.trafficAllocation()) {
                if (this.checkRule(experiment, request, response)) {
                    experiments.add(experiment);
                }
            }
        }

        return experiments;
    }

    private HTMLPageAsset getHtmlPageAsset(final Experiment experiment) {

        try {
            final Contentlet contentlet = APILocator.getContentletAPI()
                    .findContentletByIdentifierAnyLanguage(experiment.pageId(), false);
            final HTMLPageAsset htmlPageAsset = APILocator.getHTMLPageAssetAPI()
                    .fromContentlet(contentlet);
            return htmlPageAsset;
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    private boolean checkRule(final Experiment experiment, final HttpServletRequest request,
            final HttpServletResponse response) throws DotDataException, DotSecurityException {

        final Optional<Rule> rule = APILocator.getExperimentsAPI().getRule(experiment);

        if (rule.isEmpty()) {
            return true;
        }

        rule.get().checkValid();
        return rule.get().evaluate(request, response);
    }

    /**
     * Return the Experiment Js Code to inject when there are not any Experiment Running
     * @return
     */
    private String getNotExperimentRunningJSCode() {
        return "<SCRIPT>localStorage.removeItem('experiment_data');</SCRIPT>";
    }

    /**
     * Return the Experiment Js Code to inject when there are any Experiment Running
     *
     * @param host Host to use the {@link AnalyticsApp}
     * @param request To get the Domain name
     * @return
     */
    private String getJSCode(final Host host, final HttpServletRequest request) {

        try {
            final String jsJitsuCode =  replaceIntoHTMLCode(
                    getFileContentFromResourceContext("experiment/html/experiment_head.html"),
                    host, request);

            final Host currentHost = getCurrentHost(request).orElseThrow();

            final String shouldBeInExperimentCalled =  replaceIntoJsCode(
                    getFileContentFromResourceContext("experiment/js/init_script.js"),
                    APILocator.getExperimentsAPI().getRunningExperiments(currentHost));

            return jsJitsuCode + "\n<SCRIPT>" + shouldBeInExperimentCalled + "</SCRIPT>";
        } catch (IOException | DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    private String replaceIntoJsCode(final String jsCode, final List<Experiment> experiments) {

            final String runningExperimentsId = experiments.stream()
                    .map(experiment -> "'" + experiment.id().get() + "'")
                    .collect(Collectors.joining(","));

            final Map<String, String> subStringToReplace = new HashMap<>();
            subStringToReplace.put(
                    "${running_experiments_list}", runningExperimentsId
            );

            return replace(jsCode, subStringToReplace);
    }

    private String replaceIntoHTMLCode(final String htmlCode, final Host host,
            final HttpServletRequest request) {

        final Map<String, String> subStringToReplace = new HashMap<>();
        subStringToReplace.put("${jitsu_key}", ConfigExperimentUtil.INSTANCE.getAnalyticsKey(host));
        subStringToReplace.put("${site}", getLocalServerName(request));

        return replace(htmlCode, subStringToReplace);
    }

    private static String replace(final String htmlCode,
            final Map<String, String> subStringToReplace) {

        String result = htmlCode;

        for (final Entry<String, String> subStringEntry : subStringToReplace.entrySet()) {
            final String toReplace = subStringEntry.getKey();
            final int index = result.indexOf(toReplace);

            if (index != -1) {
                final String before = result.substring(0, index);
                final String after = result.substring(index + toReplace.length());

                result = before + subStringEntry.getValue() + after;
            }
        }

        return result;
    }

    private String getLocalServerName(HttpServletRequest request) {
        return request.getLocalName() + ":" + request.getLocalPort();
    }

    /**
     * Return the HTML/JS Code needed to support Experiments into the Browser, this code is
     * generated according the follows rules:
     *
     * - If the {@link ConfigExperimentUtil#isExperimentEnabled()} is disabled then a
     * {@link Optional#empty()} is returned.
     * - If the {@link ConfigExperimentUtil#isExperimentAutoJsInjection()} is disabled then a
     * {@link Optional#empty()} is returned.
     * - If both {@link ConfigExperimentUtil#isExperimentEnabled()} and {@link ConfigExperimentUtil#isExperimentAutoJsInjection()}
     * are TRUE but we don't have any {@link com.dotcms.experiments.model.Experiment} RUNNING then
     * the generated code is just to remove the localStorage object if it exists.
     * - If both {@link ConfigExperimentUtil#isExperimentEnabled()} and {@link ConfigExperimentUtil#isExperimentAutoJsInjection()}
     * are TRUE and also it is at least one  {@link com.dotcms.experiments.model.Experiment} RUNNING then
     * the generated code do the Follow:
     *      - Hit the Endpoint to know if the USer should go into the Experiment.
     *      - Create the localStorage Object to keep all the Experiment's data needed.
     *      - Redirect to a Page's Variant version if it is needed according to the Variant into what the user was assigned.
     *      - Clean the localStorage when a Experiment is sttoped but still exists at least one more RUNNING.
     *
     * @param host
     * @param request
     * @return
     */
    @Override
    public Optional<String> getCode(final Host host, final HttpServletRequest request) {
        final PageMode pageMode = PageMode.get(request);

        if (ConfigExperimentUtil.INSTANCE.isExperimentEnabled() && pageMode == PageMode.LIVE) {

            try {
                final Host currentHost = getCurrentHost(request).orElseThrow();

                final String code = APILocator.getExperimentsAPI().isAnyExperimentRunning(currentHost) ?
                        getJSCode(host, request) : getNotExperimentRunningJSCode();
                return Optional.of(code);
            } catch (DotDataException e) {
                Logger.error(ExperimentWebAPIImpl.class, "It is not possible to generate the Experiment JS COde:" + e.getMessage());
            }
        }

        return Optional.empty();
    }
}
