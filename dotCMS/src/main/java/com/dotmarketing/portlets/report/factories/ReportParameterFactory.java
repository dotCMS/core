package com.dotmarketing.portlets.report.factories;

import java.util.ArrayList;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.report.model.Report;
import com.dotmarketing.portlets.report.model.ReportParameter;

/**
 * ReportParmater class's factory
 * @author Jason Tesser
 *
 */
public class ReportParameterFactory {

	private static final String GETAllRPHQL= "from ReportParameter r where r.reportInode = ? order by inode";
	private static final String DELETEALLRPSQL = "delete from report_parameter where report_inode = ?";
	
  /**
   * Fills a Report Object with its parmaters. The method returns void but will set the parametrs attribute of the passed in report object.
   * @param report The report to load parametrs for
   * @return void
   */	
	public static void getReportParameters(Report report)throws DotHibernateException{
		if(report.isRequiresInput()){
			HibernateUtil hu = new HibernateUtil(ReportParameter.class);
			hu.setQuery(GETAllRPHQL);
			hu.setParam(report.getInode());
			report.setParameters(new ArrayList<ReportParameter>(hu.list()));
		}
	}
  /**
   * Deletes all ReportParamters for a Report Object. 
   * @param report Report to delete all paramters for
   * @return void
   */
	public static void deleteReportsParameters(Report report){
		DotConnect dc = new DotConnect();
		dc.setSQL(DELETEALLRPSQL);
		dc.addParam(report.getInode());
		dc.getResult();		
	}
  /**
   * Saves a ReportParameter object
   * @param rp ReportParameter to save
   * @return void
   */
	public static void saveReportParameter(ReportParameter rp)throws DotHibernateException{
		HibernateUtil.save(rp);		
	}
  /**
   * Deletes a ReportParameter object
   * @param rp ReportParameter to delete
   * @return void
   */
	public static void deleteReportParameter(ReportParameter rp)throws DotHibernateException{
		HibernateUtil.delete(rp);
	}
}
