package com.dotmarketing.db;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.bulk.BulkRequestBuilder;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

public abstract class ReindexRunnable extends DotRunnable {

	public enum Action{ADDING, REMOVING};

	
	private final Action action;
	private final List<Contentlet> contentToIndex;
	private final BulkRequestBuilder bulk;
	private boolean reindexOnly;

	public List<Contentlet> getReindexIds() {
		return contentToIndex;
	}

	public ReindexRunnable(List<Contentlet> reindexIds, Action action, BulkRequestBuilder bulk, boolean reindexOnly) {
		super();
		this.contentToIndex = reindexIds;
		this.action = action;
		this.bulk = bulk;
		this.reindexOnly = reindexOnly;
	}

	public ReindexRunnable(Contentlet reindexId, Action action, BulkRequestBuilder bulk) {
		super();

		contentToIndex = new ArrayList<Contentlet>();
		contentToIndex.add(reindexId);
		this.action = action;
		this.bulk = bulk;
	}

	public Action getAction() {
		return action;
	}

    public void run() {

        try {
        	if(action.equals(Action.ADDING)){
        		APILocator.getContentletIndexAPI().indexContentList(contentToIndex, bulk, reindexOnly);
        	}
        	else{
        		throw new DotStateException("REMOVE ACTION NEEDS TO OVERRIDE THE run() method");
        	}
        } catch (Exception e) {
			throw new RuntimeException(e);
        }
    }
    
}
