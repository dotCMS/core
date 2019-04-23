package com.dotcms.content.elasticsearch.business;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;

/**
 * Wrapper class that encapsulates method implementations
 * for {@link BulkRequestBuilder} and {@link BulkProcessor}
 * @author nollymar
 *
 */
public class BulkIndexWrapper {

    private BulkRequestBuilder requestBuilder;
    private BulkProcessor bulkProcessor;

    public BulkIndexWrapper(final BulkRequestBuilder requestBuilder){
        this.requestBuilder = requestBuilder;
    }

    public BulkIndexWrapper(final BulkProcessor bulkProcessor){
        this.bulkProcessor = bulkProcessor;
    }

    public void add(final IndexRequest request) {
        if (this.requestBuilder != null) {
            this.requestBuilder.add(request);
        } else if (this.bulkProcessor != null) {
            this.bulkProcessor.add(request);
        }
    }

    public void add(final DeleteRequest request){
        if(this.requestBuilder != null){
            this.requestBuilder.add(request);
        } else if(this.bulkProcessor != null){
            this.bulkProcessor.add(request);
        }
    }

    public BulkRequestBuilder getRequestBuilder(){
        return this.requestBuilder;
    }

    public BulkProcessor getBulkProcessor(){
        return this.bulkProcessor;
    }
}
