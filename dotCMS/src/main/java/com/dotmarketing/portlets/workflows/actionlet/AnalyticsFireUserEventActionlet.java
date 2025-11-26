package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.analytics.track.collectors.Collector;
import com.dotcms.analytics.track.collectors.EventSource;
import com.dotcms.analytics.track.collectors.WebEventsCollectorService;
import com.dotcms.analytics.track.collectors.WebEventsCollectorServiceFactory;
import com.dotcms.analytics.track.matchers.UserCustomDefinedRequestMatcher;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This Actionlet allows to send an analytics user custom event
 * @author jsanca
 */
public class AnalyticsFireUserEventActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;
    public static final String EVENT_TYPE = "eventType";
    public static final String OBJECT_TYPE = "objectType";
    public static final String OBJECT_ID = "objectId";
    public static final String REQUEST_ID = "requestId";
    public static final String CONTENT = "CONTENT";
    public static final String EVENT_TYPE_DISPLAY = "Event type";
    public static final String OBJECT_TYPE_DISPLAY = "Object type";
    public static final String OBJECT_ID_DISPLAY = "Object ID";

    private final transient WebEventsCollectorService webEventsCollectorService;

    public static final UserCustomDefinedRequestMatcher USER_CUSTOM_DEFINED_REQUEST_MATCHER =  new UserCustomDefinedRequestMatcher();

    public AnalyticsFireUserEventActionlet() {
        this(WebEventsCollectorServiceFactory.getInstance().getWebEventsCollectorService());
    }
    public AnalyticsFireUserEventActionlet(final WebEventsCollectorService webEventsCollectorService) {
        this.webEventsCollectorService = webEventsCollectorService;
    }

    @Override
    public List<WorkflowActionletParameter> getParameters() {
        List<WorkflowActionletParameter> workflowActionletParameters = new ArrayList<>();

        workflowActionletParameters.add(new WorkflowActionletParameter(EVENT_TYPE, EVENT_TYPE_DISPLAY, StringPool.BLANK, true));
        workflowActionletParameters.add(new WorkflowActionletParameter(OBJECT_TYPE, OBJECT_TYPE_DISPLAY,  StringPool.BLANK, false));
        workflowActionletParameters.add(new WorkflowActionletParameter(OBJECT_ID, OBJECT_ID_DISPLAY,  StringPool.BLANK, false));

        return workflowActionletParameters;
    }

    @Override
    public String getName() {
        return "Fire analytics event";
    }

    @Override
    public String getHowTo() {
        return "This actionlet will send an event to the analytics system.";
    }

    @Override
    public void executeAction(final WorkflowProcessor processor,
                              final Map<String, WorkflowActionClassParameter> params)
            throws WorkflowActionFailureException {

        final Contentlet contentlet = processor.getContentlet();
        final String identifier = contentlet.getIdentifier();
        final String eventType = params.get(EVENT_TYPE).getValue();
        final String objectType = params.get(OBJECT_TYPE).getValue();
        final String objectId = params.get(OBJECT_ID).getValue();
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final HttpServletResponse response = HttpServletResponseThreadLocal.INSTANCE.getResponse();
        if (Objects.nonNull(request) && Objects.nonNull(response)) {

            request.setAttribute(REQUEST_ID, Objects.nonNull(request.getAttribute(REQUEST_ID)) ?
                    request.getAttribute(REQUEST_ID) : UUIDUtil.uuid());
            final HashMap<String, String> objectDetail = new HashMap<>();
            final Map<String, Serializable> userEventPayload = new HashMap<>();

            userEventPayload.put(Collector.ID, UtilMethods.isSet(objectId) ? objectId.trim() : identifier);

            objectDetail.put(Collector.ID, identifier);
            objectDetail.put(Collector.CONTENT_TYPE_VAR_NAME, UtilMethods.isSet(objectType) ? objectType.trim() : CONTENT);
            userEventPayload.put(Collector.OBJECT, objectDetail);
            userEventPayload.put(Collector.EVENT_SOURCE, EventSource.WORKFLOW.getName());
            userEventPayload.put(Collector.EVENT_TYPE, UtilMethods.isSet(eventType)? eventType.trim(): eventType);
            webEventsCollectorService.fireCollectorsAndEmitEvent(request, response,
                    USER_CUSTOM_DEFINED_REQUEST_MATCHER, userEventPayload, Map.of());
        } else {
            Logger.warn(this, "The request or response is null, can't send the event for the contentlet: " + identifier);
            // note: here we need more info to populate what the collectors used to populate from the request
            // but we need the ui details in order to get the extra information
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AnalyticsFireUserEventActionlet that = (AnalyticsFireUserEventActionlet) o;
        return Objects.equals(webEventsCollectorService, that.webEventsCollectorService);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(webEventsCollectorService);
    }
}
