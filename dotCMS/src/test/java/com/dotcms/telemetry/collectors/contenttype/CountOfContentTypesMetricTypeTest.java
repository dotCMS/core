package com.dotcms.telemetry.collectors.contenttype;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CountOfContentTypesMetricTypeTest {

    private final CountOfContentTypesMetricType metric = new CountOfContentTypesMetricType();

    @Test
    public void testGetName() {
        assertEquals("COUNT_OF_CONTENT_TYPES", metric.getName());
    }

    @Test
    public void testGetDescription() {
        assertNotNull(metric.getDescription());
        assertEquals("Total number of content types (structures)", metric.getDescription());
    }

    @Test
    public void testGetCategory() {
        assertEquals(MetricCategory.DIFFERENTIATING_FEATURES, metric.getCategory());
    }

    @Test
    public void testGetFeature() {
        assertEquals(MetricFeature.CONTENT_TYPES, metric.getFeature());
    }

    @Test
    public void testGetSqlQuery() {
        final String query = metric.getSqlQuery();
        assertNotNull("SQL query should not be null", query);
        assertEquals("SELECT COUNT(*) AS value FROM structure", query);
    }
}
