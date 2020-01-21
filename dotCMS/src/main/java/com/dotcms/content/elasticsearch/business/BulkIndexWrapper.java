package com.dotcms.content.elasticsearch.business;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
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

    private BulkRequest bulkRequest;
    private BulkProcessor bulkProcessor;

    public BulkIndexWrapper(final BulkRequest bulkRequest){
        this.bulkRequest = bulkRequest;
    }

    public BulkIndexWrapper(final BulkProcessor bulkProcessor){
        this.bulkProcessor = bulkProcessor;
    }

    public void add(final IndexRequest request) {
        if (this.bulkRequest != null) {
            this.bulkRequest.add(request);
        } else if (this.bulkProcessor != null) {
            this.bulkProcessor.add(request);
        }
    }

    public void add(final DeleteRequest request){
        if(this.bulkRequest != null){
            this.bulkRequest.add(request);
        } else if(this.bulkProcessor != null){
            this.bulkProcessor.add(request);
        }
    }

    public BulkRequest getRequestBuilder(){
        return this.bulkRequest;
    }

    public BulkProcessor getBulkProcessor(){
        return this.bulkProcessor;
    }
}
