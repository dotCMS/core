package com.dotcms.telemetry.collectors.container;

import com.dotcms.telemetry.business.MetricsAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.templates.model.Template;

import java.util.Collection;

/**
 * Total of containers used in LIVE pages, it extends from
 * {@link TotalContainersInTemplateDatabaseMetricType} because it is a counter of the containers
 * used in templates is this case we want to take account just the LIVE version of each one, so it
 * override the follow behavior:
 * <ul>
 *     <li>Searching Templates: Override the getTemplatesIds method to return just the Template that
 *     has LIVE version.</li>
 *     <li>Retrieve the Template Version: Override the getTemplate method to get the last LIVE
 *     version of the Template.</li>
 * </ul>
 *
 * @see TotalContainersInTemplateDatabaseMetricType
 */
public abstract class TotalContainersInLivePageDatabaseMetricType extends TotalContainersInTemplateDatabaseMetricType {

    private static final  String LIVE_USED_TEMPLATES_INODES_QUERY = "SELECT " +
            "distinct contentlet_as_json -> 'fields' -> 'template' ->> 'value' as value " +
            "FROM " +
            "contentlet INNER JOIN contentlet_version_info ON contentlet.inode = contentlet_version_info.live_inode " +
            "WHERE structure_inode IN (SELECT inode FROM structure where name = 'Page') AND " + "deleted = false";

    protected MetricsAPI metricsAPI;

    private Collection<String> getLiveUsedTemplatesInodes() {
        return metricsAPI.getList(LIVE_USED_TEMPLATES_INODES_QUERY);
    }

    @Override
    Collection<String> getTemplatesIds() {
        return getLiveUsedTemplatesInodes();
    }

    @Override
    final Template getTemplate(String id) throws DotDataException, DotSecurityException {
        return APILocator.getTemplateAPI().findLiveTemplate(id, APILocator.systemUser(), false);
    }

}
