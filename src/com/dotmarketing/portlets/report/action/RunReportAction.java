/**
 * Copyright (c) 2000-2004 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dotmarketing.portlets.report.action;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.j2ee.servlets.BaseHttpServlet;
import net.sf.jasperreports.j2ee.servlets.ImageServlet;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import bsh.EvalError;

import com.dotmarketing.business.Role;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.report.businessrule.ReportParamterBR;
import com.dotmarketing.portlets.report.factories.ReportFactory;
import com.dotmarketing.portlets.report.factories.ReportParameterFactory;
import com.dotmarketing.portlets.report.model.Report;
import com.dotmarketing.portlets.report.model.ReportParameter;
import com.dotmarketing.portlets.webforms.action.GenerateWebFormsReportAction;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Parameter;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.ActionResponseImpl;
import com.liferay.util.servlet.SessionMessages;

/**
 *
 * @author  Jason Tesser
 *
 */
 

public class RunReportAction extends DotPortletAction {

    public static final String CMD_RUN = "run";
    public static final String CMD_WEBFORM_RUN = "webform_run";
    public static final String PARAMETERS_SUBMITTED = "parsSubmitted";
    private String referrer;
    private boolean pdf = false;
    private boolean xls = false;
    private boolean rtf = false;
    private boolean html = false;
    
    private ArrayList<ReportParameter> reportParameters = new ArrayList<ReportParameter>();
    
