/**
 *  Package com.dotmarketing.portlets.report.action
 *  
 * @author Jason Tesser
 */

package com.dotmarketing.portlets.report.action;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import com.dotcms.util.SecurityUtils;
import com.dotmarketing.business.Role;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.report.businessrule.ReportParamterBR;
import com.dotmarketing.portlets.report.factories.ReportFactory;
import com.dotmarketing.portlets.report.factories.ReportParameterFactory;
import com.dotmarketing.portlets.report.model.Report;
import com.dotmarketing.portlets.report.model.ReportParameter;
import com.dotmarketing.portlets.report.struts.ReportForm;
import com.dotmarketing.portlets.report.struts.ReportForm.DataSource;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.Validator;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.ActionResponseImpl;
import com.liferay.util.FileUtil;
import com.liferay.util.servlet.SessionMessages;
import com.liferay.util.servlet.UploadPortletRequest;

/**
 * Class to manage actions related to the cms jasper reports
 * 
 * @author Jason Tesser
 */

public class EditReportAction extends DotPortletAction {

	private boolean requiresInput = false;
	private Report report;
	private boolean badParameters = false;
	boolean newReport = false;

	/**
	 * processAction
	 * 
	 * @param mapping
	 *            ActionMapping
	 * @param form
	 *            ActionForm
	 * @param config
	 *            PortletConfig
	 * @param req
	 *            ActionRequest
	 * @param res
	 *            ActionResponse
	 */

