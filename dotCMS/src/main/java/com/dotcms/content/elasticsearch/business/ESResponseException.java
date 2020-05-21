package com.dotcms.content.elasticsearch.business;

import org.elasticsearch.action.support.master.AcknowledgedResponse;

public class ESResponseException extends Exception {
    private final AcknowledgedResponse response;

    public ESResponseException(final AcknowledgedResponse response) {
        super(response.toString());
        this.response  = response;
    }

    public AcknowledgedResponse getResponse() {
        return response;
    }
}
