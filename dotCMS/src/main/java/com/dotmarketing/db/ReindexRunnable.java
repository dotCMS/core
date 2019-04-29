package com.dotmarketing.db;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

public abstract class ReindexRunnable implements Runnable {

	public enum Action{ADDING, REMOVING};

	
	private final Action action;
	private final List<Contentlet> contentToIndex;



	public List<Contentlet> getReindexIds() {
		return contentToIndex;
	}

	public ReindexRunnable(final List<Contentlet> reindexIds, final Action action) {
		super();
		this.contentToIndex = reindexIds;
		this.action = action;


	}

	public ReindexRunnable(Contentlet reindexId, Action action) {
		super();

		contentToIndex = new ArrayList<Contentlet>();
		contentToIndex.add(reindexId);
		this.action = action;

	}

	public Action getAction() {
		return action;
	}

    public void run() {

        try {
        	if(action.equals(Action.ADDING)){
        		APILocator.getContentletIndexAPI().addContentToIndex(contentToIndex);
        	}
        	else{
        		throw new DotStateException("REMOVE ACTION NEEDS TO OVERRIDE THE run() method");
        	}
        } catch (Exception e) {
			throw new RuntimeException(e);
        }
    }
    
}
