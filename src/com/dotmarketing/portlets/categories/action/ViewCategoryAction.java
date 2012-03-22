package com.dotmarketing.portlets.categories.action;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.util.servlet.SessionMessages;

public class ViewCategoryAction extends DotPortletAction {

	private CategoryAPI categoryAPI = APILocator.getCategoryAPI();
	
	
    public CategoryAPI getCategoryAPI() {
		return categoryAPI;
	}

	public void setCategoryAPI(CategoryAPI categoryAPI) {
		this.categoryAPI = categoryAPI;
	}

	public ActionForward render(ActionMapping mapping, ActionForm form, PortletConfig config, RenderRequest req,
            RenderResponse res) throws Exception {
        
		_viewCategory(form,req,res);

		return mapping.findForward("portlet.ext.categories.view_categories");

    }
    
//    public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req,
//        ActionResponse res) throws Exception {
//        String cmd = req.getParameter(com.liferay.portal.util.Constants.CMD);
//        
//		//wraps request to get session object
//		ActionRequestImpl reqImpl = (ActionRequestImpl)req;
//		HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
//		HttpSession session = httpReq.getSession();
//        String referer = req.getParameter("referer");
//        //get category from inode
//        _viewCategory(form,req,res);
//        User user = _getUser(req);
//        // Delete a Category
//        if (com.liferay.portal.util.Constants.DELETE.equals(cmd)) {
//            Logger.debug(this, "Category:  Deleting Category");
//
//			try {
//				_deleteCategory(form,req,res, user, session);
//				
//				if (req.getParameter("parent") !=null) {
//
//					java.util.Map<String, String[]> params = new java.util.HashMap<String, String[]>();
//					params.put("struts_action",new String[] {"/ext/categories/view_category"});
//					params.put("inode", new String[] { req.getParameter("parent") });
//	
//					referer = com.dotmarketing.util.PortletURLUtil.getActionURL(httpReq,WindowState.MAXIMIZED.toString(),params);
//				}
//			}
//			catch(Exception e) {
//				_handleException(e, req);
//			}
//			_sendToReferral(req,res,referer);
//        }
//
//		// Reordering categories
//		else if ((cmd != null) && cmd.equals("REORDER")) {
//            Logger.debug(this, "Category:  Reordering Categories");
//			
//			try {
//				_reorderCategories(form,req,res);
//			}
//			catch(Exception e) {
//				_handleException(e, req);
//			}
//			setForward(req,"portlet.ext.categories.view_categories");
//		}
//
//		// Add child categories
//		else if ((cmd != null) && cmd.equals("ADDCHILD")) {
//            Logger.debug(this, "Category:  Add Child Categories");
//			
//			try {
//				_addChildCategories(form,req,res);
//			}
//			catch(Exception e) {
//				_handleException(e, req);
//			}
//			setForward(req,"portlet.ext.categories.view_categories");
//		}
//
//		// Add child categories
//		else if ((cmd != null) && cmd.equals("DELCHILD")) {
//            Logger.debug(this, "Category:  Delete Child Categories");
//			
//			try {
//				_deleteChildCategories(form,req,res);
//			}
//			catch(Exception e) {
//				_handleException(e, req);
//			}
//			setForward(req,"portlet.ext.categories.view_categories");
//		}
//    }
    
    
    /*Private Methods*/
	//delete category
    
    
	private void _deleteCategory(ActionForm form, ActionRequest req, ActionResponse res, User user, HttpSession session) throws Exception {
        Category cat = ( Category ) req.getAttribute(WebKeys.CATEGORY_EDIT);
        ActionErrors errors = new ActionErrors();
        if(!categoryAPI.hasDependencies(cat)){
			req.removeAttribute(WebKeys.CATEGORY_EDIT);
			categoryAPI.delete(cat, user, false);
			SessionMessages.add(req, "message", "message.category.delete");
		}
		else{ 
			SessionMessages.add(req, "error", "message.category.delete.failed.has.dependencies");
		}

	}
	
	//reorder categories
	private void _reorderCategories(ActionForm form, ActionRequest req, ActionResponse res) throws Exception {
		User user = _getUser(req);
		int count = 0;
		try {
			count = Integer.parseInt(req.getParameter("count"));
		}
		catch (Exception e) {}
		String[] order = new String[(count)];
		for (int i = 0; i < order.length; i++) {
			Category cat = (Category) categoryAPI.find(req.getParameter("inode" + i), user, false);
			cat.setSortOrder(req.getParameter("newOrder" + i));
			categoryAPI.save(null, cat, user, false);
		}
		
		/* http://jira.dotmarketing.net/browse/DOTCMS-2235
		 * This method is a public method from the CategoryAPI. The reordering should be done
		 * in the API; this method is here for 1.6.5b. This should change in a later version and
		 * reordering should be done in a private method in the API
		 */
		categoryAPI.flushChildrenCache();
		SessionMessages.add(req, "message", "message.category.reorder");

	}
	
	//add child categories
	private void _addChildCategories(ActionForm form, ActionRequest req, ActionResponse res) throws Exception {

		User user = _getUser(req);
		Category cat = ( Category ) req.getAttribute(WebKeys.CATEGORY_EDIT);
		Category child = (Category) categoryAPI.find(req.getParameter("child"), user, false);
		
		categoryAPI.addChild(cat, child, user, false);
		categoryAPI.save(cat, null, user, false);
        Logger.debug(this, "Child Category Added");
		SessionMessages.add(req, "message", "message.category.addchild");
	}
//	
//	//delete child categories
//	private void _deleteChildCategories(ActionForm form, ActionRequest req, ActionResponse res) throws Exception {
//
//		User user = _getUser(req);
//		Category cat = ( Category ) req.getAttribute(WebKeys.CATEGORY_EDIT);
//		Category child = (Category) categoryAPI.find(req.getParameter("child"), user, false);
//		categoryAPI.removeChild(cat, child, user, false);
//		InodeFactory.saveInode(cat);
//		req.setAttribute(WebKeys.CATEGORY_EDIT,cat);
//		SessionMessages.add(req, "message", "message.category.delchild");
//	}

	//view category for Action request
    private void _viewCategory(ActionForm form, ActionRequest req, ActionResponse res) throws Exception {

    	User user = _getUser(req);
    	
    	String inode = req.getParameter("inode");
    	Category cat = new Category();
    	if(inode != null && !inode.equals("null") && InodeUtils.isSet((req.getParameter("inode"))))
    		cat = (Category) categoryAPI.find(req.getParameter("inode"), user, false);
		req.setAttribute(WebKeys.CATEGORY_EDIT, cat);
		req.setAttribute(WebKeys.CATEGORY_LIST_TOP, categoryAPI.findTopLevelCategories(user, false));

    }
	//view category for Render request
	private void _viewCategory(ActionForm form, RenderRequest req, RenderResponse res) throws Exception {

    	User user = _getUser(req);

		Category cat = new Category();
		if(req.getParameter("inode") != null && !req.getParameter("inode").equals("null") && InodeUtils.isSet((req.getParameter("inode"))))
			cat = (Category) categoryAPI.find(req.getParameter("inode"), user, false);
		req.setAttribute(WebKeys.CATEGORY_EDIT, cat);
		req.setAttribute(WebKeys.CATEGORY_LIST_TOP, categoryAPI.findTopLevelCategories(user, false));

	}
}
