package com.dotmarketing.cms.formpipeline.pipes;

import java.util.LinkedHashMap;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.cms.formpipeline.business.FormPipe;
import com.dotmarketing.cms.formpipeline.business.FormPipeBean;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

public class EmailFormPipe implements FormPipe {

	private HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
	
	public void runForm(FormPipeBean bean) {

		try {
			Host host = hostWebAPI.getCurrentHost(bean.getUnderlyingRequest());
			
			String from = UtilMethods.replace(bean.getString("from"), "spamx", "");
			String to = UtilMethods.replace(bean.getString("to"), "spamx", "");
			String cc = UtilMethods.replace(bean.getString("cc"), "spamx", "");
			String bcc = UtilMethods.replace(bean.getString("bcc"), "spamx", "");
			try { from = PublicEncryptionFactory.decryptString(from); } catch (Exception e) { }
			try { to = PublicEncryptionFactory.decryptString(to); } catch (Exception e) { }
			try { cc = PublicEncryptionFactory.decryptString(cc); } catch (Exception e) { }
			try { bcc = PublicEncryptionFactory.decryptString(bcc); } catch (Exception e) { }
			String subject = (bean.getString("subject") != null) ? bean.getString("subject"): "Mail from " + host.getHostname() ;
			
			
			// Sort the forms' fields by the given order parameter
			//String order = (String)getMapValue("order", parameters);
			Map<String, Object> orderedMap = new LinkedHashMap<String, Object>();

			// Parameter prettyOrder is used to map
			// the pretty names of the variables used in the order field
			// E.G: order = firstName, lastName
			//		prettyOrder = First Name, Last Name
			//String prettyOrder = (String)getMapValue("prettyOrder", parameters);
			Map<String, String> prettyVariableNamesMap = new LinkedHashMap<String, String>();

			// Parameter attachFiles is used to specify the file kind of fields you want to attach
			// to the mail is sent by this method
			// E.G: attachFiles = file1, file2, ...
			//String attachFiles = (String)getMapValue("attachFiles", parameters);
			
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (PortalException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (SystemException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(this, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
	
	}

    
    public String getDescription() {
        // TODO Auto-generated method stub
        return null;
    }


    public String getExampleUsage() {
        // TODO Auto-generated method stub
        return null;
    }


    public String getTitle() {
        // TODO Auto-generated method stub
        return null;
    }

}
