package com.dotcms.workflow.form;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Map;

@JsonDeserialize(builder = FireMultipleActionForm.Builder.class)
public class FireMultipleActionForm extends Validated {

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

    private final List<Map<String, Object>> contentletsFormData;

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

    public List<Map<String, Object>> getContentletsFormData() {
        return contentletsFormData;
    }

    public FireMultipleActionForm(final Builder builder) {

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
        this.contentletsFormData =
                builder.contentlets;
    }

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
        @JsonProperty("contentlets")
        private List<Map<String, Object>> contentlets;

        public Builder comments(final String comments) {
            this.comments = comments;
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

        @JsonProperty("contentlet")
        public Builder contentlets(final List<Map<String, Object>> contentlets) {
            this.contentlets = contentlets;
            return this;
        }

        public FireMultipleActionForm build() {
            return new FireMultipleActionForm(this);
        }
    }
}
