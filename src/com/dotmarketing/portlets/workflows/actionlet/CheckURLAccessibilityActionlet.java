package com.dotmarketing.portlets.workflows.actionlet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;

import com.dotcms.enterprise.LicenseUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.linkchecker.bean.InvalidLink;
import com.dotmarketing.portlets.linkchecker.util.LinkCheckerUtil;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

/**
 * Workflow actionlet that call the link checker on the given content fields. 
 * 
 * @author	Graziano Aliberti - Engineering Ingegneria Informatica
 * @date	Feb 28, 2012
 */
public class CheckURLAccessibilityActionlet extends WorkFlowActionlet {

	private static final long serialVersionUID = 5110097144813925204L;
	private static List<WorkflowActionletParameter> paramList = null;
	private UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
	
	@Override
	public List<WorkflowActionletParameter> getParameters() {
		WebContext ctx = WebContextFactory.get();
		HttpServletRequest request = ctx.getHttpServletRequest();
		try{
			if(null==paramList){
				paramList = new ArrayList<WorkflowActionletParameter>();
				paramList.add(new WorkflowActionletParameter("fieldList", LanguageUtil.get(uWebAPI.getLoggedInUser(request), "checkURL.fieldList"), "", true));
			}
		}catch(Exception e){			
		}
		return paramList;
	}

	@Override
	public String getName() {		
		return "Link Checker";
	}

	@Override
	public String getHowTo() {
		return "This actionlet check the links into the specified fields and reject the content with broken links";
	}

	@Override
	public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {
	    if(LicenseUtil.getLevel()<200)
            return; // the apis will do nothing anyway
	    
        WebContext ctx = WebContextFactory.get();
        HttpServletRequest request = ctx.getHttpServletRequest();
	    User user=null;
        try {
            user = uWebAPI.getLoggedInUser(request);
        } catch (Exception exx) {
            throw new WorkflowActionFailureException(exx.getMessage());
        }
		Contentlet con = processor.getContentlet();
		
		for(Field f : FieldsCache.getFieldsByStructureInode(con.getStructureInode())) {
			if(f.getFieldType().equals(Field.FieldType.WYSIWYG.toString())){
				
				//get the value
				String value = con.getStringProperty(f.getVelocityVarName());
				
				List<InvalidLink> httpResponse=null;
                try {
                    httpResponse = APILocator.getLinkCheckerAPI().findInvalidLinks(value,user);
                } catch (Exception e1) {
                    Logger.error(this, e1.getMessage(), e1);
                    throw new WorkflowActionFailureException(e1.getMessage());
                }
				
				//if there are unreachable URL...
				if(httpResponse.size()>0){
				    String msg="";
                    try {
                        msg = LanguageUtil.get(user, "checkURL.errorBrokenLinks");
                    } catch (Exception e) {
                        
                    } 
					throw new WorkflowActionFailureException(LinkCheckerUtil.buildPopupMsgWithLinksList(msg, httpResponse));
				}
			}
		}
	}

}
