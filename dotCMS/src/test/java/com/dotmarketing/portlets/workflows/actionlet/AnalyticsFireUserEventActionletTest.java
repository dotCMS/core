package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.analytics.track.collectors.Collector;
import com.dotcms.analytics.track.collectors.EventSource;
import com.dotcms.analytics.track.collectors.WebEventsCollectorService;
import com.dotcms.analytics.track.matchers.RequestMatcher;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AnalyticsFireUserEventActionletTest {

    /**
     * When: {@link AnalyticsFireUserEventActionlet#executeAction(WorkflowProcessor, Map)} ()} is null
     * Should: the WebEventsCollectorService is called
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void executeAction_fire_event_with_defaults() throws DotSecurityException, DotDataException {

        final WebEventsCollectorService webEventsCollectorService = new WebEventsCollectorService() {
            @Override
            public void fireCollectors(HttpServletRequest request, HttpServletResponse response, RequestMatcher requestMatcher) {

            }

            @Override
            public void addCollector(Collector... collectors) {

            }

            @Override
            public void removeCollector(String collectorId) {

            }

            @Override
            public void fireCollectorsAndEmitEvent(HttpServletRequest request, HttpServletResponse response, RequestMatcher requestMatcher,
                                                   Map<String, Serializable> userEventPayload, Map<String, Object> baseContext) {

                Assert.assertNotNull(userEventPayload);
                Assert.assertEquals(EventSource.WORKFLOW.getName(), userEventPayload.get(Collector.EVENT_SOURCE));
                Assert.assertEquals("123", userEventPayload.get(Collector.ID));

                final Map<String, String> object = (Map<String, String>) userEventPayload.get(Collector.OBJECT);
                Assert.assertNotNull(object);
                Assert.assertEquals("123", object.get(Collector.ID));
                Assert.assertEquals("CONTENT", object.get(Collector.CONTENT_TYPE_VAR_NAME));
            }
        };
        final AnalyticsFireUserEventActionlet analyticsFireUserEventActionlet = new AnalyticsFireUserEventActionlet(webEventsCollectorService);
        final WorkflowProcessor processor = mock(WorkflowProcessor.class);
        final Map<String, WorkflowActionClassParameter> params = new HashMap<>();
        params.put(AnalyticsFireUserEventActionlet.EVENT_TYPE, new WorkflowActionClassParameter("page"));
        params.put(AnalyticsFireUserEventActionlet.OBJECT_TYPE, new WorkflowActionClassParameter(""));
        params.put(AnalyticsFireUserEventActionlet.OBJECT_ID, new WorkflowActionClassParameter(""));

        final Contentlet contentlet = mock(Contentlet.class);
        final User user  = mock(User.class);
        final var request = mock(HttpServletRequest.class);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        final var response = mock(HttpServletResponse.class);
        HttpServletResponseThreadLocal.INSTANCE.setResponse(response);

        when(processor.getUser()).thenReturn(user);
        when(processor.getContentlet()).thenReturn(contentlet);
        when(contentlet.getIdentifier()).thenReturn("123");

        when(processor.getContentletDependencies()).thenReturn(null);

        analyticsFireUserEventActionlet.executeAction(processor, params);
    }
}