	public void processAction(ActionMapping mapping, ActionForm form,
			PortletConfig config, ActionRequest req, ActionResponse res)
			throws Exception {
		boolean editor = false;
		req.setAttribute(ViewReportsAction.REPORT_EDITOR_OR_ADMIN, false);
		User user = _getUser(req);
		/*
		List<Role> roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
		for (Role role : roles) {
			if (role.getName().equals("Report Administrator")
					|| role.getName().equals("Report Editor")
					|| role.getName().equals("CMS Administrator")) {
				req
						.setAttribute(ViewReportsAction.REPORT_EDITOR_OR_ADMIN,
								true);
				editor = true;
				break;
			}
		}
		*/
		req
		.setAttribute(ViewReportsAction.REPORT_EDITOR_OR_ADMIN,
				true);
		editor = true;
		requiresInput = false;
		badParameters = false;
		newReport = false;
		ActionRequestImpl reqImpl = (ActionRequestImpl) req;
		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
		String cmd = req.getParameter(Constants.CMD);
		Logger.debug(this, "Inside EditReportAction cmd=" + cmd);
		ReportForm rfm = (ReportForm) form;
		String owner = rfm.getOwner();
		ArrayList<String> ds = (DbConnectionFactory.getAllDataSources());
		ArrayList<DataSource> dsResults = new ArrayList<DataSource>();
		for (String dataSource : ds) {
			DataSource d = rfm.getNewDataSource();
			if (dataSource
					.equals(com.dotmarketing.util.Constants.DATABASE_DEFAULT_DATASOURCE)) {
				d.setDsName("DotCMS Datasource");
			} else {
				d.setDsName(dataSource);
			}
			dsResults.add(d);
		}
		rfm.setDataSources(dsResults);
		httpReq.setAttribute("dataSources", rfm.getDataSources());

		String reportId = req.getParameter("reportId");
		String referrer = SecurityUtils.stripReferer(req.getParameter("referrer"));

		// Report Exists
		if (UtilMethods.isSet(reportId)) {
			report = ReportFactory.getReport(reportId);
			ArrayList<String> adminRoles = new ArrayList<String>();
			adminRoles
					.add("CMS Administrator");
			if (user.getUserId().equals(report.getOwner())) {
				_checkWritePermissions(report, user, httpReq, adminRoles);
			}
			if (cmd == null || !cmd.equals(Constants.EDIT)) {
				rfm.setSelectedDataSource(report.getDs());
				rfm.setReportName(report.getReportName());
				rfm.setReportDescription(report.getReportDescription());
				rfm.setReportId(report.getInode());
				rfm.setWebFormReport(report.isWebFormReport());
				httpReq.setAttribute("selectedDS", report.getDs());
			}
		} else {
			if (!editor) {
				throw new DotRuntimeException(
						"user not allowed to create a new report");
			}
			report = new Report();
			report.setOwner(_getUser(req).getUserId());
			newReport = true;
		}
		req.setAttribute(WebKeys.REPORT_EDIT, report);

		if ((cmd != null) && cmd.equals(Constants.EDIT)) {
			if (Validator.validate(req, form, mapping)) {
				report.setReportName(rfm.getReportName());
				report.setReportDescription(rfm.getReportDescription());
				report.setWebFormReport(rfm.isWebFormReport());
				if (rfm.isWebFormReport())
					report.setDs("None");
				else
					report.setDs(rfm.getSelectedDataSource());
				String jrxmlPath = "";
				String jasperPath = "";
				String reportPath = "";

				try {
					HibernateUtil.startTransaction();
					ReportFactory.saveReport(report);

					if (!rfm.isWebFormReport()) {

						if (UtilMethods.isSet(Config
								.getStringProperty("ASSET_REAL_PATH"))) {
							jrxmlPath = Config
									.getStringProperty("ASSET_REAL_PATH")
									+ File.separator
									+ Config.getStringProperty("REPORT_PATH")
									+ File.separator
									+ report.getInode()
									+ ".jrxml";
							jasperPath = Config
									.getStringProperty("ASSET_REAL_PATH")
									+ File.separator
									+ Config.getStringProperty("REPORT_PATH")
									+ File.separator
									+ report.getInode()
									+ ".jasper";
							reportPath = Config
										.getStringProperty("ASSET_REAL_PATH")
										+ File.separator
										+ Config.getStringProperty("REPORT_PATH");
						} else {
							jrxmlPath = FileUtil
									.getRealPath(File.separator
											+ Config
													.getStringProperty("ASSET_PATH")
											+ File.separator
											+ Config
													.getStringProperty("REPORT_PATH")
											+ File.separator
											+ report.getInode() + ".jrxml");
							jasperPath = FileUtil
									.getRealPath(File.separator
											+ Config
													.getStringProperty("ASSET_PATH")
											+ File.separator
											+ Config
													.getStringProperty("REPORT_PATH")
											+ File.separator
											+ report.getInode() + ".jasper");
							reportPath = FileUtil
							              .getRealPath(File.separator
									      + Config
											.getStringProperty("ASSET_PATH")
									      + File.separator
									      + Config
											.getStringProperty("REPORT_PATH"));
						}

						UploadPortletRequest upr = PortalUtil
								.getUploadPortletRequest(req);
						File importFile = upr.getFile("jrxmlFile");
						if (importFile.exists()) {
							byte[] currentData = new byte[0];
							FileInputStream is = new FileInputStream(importFile);
							int size = is.available();
							currentData = new byte[size];
							is.read(currentData);
							java.io.File reportFolder = new java.io.File(reportPath);
							
							if (!reportFolder.exists())
								reportFolder.mkdirs();
		
							File f = new File(jrxmlPath);
							FileChannel channelTo = new FileOutputStream(f)
									.getChannel();
							ByteBuffer currentDataBuffer = ByteBuffer
									.allocate(currentData.length);
							currentDataBuffer.put(currentData);
							currentDataBuffer.position(0);
							channelTo.write(currentDataBuffer);
							channelTo.force(false);
							channelTo.close();
							try {
								JasperCompileManager.compileReportToFile(
										jrxmlPath, jasperPath);

							} catch (Exception e) {
								Logger.error(this,
										"Unable to compile or save jrxml: "
												+ e.toString(), e);
								try {
									f = new File(jrxmlPath);
									f.delete();
								} catch (Exception ex) {
									Logger
											.info(this,
													"Unable to delete jrxml. This is usually a permissions problem.");
								}
								try {
									f = new File(jasperPath);
									f.delete();
								} catch (Exception ex) {
									Logger
											.info(this,
													"Unable to delete jasper. This is usually a permissions problem.");
								}
								HibernateUtil.rollbackTransaction();
								SessionMessages.add(req, "error", UtilMethods
										.htmlLineBreak(e.getMessage()));
								setForward(req,
										"portlet.ext.report.edit_report");
								return;
							}
							JasperReport jasperReport = (JasperReport) JRLoader
									.loadObject(jasperPath);
							ReportParameterFactory
									.deleteReportsParameters(report);
							_loadReportParameters(jasperReport.getParameters());
							report.setRequiresInput(requiresInput);
							HibernateUtil.save(report);
						} else if (newReport) {
							HibernateUtil.rollbackTransaction();
							SessionMessages.add(req, "error",
									"message.report.compile.error");
							setForward(req, "portlet.ext.report.edit_report");
							return;
						}
					}
					HibernateUtil.commitTransaction();
					HashMap params = new HashMap();
					SessionMessages.add(req, "message",
							"message.report.upload.success");
					params.put("struts_action",
							new String[] { "/ext/report/view_reports" });
					referrer = com.dotmarketing.util.PortletURLUtil
							.getRenderURL(((ActionRequestImpl) req)
									.getHttpServletRequest(),
									javax.portlet.WindowState.MAXIMIZED
											.toString(), params);
					_sendToReferral(req, res, referrer);
					return;
				} catch (Exception ex) {
					HibernateUtil.rollbackTransaction();
					Logger.error(this, "Unable to save Report: "
							+ ex.toString());
					File f;
					Logger.info(this, "Trying to delete jrxml");
					try {
						f = new File(jrxmlPath);
						f.delete();
					} catch (Exception e) {
						Logger
								.info(this,
										"Unable to delete jrxml. This is usually because the file doesn't exist.");
					}
					try {
						f = new File(jasperPath);
						f.delete();
					} catch (Exception e) {
						Logger
								.info(this,
										"Unable to delete jasper. This is usually because the file doesn't exist.");
					}
					if (badParameters) {
						SessionMessages.add(req, "error", ex.getMessage());
					} else {
						SessionMessages.add(req, "error",
								"message.report.compile.error");
					}
					setForward(req, "portlet.ext.report.edit_report");
					return;
					// _sendToReferral(req, res, referrer);
				}
			} else {
				setForward(req, "portlet.ext.report.edit_report");
			}
		}

		if ((cmd != null) && cmd.equals("downloadReportSource")) {
			ActionResponseImpl resImpl = (ActionResponseImpl) res;
			HttpServletResponse response = resImpl.getHttpServletResponse();
			if (!downloadSourceReport(reportId, httpReq, response)) {
				SessionMessages.add(req, "error",
						"message.report.source.file.not.found");
			}
		}

		setForward(req, "portlet.ext.report.edit_report");
	}

