package com.dotcms.telemetry.collectors.container;

import com.dotcms.telemetry.business.MetricsAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.templates.model.Template;

import java.util.Collection;

/**
 * Total of containers used in Working pages.
 * <p>
 * This class count the amount of containers used in WORKING pages, it means all these pages that
 * don’t have LIVE Version. This class extends from
 * {@link TotalContainersInTemplateDatabaseMetricType} because it is a counter of the containers
 * used in templates is this case we want to take account just the WORKING version of each one, so
 * it override the follow behavior:
 * <ul>
 *     <li>Searching Templates: Override the {@code getTemplatesIds} method to return only the
 *     Template IDs for the templates used on a WORKING page.This is achieved using a SQL UNION
 *     query. The first part of the query retrieves standard templates from the
 *     {@code template_version_info} table, identifying those that have only a working version. The
 *     second part of the query retrieves file templates from the {@code contenlet_version_info}
 *     table, also focusing on those with just a working version.</li>
 *     <li>Retrieve the Template Version: Override the getTemplate method to get the last WORKING
 *     version of the Template.</li>
 * </ul>
 */
public abstract class TotalContainersInWorkingPageDatabaseMetricType extends TotalContainersInTemplateDatabaseMetricType {

    private static final String WORKING_USED_TEMPLATES_INODES_QUERY =
            "SELECT tvi.identifier AS value " +
            "FROM template_version_info tvi " +
                "INNER JOIN ( " +
                    "SELECT " +
                        "DISTINCT contentlet_as_json -> 'fields' -> 'template' ->> 'value' AS template_id, " +
                            "cvi.working_inode, " +
                            "cvi.live_inode " +
                    "FROM contentlet c " +
                        "INNER JOIN contentlet_version_info cvi ON c.inode = cvi.working_inode " +
                    "WHERE c.structure_inode IN (SELECT inode FROM structure WHERE name = 'Page') " +
                        "AND cvi.deleted = false " +
                ") page ON page.template_id = tvi.identifier " +
            "WHERE tvi.deleted = false " +
                "AND ( " +
                    "(page.live_inode IS NOT NULL AND (tvi.live_inode IS NULL OR tvi.live_inode <> tvi.working_inode)) " +
                    "OR page.live_inode IS NULL " +
                ")";

    private static final String WORKING_USED_FILE_TEMPLATES_INODES_QUERY =
            "SELECT id.id AS value " +
            "FROM identifier id " +
                "INNER JOIN ( " +
                    "SELECT contentlet_as_json->'fields'->'hostName'->>'value' hostName, identifier " +
                    "FROM contentlet " +
                ") host ON host.identifier = id.host_inode " +
                "INNER JOIN ( " +
                    "SELECT " +
                        "DISTINCT contentlet_as_json -> 'fields' -> 'template' ->> 'value' AS template_path, " +
                        "cvi.working_inode, " +
                        "cvi.live_inode " +
                    "FROM contentlet c " +
                        "INNER JOIN contentlet_version_info cvi ON c.inode = cvi.working_inode " +
                    "WHERE c.structure_inode IN (SELECT inode FROM structure WHERE name = 'Page') " +
                        "AND cvi.deleted = false " +
                ") page ON page.template_path = CONCAT('//', hostName, id.parent_path) AND asset_name = 'body.vtl' " +
                "INNER JOIN contentlet_version_info cvi on id.id = cvi.identifier " +
            "WHERE cvi.deleted = false " +
                "AND ( " +
                    "(page.live_inode IS NOT NULL AND (cvi.live_inode IS NULL OR cvi.live_inode <> cvi.working_inode)) " +
                    "OR page.live_inode IS NULL " +
                ")";

    protected MetricsAPI metricsAPI;

    private Collection<String> getWorkingUsedTemplatesInodes() {
        return metricsAPI.getList(WORKING_USED_TEMPLATES_INODES_QUERY + " UNION " +
                WORKING_USED_FILE_TEMPLATES_INODES_QUERY);
    }

    @Override
    Collection<String> getTemplatesIds() {
        return getWorkingUsedTemplatesInodes();
    }

    @Override
    Template getTemplate(String id) throws DotDataException, DotSecurityException {
        return APILocator.getTemplateAPI()
                .findWorkingTemplate(id, APILocator.systemUser(), false);
    }

}

