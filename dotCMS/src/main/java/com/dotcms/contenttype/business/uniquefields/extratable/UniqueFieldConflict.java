package com.dotcms.contenttype.business.uniquefields.extratable;

import com.fasterxml.jackson.annotation.JsonGetter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a unique field conflict between Contentlets that share the same unique value. Such
 * conflicts are meant to be reported to user in the back-end so that they can be manually fix them.
 *
 * @author Jose Castro
 * @since Aug 6th, 2025
 */
public class UniqueFieldConflict {

    private final String fieldName;
    private final String contentTypeId;
    private final String originalValue;
    private final List<Map<String, Object>> conflictingData;

    private UniqueFieldConflict(final Builder builder) {
        this.fieldName = builder.fieldName;
        this.contentTypeId = builder.contentTypeId;
        this.originalValue = builder.originalValue;
        this.conflictingData = builder.conflictingData;
    }

    /**
     * Returns the name of the field whose value is unique and conflicts with the value of one or
     * more Contentlets.
     *
     * @return The Velocity Variable Name of the field.
     */
    @JsonGetter("fieldName")
    public String fieldName() {
        return this.fieldName;
    }

    /**
     * Returns the ID of the Content Type that contains the field with the conflicting unique
     * value.
     *
     * @return The ID of the Content Type.
     */
    @JsonGetter("contentTypeId")
    public String contentTypeId() {
        return this.contentTypeId;
    }

    /**
     * Returns the value of the field that is unique and conflicts with the value of one or more
     * Contentlets.
     *
     * @return The conflicting unique value.
     */
    @JsonGetter("originalValue")
    public String originalValue() {
        return this.originalValue;
    }

    /**
     * Returns the list of conflicting Contentlets that share the same unique value. It provides the
     * Contentlet ID and its language ID.
     *
     * @return The list of conflicting Contentlets.
     */
    @JsonGetter("conflictingData")
    public List<Map<String, Object>> conflictingData() {
        return this.conflictingData;
    }

    @Override
    public String toString() {
        return "UniqueFieldConflict{" +
                "fieldName='" + fieldName + '\'' +
                ", contentTypeId='" + contentTypeId + '\'' +
                ", originalValue='" + originalValue + '\'' +
                ", conflictingData=" + conflictingData +
                '}';
    }

    /**
     * Allows you to create an instance of the {@link UniqueFieldConflict} class.
     */
    public static class Builder {

        private String fieldName;
        private String contentTypeId;
        private String originalValue;
        private List<Map<String, Object>> conflictingData;

        public Builder fieldName(final String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        public String fieldName() {
            return this.fieldName;
        }

        public Builder contentTypeId(final String contentTypeId) {
            this.contentTypeId = contentTypeId;
            return this;
        }

        public Builder originalValue(final String originalValue) {
            this.originalValue = originalValue;
            return this;
        }

        public Builder conflictingData(final Map<String, Object> conflictingData) {
            if (null == this.conflictingData) {
                this.conflictingData = new ArrayList<>();
            }
            this.conflictingData.add(conflictingData);
            return this;
        }

        public Builder conflictingData(final List<Map<String, Object>> conflictingData) {
            this.conflictingData = conflictingData;
            return this;
        }

        public UniqueFieldConflict build() {
            return new UniqueFieldConflict(this);
        }

    }

}
