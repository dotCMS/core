package com.dotcms.workflow.form;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Push-publish settings applied when the resolved workflow action wires "
        + "the Push Publish actionlet.")
public class PushPublishBean {

    @Schema(description = "Push-publish environment identifier or name to receive the content.")
    private final String whereToSend;

    @Schema(description = "Publish date (yyyy-MM-dd).")
    private final String publishDate;

    @Schema(description = "Publish time (HH:mm).")
    private final String publishTime;

    @Schema(description = "Expire date (yyyy-MM-dd). Ignored when 'neverExpire' is true.")
    private final String expireDate;

    @Schema(description = "Expire time (HH:mm). Ignored when 'neverExpire' is true.")
    private final String expireTime;

    @Schema(description = "If 'true', the content does not expire. Overrides expireDate/expireTime.")
    private final String neverExpire;

    @Schema(description = "Push-publish filter key (defines what gets bundled with the content).")
    private final String filterKey;

    @Schema(description = "What the caller intends to do with the content (push-publish flow flag).")
    private final String iWantTo;

    @Schema(description = "Timezone identifier (e.g. 'America/New_York') for publish/expire dates.")
    private final String timezoneId;

    @JsonCreator
    public PushPublishBean(
            @JsonProperty("whereToSend") final String whereToSend,
            @JsonProperty("publishDate") final String publishDate,
            @JsonProperty("publishTime") final String publishTime,
            @JsonProperty("expireDate") final String expireDate,
            @JsonProperty("expireTime") final String expireTime,
            @JsonProperty("neverExpire") final String neverExpire,
            @JsonProperty("filterKey") final String filterKey,
            @JsonProperty("iWantTo") final String iWantTo,
            @JsonProperty("timezoneId") final String timezoneId) {
        this.whereToSend = whereToSend;
        this.publishDate = publishDate;
        this.publishTime = publishTime;
        this.expireDate = expireDate;
        this.expireTime = expireTime;
        this.neverExpire = neverExpire;
        this.filterKey = filterKey;
        this.iWantTo = iWantTo;
        this.timezoneId = timezoneId;
    }

    public String getWhereToSend() {
        return whereToSend;
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

    public String getFilterKey() {
        return filterKey;
    }

    public String getIWantTo() {
        return iWantTo;
    }

    public String getTimezoneId(){
        return timezoneId;
    }

}
