package com.dotcms.content.elasticsearch.business;

import org.elasticsearch.action.support.master.AcknowledgedResponse;

/**
 * Throw when a Elasticsearch request fail
 */
public class ElasticsearchResponseException extends Exception {
    private final AcknowledgedResponse response;

    public ElasticsearchResponseException(final AcknowledgedResponse response) {
        super(response.toString());
        this.response  = response;
    }

    public AcknowledgedResponse getResponse() {
        return response;
    }
}
