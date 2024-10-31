package com.dotcms.telemetry;

/**
 * Represents the Metric Category where the {@link MetricType} belongs to
 */
public enum MetricCategory {

    DIFFERENTIATING_FEATURES("Differentiating Features"),
    PAID_FEATURES("Paid Features"),
    PLATFORM_SPECIFIC_CUSTOMIZATION("Platform-Specific Customization"),
    SOPHISTICATED_CONTENT_ARCHITECTURE("Sophisticated Content Architecture"),
    POSITIVE_USER_EXPERIENCE("Positive User Experience (incl. content velocity)"),
    PLATFORM_SPECIFIC_DEVELOPMENT("Platform-Specific Development"),
    RECENT_ACTIVITY("Recent Activity"),
    HISTORY_LEGACY_DEPRECATED_FEATURES("History (incl. legacy/deprecated features");

    private final String label;

    MetricCategory(final String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

}
