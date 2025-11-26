package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.analytics.track.collectors.Collector;
import com.dotcms.analytics.track.collectors.EventSource;
import com.dotcms.analytics.track.collectors.WebEventsCollectorService;
import com.dotcms.analytics.track.collectors.WebEventsCollectorServiceFactory;
import com.dotcms.analytics.track.matchers.UserCustomDefinedRequestMatcher;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.display.TextInput;
import com.dotmarketing.portlets.rules.parameter.type.TextType;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.WebKeys;
import com.liferay.util.StringPool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * This actionlet allows to fire an user event to the analytics backend.
 * @author jsanca
 */
public class RuleAnalyticsFireUserEventActionlet extends RuleActionlet<RuleAnalyticsFireUserEventActionlet.Instance> {

    public static final UserCustomDefinedRequestMatcher USER_CUSTOM_DEFINED_REQUEST_MATCHER =  new UserCustomDefinedRequestMatcher();
    private final transient WebEventsCollectorService webEventsCollectorService;

    private static final long serialVersionUID = 1L;
    public static final String RULE_EVENT_TYPE = "eventType";
    public static final String RULE_OBJECT_TYPE = "objectType";
    public static final String RULE_OBJECT_ID = "objectId";
    public static final String RULE_REQUEST_ID = "requestId";
    public static final String RULE_CONTENT = "CONTENT";

    public RuleAnalyticsFireUserEventActionlet() {
        this(WebEventsCollectorServiceFactory.getInstance().getWebEventsCollectorService());
    }

    public RuleAnalyticsFireUserEventActionlet(final WebEventsCollectorService webEventsCollectorService) {
        super("api.system.ruleengine.actionlet.analytics_user_event",
                new ParameterDefinition<>(0, RULE_EVENT_TYPE, new TextInput<>(new TextType().required())),
                new ParameterDefinition<>(1, RULE_OBJECT_TYPE, new TextInput<>(new TextType())),
                new ParameterDefinition<>(2, RULE_OBJECT_ID, new TextInput<>(new TextType()))
        );

        this.webEventsCollectorService = webEventsCollectorService;
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(parameters);
    }

    @Override
    public boolean evaluate(final HttpServletRequest request, final HttpServletResponse response, final Instance instance) {

        final String identifier = getRuleId(request);

        request.setAttribute(RULE_REQUEST_ID, Objects.nonNull(request.getAttribute(RULE_REQUEST_ID)) ?
                request.getAttribute(RULE_REQUEST_ID) : UUIDUtil.uuid());
        final HashMap<String, String> objectDetail = new HashMap<>();
        final Map<String, Serializable> userEventPayload = new HashMap<>();

        userEventPayload.put(Collector.ID, Objects.nonNull(instance.objectId) ? instance.objectId : identifier);

        objectDetail.put(Collector.ID, identifier);
        objectDetail.put(Collector.CONTENT_TYPE_VAR_NAME, Objects.nonNull(instance.objectType) ? instance.objectType : RULE_CONTENT);
        userEventPayload.put(Collector.OBJECT, objectDetail);
        userEventPayload.put(Collector.EVENT_TYPE, instance.eventType);
        userEventPayload.put(Collector.EVENT_SOURCE, EventSource.RULE.getName());
        webEventsCollectorService.fireCollectorsAndEmitEvent(request, response, USER_CUSTOM_DEFINED_REQUEST_MATCHER, userEventPayload, Map.of());

        return true;
    }

    private String getRuleId(final HttpServletRequest request) {

        return Optional.of(request.getAttribute(WebKeys.RULES_ENGINE_PARAM_CURRENT_RULE_ID)).orElseGet(()-> StringPool.UNKNOWN).toString();
    }

    public class Instance implements RuleComponentInstance {

        private final String eventType;
        private final String objectType;
        private final String objectId;

        public Instance(final Map<String, ParameterModel> parameters) {

            DotPreconditions.checkNotNull(parameters, "parameters can't be null");
            this.eventType = parameters.getOrDefault(RULE_EVENT_TYPE, new ParameterModel()).getValue();
            this.objectType = parameters.getOrDefault(RULE_OBJECT_TYPE, new ParameterModel()).getValue();
            this.objectId = parameters.getOrDefault(RULE_OBJECT_ID, new ParameterModel()).getValue();
        }
    }


}
