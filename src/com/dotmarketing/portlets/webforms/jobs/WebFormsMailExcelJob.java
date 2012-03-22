package com.dotmarketing.portlets.webforms.jobs;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotmarketing.portlets.webforms.factories.WebFormFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;

public class WebFormsMailExcelJob implements Job {

	public void execute(JobExecutionContext context) throws JobExecutionException 
	{
		Logger.info(this, "START WebFormsMailExcelJob");
		try
		{
			//Get the actual Date
			GregorianCalendar gc = new GregorianCalendar();
			//Get initialDate
			gc.set(Calendar.HOUR_OF_DAY,0);
			gc.set(Calendar.MINUTE,0);
			gc.set(Calendar.SECOND,0);
			Date initialDate = gc.getTime();

			//Get finalDate
			gc.set(Calendar.HOUR_OF_DAY,23);
			gc.set(Calendar.MINUTE,59);
			gc.set(Calendar.SECOND,59);
			Date finalDate = gc.getTime();

			//Get the parameters from the context or the property file
			JobDataMap data = context.getJobDetail().getJobDataMap();
			String fromAddress = data.getString(WebKeys.WEBFORMS_MAIL_EXCEL_FROM_ADDRESS) != null ? data.getString(WebKeys.WEBFORMS_MAIL_EXCEL_FROM_ADDRESS):  Config.getStringProperty(WebKeys.WEBFORMS_MAIL_EXCEL_FROM_ADDRESS);
			String fromName = data.getString(WebKeys.WEBFORMS_MAIL_EXCEL_FROM_NAME) != null ? data.getString(WebKeys.WEBFORMS_MAIL_EXCEL_FROM_NAME):  Config.getStringProperty(WebKeys.WEBFORMS_MAIL_EXCEL_FROM_NAME);
			String subject = data.getString(WebKeys.WEBFORMS_MAIL_EXCEL_FROM_SUBJECT) != null ? data.getString(WebKeys.WEBFORMS_MAIL_EXCEL_FROM_SUBJECT):  Config.getStringProperty(WebKeys.WEBFORMS_MAIL_EXCEL_FROM_SUBJECT);
			String roleKey = data.getString(WebKeys.WEBFORMS_MAIL_EXCEL_GROUP_NAME) != null ? data.getString(WebKeys.WEBFORMS_MAIL_EXCEL_GROUP_NAME):  Config.getStringProperty(WebKeys.WEBFORMS_MAIL_EXCEL_GROUP_NAME);
			String webFormType = data.getString(WebKeys.WEBFORMS_MAIL_EXCEL_WEBFORM_TYPE) != null ? data.getString(WebKeys.WEBFORMS_MAIL_EXCEL_WEBFORM_TYPE):  Config.getStringProperty(WebKeys.WEBFORMS_MAIL_EXCEL_WEBFORM_TYPE);

			//Call the worker
			WebFormFactory.emailExcelWebForms(roleKey,webFormType,initialDate,finalDate,subject,fromAddress,fromName);
			Logger.info(this, "END WebFormsMailExcelJob");
		}
		catch(Exception ex)
		{
			String message = ex.toString();
			Logger.debug(WebFormsMailExcelJob.class,message);
		}
	}
}