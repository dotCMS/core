package com.dotmarketing.portlets.categories.action;

import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.Validator;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.util.servlet.SessionMessages;
import java.lang.reflect.InvocationTargetException;
import org.apache.commons.beanutils.BeanUtils;


public class EditCategoryAction extends DotPortletAction {
	
	private CategoryAPI categoryAPI;
	
	public EditCategoryAction () {
		categoryAPI = APILocator.getCategoryAPI();
	}

    /**
     * Handles the requests of the category edit screen. The category named by the {@code inode}
     * parameter is loaded for the current user; a request without a category and with an
     * {@code itsnew} parameter is forwarded to the input page. Otherwise, with {@code cmd=add} the
     * posted form is validated and, when valid, the category is saved; any other request shows the
     * edit form for the loaded category. Categories are deleted through
     * {@link com.dotmarketing.portlets.categories.business.CategoryAPI}, not through this action.
     *
     * @param mapping the Struts mapping of the action
     * @param form    the posted category form
     * @param config  the portlet configuration
     * @param req     the portlet request, with the {@code cmd}, {@code inode} and {@code referer}
     *                parameters
     * @param res     the portlet response
     * @throws Exception when the category cannot be loaded or the form cannot be filled
     */
    @Override
    public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req,
            ActionResponse res) throws Exception {
        String cmd = req.getParameter(com.liferay.portal.util.Constants.CMD);
        
        String referer = req.getParameter("referer");
        
        //get category from inode
        // checking if it is a new category http://jira.dotmarketing.net/browse/DOTCMS-6270
        if(_viewCategory(form, req, res) && req.getParameter("itsnew")!=null) {
        	setForward(req, mapping.getInput());
        	return;
        }
        
        // Save / Update a Category
        if (com.liferay.portal.util.Constants.ADD.equals(cmd)) {
            
            ///Validate category
            if (!Validator.validate(req, form, mapping)) {
                Logger.debug(this, "Category:  Validation Category Failed");
                setForward(req, mapping.getInput());
                
                return;
            } else {
                try {
                    if(_saveCategory(form, req, res))
                        _sendToReferral(req, res, referer);
                    else
                        setForward(req, mapping.getInput());
                } catch (Exception e) {
                    _handleException(e, req);
                }
                
                
                return;
            }
        }

        BeanUtils.copyProperties(form, req.getAttribute(WebKeys.CATEGORY_EDIT));
        setForward(req, "portlet.ext.categories.edit_category");
    }
    
    /*Private Methods*/
    
    //save category
    private boolean _saveCategory(ActionForm form, ActionRequest req, ActionResponse res) 
    	throws IllegalAccessException, InvocationTargetException, DotDataException, DotSecurityException {
    	
    	User user = _getUser(req);
        BeanUtils.copyProperties(req.getAttribute(WebKeys.CATEGORY_EDIT), form);
        Category cat = (Category) req.getAttribute(WebKeys.CATEGORY_EDIT);
        Category parent = null;
               
        String catvelvar=cat.getCategoryVelocityVarName();
        Boolean Proceed=false;
        if(!UtilMethods.isSet(catvelvar)){
        	catvelvar=StringUtils.camelCaseLower(cat.getCategoryName());
        	Proceed=true;
        }
		if(!InodeUtils.isSet(cat.getInode())|| Proceed){	 
			if(VelocityUtil.isNotAllowedVelocityVariableName(catvelvar)){
			    catvelvar= catvelvar + "Field";
			}
			catvelvar = categoryAPI.suggestVelocityVarName(catvelvar);
			cat.setCategoryVelocityVarName(catvelvar);
		}
	
        String parentInode =  req.getParameter("parent");
        if(InodeUtils.isSet(parentInode))
        	parent = (Category) categoryAPI.find(req.getParameter("parent"), user, false);

        if (!UtilMethods.isSet(cat.getKey()))
        	cat.setKey(null);
        
        if (!UtilMethods.isSet(cat.getKeywords()))
        	cat.setKeywords(null);
        
        Logger.debug(this, "_saveCategory: Inode = " + cat.getInode());

        try {
			categoryAPI.save(parent, cat, user, false);
		} catch (DotSecurityException e) {
	    	SessionMessages.add(req, "error", "message.category.permission.error");
	    	return false;
		}
        
        SessionMessages.add(req, "message", "message.category.update");
        return true;
        
    }
    
    //view category for Action request
    private boolean _viewCategory(ActionForm form, ActionRequest req, ActionResponse res)
    throws Exception {
    	User user = _getUser(req);
    	Category cat = new Category();
    	boolean isnew=false;
    	if(InodeUtils.isSet(req.getParameter("inode")) && !req.getParameter("inode").equals("0"))
    		cat = (Category) categoryAPI.find(req.getParameter("inode"), user, false);
    	else
    		isnew=true;
        req.setAttribute(WebKeys.CATEGORY_EDIT, cat);
        req.setAttribute(WebKeys.CATEGORY_LIST_TOP, categoryAPI.findTopLevelCategories(user, false));
        return isnew;
    }
}
