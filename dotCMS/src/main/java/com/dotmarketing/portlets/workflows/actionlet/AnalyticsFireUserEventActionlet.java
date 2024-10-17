package com.dotmarketing.portlets.workflows.actionlet;

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

    private final WebEventsCollectorService webEventsCollectorService;

    private static final UserCustomDefinedRequestMatcher USER_CUSTOM_DEFINED_REQUEST_MATCHER =  new UserCustomDefinedRequestMatcher();

    public AnalyticsFireUserEventActionlet() {
        this(WebEventsCollectorServiceFactory.getInstance().getWebEventsCollectorService());
    }
    public AnalyticsFireUserEventActionlet(final WebEventsCollectorService webEventsCollectorService) {
        this.webEventsCollectorService = webEventsCollectorService;
    }

    @Override
    public List<WorkflowActionletParameter> getParameters() {
        List<WorkflowActionletParameter> params = new ArrayList<>();

        params.add(new WorkflowActionletParameter(EVENT_TYPE, "Event type", "", true));
        params.add(new WorkflowActionletParameter(OBJECT_TYPE, "Object type", "", false));
        params.add(new WorkflowActionletParameter(OBJECT_ID, "Object ID", "", false));

        return params;
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

            request.setAttribute("requestId", Objects.nonNull(request.getAttribute("requestId")) ?
                    request.getAttribute("requestId") : UUIDUtil.uuid());
            final Map<String, Serializable> userEventPayload = new HashMap<>();
            userEventPayload.put("id", Objects.nonNull(objectId) ? objectId : identifier);
            userEventPayload.put("event_type", eventType);
            // todo: I am not sure about the object type, I will add it as a custom field
            webEventsCollectorService.fireCollectorsAndEmitEvent(request, response, USER_CUSTOM_DEFINED_REQUEST_MATCHER, userEventPayload);
        } else {
            Logger.warn(this, "The request or response is null, can't send the event for the contentlet: " + identifier);
        }
    }


}
