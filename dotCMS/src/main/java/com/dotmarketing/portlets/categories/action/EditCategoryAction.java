package com.dotmarketing.portlets.categories.action;

import java.lang.reflect.InvocationTargetException;


import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;

import com.dotcms.repackage.org.apache.commons.beanutils.BeanUtils;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
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


public class EditCategoryAction extends DotPortletAction {
	
	private CategoryAPI categoryAPI;
	
	public EditCategoryAction () {
		categoryAPI = APILocator.getCategoryAPI();
	}

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
        // Delete a Category
        else if (com.liferay.portal.util.Constants.DELETE.equals(cmd)) {
            Logger.debug(this, "Category:  Deleting Category");
            
            try {
                if(_deleteCategory(form, req, res))
                    _sendToReferral(req, res, referer);
                else
                    setForward(req, mapping.getInput());
            } catch (Exception e) {
                _handleException(e, req);
            }
            
            _sendToReferral(req, res, referer);
            
            return;
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
    
    /**
     * REturns true if the category was correctly deleted, false if the user didn't have permission to delete it
     * @param form
     * @param req
     * @param res
     * @return
     * @throws DotDataException 
     */
    private boolean _deleteCategory(ActionForm form, ActionRequest req, ActionResponse res) throws DotDataException {
    	User user = _getUser(req);
        Category cat = (Category) req.getAttribute(WebKeys.CATEGORY_EDIT);
        
        InodeFactory.deleteInode(cat);
		try {
			categoryAPI.delete(cat, user, false);
		} catch (DotSecurityException e) {
        	SessionMessages.add(req, "error", "message.category.permission.error");
        	return false;
		}

		req.setAttribute(WebKeys.CATEGORY_EDIT, cat);
        SessionMessages.add(req, "message", "message.category.delete");
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
