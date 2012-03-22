package com.dotmarketing.servlets;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.factories.ClickstreamFactory;
import com.dotmarketing.portlets.campaigns.factories.RecipientFactory;
import com.dotmarketing.portlets.campaigns.model.Recipient;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public class CampaignReportingServlet extends HttpServlet {

	//~ Instance/static variables ..................................................................

	private static final long serialVersionUID = 1L;
	String UPDATE_OPENED = "UPDATE recipient set opened = now() where recipient_id = ?";


	//~ Methods ....................................................................................

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {


		Recipient r = RecipientFactory.getRecipient(request.getParameter("r"));

		if (InodeUtils.isSet(r.getInode())) {
			r.setOpened(new java.util.Date());
			r.setLastResult(200);
			r.setLastMessage("Opened Email");
			boolean wasError = false;
			try {
				HibernateUtil.startTransaction();
				HibernateUtil.saveOrUpdate(r);
			} catch (DotHibernateException e1) {
				wasError = true;
				try {
					HibernateUtil.rollbackTransaction();
				} catch (DotHibernateException e) {
					Logger.error(CampaignReportingServlet.class,e.getMessage(),e);
				}
				Logger.error(CampaignReportingServlet.class,e1.getMessage(),e1);
			}
			try {
				HibernateUtil.commitTransaction();
			} catch (DotHibernateException e1) {
				wasError = true;
				Logger.error(CampaignReportingServlet.class,e1.getMessage(),e1);
			}
			if(wasError) return;
			
			User user;
			UserProxy sub;
			try {
				user = APILocator.getUserAPI().loadByUserByEmail(r.getEmail(), APILocator.getUserAPI().getSystemUser(), false);
				sub = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);
			} catch (Exception e) {
				Logger.error(CampaignReportingServlet.class,e.getMessage(), e);
				return;
			}
			
			if (InodeUtils.isSet(sub.getInode())) {
				sub.setLastResult(200);
				sub.setLastMessage("Opened Email");
				try {
					com.dotmarketing.business.APILocator.getUserProxyAPI().saveUserProxy(sub,APILocator.getUserAPI().getSystemUser(), false);
				} catch (Exception e) {
					Logger.error(this, "Unable to save userProxy : " + e.getMessage(), e);
				}
			}
			if(Config.getBooleanProperty("ENABLE_CLICKSTREAM_TRACKING", false)){
				ClickstreamFactory.setClickStreamUser(user.getUserId(), request); 
			}
		}
		
		
		// Write out the shim
		try {

			ServletOutputStream out = response.getOutputStream();
			response.setContentType("image/gif");
			FileInputStream fis = new FileInputStream(Config.CONTEXT.getRealPath("/html/images/shim.gif"));

			byte[] buf = new byte[1024];
			int i = 0;

			while ((i = fis.read(buf)) != -1) {
				out.write(buf, 0, i);
			}

			fis.close();
			out.close();
		}
		catch (FileNotFoundException e) {
			Logger.warn(this, e.toString(), e);
		}
		return;
	}


}
