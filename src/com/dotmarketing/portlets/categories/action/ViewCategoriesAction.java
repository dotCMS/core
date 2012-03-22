package com.dotmarketing.portlets.categories.action;

import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

public class ViewCategoriesAction extends DotPortletAction {

	private CategoryAPI categoryAPI = APILocator.getCategoryAPI();
	
    public CategoryAPI getCategoryAPI() {
		return categoryAPI;
	}

	public void setCategoryAPI(CategoryAPI categoryAPI) {
		this.categoryAPI = categoryAPI;
	}

	public ActionForward render(ActionMapping mapping, ActionForm form, PortletConfig config, RenderRequest req,
            RenderResponse res) throws Exception {
        
		User user = _getUser(req);
		
        req.setAttribute(WebKeys.CATEGORY_LIST_TOP, categoryAPI.findTopLevelCategories(user, false));
        
        if (req.getWindowState().equals(WindowState.NORMAL)) {
            return mapping.findForward("portlet.ext.categories.view");
        } else {
            return mapping.findForward("portlet.ext.categories.view_categories");
        }
    }
    
}
