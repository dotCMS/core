package com.dotcms.telemetry.collectors.container;

import com.dotcms.telemetry.business.MetricsAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.templates.model.Template;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class count the amount of containers used in LIVE templates, in this case no matter the
 * pages so this templates can be used or not on a page. This class extends from
 * {@link TotalContainersInTemplateDatabaseMetricType} because it is a counter of the containers
 * used in templates, and it overrides the follow behavior:
 * <ul>
 *     <li>Searching Templates: Override the getTemplatesIds method to return only the Template
 *     IDs for the LIVE templates. This is achieved using a SQL UNION query. The first part of the
 *     query retrieves standard templates from the {@code template_version_info} table, identifying
 *     those that have LIVE versions. The second part of the query retrieves file templates from the
 *     {@code contenlet_version_info} table, also focusing on those with live versions.</li>
 *     <li>Retrieve the Template Version: Override the {@code getTemplate} method to get the last
 *     LIVE version of the Template.</li>
 * </ul>
 */
public abstract class TotalContainersInLiveTemplatesDatabaseMetricType extends TotalContainersInTemplateDatabaseMetricType {

    private static final String LIVE_TEMPLATES_INODES_QUERY = "SELECT DISTINCT template" +
            ".identifier as value " +
            "FROM template_version_info " +
            "INNER JOIN template ON template_version_info.identifier = template.identifier " +
            "WHERE title NOT LIKE 'anonymous_layout_%' and deleted = false";

    private static final String LIVE_FILE_TEMPLATES_INODES_QUERY = "SELECT distinct id" +
            ".parent_path as value " +
            "FROM contentlet_version_info cvi INNER JOIN identifier id ON cvi.identifier = id.id " +
            "WHERE id.parent_path LIKE '/application/templates/%' AND id.asset_name = 'body.vtl' " +
            "AND deleted = false AND live_inode is not null";

    protected MetricsAPI metricsAPI;

    @Override
    Collection<String> getTemplatesIds() {
        final List<String> dataBaseTemplateInode = metricsAPI.getList(LIVE_TEMPLATES_INODES_QUERY);
        final List<String> dataBaseFileTemplateInode = metricsAPI.getList(LIVE_FILE_TEMPLATES_INODES_QUERY);

        return Stream.concat(dataBaseTemplateInode.stream(),
                dataBaseFileTemplateInode.stream()).collect(Collectors.toSet());
    }

    @Override
    final Template getTemplate(String id) throws DotDataException, DotSecurityException {
        return APILocator.getTemplateAPI()
                .findLiveTemplate(id, APILocator.systemUser(), false);
    }

}

