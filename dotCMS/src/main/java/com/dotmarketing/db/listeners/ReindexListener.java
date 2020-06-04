package com.dotmarketing.db.listeners;

import java.util.List;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.google.common.collect.ImmutableList;

public class ReindexListener implements DotListener {

	public enum Action{ADDING, REMOVING};

	
	private final Action action;
	private final List<Contentlet> contentToIndex;



	public List<Contentlet> getReindexIds() {
		return contentToIndex;
	}

    public ReindexListener(final List<Contentlet> reindexIds) {
        this(reindexIds,Action.ADDING);
    }
	
	public ReindexListener(final List<Contentlet> reindexIds, final Action action) {
		super();
		this.contentToIndex = reindexIds;
		this.action = action;
	}

	
    public ReindexListener(Contentlet reindexId) {
        this(reindexId,Action.ADDING);
    }
	
	public ReindexListener(Contentlet reindexId, Action action) {
		this(ImmutableList.of(reindexId), action);

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
    
    @Override
    public final boolean equals(Object obj) {
        
        
        if(obj == null ||!( obj instanceof ReindexListener)) {
            return false;
        }
        ReindexListener other = (ReindexListener)obj;
        if(this.key() ==null ) {
            return false;
        }
        return this.key().toLowerCase().equalsIgnoreCase(other.key());

    }
    
    @Override
    public final String key() {
        return this.getClass().getName() + ":" + this.action + ":" + contentToIndex.hashCode();
        
    }
    
    @Override
    public final String toString() {
        
        return this.getClass().getName() + ":" + this.key();
        
        
    }
}
