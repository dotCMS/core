package com.dotmarketing.portlets.webforms.action;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.portlets.webforms.factories.WebFormFactory;
import com.dotmarketing.portlets.webforms.model.WebForm;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.struts.PortletAction;
import com.liferay.portlet.ActionResponseImpl;
import com.liferay.util.servlet.SessionMessages;

public class GenerateWebFormsReportAction extends PortletAction {

	public void processAction(
			ActionMapping mapping, ActionForm form, PortletConfig config,
			ActionRequest req, ActionResponse res)
	throws Exception {
		Logger.debug(this, "Running GenerateWebFormsReportAction!!");

		String cmd = req.getParameter("cmd");
		String reportType = req.getParameter("report_type");
		List<WebForm> list = new ArrayList<WebForm> ();
		list = WebFormFactory.getWebFormsByType(reportType);

		if (UtilMethods.isSet(cmd) && cmd.equals("delete") && UtilMethods.isSet(reportType)) {
			WebFormFactory.removeWebFormsByType(reportType);
			SessionMessages.add(req, "message.webforms.content.deleted");
			setForward(req, "portlet.ext.webforms.view");
			return;
		}

		String reportName = reportType + " Web Forms Report";
		Date reportDate = new Date();

		String reportComments = "Web Forms Submitted for " + reportType;

		String dateString = (new java.text.SimpleDateFormat("M-d-yyyy")).format(reportDate);
		String reportFileName = reportType.replaceAll(" ", "") + "Report-" + dateString + ".xls";

		List<String> reportHeaders = new ArrayList<String> ();
		List<String> customHeaders = new ArrayList<String> ();

		reportHeaders.add("Submit Date/Time");

        //Create the object that store the data
        List<List<String>> reportData = new ArrayList<List<String>> ();
        
		//Load the report date
		WebFormFactory.getReportHeaderAndData(list,reportHeaders,customHeaders,reportData);	

        //Return the retorn to the browser
        _writeReport(reportName, reportHeaders, reportData, reportDate, reportFileName, reportComments, res);

	}

	private void _writeReport (String reportName, List<String> reportHeaders, List<List<String>> reportData, Date reportDate, 
			String reportFileName, String reportComments, ActionResponse res) throws IOException 
	{

		HttpServletResponse response = ((ActionResponseImpl)res).getHttpServletResponse();
		response.setContentType("application/octet-stream");
		response.setHeader("Content-Disposition", "attachment; filename=\"" + reportFileName + "\"");

		OutputStreamWriter out = new OutputStreamWriter(response.getOutputStream(), "UTF8");

		//Get the code for the excel file
		String code = WebFormFactory.getReportCode(reportHeaders,reportHeaders,reportData,reportName,reportComments,reportDate);
		out.write(code);

		out.flush();
		out.close();
	}

}
