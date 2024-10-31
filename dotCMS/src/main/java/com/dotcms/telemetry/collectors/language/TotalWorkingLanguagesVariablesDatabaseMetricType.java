package com.dotcms.telemetry.collectors.language;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.collectors.DBMetricType;

/**
 * Collect the count of Language Variable that does not have Live Version.
 */
public class TotalWorkingLanguagesVariablesDatabaseMetricType implements DBMetricType {
    @Override
    public String getName() {
        return "WORKING_LANGUAGE_VARIABLE_COUNT";
    }

    @Override
    public String getDescription() {
        return "Count of Working Language Variables";
    }

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.LANGUAGES;
    }

    @Override
    public String getSqlQuery() {
        return "SELECT COUNT(distinct c.identifier) AS value " +
                "FROM contentlet AS c " +
                    "INNER JOIN structure ON c.structure_inode=structure.inode " +
                    "INNER JOIN contentlet_version_info ON c.identifier = contentlet_version_info.identifier " +
                "WHERE structuretype = " + BaseContentType.KEY_VALUE.getType() + " AND deleted = false AND live_inode is null " +
                    "AND working_inode IS NOT null AND (SELECT COUNT(DISTINCT identifier) " +
                                                        "FROM contentlet_version_info AS cvi2 " +
                                                        "WHERE live_inode IS NOT null " +
                                                            "AND c.identifier = cvi2.identifier) = 0";
    }
}
