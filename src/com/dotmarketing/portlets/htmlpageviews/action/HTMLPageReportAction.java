package com.dotmarketing.portlets.htmlpageviews.action;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierFactory;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.htmlpageviews.factories.HTMLPageViewFactory;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portlet.ActionResponseImpl;

/**
 * <a href="ViewQuestionsAction.java.html"> <b><i>View Source </i> </b> </a>
 * 
 * @author Maria Ahues
 * @version $Revision: 1.3 $
 *  
 */
public class HTMLPageReportAction extends DotPortletAction {

	protected HostAPI hostAPI = APILocator.getHostAPI();
    
	public void processAction(
			 ActionMapping mapping, ActionForm form, PortletConfig config,
			 ActionRequest req, ActionResponse res)
		 throws Exception {

		//wraps request to get session object
		ActionResponseImpl resImpl = (ActionResponseImpl)res;
		HttpServletResponse httpRes = resImpl.getHttpServletResponse();

		if(! "userReport".equals(req.getParameter(Constants.CMD))){
		    return;
		}
		_writeCSV(httpRes,req,res,form);
			return;
    
	}
	
    public ActionForward render(ActionMapping mapping, ActionForm form, PortletConfig config, RenderRequest req,
            RenderResponse res) throws Exception {
        return mapping.findForward("portlet.ext.htmlpageviews.html_page_report");
	
    }
	private void _writeCSV(HttpServletResponse httpRes,ActionRequest req, ActionResponse res,ActionForm form) throws Exception {

		User systemUser = APILocator.getUserAPI().getSystemUser();

		ServletOutputStream out = httpRes.getOutputStream();
		httpRes.setContentType("application/octet-stream");
		httpRes.setHeader("Content-Disposition", "attachment; filename=\"report" + System.currentTimeMillis() +".csv\"");

		//httpRes.setContentType("text/csv");
		//httpRes.setHeader("Content-Disposition", "attachment; filename=\"report" + System.currentTimeMillis() +".csv\"");
	 
		//print the header
		
        com.liferay.portlet.RenderRequestImpl reqImpl = (com.liferay.portlet.RenderRequestImpl) req;
        HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
        //gets the session object for the messages
        HttpSession session = httpReq.getSession();
        Logger.debug(this, "########## req.getParameter(\"searchStartDate\") " + req.getParameter("searchStartDate"));

        java.util.Date startDate = null;
        if (req.getParameter("searchStartDate") != null) {
            Logger.debug(this, "searchStartDate" + req.getParameter("searchStartDate"));
            startDate = UtilMethods.htmlToDate(req.getParameter("searchStartDate"));
        } else {
            GregorianCalendar gcal = new GregorianCalendar();
            gcal.set(Calendar.DAY_OF_MONTH, 1);
            gcal.set(Calendar.HOUR, 0);
            gcal.set(Calendar.MINUTE, 0);
            gcal.set(Calendar.SECOND, 0);
            startDate = gcal.getTime();

        }
        Logger.debug(this, "startDate" + startDate);
        req.setAttribute("startDate", startDate);

        java.util.Date endDate = null;
        if (req.getParameter("searchEndDate") != null) {
            Logger.debug(this, "searchEndDate" + req.getParameter("searchEndDate"));
            endDate = UtilMethods.htmlToDate(req.getParameter("searchEndDate"));
        } else {
            GregorianCalendar gcal = new GregorianCalendar();
            gcal.add(Calendar.MONTH, 1);
            gcal.set(Calendar.DAY_OF_MONTH, 1);
            gcal.set(Calendar.HOUR, 0);
            gcal.set(Calendar.MINUTE, 0);
            gcal.set(Calendar.SECOND, 0);
            endDate = gcal.getTime();

        }
        Logger.debug(this, "endDate" + endDate);
        req.setAttribute("endDate", endDate);

        String uri = null;
        Host host = null;
        if (req.getParameter("htmlpage") != null) {
            HTMLPage myHTMLPage = (HTMLPage) InodeFactory.getInode(req.getParameter("htmlpage"), HTMLPage.class);
			host = hostAPI.findParentHost(myHTMLPage, systemUser, false);
            uri = APILocator.getIdentifierAPI().find(myHTMLPage).getURI();
            req.setAttribute("htmlPage", myHTMLPage);
        } else if (req.getParameter("pageIdentifier") != null) {
            //Identifier id = (Identifier) InodeFactory.getInode(req.getParameter("pageIdentifier"), Identifier.class);
        	Identifier id = APILocator.getIdentifierAPI().find(req.getParameter("pageIdentifier"));
            uri = id.getURI();
            HTMLPage myHTMLPage = (HTMLPage) APILocator.getVersionableAPI().findLiveVersion(id,APILocator.getUserAPI().getSystemUser(),false);
			host = hostAPI.findParentHost(myHTMLPage, systemUser, false);
            req.setAttribute("htmlPage", myHTMLPage);
        }
        
        String hostIdentifier = host.getIdentifier();
        java.util.List allUsers = HTMLPageViewFactory.getAllUsers(uri, startDate, endDate, hostIdentifier);

		
		
		
		out.print("Name, eMail");
		out.print("\n");
		java.util.Iterator i = allUsers.iterator();
		int x = 1;
		while(i.hasNext()){
		    Map map = (Map) i.next();
		    out.print("'" + ( map.get("firstname") + " " + map.get("lastname")).replace('\'', '`') + "','" + map.get("emailaddress") + "'");
			out.print("\n");
		}


		
		out.close();
		
	}
	
	
	
	
	
	
	
	
	
	
	
}