	/**
	 * _loadReportParameters
	 * 
	 * @param pars
	 *            JRParameter[]
	 * @exception Exception
	 */

	private void _loadReportParameters(JRParameter[] pars) throws Exception {
		for (JRParameter jrPar : pars) {
			if (jrPar.isForPrompting() && !jrPar.isSystemDefined()) {
				if (!ReportParamterBR.isAllowedParameter(jrPar
						.getValueClassName())) {
					badParameters = true;
					throw new Exception(
							"You may only use String, Date, javax.sql.Datasource, Long, Float, Double, BigDecimal, Integer, Boolean, or java.lang.Object as Parameter Types");
				}
				requiresInput = true;
				ReportParameter rp = new ReportParameter();
				rp.setName(jrPar.getName());
				rp.setDescription(jrPar.getDescription());
				rp.setClassType(jrPar.getValueClassName());
				try {
					Object o = new bsh.Interpreter().eval(jrPar
							.getDefaultValueExpression().getText());
					if (o instanceof Date) {
						o = ((Date) o).getTime();
					}
					rp.setDefaultValue(jrPar.getDefaultValueExpression()
							.getText());
				} catch (Exception e) {
					Logger.debug(this, "No default Value for parameter"
							+ e.toString());
				}
				rp.setReportInode(report.getInode());
				ReportParameterFactory.saveReportParameter(rp);
			}
		}
	}

	/**
	 * downloadSourceReport
	 * 
	 * @param reportId
	 *            String
	 * @param request
	 *            HttpServletRequest
	 * @param response
	 *            HttpServletResponse
	 */

	private boolean downloadSourceReport(String reportId,
			HttpServletRequest request, HttpServletResponse response) {
		String filePath = null;
		if (UtilMethods.isSet(Config.getStringProperty("ASSET_REAL_PATH"))) {
			filePath = Config.getStringProperty("ASSET_REAL_PATH")
					+ File.separator + Config.getStringProperty("REPORT_PATH")
					+ File.separator + report.getInode() + ".jrxml";
		} else {
			filePath = request.getSession().getServletContext().getRealPath(
					File.separator + Config.getStringProperty("ASSET_PATH")
							+ File.separator
							+ Config.getStringProperty("REPORT_PATH")
							+ File.separator + report.getInode() + ".jrxml");
		}

		File reportFile = new File(filePath);
		if (!reportFile.exists()) {
			return false;
		}

		BufferedReader reader = null;
		ServletOutputStream out = null;

		try {
			reader = new BufferedReader(new FileReader(reportFile));

			response.setContentType("text/xml");
			response.setHeader("Content-Disposition", "attachment; filename=\""
					+ reportId + ".jrxml\"");

			out = response.getOutputStream();

			String line;
			for (; (line = reader.readLine()) != null;) {
				out.println(line);
			}

			out.flush();
			out.close();
			reader.close();
		} catch (Exception e) {
			if (out != null) {
				try {
					out.close();
				} catch (Exception ex) {
				}
			}

			if (reader != null) {
				try {
					reader.close();
				} catch (Exception ex) {
				}
			}

			Logger.warn(this, e.toString());
			return false;
		}

		return true;
	}
}