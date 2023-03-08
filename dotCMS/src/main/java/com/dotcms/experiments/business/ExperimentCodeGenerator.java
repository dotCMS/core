package com.dotcms.experiments.business;

import static com.dotmarketing.util.FileUtil.getFileContentFromResourceContext;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

/**
 * Util class to generate the JS/HTML code needed to Support Experiment into the Browser.
 */
public enum ExperimentCodeGenerator {
    INSTANCE;

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
            final String analyticsKey = ConfigExperimentUtil.INSTANCE.getAnalyticsKey(host);
            final String jsJitsuCode =  getFileContentFromResourceContext("experiment/html/experiment_head.html")
                    .replaceAll("\\$\\{jitsu_key}", analyticsKey)
                    .replaceAll("\\$\\{site}", getLocalServerName(request));

            final String runningExperimentsId = APILocator.getExperimentsAPI().getRunningExperiments().stream()
                    .map(experiment -> "'" + experiment.id().get() + "'")
                    .collect(Collectors.joining(","));

            final String shouldBeInExperimentCalled =  getFileContentFromResourceContext("experiment/js/init_script.js")
                    .replaceAll("\\$\\{running_experiments_list}", runningExperimentsId);

            return jsJitsuCode + "\n<SCRIPT>" + shouldBeInExperimentCalled + "</SCRIPT>";
        } catch (IOException | DotDataException e) {
            throw new RuntimeException(e);
        }
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
    public Optional<String> getCode(final Host host, final HttpServletRequest request) {
        if (ConfigExperimentUtil.INSTANCE.isExperimentEnabled()) {

            try {
                final String code = APILocator.getExperimentsAPI().isAnyExperimentRunning() ?
                        getJSCode(host, request) : getNotExperimentRunningJSCode();
                return Optional.of(code);
            } catch (DotDataException e) {
                Logger.error(ExperimentCodeGenerator.class, "It is not possible to generate the Experiment JS COde:" + e.getMessage());
            }
        }

        return Optional.empty();
    }
}
