package com.dotcms.publisher.myTest;

import java.util.ArrayList;
import java.util.List;

import com.dotcms.publishing.PublisherConfig;

public class PushPublisherConfig extends PublisherConfig {
	public static enum Operation {
		PUBLISH,
		UNPUBLISH
	}
	
	private Operation operation;
	
	public PushPublisherConfig() {
		super();
	}
	
	boolean switchIndexWhenDone = false;
	
	public boolean switchIndexWhenDone(){
		return switchIndexWhenDone;
	}
	
	
	public void setSwitchIndexWhenDone(boolean switchIndexWhenDone) {
		this.switchIndexWhenDone = switchIndexWhenDone;
	}


	private enum MyConfig {
		RUN_NOW,INDEX_NAME;
	};
	

	public boolean runNow(){
		return this.get(MyConfig.RUN_NOW.toString()) !=null && new Boolean((String) this.get(MyConfig.RUN_NOW.toString()));
		
	}
	
	public Operation getOperation() {
		return operation;
	}


	public void setOperation(Operation operation) {
		this.operation = operation;
	}


	public void setRunNow(boolean once){
		this.put(MyConfig.RUN_NOW.toString(), once);
		
	}
	
	public String getIndexName(){
		return (String) this.get(MyConfig.INDEX_NAME.toString());
		
	}
	
	public void setIndexName(String name){
		this.put(MyConfig.INDEX_NAME.toString(), name);
		
		
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public List<Class> getPublishers(){
		List<Class> clazz = new ArrayList<Class>();
		clazz.add(PushPublisher.class);
		return clazz;
	}
}
