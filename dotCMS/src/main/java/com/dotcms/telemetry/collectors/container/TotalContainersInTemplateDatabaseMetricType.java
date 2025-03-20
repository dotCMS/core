package com.dotcms.telemetry.collectors.container;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.MetricType;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.design.util.DesignTemplateUtil;
import com.dotmarketing.portlets.templates.model.Template;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Super class to use if you want to generate a Metric to count the containers Used in templates
 *
 * To create a metric that counts the number of containers used in templates, you need to subclass this class.
 * This class overrides the getValue method to perform a search for containers, which can be customized in your implementation.
 * Here's what the new implementation of the getValue method does:
 * - Searching Templates: This method identifies which templates to consider. You can customize the selection by overriding
 * the getTemplateIds method in your subclass. For example, you might decide to include only LIVE, WORKING,
 * or Advanced Templates. To apply your own criteria for filtering templates, you need to override the getTemplateIds method.
 * - Retrieve the Template Version: You can customize this step to suit your needs, such as selecting the most recent
 * LIVE or WORKING version. To modify this behavior, override the getTemplate method.
 * - Retrieve Containers: It gathers the containers used by each template version selected on the two previous steps.
 * - Filtering Containers: The method filters the containers based on your specific requirements.
 * You can also customize this step by overriding the filterContainer method in your subclass.
 *
 * @see TotalContainersInLivePageDatabaseMetricType
 * @see TotalContainersInWorkingPageDatabaseMetricType
 */
public abstract class TotalContainersInTemplateDatabaseMetricType implements MetricType {

    @Override
    public final MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public final MetricFeature getFeature() {
        return MetricFeature.LAYOUT;
    }

    @Override
    public final Optional<Object> getValue() {
        final Collection<String> templatesIds = getTemplatesIds();
        final Set<String> containersUsed = new HashSet<>();

        try {
            for (String id : templatesIds) {
                final Template template = getTemplate(id);

                if (template == null) {
                    continue;
                }

                final Collection<String> containersId = getContainersInTemplate(template).stream()
                        .filter(this::filterContainer)
                        .collect(Collectors.toSet());
                containersUsed.addAll(containersId);
            }

            return Optional.of(containersUsed.size());
        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }


    }

    abstract boolean filterContainer(final String containerId);

    public Collection<String> getContainersInTemplate(final Template template) {
        final Collection<ContainerUUID> containerUUIDs;

        if(Boolean.TRUE.equals(template.isDrawed())) {
            final TemplateLayout layout = template.getDrawedBody() != null ? getLayout(template.getDrawedBody()) : null;
            containerUUIDs = layout != null ? APILocator.getTemplateAPI().getContainersUUID(layout) :
                Collections.emptySet();
        } else {
            containerUUIDs = new HashSet<>(
                    APILocator.getTemplateAPI().getContainersUUIDFromDrawTemplateBody(template.getBody()));
        }

        return containerUUIDs.stream()
                .map(ContainerUUID::getIdentifier)
                .filter(identifier -> !Container.SYSTEM_CONTAINER.equals(identifier))
                .distinct()
                .collect(Collectors.toList());
    }

    private TemplateLayout getLayout(final String drawedBody) {

        try {
            return DotTemplateTool.getTemplateLayoutFromJSON(drawedBody);
        } catch (IOException var4) {
            return DesignTemplateUtil.getDesignParameters(drawedBody, false);
        }
    }

    abstract Collection<String> getTemplatesIds();
    abstract Template getTemplate(final String id) throws DotDataException, DotSecurityException;
}