    @Override
	public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req, ActionResponse res) throws Exception {
	Logger.debug(this, "Running report");
	req.setAttribute(ViewReportsAction.REPORT_EDITOR_OR_ADMIN, false);
	User user = _getUser(req);
	List<Role> roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
	req.setAttribute(ViewReportsAction.REPORT_EDITOR_OR_ADMIN, true);

	ActionRequestImpl reqImpl = (ActionRequestImpl) req;
	HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
	ActionResponseImpl resImpl = (ActionResponseImpl) res;
	HttpServletResponse httpRes = resImpl.getHttpServletResponse();
	reportParameters = new ArrayList<ReportParameter>();
	String cmd = req.getParameter(Constants.CMD);
	pdf = Parameter.getBooleanFromString(req.getParameter("pdf"),false);
	xls = Parameter.getBooleanFromString(req.getParameter("xls"),false);
	rtf = Parameter.getBooleanFromString(req.getParameter("rtf"),false);
	html = Parameter.getBooleanFromString(req.getParameter("html"),false);
	String reportId = req.getParameter("reportId");
	Long pageIndex = UtilMethods.parseLong(req.getParameter("pageIndex"), 0);
	
	//set default referrer
	referrer = req.getParameter("referrer");
	if(referrer == null || referrer.equals("")){
	    java.util.Hashtable params = new java.util.Hashtable ();
	    params.put("struts_action", new String [] {"/ext/report/view_reports"} );
	    //			params.put("pageNumber",new String[] { pageNumber + "" });
	    
	    referrer = com.dotmarketing.util.PortletURLUtil.getRenderURL(httpReq, javax.portlet.WindowState.MAXIMIZED.toString(), params);
	}
	Logger.debug(this, "Inside RunReportAction cmd=" + cmd);
	File reportFile;
	Report report;
	if ((cmd != null) && cmd.equals(CMD_RUN)) {
	    try{
		report = ReportFactory.getReport(reportId);
		ArrayList<String> adminRoles = new ArrayList<String>();
		adminRoles.add("CMS Administrator");
		_checkReadPermissions(report,user, httpReq, adminRoles);
	    }catch(DotHibernateException he){
		Logger.error(this, "Unable to Load Report " + he.toString());
		HashMap<String, String[]> params = new HashMap<String, String[]>();
		SessionMessages.add(req,"message.report.run.load.report.error");
		params.put("struts_action", new String [] {"/ext/report/view_reports"} );
		_sendToReferral(req, res, referrer);
		return;
	    }
	    
	    String filePath = null;
	    if (UtilMethods.isSet(Config.getStringProperty("ASSET_REAL_PATH"))) {
	    	filePath = Config.getStringProperty("ASSET_REAL_PATH") + File.separator + Config.getStringProperty("REPORT_PATH") + File.separator + report.getInode() + ".jasper";
	    } else {
	    	filePath = httpReq.getSession().getServletContext().getRealPath(File.separator + Config.getStringProperty("ASSET_PATH") + File.separator + Config.getStringProperty("REPORT_PATH") + File.separator + report.getInode() + ".jasper");
	    }
	    
	    
	    reportFile = new File(filePath);
	    if(!reportFile.exists()){
		Logger.error(this, "Unable to Load Jasper File: " + filePath);
		HashMap<String, String[]> params = new HashMap<String, String[]>();
		SessionMessages.add(req,"error","message.report.run.load.jasper.error");
		params.put("struts_action", new String [] {"/ext/report/view_reports"} );
		_sendToReferral(req, res, referrer);
		return;
	    }
	    if(report.isRequiresInput()){
		ReportParameterFactory.getReportParameters(report);
		reportParameters = report.getParameters();
		httpReq.setAttribute("reportParameters", reportParameters);
		if(!Parameter.getBooleanFromString(req.getParameter(PARAMETERS_SUBMITTED), false)){
		    _setExportAttribute(httpReq);
		    setForward(req, "portlet.ext.report.get_parameters");
		    return;
		}
	    }
	    try{
		_generateReport(report, reportFile, pageIndex.intValue(), httpReq, reportParameters);
	    }catch(Exception ex){
		SessionMessages.add(req,"error", "message.report.run.error");
		Logger.error(this, "Unable to Run Report 3: " + ex.toString());
		HashMap<String, String[]> params = new HashMap<String, String[]>();
		params.put("struts_action", new String [] {"/ext/report/view_reports"} );
		_sendToReferral(req, res, referrer);
		return;
	    }
	    if(pdf){
		res.sendRedirect("/servlets/pdf");
	    }else if(xls){
		res.sendRedirect("/servlets/jxl");
	    }else if(rtf){
		res.sendRedirect("/servlets/rtf");
	    }else{
		setForward(req, "portlet.ext.report.run_report");
	    }
	    return;
	}
	
	if ((cmd != null) && cmd.equals(CMD_WEBFORM_RUN)) {
		GenerateWebFormsReportAction generateWebFormsReportAction = new GenerateWebFormsReportAction();
		generateWebFormsReportAction.processAction(mapping, form, config, reqImpl, resImpl);
		setForward(req, "portlet.ext.report.run_report");
		return;
	}
    }

    private void _generateReport(Report report, File reportFile, int pageIndex, HttpServletRequest request, ArrayList<ReportParameter> rPars) 
	throws JRException, EvalError{
	Logger.debug(this, "Executing _generateReport");
	JasperReport jasperReport = (JasperReport) JRLoader.loadObject(reportFile.getPath());
	HashMap<String, String> submittedPars = new HashMap<String, String>(); 
	Map<String, Object> parameters = new HashMap<String, Object>();
	//		parameters.put("ReportTitle", "Jira Reporter Report");
	parameters.put("BaseDir", reportFile.getParentFile());
	
	for (ReportParameter par : rPars) {
	    if(ReportParamterBR.isDateParameter(par.getClassType())){
		Logger.debug(this, "Getting date parameter" + par.getName());
		Calendar c = Calendar.getInstance();
		int month = c.get(Calendar.MONTH);
		int day = c.get(Calendar.DATE);
		int year = c.get(Calendar.YEAR);
		String date = request.getParameter(par.getName() + "date");
		if(date.matches(com.dotmarketing.util.Constants.REG_EX_VALIDATION_DATE_WITH_FORWARDSLASH) && date != null){
		    String[] splitDate = date.split("/");
		    month = Integer.valueOf(splitDate[0]) - 1;
		    day = Integer.valueOf(splitDate[1]);
		    year = Integer.valueOf(splitDate[2]);
		}
		int hour = Parameter.getInt(request.getParameter(par.getName() + "hour"), 0) == 0 || Parameter.getInt(request.getParameter(par.getName() + "hour")) == 12 ? 0 : Parameter.getInt(request.getParameter(par.getName() + "hour"));
		int min = Parameter.getInt(request.getParameter(par.getName() + "min"), 0);
		int sec = Parameter.getInt(request.getParameter(par.getName() + "sec"), 0);
		int partOfDay;
		if(request.getParameter(par.getName() + "dayPart").equalsIgnoreCase("am")){
		    partOfDay = Calendar.AM;
		    submittedPars.put(par.getName() + "dayPart", "am");
		}else{
		    partOfDay = Calendar.PM;
		    submittedPars.put(par.getName() + "dayPart", "pm");
		}
		c.set(Calendar.MONTH, month);
		c.set(Calendar.DATE, day);
		c.set(Calendar.YEAR, year);
		c.set(Calendar.HOUR, hour);
		c.set(Calendar.MINUTE, min);
		c.set(Calendar.SECOND, sec);
		c.set(Calendar.AM_PM, partOfDay);
		parameters.put(par.getName(), c.getTime());
		Date d = c.getTime();
		submittedPars.put(par.getName() + "date", (month + 1) + "/" + day + "/" + year);
		submittedPars.put(par.getName() + "year", String.valueOf(c.get(Calendar.YEAR)));
		submittedPars.put(par.getName() + "month", String.valueOf(c.get(Calendar.MONTH)));
		submittedPars.put(par.getName() + "day", String.valueOf(c.get(Calendar.DAY_OF_MONTH)));
		submittedPars.put(par.getName() + "hour", Parameter.getString(request.getParameter(par.getName() + "hour"),"0"));
		submittedPars.put(par.getName() + "min", Parameter.getString(request.getParameter(par.getName() + "min"),"0"));
		submittedPars.put(par.getName() + "sec", Parameter.getString(request.getParameter(par.getName() + "sec"),"0"));
		
	    }else if(ReportParamterBR.isStringParameter(par.getClassType())){
		Logger.debug(this, "Getting string parameter " + par.getName());
		parameters.put(par.getName(), new bsh.Interpreter().eval("new " + par.getClassType() + " (\"" + Parameter.getString(request.getParameter(String.valueOf(par.getName())) + "\")","")));
		submittedPars.put(String.valueOf(par.getName()), Parameter.getString(request.getParameter(String.valueOf(par.getName())),"0"));
	    }else if(ReportParamterBR.isBooleanParameter(par.getClassType())){
		Logger.debug(this, "Getting boolean parameter " + par.getName());
		parameters.put(par.getName(), new bsh.Interpreter().eval("new " + par.getClassType() + " (" + Parameter.getString(request.getParameter(String.valueOf(par.getName())) + ")","false")));
		submittedPars.put(String.valueOf(par.getName()), Parameter.getString(request.getParameter(String.valueOf(par.getName())),"false"));
	    }else if(ReportParamterBR.isObjectParameter(par.getClassType())){
		Logger.debug(this, "Getting object parameter " + par.getName());
		Logger.debug(this, "Object parameter: " + request.getParameter(String.valueOf(par.getName())));
		Logger.debug(this, "Object parameter of type: " + request.getParameter(String.valueOf(par.getName())).getClass());
		if (UtilMethods.isSet(par.getDefaultValue()) &&
			par.getDefaultValue().contains("SelectParameter") &&
			par.getDefaultValue().contains("multiple")) {
			Object param = request.getParameterValues(String.valueOf(par.getName()));
			if (param instanceof String[]) {
				String[] stringParam = (String[]) param;
				StringBuilder params = new StringBuilder(256);
				params.ensureCapacity(128);
				for (int i = 0; i < stringParam.length; ++i) {
					if (params.length() == 0)
						params.append(stringParam[i]);
					else
						params.append(", " + stringParam[i]);
				}
				
				parameters.put(par.getName(), new bsh.Interpreter().eval("new String (\"" + Parameter.getString(params.toString() + "\")","")));
			}
		} else {
			Object param = request.getParameter(String.valueOf(par.getName()));
			if (param instanceof String) {
				parameters.put(par.getName(), new bsh.Interpreter().eval("new String (\"" + Parameter.getString(request.getParameter(String.valueOf(par.getName())) + "\")","")));
				submittedPars.put(String.valueOf(par.getName()), Parameter.getString(request.getParameter(String.valueOf(par.getName())), "0"));
			}
		}
	    }else{
		Logger.debug(this, "Getting parameter " + par.getName());
		parameters.put(par.getName(), new bsh.Interpreter().eval("new " + par.getClassType() + " (" + Parameter.getString(request.getParameter(String.valueOf(par.getName())) + ")","0")));
		submittedPars.put(String.valueOf(par.getName()), Parameter.getString(request.getParameter(String.valueOf(par.getName())),"0"));
	    }
	}
	
	Logger.debug(this, "Call to fillReport with Data Source: " + report.getDs());
	JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, DbConnectionFactory.getConnection(report.getDs()));
	
	Logger.debug(this, "Creating exporter");
	JRHtmlExporter exporter = new JRHtmlExporter();
	
	Logger.debug(this, "Getting pages");
	int lastPageIndex = 0;
	if (jasperPrint.getPages() != null) {
	    lastPageIndex = jasperPrint.getPages().size() - 1;
	}
	
	if (!(pageIndex >= 0)) {
	    pageIndex = 0;
	}
	
	if (pageIndex > lastPageIndex) {
	    pageIndex = lastPageIndex;
	}
	
	StringBuffer sbuffer = new StringBuffer();
	
	request.getSession().setAttribute(ImageServlet.DEFAULT_JASPER_PRINT_SESSION_ATTRIBUTE,jasperPrint);

	Logger.debug(this, "Setting parameters");
	exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
	exporter.setParameter(JRExporterParameter.OUTPUT_STRING_BUFFER,	sbuffer);
	if (jasperPrint.getPages() != null && jasperPrint.getPages().size()>0){
	    exporter.setParameter(JRExporterParameter.PAGE_INDEX, new Integer(pageIndex));
	}
	exporter.setParameter(JRHtmlExporterParameter.HTML_HEADER, "");
	exporter.setParameter(JRHtmlExporterParameter.BETWEEN_PAGES_HTML, "");
	exporter.setParameter(JRHtmlExporterParameter.HTML_FOOTER, "");
	exporter.setParameter(JRHtmlExporterParameter.IMAGES_URI, "/servlets/jrimage?image=");
	
	request.getSession().setAttribute(BaseHttpServlet.DEFAULT_JASPER_PRINT_SESSION_ATTRIBUTE,jasperPrint);
	
	Logger.debug(this, "Exporting report");
	exporter.exportReport();
	request.setAttribute("reportSB", sbuffer);
	request.setAttribute("pageIndex", pageIndex);
	request.setAttribute("lastPageIndex", lastPageIndex);
	request.setAttribute("reportId", report.getInode());
	request.setAttribute("submittedPars", submittedPars);
	
    }
    private void _setExportAttribute(HttpServletRequest req){
	req.setAttribute("pdf", pdf);
	req.setAttribute("xls", xls);
	req.setAttribute("rtf", rtf);
	req.setAttribute("html", html);
    }
}