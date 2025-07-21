package com.dotcms.rest.api.v1.content;

import javax.validation.constraints.NotNull;
import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Encapsulates the form to pull related content
 * @author jsanca
 */
@JsonDeserialize(builder = PullRelatedForm.Builder.class)
public class PullRelatedForm extends Validated{

    @NotNull
    private final String fieldVariable;

    @NotNull
    private final String identifier;

    private final String condition;

    @NotNull
    private final int limit;

    @NotNull
    private final int offset;

    @NotNull
    private final String orderBy;

    public String getFieldVariable() {
        return fieldVariable;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getCondition() {
        return condition;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public String getOrderBy() {
        return orderBy;
    }

    private PullRelatedForm(final PullRelatedForm.Builder builder) {
        fieldVariable   = builder.fieldVariable;
        identifier      = builder.identifier;
        condition = builder.condition;
        limit     = builder.limit;
        offset    = builder.offset;
        orderBy   = builder.orderBy;

        checkValid();
    }

    public static final class Builder {

        @JsonProperty(required = true)
        private  String fieldVariable;

        @JsonProperty(required = true)
        private  String identifier;

        @JsonProperty
        private  String condition;

        @JsonProperty
        private  int limit = 40;

        @JsonProperty
        private  int offset = 0;

        @JsonProperty
        private  String orderBy = "moddate desc";

        public PullRelatedForm.Builder fieldVariable(final String fieldVariable) {
            this.fieldVariable = fieldVariable;
            return this;
        }

        public PullRelatedForm.Builder identifier(final String identifier) {
            this.identifier = identifier;
            return this;
        }

        public PullRelatedForm.Builder condition(final String condition) {
            this.condition = condition;
            return this;
        }

        public PullRelatedForm.Builder limit(final int limit) {
            this.limit = limit;
            return this;
        }

        public PullRelatedForm.Builder offset(final int offset) {
            this.offset = offset;
            return this;
        }

        public PullRelatedForm.Builder orderBy(final String orderBy) {
            this.orderBy = orderBy;
            return this;
        }

        public PullRelatedForm build() {
            return new PullRelatedForm(this);
        }
    }
}
