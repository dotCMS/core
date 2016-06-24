package com.dotmarketing.portlets.rules.parameter.type.constraint;


/**
 * @author Geoff M. Granum
 */
public class EnumerationConstraint {

    public EnumerationConstraint(Builder builder) {

    }

    public static class Builder {

        public Builder() {
        }

        public EnumerationConstraint build() {
            return new EnumerationConstraint(this);
        }

        public Builder values(String... values) {
            return this;
        }
    }
}
 
