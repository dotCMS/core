package com.dotcms.publishing;


public class SiteSearchConfig extends PublisherConfig {

	public SiteSearchConfig() {
		super();

	}
	private enum MyConfig {
		CRON_EXPRESSION, QUARTZ_JOB_ID, RUN_ONCE,INDEX_NAME; 
	};

	
	
	public String getCronExpression(){
		return (String) this.get(MyConfig.CRON_EXPRESSION.toString());
		
	}
	
	public void setCronExpression(String cron){
		this.put(MyConfig.CRON_EXPRESSION.toString(), cron);
		
	}
	
	public String getJobId(){
		return (String) this.get(MyConfig.QUARTZ_JOB_ID.toString());
		
	}
	
	public void setJobId(String id){
		this.put(MyConfig.QUARTZ_JOB_ID.toString(), id);
		
	}
	public boolean runOnce(){
		return this.get(MyConfig.RUN_ONCE.toString()) !=null && (Boolean)this.get(MyConfig.RUN_ONCE.toString());
		
	}
	public void setRunOnce(boolean once){
		this.put(MyConfig.QUARTZ_JOB_ID.toString(), once);
		
	}
	
	public String getIndexName(){
		return (String) this.get(MyConfig.INDEX_NAME.toString());
		
	}
	
	public void setIndexName(String name){
		this.put(MyConfig.INDEX_NAME.toString(), name);
		
	}
}
