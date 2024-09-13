package com.dotcms.analytics.content;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jose Castro
 * @since Sep 13th, 2024
 */
public class ReportRequest implements Serializable {

    private final String[] dimensions;
    private final String[] measures;
    private final List<Map<String, Object>> filters;

    private final Map<String, String> orderByClauses;
    private final Map<String, String> timeDimensions;

    private final long limit;
    private final long offset;

    private ReportRequest(final Builder builder) {
        this.dimensions = builder.dimensions;
        this.measures = builder.measures;
        this.filters = builder.filters;
        this.orderByClauses = builder.orderByClauses;
        this.limit = builder.limit;
        this.offset = builder.offset;
        this.timeDimensions = builder.timeDimensions;
    }

    public String[] dimensions() {
        return this.dimensions;
    }

    public String[] measures() {
        return this.measures;
    }

    public List<Map<String, Object>> filters() {
        return this.filters;
    }

    public Map<String, String> orderByClauses() {
        return this.orderByClauses;
    }

    public Map<String, String> timeDimensions() {
        return this.timeDimensions;
    }

    public long limit() {
        return this.limit;
    }

    public long offset() {
        return this.offset;
    }

    @Override
    public String toString() {
        return "ReportRequest{" +
                "dimensions=" + Arrays.toString(dimensions) +
                ", measures=" + Arrays.toString(measures) +
                ", filters=" + filters +
                ", orderByClauses=" + orderByClauses +
                ", timeDimensions=" + timeDimensions +
                ", limit=" + limit +
                ", offset=" + offset +
                '}';
    }

    public static class Builder {

        private String[] dimensions;
        private String[] measures;
        private List<Map<String, Object>> filters;

        private Map<String, String> orderByClauses;
        private Map<String, String> timeDimensions;

        private long limit = -1;
        private long offset = -1;

        public Builder dimensions(final String[] dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public Builder measures(final String[] measures) {
            this.measures = measures;
            return this;
        }

        public Builder filters(final List<Map<String, Object>> filters) {
            this.filters = filters;
            return this;
        }

        public Builder orderByClauses(final Map<String, String> orderByClauses) {
            this.orderByClauses = orderByClauses;
            return this;
        }

        public Builder timeDimensions(final Map<String, String> timeDimensions) {
            this.timeDimensions = timeDimensions;
            return this;
        }

        public Builder limit(final long limit) {
            this.limit = limit;
            return this;
        }

        public Builder offset(final long offset) {
            this.offset = offset;
            return this;
        }

        public ReportRequest build() {
            return new ReportRequest(this);
        }

    }

}
