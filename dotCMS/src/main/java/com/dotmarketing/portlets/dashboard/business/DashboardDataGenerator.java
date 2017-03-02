package com.dotmarketing.portlets.dashboard.business;

import java.util.List;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.db.DbConnectionFactory;

public abstract class DashboardDataGenerator {
	
	protected String getContentQuery(){
	return "select contentlet.identifier as inode, contentlet.title as title "+
	" from contentlet join identifier identifier on identifier.id = contentlet.identifier " +
	" join contentlet_version_info vinfo on vinfo.live_inode=contentlet.inode "+
	" where identifier.host_inode = ? ";
	}
	
	public abstract void setFlag(boolean flag);//DOTCMS-5511

	public abstract boolean isFinished();

	public abstract double getProgress();

	public abstract List<String> getErrors();

	public abstract long getRowCount();

	public abstract int getMonthFrom();

	public abstract int getYearFrom();

	public abstract int getMonthTo();

	public abstract int getYearTo();
	
	public abstract void start();

}
