package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.UnitTestBase;
import com.dotcms.analytics.track.collectors.Collector;
import com.dotcms.analytics.track.collectors.WebEventsCollectorService;
import com.dotcms.analytics.track.matchers.RequestMatcher;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.util.WebKeys;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This actionlet allows to fire an user event rule to the analytics backend.
 * @author jsanca
 */
public class RuleAnalyticsFireUserEventActionletTest extends UnitTestBase {

    /**
     * * Method to test: RuleAnalyticsFireUserEventActionlet.evaluate
     * * Given Scenario: Creates the context and send the information to the rule
     * * ExpectedResult: The Payload created has to have the expected values based on the inputs
     * @throws Exception
     */
    @Test
    public void testActionletSetsFireUserEventOnHappyPath() throws Exception {

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
            public void fireCollectorsAndEmitEvent(HttpServletRequest request, HttpServletResponse response,
                                                   RequestMatcher requestMatcher, Map<String, Serializable> userEventPayload,
                                                   Map<String, Object> baseContext) {

                Assert.assertNotNull(userEventPayload);
                Assert.assertEquals("page", userEventPayload.get(Collector.EVENT_TYPE));
                Assert.assertEquals("345", userEventPayload.get(Collector.ID));

                final Map<String, String> object = (Map<String, String>) userEventPayload.get(Collector.OBJECT);
                Assert.assertNotNull(object);
                Assert.assertEquals("123", object.get(Collector.ID));
                Assert.assertEquals("CONTENT", object.get(Collector.CONTENT_TYPE_VAR_NAME));
            }
        };

        final RuleAnalyticsFireUserEventActionlet analyticsFireUserEventActionlet = new RuleAnalyticsFireUserEventActionlet(webEventsCollectorService);
        final Map<String, ParameterModel> params = new HashMap<>();
        params.put(RuleAnalyticsFireUserEventActionlet.RULE_EVENT_TYPE, new ParameterModel(RuleAnalyticsFireUserEventActionlet.RULE_EVENT_TYPE, "page"));
        params.put(RuleAnalyticsFireUserEventActionlet.RULE_OBJECT_TYPE, new ParameterModel());
        params.put(RuleAnalyticsFireUserEventActionlet.RULE_OBJECT_ID, new ParameterModel(RuleAnalyticsFireUserEventActionlet.RULE_EVENT_TYPE, "345"));

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getAttribute(WebKeys.RULES_ENGINE_PARAM_CURRENT_RULE_ID)).thenReturn("123");
        final RuleAnalyticsFireUserEventActionlet.Instance instance = analyticsFireUserEventActionlet.instanceFrom(params);

        analyticsFireUserEventActionlet.evaluate(request, response, instance);
    }

}
