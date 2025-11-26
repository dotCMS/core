package com.dotcms.telemetry;

/**
 * Represents all the information needed to identify a Metric:
 * <ul>
 *     <li>Name</li>
 *     <li>Description</li>
 *     <li>Category</li>
 *     <li>Feature</li>
 * </ul>
 */
public class Metric {

    private final String name;
    private final String description;
    private final MetricCategory category;
    private final MetricFeature feature;

    public Metric(final Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.category = builder.category;
        this.feature = builder.feature;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public MetricCategory getCategory() {
        return category;
    }

    public MetricFeature getFeature() {
        return feature;
    }

    @Override
    public String toString() {
        return "Metric{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", category=" + category +
                ", feature=" + feature +
                '}';
    }

    public static class Builder {
        String name;
        String description;
        MetricCategory category;
        MetricFeature feature;

        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder description(final String description) {
            this.description = description;
            return this;
        }

        public Builder category(final MetricCategory category) {
            this.category = category;
            return this;
        }

        public Builder feature(final MetricFeature feature) {
            this.feature = feature;
            return this;
        }

        public Metric build() {
            return new Metric(this);
        }
    }

}
