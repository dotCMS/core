package com.dotcms.experiments.business.web;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.ExperimentVariant;
import com.dotcms.experiments.model.TrafficProportion;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.bytebuddy.utility.RandomString;

/**
 * Default implementation of {@link ExperimentWebAPI}
 */
public class ExperimentWebAPIImpl implements ExperimentWebAPI {

    @Override
    public List<SelectedExperiment> isUserIncluded(final HttpServletRequest request, final HttpServletResponse response)
            throws DotDataException, DotSecurityException {
        final List<Experiment> experimentRunning = APILocator.getExperimentsAPI()
                .getRunningExperiment();

        final List<Experiment> experiments = pickExperiments(experimentRunning, request, response);

        if (!experiments.isEmpty()) {
            return  experiments.stream()
                        .map(experiment -> createSelectedExperimentAndCreateCookie(response,
                                experiment))
                        .collect(Collectors.toList());
        } else {
            setCookie(NONE_EXPERIMENT, response, getDefaultExpireDate());
            return list(NONE_EXPERIMENT);
        }
    }

    private SelectedExperiment createSelectedExperimentAndCreateCookie(
            final HttpServletResponse response,
            final Experiment experiment) {
        final SelectedExperiment selectedExperiment = createSelectedExperiment(
                experiment);
        final Instant instant = experiment.scheduling().get().endDate().get();
        setCookie(selectedExperiment, response, instant);
        return selectedExperiment;
    }

    private Instant getDefaultExpireDate() {
        return Instant.now().plus(
                Config.getIntProperty("EXPIRE_DAYS_TO_NONE_EXPERIMENT", 30),
                ChronoUnit.DAYS);
    }

    private void setCookie(
            final SelectedExperiment experimentSelected,
            final HttpServletResponse response, final Instant expire) {

        final String cookieValue = getCookieValue(experimentSelected);
        final String cookieName = getCookieName(experimentSelected);
        final  Cookie runningExperimentCookie = new Cookie(cookieName, cookieValue);

        final Duration res = Duration.between(Instant.now(), expire);
        runningExperimentCookie.setMaxAge((int) res.getSeconds());

        response.addCookie(runningExperimentCookie);
    }

    private String getCookieName(final SelectedExperiment experimentSelected) {
        return "runningExperiment_" + experimentSelected.id();
    }

    private String getCookieValue(final SelectedExperiment experimentSelected) {
        final String cookieValue = Arrays.stream((new String[]{"experiment:" + experimentSelected.id(),
                        "variant:" + experimentSelected.variant().name(),
                        "lookBackWindow:" + nextLookBackWindow()})).sequential()
                .collect(Collectors.joining(StringPool.AMPERSAND));
        return cookieValue;
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
                return new SelectedVariant(variant.id(), "");
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

        return new SelectedExperiment(experiment.id().get(), experiment.name(), htmlPageAsset.getPageUrl(),
                variantSelected);
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

        if (!rule.isPresent()) {
            return true;
        }

        rule.get().checkValid();
        return rule.get().evaluate(request, response);
    }
}
