package com.dotcms.util.diff;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class DiffItem {

    private final String variable;
    private final int    line;
    private final String message;
    private final Object detail;

    public DiffItem(final Builder builder) {

        this.variable = builder.variable;
        this.line     = builder.line;
        this.message  = builder.message;
        this.detail   = builder.detail;
    }

    public String getVariable() {
        return variable;
    }

    public int getLine() {
        return line;
    }

    public String getMessage() {
        return message;
    }

    public Object getDetail() {
        return detail;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiffItem diffItem = (DiffItem) o;
        return Objects.equals(variable, diffItem.variable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variable);
    }

    @Override
    public String toString() {
        return "DiffItem{" +
                "variable='" + variable + '\'' +
                ", line=" + line +
                ", message='" + message + '\'' +
                ", detail=" + detail +
                '}';
    }

    public static final class Builder {
        @JsonProperty(required = true)
        private String variable;
        @JsonProperty
        private int    line = -1;
        @JsonProperty
        private String message;
        @JsonProperty
        private Object detail;

        public Builder variable(final String variable) {
            this.variable = variable;
            return this;
        }

        public Builder line(final int line) {
            this.line = line;
            return this;
        }

        public Builder detail(final Object detail) {
            this.detail = detail;
            return this;
        }

        public Builder message(final String message) {
            this.message = message;
            return this;
        }
        public DiffItem build() {
            return new DiffItem(this);
        }
    }
}
