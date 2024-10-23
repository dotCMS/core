package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.analytics.track.collectors.WebEventsCollectorService;
import com.dotcms.analytics.track.collectors.WebEventsCollectorServiceFactory;
import com.dotcms.analytics.track.matchers.UserCustomDefinedRequestMatcher;
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
    private transient final WebEventsCollectorService webEventsCollectorService;

    private static final long serialVersionUID = 1L;
    public static final String EVENT_TYPE = "eventType";
    public static final String OBJECT_TYPE = "objectType";
    public static final String OBJECT_ID = "objectId";
    public static final String REQUEST_ID = "requestId";
    public static final String CONTENT = "CONTENT";
    public static final String ID = "id";
    public static final String OBJECT_CONTENT_TYPE_VAR_NAME = "object_content_type_var_name";
    public static final String OBJECT = "object";
    public static final String EVENT_TYPE1 = "event_type";

    public RuleAnalyticsFireUserEventActionlet() {
        this(WebEventsCollectorServiceFactory.getInstance().getWebEventsCollectorService());
    }

    public RuleAnalyticsFireUserEventActionlet(final WebEventsCollectorService webEventsCollectorService) {
        super("api.system.ruleengine.actionlet.analytics_user_event",
                new ParameterDefinition<>(0, EVENT_TYPE, new TextInput<>(new TextType().required())),
                new ParameterDefinition<>(1, OBJECT_TYPE, new TextInput<>(new TextType())),
                new ParameterDefinition<>(2, OBJECT_ID, new TextInput<>(new TextType()))
        );

        this.webEventsCollectorService = webEventsCollectorService;
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(parameters);
    }

    @Override
    public boolean evaluate(final HttpServletRequest request, final HttpServletResponse response, final Instance instance) {

        final String identifier = getRuleId(request, response);

        request.setAttribute(REQUEST_ID, Objects.nonNull(request.getAttribute(REQUEST_ID)) ?
                request.getAttribute(REQUEST_ID) : UUIDUtil.uuid());
        final HashMap<String, String> objectDetail = new HashMap<>();
        final Map<String, Serializable> userEventPayload = new HashMap<>();

        userEventPayload.put(ID, Objects.nonNull(instance.objectId) ? instance.objectId : identifier);

        objectDetail.put(ID, identifier);
        objectDetail.put(OBJECT_CONTENT_TYPE_VAR_NAME, Objects.nonNull(instance.objectType) ? instance.objectType : CONTENT);
        userEventPayload.put(OBJECT, objectDetail);
        userEventPayload.put(EVENT_TYPE1, instance.eventType);
        webEventsCollectorService.fireCollectorsAndEmitEvent(request, response, USER_CUSTOM_DEFINED_REQUEST_MATCHER, userEventPayload);

        return true;
    }

    private String getRuleId(final HttpServletRequest request, final HttpServletResponse response) {

        return Optional.of(request.getAttribute(WebKeys.RULES_ENGINE_PARAM_CURRENT_RULE_ID)).orElseGet(()-> StringPool.UNKNOWN).toString();
    }

    public class Instance implements RuleComponentInstance {

        private final String eventType;
        private final String objectType;
        private final String objectId;

        public Instance(final Map<String, ParameterModel> parameters) {

            assert parameters != null;
            this.eventType = parameters.getOrDefault(EVENT_TYPE, new ParameterModel()).getValue();
            this.objectType = parameters.getOrDefault(OBJECT_TYPE, new ParameterModel()).getValue();
            this.objectId = parameters.getOrDefault(OBJECT_ID, new ParameterModel()).getValue();
        }

    }
}
