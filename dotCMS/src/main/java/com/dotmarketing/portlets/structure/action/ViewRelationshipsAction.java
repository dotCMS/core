package com.dotmarketing.portlets.structure.action;

import java.util.List;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;

import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portal.struts.DotPortletAction;

import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

public class ViewRelationshipsAction extends DotPortletAction {
	public ActionForward render(ActionMapping mapping, ActionForm form,
			PortletConfig config, RenderRequest req, RenderResponse res)
			throws Exception {
		String orderBy = req.getParameter("orderBy");
		String structureId = req.getParameter("structure_id");
		orderBy = (UtilMethods.isSet(orderBy) ? orderBy : "relation_type_value");
		structureId = (UtilMethods.isSet(structureId) ? structureId : "all");
		_loadRelationships(form, req, res, orderBy, structureId);
		return mapping.findForward("portlet.ext.structure.view_relationships");
	}

	private void _loadRelationships(ActionForm form, RenderRequest req,
			RenderResponse res, String orderBy, String structureId) throws DotDataException, DotSecurityException {
	  
	  
	    User user = _getUser(req);
	    ContentType type = ("all".equals(structureId)) 
	          ? ContentTypeBuilder
	              .builder(SimpleContentType.class)
	              .id("all")
	              .variable("all")
	              .name("all").build() 
	          : APILocator.getContentTypeAPI(user).find(structureId);
		List<Relationship> list = FactoryLocator.getRelationshipFactory().byContentType(type , orderBy);
		req.setAttribute(WebKeys.Relationship.RELATIONSHIPS, list);
	}
}
