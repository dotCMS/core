package com.dotcms.publishing.sitesearch;

import java.util.ArrayList;
import java.util.List;

import com.dotcms.publishing.PublisherConfig;


public class SiteSearchConfig extends PublisherConfig {

	public SiteSearchConfig() {
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
		CRON_EXPRESSION, QUARTZ_JOB_NAME, RUN_NOW,INDEX_NAME;
	};
	
	public String getCronExpression(){
		return (String) this.get(MyConfig.CRON_EXPRESSION.toString());
		
	}
	
	public void setCronExpression(String cron){
		this.put(MyConfig.CRON_EXPRESSION.toString(), cron);
		
	}
	
	public String getJobName(){
		return (String) this.get(MyConfig.QUARTZ_JOB_NAME.toString());
		
	}
	
	public void setJobId(String id){
		this.put(MyConfig.QUARTZ_JOB_NAME.toString(), id);
		
	}
	public boolean runNow(){
		return this.get(MyConfig.RUN_NOW.toString()) !=null && new Boolean((String) this.get(MyConfig.RUN_NOW.toString()));
		
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
	
	@Override
	public List<Class> getPublishers(){
		List<Class> clazz = new ArrayList<Class>();
		clazz.add(ESSiteSearchPublisher.class);
		return clazz;
	}
	
	
}
