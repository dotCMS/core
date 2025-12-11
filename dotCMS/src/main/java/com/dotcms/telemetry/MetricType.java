package com.dotcms.telemetry;

import com.dotmarketing.exception.DotDataException;

import java.util.Optional;

/**
 * This interface represents a Metric that needs to be calculated and included in a MetricSnapshot.
 * For each of these a new concrete class implements this interface must be created. The interface
 * provides a set of methods to define the metadata for the Metric, such as its name, description,
 * category, and feature. It also includes a method to calculate the Metric's value.
 * <ul>
 *     <li>Metadata Methods: The getName, getDescription, getCategory, and getFeature methods can
 *     be overridden to set the metadata for a specific Metric. The getMetric method creates a
 *     Metric object using the values returned by these methods. The Metric class represents the
 *     metadata of a single Metric.</li>
 *     <li>Value Calculation Method: You can override the getValue method to define how the
 *     Metric value is calculated. This method returns an object, so the value can be of any type.
 *     To perform this calculation, you can use SQL queries, dotCMS API methods, Java core code, or
 *     any other approach that suits your needs.</li>
 * </ul>
 * <p>Some of the Metrics to collect are:</p>
 * <ul>
 *     <li>Count of workflow schemes</li>
 *     <li>Count of Site Search indexes</li>
 *     <li>Count of sites</li>
 *     <li>Count of content types with URL maps</li>
 *     <li>etc</li>
 * </ul>
 *
 * @see MetricCategory
 * @see MetricFeature
 */
public interface MetricType {

    String getName();

    String getDescription();

    MetricCategory getCategory();

    MetricFeature getFeature();

    Optional<Object> getValue();

    /**
     * Returns a human-readable display label for this metric.
     * Used by UI components to display user-friendly names instead of technical metric names.
     * 
     * <p>Default implementation generates a short label from the metric name by:
     * <ul>
     *   <li>Converting underscores to spaces</li>
     *   <li>Removing common prefixes like "COUNT_OF_", "TOTAL_", etc.</li>
     *   <li>Capitalizing words appropriately</li>
     * </ul>
     * 
     * <p>For better control, metrics should override this method to provide
     * concise, user-friendly labels (typically 2-4 words).</p>
     * 
     * @return the display label for this metric
     */
    default String getDisplayLabel() {
        final String name = getName();
        if (name == null || name.isEmpty()) {
            return "Metric";
        }
        
        // Convert name to a readable format
        String label = name
                .replace("COUNT_OF_", "")
                .replace("COUNT_", "")
                .replace("TOTAL_", "")
                .replace("_", " ")
                .toLowerCase();
        
        // Capitalize first letter of each word
        final StringBuilder result = new StringBuilder();
        final String[] words = label.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            if (!words[i].isEmpty()) {
                result.append(Character.toUpperCase(words[i].charAt(0)));
                if (words[i].length() > 1) {
                    result.append(words[i].substring(1));
                }
            }
        }
        
        return result.toString();
    }

    default Metric getMetric() {
        return new Metric.Builder()
                .name(getName())
                .description(getDescription())
                .category(getCategory())
                .feature(getFeature())
                .build();
    }

    default Optional<MetricValue> getStat() throws DotDataException {
        return getValue().map(o -> new MetricValue(this.getMetric(), o));
    }

}
