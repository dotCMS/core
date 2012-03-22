<%@ include file="/html/portlet/ext/usermanager/init.jsp" %>

<!--form method="post" name="fmnu" id="fmnu" style="margin:0px; padding:0px;" action="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/usermanager/edit_usermanager" /><portlet:param name="cmd" value="load_register_user" /></portlet:actionURL>" >
<input type="hidden" name="referer" value="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/usermanager/view_usermanagerlist" /></portlet:actionURL>" >

<table border="0" cellpadding="4" cellspacing="0" width="100%" >
	<tr class="beta">
		<td >
			<font class="beta" size="2"><a class="beta" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/usermanager/view_usermanagerlist" /></portlet:renderURL>"><%= LanguageUtil.get(pageContext, "Search-Users") %></a></font>
				&nbsp;&nbsp;|&nbsp;&nbsp;
			<font class="beta" size="2"><a class="beta" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/usermanager/edit_usermanager" /><portlet:param name="cmd" value="load_register_user" /></portlet:actionURL>"><%= LanguageUtil.get(pageContext, "Add-New-User") %></a></font>
				&nbsp;&nbsp;|&nbsp;&nbsp;
			<font class="beta" size="2"><a class="beta" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/usermanager/view_usermanagerlist" /><portlet:param name="cmd" value="load" /></portlet:renderURL>"><%= LanguageUtil.get(pageContext, "Load-Users-title") %></a></font>
		</td>
	</tr>
</table>

</form-->

<%
	RenderRequestImpl rreq = (RenderRequestImpl) pageContext.getAttribute("renderRequest");
	String portletId1 = rreq.getPortletName();
	
	if (!UtilMethods.isSet(portletId1))
		portletId1 = layouts[0].getPortletIds().get(0);
	
	Portlet portlet1 = PortletManagerUtil.getPortletById(company.getCompanyId(), portletId1);
	String strutsAction = ParamUtil.get(request, "struts_action", null);
	String cmd = ParamUtil.get(request, "cmd", null);
	String referer = UtilMethods.decodeURL(UtilMethods.decodeURL(ParamUtil.get(request, "referer", null)));
	String inode = ParamUtil.get(request, "inode", "");
	
	if (!UtilMethods.isSet(strutsAction) || strutsAction.equals(portlet1.getInitParams().get("view-action"))) {
		List<CrumbTrailEntry> crumbTrailEntries = new ArrayList<CrumbTrailEntry>();
		
		crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), null));
		
		request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, crumbTrailEntries);
	} else if (referer.contains("/ext/order_manager/view_users")) {
		List<CrumbTrailEntry> crumbTrailEntries = new ArrayList<CrumbTrailEntry>();
		
		Map params = new HashMap();
		params.put("struts_action", new String[] {"/ext/order_manager/view_orders"});
		String crumbTrailReferer = PortletURLUtil.getRenderURL(request, WindowState.MAXIMIZED.toString(), params);
		crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), crumbTrailReferer));
		
		params = new HashMap();
		params.put("struts_action", new String[] {"/ext/order_manager/view_products"});
		crumbTrailReferer = PortletURLUtil.getActionURL(request, WindowState.MAXIMIZED.toString(), params);
		crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Add-Product"), crumbTrailReferer));
		
		params = new HashMap();
		params.put("struts_action", new String[] {"/ext/order_manager/view_products"});
		params.put("cmd", new String[] {"view"});
		crumbTrailReferer = PortletURLUtil.getActionURL(request, WindowState.MAXIMIZED.toString(), params);
		crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "View-Shopping-Cart"), crumbTrailReferer));
		
		crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Select-User"), "javascript: doCancel()"));
		
		crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Add-User"), null));
		
		request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, crumbTrailEntries);
	} else if (UtilMethods.isSet(inode)) {
		List<CrumbTrailEntry> crumbTrailEntries = new ArrayList<CrumbTrailEntry>();
		
		Map params = new HashMap();
		params.put("struts_action", new String[] {"/ext/order_manager/view_orders"});
		String crumbTrailReferer = PortletURLUtil.getRenderURL(request, WindowState.MAXIMIZED.toString(), params);
		crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), crumbTrailReferer));
		
		params = new HashMap();
		params.put("struts_action", new String[] {"/ext/order_manager/edit_order"});
		params.put("inode", new String[] {inode});
		params.put("cmd", new String[] {Constants.EDIT});
		crumbTrailReferer = PortletURLUtil.getActionURL(request, WindowState.MAXIMIZED.toString(), params);
		crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Edit-Order"), crumbTrailReferer));
		
		crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "edit-user"), null));
		
		request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, crumbTrailEntries);
	} else if (cmd.equals("load")) {
		List<CrumbTrailEntry> crumbTrailEntries = new ArrayList<CrumbTrailEntry>();
		
		Map params = new HashMap();
		params.put("struts_action", new String[] {"/ext/mailinglists/view_mailinglists"});
		String crumbTrailReferer = PortletURLUtil.getRenderURL(request, WindowState.MAXIMIZED.toString(), params);
		crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), crumbTrailReferer));
		
		crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Load-Users"), null));
		
		request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, crumbTrailEntries);
	} else {
		List<CrumbTrailEntry> crumbTrailEntries = new ArrayList<CrumbTrailEntry>();
		
		Map params = new HashMap();
		params.put("struts_action", new String[] {"/ext/mailinglists/view_mailinglists"});
		String crumbTrailReferer = PortletURLUtil.getRenderURL(request, WindowState.MAXIMIZED.toString(), params);
		crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), crumbTrailReferer));
		
		crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Edit-Mailing-List"), null));
		
		request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, crumbTrailEntries);
	}
	
	request.setAttribute(com.dotmarketing.util.WebKeys.DONT_DISPLAY_SUBNAV_ALL_HOSTS, false);

%>

<%@ include file="/html/portlet/ext/common/sub_nav_inc.jsp" %>