package com.dotcms.cdn.workflow;

import com.dotcms.cdn.api.DotCDNAPI;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class DotCDNInvalidateActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;
    private static final String PURGE_CONTENTLET_PARAM = "purgeContentlet";
    private static final String PURGE_URL_PARAM = "purgeUrl";

    @Override
    public List<WorkflowActionletParameter> getParameters() {
        final List<WorkflowActionletParameter> params = new ArrayList<>();
        params.add(new WorkflowActionletParameter(PURGE_CONTENTLET_PARAM,
                "Purge Contentlet", "true", false));
        params.add(new WorkflowActionletParameter(PURGE_URL_PARAM,
                "Additional Url(s) to Purge", "", false));
        return params;
    }

    @Override
    public String getName() {
        return "dotCDN Purge";
    }

    @Override
    public String getHowTo() {
        return "<strong>URL(s) to purge:</strong> can be set to specific url(s) to be purged. "
                + "Use comma (,) as a delimiter.</br>"
                + "<strong>Purge Contentlet:</strong> It creates a list of patterns that should "
                + "be purged using the $contentlet object."
                + "e.g. /dA/$contentlet.identifier/* or /dA/$contentlet.shortyInode/*";
    }

    @Override
    public void executeAction(WorkflowProcessor processor,
            Map<String, WorkflowActionClassParameter> params)
            throws WorkflowActionFailureException {

        final Contentlet contentlet = processor.getContentlet();
        final String purgeUrls = params.get(PURGE_URL_PARAM).getValue();
        final boolean isPurgeContentlet = Try.of(
                () -> Boolean.parseBoolean(
                        params.get(PURGE_CONTENTLET_PARAM).getValue().trim()))
                .getOrElse(true);
        final Host host = Try.of(() -> APILocator.getHostAPI()
                .find(contentlet.getHost(), APILocator.systemUser(), false))
                .getOrNull();
        if (host == null) {
            Logger.warn(this.getClass().getName(), "Contentlet Host is Null");
            return;
        }
        if (!DotCDNAPI.isConfigured(host)) {
            Logger.debug(this.getClass().getName(),
                    "dotCDN not configured for host: " + host.getHostname() + ", skipping");
            return;
        }
        final DotCDNAPI cdnApi = DotCDNAPI.api(host);
        final List<String> urlsToPurge = new ArrayList<>();

        if (!UtilMethods.isEmpty(purgeUrls)) {
            urlsToPurge.addAll(parseUrlsParam(purgeUrls));
        }

        Logger.info(this, "PurgeContentlet: " + isPurgeContentlet);
        if (isPurgeContentlet) {
            DotConcurrentFactory.getInstance().getSubmitter().submit(
                    () -> cdnApi.invalidateContentlet(contentlet, urlsToPurge));
        } else {
            DotConcurrentFactory.getInstance().getSubmitter().submit(
                    () -> cdnApi.invalidateRelatedPages(contentlet, urlsToPurge));
        }
    }

    private List<String> parseUrlsParam(final String urlsParam) {
        final List<String> urls = new ArrayList<>();
        if (urlsParam == null) {
            return urls;
        }
        final StringTokenizer st = new StringTokenizer(urlsParam, ",");
        while (st.hasMoreTokens()) {
            urls.add(st.nextToken().trim());
        }
        urls.removeIf(UtilMethods::isEmpty);
        return urls;
    }
}
