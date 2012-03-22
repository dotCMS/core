package com.dotmarketing.quartz.job;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.util.Config;

public class DeleteOldClickstreams extends DotStatefulJob {
	
	public DeleteOldClickstreams() {
	}

	@Override
	public void run(JobExecutionContext jobContext)
			throws JobExecutionException {
		int days = Config.getIntProperty("DELETE_CLICKSTREAMS_OLDER_THAN", 1);	
		String sdate = null;	
		SimpleDateFormat df_oracle = new SimpleDateFormat("dd-MMM-yy");
		SimpleDateFormat df_other = new SimpleDateFormat("yyyy-MM-dd");
		Calendar now = Calendar.getInstance();			
		now.add(Calendar.DATE, -(days-1));
		
		try {
			if(DbConnectionFactory.isOracle()){				
				sdate =df_oracle.format(now.getTime());				
			}else{
				sdate =df_other.format(now.getTime());
			}
			HibernateUtil.startTransaction();				
			HibernateUtil.delete("from clickstream_request in class com.dotmarketing.beans.ClickstreamRequest where timestampper < '"+sdate+"'");
			HibernateUtil.delete("from clickstream in class com.dotmarketing.beans.Clickstream where start_date < '"+sdate+"'");
			HibernateUtil.delete("from clickstream_404 in class com.dotmarketing.beans.Clickstream404 where timestampper < '"+sdate+"'");
			HibernateUtil.commitTransaction();	
			
		} catch (Exception e) {
			try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e1) {				
				e1.printStackTrace();
			}
		}
		
	}

}	