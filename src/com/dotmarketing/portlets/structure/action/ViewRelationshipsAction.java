package com.dotmarketing.portlets.structure.action;

import java.util.List;

import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;

public class ViewRelationshipsAction extends DotPortletAction {
	public ActionForward render(ActionMapping mapping, ActionForm form,
			PortletConfig config, RenderRequest req, RenderResponse res)
			throws Exception {
		String orderBy = req.getParameter("orderBy");
		orderBy = (UtilMethods.isSet(orderBy) ? orderBy : "relation_type_value");
		_loadRelationships(form, req, res, orderBy);
		return mapping.findForward("portlet.ext.structure.view_relationships");
	}

	private void _loadRelationships(ActionForm form, RenderRequest req,
			RenderResponse res, String orderBy) {
		List list = RelationshipFactory.getRelationships(orderBy);
		req.setAttribute(WebKeys.Relationship.RELATIONSHIPS, list);
	}
}
