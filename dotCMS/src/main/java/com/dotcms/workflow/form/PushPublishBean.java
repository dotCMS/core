package com.dotcms.workflow.form;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonCreator;
import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;

public class PushPublishBean {

    private final String whereToSend;
    private final String publishDate;
    private final String publishTime;
    private final String expireDate;
    private final String expireTime;
    private final String neverExpire;
    private final String forcePush;

    @JsonCreator
    public PushPublishBean(
            @JsonProperty("whereToSend") String whereToSend,
            @JsonProperty("publishDate") String publishDate, @JsonProperty("publishTime") String publishTime,
            @JsonProperty("expireDate") String expireDate,  @JsonProperty("expireTime") String expireTime,
            @JsonProperty("neverExpire") String neverExpire, @JsonProperty("forcePush") String forcePush) {
        this.whereToSend = whereToSend;
        this.publishDate = publishDate;
        this.publishTime = publishTime;
        this.expireDate = expireDate;
        this.expireTime = expireTime;
        this.neverExpire = neverExpire;
        this.forcePush = forcePush;
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

    public String getForcePush() {
        return forcePush;
    }

}
