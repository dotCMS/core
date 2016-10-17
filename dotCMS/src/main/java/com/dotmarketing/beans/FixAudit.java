package com.dotmarketing.beans;

import java.util.Date;

public class FixAudit {
	
    private static final long serialVersionUID = 1L;
        
    private String Id ;
    private String tableName;
    private String action;
    private int recordsAltered;
    private Date datetime;
    
    
	public String getId() {
		return Id;
	}
	public void setId(String id) {
		Id = id;
	}
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public int getRecordsAltered() {
		return recordsAltered;
	}
	public void setRecordsAltered(int recordsAltered) {
		this.recordsAltered = recordsAltered;
	}
	public Date getDatetime() {
		return datetime;
	}
	public void setDatetime(Date datetime) {
		this.datetime = datetime;
	}
	

    

}
