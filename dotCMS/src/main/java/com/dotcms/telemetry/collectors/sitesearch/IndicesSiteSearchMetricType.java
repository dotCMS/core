package com.dotcms.telemetry.collectors.sitesearch;

import com.dotcms.content.elasticsearch.business.IndexStats;
import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.MetricType;
import com.dotcms.telemetry.MetricValue;
import com.dotcms.telemetry.util.MetricCaches;
import com.dotmarketing.exception.DotDataException;

import javax.ws.rs.NotSupportedException;
import java.util.Collection;
import java.util.Optional;

/**
 * This class represents any Metric that requires Site Search Index information for its calculation.
 * Any Metric that relies on the Site Search index must extend from this class.
 *
 * <h1>Overridden Methods:</h1>
 *
 * - getStat: This method is called when collecting the Metric values. It relies on a getValue method,
 * which is overridden by each subclass of MetricType. In this class, getStat uses its own version of getValue,
 * necessitating the override.
 *
 * - getValue: This method is overridden to throw a NotSupportedException. As mentioned earlier,
 * this class does not use this method to calculate values; instead, it uses a different getValue method.
 *
 * - getValue(final Collection<IndexStats> indices): This is the actual getValue method used by the getStat method.
 * The getStat method first retrieves the indices information and then calls this getValue method to calculate the Metric's value.
 *
 * <h1>In Summary:</h1>
 * The getStat method works as follows:
 * - Retrieves Site Search Indices Information: It uses the Elasticsearch Client Utility class to send a request to the
 * Elasticsearch server and retrieve the indices information.
 * - Caches the Site Search Indices Information: This information is stored in a cache. If multiple classes need this
 * information, the request to the Elasticsearch server is made only once. Each MetricType then uses the cached information.
 * The cache is cleared when all Metrics are calculated.
 * - Calls the getValue(final Collection<IndexStats> indices) Method: This method calculates the Metric value.
 * It must be overridden by each concrete class that extends from this class.
 *
 * @see MetricType
 */
public abstract class IndicesSiteSearchMetricType implements MetricType {

    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }
    public MetricFeature getFeature() {
        return MetricFeature.SITE_SEARCH;
    }

    @Override
    public Optional<MetricValue> getStat() throws DotDataException {
        return getValue(MetricCaches.SITE_SEARCH_INDICES.get())
                .map(o -> new MetricValue(this.getMetric(), o))
                .or(() -> Optional.of(new MetricValue(this.getMetric(), 0)));

    }

    @Override
    public Optional<Object> getValue() {
       throw new NotSupportedException();
    }

    abstract Optional<Object> getValue(final Collection<IndexStats> indices) throws DotDataException;


}
