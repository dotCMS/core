package com.dotmarketing.portlets.categories.action;

import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
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
