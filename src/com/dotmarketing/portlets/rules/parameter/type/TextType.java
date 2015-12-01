package com.dotmarketing.portlets.rules.parameter.type;

import com.dotmarketing.portlets.rules.parameter.type.constraint.EnumerationConstraint;

/**
 * @author Geoff M. Granum
 */
public class TextType extends DataType {

    public TextType() {
        super("text");
    }

    public TextType(Builder builder) {
        super(builder.i18nKey);

    }

    @Override
    public void checkValid(String value) {
        // noop. If you got here, the string is valid. Yes, even if it's null.
        // we'll add length validations and such eventually.
    }

    public static class Builder {

        private String i18nKey;
        private int minLength;
        private int maxLength;
        private String defaultValue;

        public Builder i18nKey(String i18nKey) {
            this.i18nKey = i18nKey;
            return this;
        }


        public TextType build() {
            return new TextType(this);
        }

        public Builder minLength(int minLength) {
            this.minLength = minLength;
            return this;
        }

        public Builder maxLength(int maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public Builder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder constrainedBy(EnumerationConstraint build) {
            return this;
        }
    }
}
 
