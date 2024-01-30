package com.dotcms.analytics.experience.metric.collector;

import com.dotcms.analytics.experience.metric.MetricCategory;
import com.dotcms.analytics.experience.metric.MetricFeature;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonValue;
import com.liferay.portal.model.User;

public enum MetricCollectorType {

    WORKFLOW_SCHEMES_COUNT(MetricCategory.KEY_FEATURES, MetricFeature.WORKFLOWS,
            (User user) -> APILocator.getWorkflowAPI().countWorkflowSchemes(user)),
    CONTENT_TYPES_NOT_USING_SYSTEM_WORKFLOW(MetricCategory.KEY_FEATURES, MetricFeature.WORKFLOWS,
            (User user) -> APILocator.getContentTypeAPI(APILocator.systemUser()).countContentTypeAssignedToNotSystemWorkflow()),
    WORKFLOW_STEPS_COUNT(MetricCategory.KEY_FEATURES, MetricFeature.WORKFLOWS,
            (User user) -> APILocator.getWorkflowAPI().countAllSchemasSteps(user)),
    WORKFLOW_ACTIONS_COUNT(MetricCategory.KEY_FEATURES, MetricFeature.WORKFLOWS,
            (User user) -> APILocator.getWorkflowAPI().countAllSchemasActions(user)),
    WORKFLOW_SUB_ACTIONS_COUNT(MetricCategory.KEY_FEATURES, MetricFeature.WORKFLOWS,
            (User user) -> APILocator.getWorkflowAPI().countAllSchemasSubActions(user)),
    WORKFLOW_UNIQUE_SUB_ACTIONS_COUNT(MetricCategory.KEY_FEATURES,
            MetricFeature.WORKFLOWS, (User user) -> APILocator.getWorkflowAPI().countAllSchemasUniqueSubActions(user));
    private final MetricCollector<?> collector;
    private final MetricCategory category;
    private final MetricFeature feature;

    MetricCollectorType(final MetricCategory category, final MetricFeature feature, MetricCollector<?> collector) {
        this.category = category;
        this.feature = feature;
        this.collector = collector;
    }

    public MetricCollector<?> getCollector() {
        return collector;
    }

    public MetricCategory getCategory() {
        return category;
    }

    public MetricFeature getFeature() {
        return feature;
    }

    @JsonValue
    public String getCamelCaseName()  {
        return StringUtils.camelCaseLower(this.toString());
    }
}
