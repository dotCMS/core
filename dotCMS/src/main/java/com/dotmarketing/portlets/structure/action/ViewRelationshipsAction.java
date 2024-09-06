package com.dotmarketing.portlets.structure.action;

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
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.util.List;
import java.util.stream.Collectors;

public class ViewRelationshipsAction extends DotPortletAction {

	private static final String ASCENDING = "ascending_";
	private static final String ORDER_BY = "orderBy";
	private static final String STRUCTURE_ID = "structure_id";

	public ActionForward render(ActionMapping mapping, ActionForm form,
								PortletConfig config, RenderRequest req, RenderResponse res)
			throws Exception {

		String structureId = req.getParameter(STRUCTURE_ID);
		String orderBy = req.getParameter(ORDER_BY);

		orderBy = (UtilMethods.isSet(orderBy) ? orderBy : "relation_type_value");
		final String ascending = req.getParameter(ASCENDING + orderBy);
		req.setAttribute(ASCENDING + orderBy, ascending);

		structureId = (UtilMethods.isSet(structureId) ? structureId : "all");
		orderBy = orderBy + ((StringPool.TRUE.equalsIgnoreCase(ascending))? SQLUtil._ASC:SQLUtil._DESC);
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
		List<Relationship> list = APILocator.getRelationshipAPI().byContentType(type , orderBy).stream()
				.filter(relationship -> !(relationship.isRelationshipField())).collect(Collectors.toList());
		req.setAttribute(WebKeys.Relationship.RELATIONSHIPS, list);
	}
}
