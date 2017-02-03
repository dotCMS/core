package com.dotmarketing.portlets.webforms.factories;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.webforms.model.WebForm;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Mailer;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class WebFormFactory {
	
	static String fileName = "WebFormFile";
	final static String fileExtension = ".xls";
	
    public static List<WebForm> getWebFormsByType (String formType) {
        String query = "select {web_form.*} from web_form where form_type = ?";
        HibernateUtil dh = new HibernateUtil (WebForm.class);
        List<WebForm> list =null;
        try {
			dh.setSQLQuery(query);
			dh.setParam(formType);
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(WebFormFactory.class,"getWebFormsByType failed:" + e, e);
		}
        return list;
    }
    
    public static List<WebForm> getWebFormsWithOutType () {
        String query = "select {web_form.*} from web_form where form_type is null or form_type = ''";
        HibernateUtil dh = new HibernateUtil (WebForm.class);
        List<WebForm> list =null;
        try {
			dh.setSQLQuery(query);
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(WebFormFactory.class,"getWebFormsWithOutType failed:" + e, e);
		}
        return list;
    }
    
    public static void removeWebFormsByType (String formType) {
        String query = "delete from web_form where form_type = ?";
        DotConnect dc = new DotConnect ();
        dc.setSQL(query);
        dc.addParam(formType);
        dc.getResult();
    }
    
    public static void removeWebFormsWithoutType () {
        String query = "delete from web_form where form_type is null or form_type = ''";
        DotConnect dc = new DotConnect ();
        dc.setSQL(query);
        dc.getResult();
    }
    
    public static String[] getWebFormsTypes () {
        String query = "select distinct(form_type) as form_type from web_form";
        DotConnect dc = new DotConnect ();
        dc.setSQL(query);
        List<Map> results =null;
        try {
			results = dc.getResults();
		} catch (DotDataException e) {
			Logger.error(WebFormFactory.class,"getWebFormsTypes method failed:"+e, e);
		}
        List<String> typesList = new ArrayList<String> ();
        for (Map result : results) {
            if (UtilMethods.isSet((String)result.get("form_type"))) {
                typesList.add((String)result.get("form_type"));
            }
        }
        return typesList.toArray(new String[0]);
    }
    
	public static List<WebForm> getWebFormsByTypeBetween (String formType,Date initialDate,Date finalDate) 
	{
		String query = "select {web_form.*} from web_form where form_type = ? and submit_date between ? and ?";
		HibernateUtil dh = new HibernateUtil (WebForm.class);
		List<WebForm> list =null;
		try {
			dh.setSQLQuery(query);
			dh.setParam(formType);
			dh.setParam(initialDate);
			dh.setParam(finalDate);
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(WebFormFactory.class,"getWebFormsByTypeBetween failed:" + e, e);
		}
		return list;
	}
    
	public static void getReportHeaderAndData(List<WebForm> webForms, List<String> reportHeaders,List<String> customHeaders,List<List<String>> reportData)
	{
		for (WebForm webForm : webForms) {

			if (!reportHeaders.contains("Title") && UtilMethods.isSet(webForm.getTitle())) 
				reportHeaders.add("Title");

			if (!reportHeaders.contains("Prefix") && UtilMethods.isSet(webForm.getPrefix())) 
				reportHeaders.add("Prefix");

			if (!reportHeaders.contains("First Name") && UtilMethods.isSet(webForm.getFirstName())) 
				reportHeaders.add("First Name");

			if (!reportHeaders.contains("Middle Initial") && UtilMethods.isSet(webForm.getMiddleInitial())) 
				reportHeaders.add("Middle Initial");

			if (!reportHeaders.contains("Middle Name") && UtilMethods.isSet(webForm.getMiddleName()))
				reportHeaders.add("Middle Name");

			if (!reportHeaders.contains("Last Name") && UtilMethods.isSet(webForm.getLastName()))
				reportHeaders.add("Last Name");

			if (!reportHeaders.contains("Full Name") && UtilMethods.isSet(webForm.getFullName()))
				reportHeaders.add("Full Name");

			if (!reportHeaders.contains("Organization") && UtilMethods.isSet(webForm.getOrganization()))
				reportHeaders.add("Organization");

			if (!reportHeaders.contains("Address") && UtilMethods.isSet(webForm.getAddress()))
				reportHeaders.add("Address");

			if (!reportHeaders.contains("Address 1") && UtilMethods.isSet(webForm.getAddress1()))
				reportHeaders.add("Address 1");

			if (!reportHeaders.contains("Address 2") && UtilMethods.isSet(webForm.getAddress2()))
				reportHeaders.add("Address 2");

			if (!reportHeaders.contains("City") && UtilMethods.isSet(webForm.getCity()))
				reportHeaders.add("City");

			if (!reportHeaders.contains("State") && UtilMethods.isSet(webForm.getState()))
				reportHeaders.add("State");

			if (!reportHeaders.contains("Zip") && UtilMethods.isSet(webForm.getZip()))
				reportHeaders.add("Zip");

			if (!reportHeaders.contains("Country") && UtilMethods.isSet(webForm.getCountry()))
				reportHeaders.add("Country");

			if (!reportHeaders.contains("Phone") && UtilMethods.isSet(webForm.getPhone()))
				reportHeaders.add("Phone");

			if (!reportHeaders.contains("Email") && UtilMethods.isSet(webForm.getEmail()))
				reportHeaders.add("Email");

			if (UtilMethods.isSet(webForm.getCustomFields())) {
				String customFields = webForm.getCustomFields();
				String[] fields = customFields.split("\\|");
				for (String field : fields) {
					String[] splitted = field.split("=");
					if (splitted.length == 2) {
						String name = splitted[0];
						if (!reportHeaders.contains(name)) 
							reportHeaders.add(name);
						if (!customHeaders.contains(name)) 
							customHeaders.add(name);
					}
				}
			}                
		}		

		for (WebForm webForm : webForms) {

			try {

				List<String> row = new ArrayList<String> ();

				Map<String, String> customFieldsMap = new HashMap<String, String> ();

				if (UtilMethods.isSet(webForm.getCustomFields())) {
					String customFields = webForm.getCustomFields();
					String[] fields = customFields.split("\\|");
					for (String field : fields) {
						String[] splitted = field.split("=");
						if (splitted.length == 2) {
							String name = splitted[0];
							String value = splitted[1];
							customFieldsMap.put(name, value);
						}
					}
				}

				row.add(UtilMethods.dateToHTMLDate(webForm.getSubmitDate(),"MM/dd/yyyy") +  " " + UtilMethods.dateToHTMLTime(webForm.getSubmitDate()));

				List<String> subList = reportHeaders.subList(1, reportHeaders.size());
				for (String header : subList) {

					if (header.equals("Title"))
						row.add(UtilMethods.isSet(webForm.getTitle())?webForm.getTitle():"");
					else if (header.equals("Prefix"))
						row.add(UtilMethods.isSet(webForm.getPrefix())?webForm.getPrefix():"");
					else if (header.equals("First Name"))
						row.add(UtilMethods.isSet(webForm.getFirstName())?webForm.getFirstName():"");
					else if (header.equals("Middle Initial"))
						row.add(UtilMethods.isSet(webForm.getMiddleInitial())?webForm.getMiddleInitial():"");
					else if (header.equals("Middle Name"))
						row.add(UtilMethods.isSet(webForm.getMiddleName())?webForm.getMiddleName():"");
					else if (header.equals("Last Name"))
						row.add(UtilMethods.isSet(webForm.getLastName())?webForm.getLastName():"");
					else if (header.equals("Full Name"))
						row.add(UtilMethods.isSet(webForm.getFullName())?webForm.getFullName():"");
					else if (header.equals("Organization"))
						row.add(UtilMethods.isSet(webForm.getOrganization())?webForm.getOrganization():"");
					else if (header.equals("Address"))
						row.add(UtilMethods.isSet(webForm.getAddress())?webForm.getAddress():"");
					else if (header.equals("Address 1"))
						row.add(UtilMethods.isSet(webForm.getAddress1())?webForm.getAddress1():"");
					else if (header.equals("Address 2"))
						row.add(UtilMethods.isSet(webForm.getAddress2())?webForm.getAddress2():"");
					else if (header.equals("City"))
						row.add(UtilMethods.isSet(webForm.getCity())?webForm.getCity():"");
					else if (header.equals("State"))
						row.add(UtilMethods.isSet(webForm.getState())?webForm.getState():"");
					else if (header.equals("Zip"))
						row.add(UtilMethods.isSet(webForm.getZip())?webForm.getZip():"");
					else if (header.equals("Country"))
						row.add(UtilMethods.isSet(webForm.getCountry())?webForm.getCountry():"");
					else if (header.equals("Phone"))
						row.add(UtilMethods.isSet(webForm.getPhone())?webForm.getPhone():"");
					else if (header.equals("Email"))
						row.add(UtilMethods.isSet(webForm.getEmail())?webForm.getEmail():"");
					else {
						if (customFieldsMap.containsKey(header)) {
							row.add(customFieldsMap.get(header));
						} else {
							row.add("");
						}
					}
				}
				reportData.add(row);
			} catch (Exception e) {
				Logger.error(WebFormFactory.class,e.getMessage(),e);
			}
		}
	}

	public static String getReportCode(List<String> reportHeaders,List<String> customHeaders,List<List<String>> reportData,String reportName,String reportComments,Date reportDate)
	{
		StringBuffer sb = new StringBuffer();
		int columns = 1;

		int dataSize = reportData.size();
		int dataColums = 0;
		if (dataSize > 0)
			dataColums = (reportData.get(0)).size();

		if (dataSize > 0 && dataColums > 0) {
			columns = (reportData.get(0)).size();
		} else if (reportHeaders.size()>0) {
			columns = reportHeaders.size();
		}
		sb.append("<table border=\"1\">");
		sb.append("<tr>");
		sb.append("<td colspan=\""+columns+"\"><font color=\"navy\"><b>"+reportName+" - Generated on: "+UtilMethods.dateToPrettyHTMLDate(reportDate)+"</b></font></td>");
		sb.append("</tr>");

		if (UtilMethods.isSet(reportComments)) { 
			sb.append("<tr>");
			sb.append("<td colspan=\""+columns+"\"><font color=\"navy\"><b>"+reportComments+"</b></font></td>");
			sb.append("</tr>");
		} 

		sb.append("<tr>");
		sb.append("<td colspan=\""+columns+"\"></td>");
		sb.append("</tr>");

		if (reportHeaders != null) {
			Iterator<String> it = reportHeaders.iterator();
			sb.append("<tr>");
			while (it.hasNext()) {
				String header = (String)it.next();
				sb.append("<td bgcolor=\"blue\"><b><font color=\"white\">"+header+"</font></b></td>");
			}
			sb.append("</tr>");
		}

		if (reportData.size() == 0) {
			sb.append("<tr>");
			sb.append("<td colspan=\"" + columns + "\" align=\"center\">No Records Found</td>");
			sb.append("</tr>");

		}

		Iterator<List<String>> it = reportData.iterator();
		while (it.hasNext()) {
			List<String> row = it.next();

			Iterator<String> it2 = null;

			it2 = row.iterator();

			sb.append("<tr>");
			while (it2.hasNext()) {
				String value = (String)it2.next();
				sb.append("<td>"+value+"</td>");
			}
			sb.append("</tr>");
		}
		sb.append("</table>");
		String code = sb.toString();
		return code;
	}
	
	public static void emailExcelWebForms(String roleKey,String reportType,Date initialDate,Date finalDate,String subject,String fromEmail,String fromName)
	{
		try
		{
			StringBuffer emailsSB = new StringBuffer();
			//Get the members in the group
			Role role = APILocator.getRoleAPI().loadRoleByKey(roleKey);
			List<User> users = new ArrayList<User>();
			List<String> uIDs = APILocator.getRoleAPI().findUserIdsForRole(role);
			for (String uid : uIDs) {
				users.add(APILocator.getUserAPI().loadUserById(uid, APILocator.getUserAPI().getSystemUser(), true));
			}

			
			//Create the emails list
			for(User user : users)
			{
				String email = user.getEmailAddress();
				emailsSB.append(email + ",");
			}
			String emails = emailsSB.toString();
			emails = emails.substring(0,emails.lastIndexOf(","));
			File attachment = saveExcelWebFormsToFS(reportType,initialDate,finalDate);

			//Create the Emails and sent it
			Mailer mailer = new Mailer();		
			mailer.setToEmail(emails);
			mailer.setSubject(subject);
			mailer.setFromEmail(fromEmail);
			mailer.setFromName(fromName);

			mailer.addAttachment(attachment);
			mailer.sendMessage();
		}
		catch(Exception ex)
		{
			String message = ex.toString();
			Logger.debug(WebFormFactory.class, message);
		}
	}

	public static File saveExcelWebFormsToFS(String reportType,Date initialDate,Date finalDate) throws Exception
	{
		try
		{
			//Temporary Folder to save the file 
			String folder = UtilMethods.getTemporaryDirPath();		

			//Date when the file is created
			SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyy");
			String fileNameDate = sdf.format(initialDate);

			//Full path of the temporary file
			String filePath = folder + fileName + "_" + reportType + "_" + fileNameDate + fileExtension;

			//Create the temporary file
			File excelFile = new File(filePath);
			if (!excelFile.exists()) 
			{
				excelFile.createNewFile();			
			}

			//Save the file to disk
			FileWriter fileWriter = new FileWriter(excelFile);		
			String code = CreateExcelWebForms(reportType,initialDate,finalDate);
			fileWriter.write(code);
			fileWriter.flush();
			fileWriter.close();
			//Return the File
			return excelFile;
		}
		catch(Exception ex)
		{
			String message = ex.toString();
			Logger.debug(WebFormFactory.class,message);
			throw ex;
		}

	}

	public static String CreateExcelWebForms(String reportType,Date initialDate,Date finalDate)
	{			
		String reportComments = "Web Forms Submitted for " + reportType;
		String reportName = reportType + " Web Forms Report";
		Date reportDate = new Date();

		List<WebForm> webForms = WebFormFactory.getWebFormsByTypeBetween(reportType, initialDate, finalDate);
		StringBuffer sb = new StringBuffer();

		List<String> reportHeaders = new ArrayList<String> ();
		List<String> customHeaders = new ArrayList<String> ();
		List<List<String>> reportData = new ArrayList<List<String>>();

		reportHeaders.add("Submit Date/Time");

		//Load the report date
		WebFormFactory.getReportHeaderAndData(webForms,reportHeaders,customHeaders,reportData);

		//Get the report code
		String code = WebFormFactory.getReportCode(reportHeaders,customHeaders,reportData,reportName,reportComments,reportDate);

		//return the code
		return code;
	}
}
