package com.dotcms.workflow.form;

import com.dotmarketing.business.PermissionAPI;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.dotcms.rest.api.Validated;

import java.util.List;
import java.util.Map;

/**
 * Form to fire an action in the workflow
 * @author jsanca
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(builder = FireActionForm.Builder.class)
public class FireActionForm extends Validated {

    private final String comments;
    private final String assign;
    private final String publishDate;
    private final String publishTime;
    private final String expireDate;
    private final String expireTime;
    private final String neverExpire;
    private final String whereToSend;
    private final String filterKey;
    private final String iWantTo;
    private final String query;
    private final String pathToMove;
    private final String timezoneId;
    private final Map<PermissionAPI.Type, List<String>> individualPermissions;

    @JsonProperty("contentlet")
    private final Map<String, Object> contentletFormData;

    public String getComments() {
        return comments;
    }

    public String getAssign() {
        return assign;
    }

    public String getPublishDate() {
        return publishDate;
    }

    public String getPublishTime() {
        return publishTime;
    }

    public String getExpireDate() {
        return expireDate;
    }

    public String getExpireTime() {
        return expireTime;
    }

    public String getNeverExpire() {
        return neverExpire;
    }

    public String getWhereToSend() {
        return whereToSend;
    }

    public String getFilterKey() {
        return filterKey;
    }

    public String getIWantTo() {
        return iWantTo;
    }

    public Map<String, Object> getContentletFormData() {
        return contentletFormData;
    }

    public String getQuery() {
        return query;
    }

    public String getPathToMove() {
        return pathToMove;
    }

    public String getTimezoneId() {
        return timezoneId;
    }

    public Map<PermissionAPI.Type, List<String>> getIndividualPermissions() {
        return individualPermissions;
    }

    public FireActionForm(final Builder builder) {

        this.comments    = builder.comments;
        this.assign      = builder.assign;
        this.publishDate = builder.publishDate;
        this.publishTime = builder.publishTime;
        this.expireDate  = builder.expireDate;
        this.expireTime  = builder.expireTime;
        this.neverExpire = builder.neverExpire;
        this.whereToSend = builder.whereToSend;
        this.filterKey   = builder.filterKey;
        this.iWantTo     = builder.iWantTo;
        this.query       = builder.query;
        this.pathToMove  = builder.pathToMove;
        this.timezoneId  = builder.timezoneId;
        this.contentletFormData    = builder.contentlet;
        this.individualPermissions = builder.individualPermissions;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Builder {

        @JsonProperty()
        private String comments;
        @JsonProperty()
        private String assign;
        @JsonProperty()
        private String publishDate;
        @JsonProperty()
        private String publishTime;
        @JsonProperty()
        private String expireDate;
        @JsonProperty()
        private String expireTime;
        @JsonProperty()
        private String neverExpire;
        @JsonProperty()
        private String whereToSend;
        @JsonProperty()
        private String filterKey;
        @JsonProperty()
        private String iWantTo;
        @JsonProperty()
        private String query;
        @JsonProperty()
        private String pathToMove;
        @JsonProperty()
        private String timezoneId;
        @JsonProperty("contentlet")
        private Map<String, Object> contentlet;
        @JsonProperty("individualPermissions")
        private Map<PermissionAPI.Type, List<String>> individualPermissions;

        public Builder individualPermissions(final Map<PermissionAPI.Type, List<String>> individualPermissions) {
            this.individualPermissions = individualPermissions;
            return this;
        }

        public Builder pathToMove(final String pathToMove) {
            this.pathToMove = pathToMove;
            return this;
        }

        public Builder comments(final String comments) {
            this.comments = comments;
            return this;
        }

        public Builder query(final String query) {
            this.query = query;
            return this;
        }

        public Builder assign(final String assign) {
            this.assign = assign;
            return this;
        }

        public Builder publishDate(final String publishDate) {
            this.publishDate = publishDate;
            return this;
        }

        public Builder publishTime(final String publishTime) {
            this.publishTime = publishTime;
            return this;
        }

        public Builder expireDate(final String expireDate) {
            this.expireDate = expireDate;
            return this;
        }

        public Builder expireTime(final String expireTime) {
            this.expireTime = expireTime;
            return this;
        }

        public Builder neverExpire(final String neverExpire) {
            this.neverExpire = neverExpire;
            return this;
        }

        public Builder whereToSend(final String whereToSend) {
            this.whereToSend = whereToSend;
            return this;
        }

        public Builder filterKey(final String filterKey) {
            this.filterKey = filterKey;
            return this;
        }

        public Builder iWantTo(final String iWantTo) {
            this.iWantTo = iWantTo;
            return this;
        }

        public Builder timezoneId(final String timezoneId){
            this.timezoneId = timezoneId;
            return this;
        }

        @JsonProperty("contentlet")
        public Builder contentlet(final  Map<String, Object> contentletFormData) {
            this.contentlet = contentletFormData;
            return this;
        }

        public FireActionForm build() {
            return new FireActionForm(this);
        }
    }
}
