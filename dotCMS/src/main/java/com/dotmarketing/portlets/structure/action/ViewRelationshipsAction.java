package com.dotmarketing.portlets.structure.action;

import java.util.List;

import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;

import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;

import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;

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
			RenderResponse res, String orderBy, String structureId) {
		List<Relationship> list = RelationshipFactory.getRelationships(orderBy, structureId);
		req.setAttribute(WebKeys.Relationship.RELATIONSHIPS, list);
	}
}
