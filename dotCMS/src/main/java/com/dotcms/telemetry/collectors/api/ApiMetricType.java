package com.dotcms.telemetry.collectors.api;


import com.dotcms.telemetry.MetricType;
import com.dotcms.telemetry.util.MetricCaches;
import com.dotcms.rest.api.v1.HTTPMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * This class is designed to track how many times a specific API endpoint is hit. To create a metric
 * that counts the number of times a particular endpoint is called, you should extend this class.
 *
 * <h4>How It Works</h4>
 * <p>
 * A process is in place to collect each hit on the endpoint. A listener monitors every hit and
 * stores the data in a database table called metrics_temp. When metrics are calculated, the
 * necessary data is retrieved from this table. If you want more details on how this process works,
 * you can refer to the accompanying diagram.
 * <p>
 * This class, or any class that extends it, is used to gather the data that will be included and
 * categorized in the MetricSnapshot. The APIMetricType class overrides the getValue method from
 * MetricType and also introduces new methods that can be implemented for any custom APIMetricType
 * you want to create.
 *
 * <h4>Method Details</h4>
 * <p>
 * - getValue(): This method is overridden to filter data from the metrics_temp table based on the
 * implementation of the getName, getFeature, and getCategory methods. It returns the average number
 * of hits per hour for a specific endpoint. The endpoint to be monitored is determined by methods
 * that each APIMetricType must implement. - getAPIUrl(): Returns a string representing the URL of
 * the endpoint to monitor. The URL should be relative. For instance, if dotCMS is running at
 * http://localhost:8080, and you want to track hits to the URL /contentAsset/image, you would
 * return the string contentAsset/image (excluding the domain name, protocol, and port). -
 * getHttpMethod(): Returns an HTTPMethod specifying which HTTP method (GET, POST, etc.) to monitor
 * for the endpoint. - shouldCount(): Returns a boolean value to determine whether a specific hit
 * should be counted. For example, you can set /contentAsset/image as the URL and GET as the HTTP
 * method, and use this method to check for specific parameters in the request. The hit is only
 * counted if the parameter is present.
 * <p>
 * Additionally, you need to override the metadata methods getName, getCategory, and getFeature from
 * MetricType, as these are not overridden by APIMetricType.
 */
public abstract class ApiMetricType implements MetricType {

    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");

    /**
     * Url of the Endpoint
     *
     * @return the URL of the endpoint
     */
    public abstract String getAPIUrl();

    /**
     * Http method of the Endpoint
     *
     * @return the HTTP method of the endpoint
     */
    public abstract HTTPMethod getHttpMethod();

    @Override
    public final Optional<Object> getValue() {
        final Optional<Map<String, Object>> metricTypeItem = getMetricTypeItem();

        return metricTypeItem.flatMap(item -> Optional.ofNullable(item.get("average_per_hour")))
                .or(() -> Optional.of(0));
    }

    /**
     * Returns the unique value, indicating how many times the Endpoint was called with the same
     * request. To determine if the request is the same, the method checks the following:
     * <ul>
     *      <li>Query Parameters</li>
     *      <li>URL Parameters</li>
     *      <li>Request Body</li>
     * </ul>
     * <p>All of these need to be exactly the same to be considered the same request.</p>
     *
     * @return the unique value
     */
    public final Object getUnique() {
        final double uniqueAveragePerHour = getMetricTypeItem()
                .map(item -> item.get("unique_average_per_hour"))
                .map(value -> Double.parseDouble(value.toString()))
                .orElse(0d);
        return FORMAT.format(uniqueAveragePerHour);
    }

    /**
     * @return
     */
    private Optional<Map<String, Object>> getMetricTypeItem() {
        final Collection<Map<String, Object>> result = MetricCaches.TEMPORARY_TABLA_DATA.get();

        return result.stream()
                .filter(item -> item.get("feature").toString().equals(getFeature().name()))
                .filter(item -> item.get("category").toString().equals(getCategory().name()))
                .filter(item -> item.get("name").toString().equals(getName()))
                .limit(1)
                .findFirst();
    }

    public boolean shouldCount(final HttpServletRequest request,
                               final HttpServletResponse response) {
        return true;
    }

}
