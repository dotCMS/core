<%@ taglib prefix="tiles" uri="/WEB-INF/tld/struts-tiles.tld" %><%
boolean access = APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(portlet.getPortletId(),user);
String licenseManagerOverrideTicket = (String)request.getParameter("licenseManagerOverrideTicket");
String roleAdminOverrideTicket = (String)request.getParameter("roleAdminOverrideTicket");
if(roleAdminOverrideTicket!=null){
	if(request.getSession().getAttribute("roleAdminOverrideTicket")!=null){
	  String overrideTicket = (String)request.getSession().getAttribute("roleAdminOverrideTicket");
	   if(overrideTicket!=null && roleAdminOverrideTicket.equalsIgnoreCase(overrideTicket)){
		  access = true;
	   }
	}
}

if(licenseManagerOverrideTicket!=null){
	if(request.getSession().getAttribute("licenseManagerOverrideTicket")!=null){
	  String overrideTicket = (String)request.getSession().getAttribute("licenseManagerOverrideTicket");
	   if(overrideTicket!=null && licenseManagerOverrideTicket.equalsIgnoreCase(overrideTicket)){
		  access = true;
	   }
	}
}


CachePortlet cachePortlet = null;
try {
	cachePortlet = PortalUtil.getPortletInstance(portlet, application);
}
/*catch (UnavailableException ue) {
	ue.printStackTrace();
}*/
catch (PortletException pe) {
	pe.printStackTrace();
}
catch (RuntimeException re) {
	re.printStackTrace();
}

//PortletPreferences portletPrefs = PortletPreferencesManagerUtil.getPreferences(company.getCompanyId(), PortalUtil.getPortletPreferencesPK(request, portlet.getPortletId()));
PortletPreferences portletPrefs = null;

PortletConfig portletConfig = PortalUtil.getPortletConfig(portlet, application);
PortletContext portletCtx = portletConfig.getPortletContext();


// Passing in a dynamic request with box_width makes it easier to render
// portlets, but will break the TCK because the request parameters will have an
// extra parameter


RenderRequestImpl renderRequest = new RenderRequestImpl(request, portlet, cachePortlet, portletCtx, null, null, portletPrefs, layoutId);

StringServletResponse stringServletRes = new StringServletResponse(response);

RenderResponseImpl renderResponse = new RenderResponseImpl(renderRequest, stringServletRes, portlet.getPortletId(), company.getCompanyId(), layoutId);

renderRequest.defineObjects(portletConfig, renderResponse);


int portletTitleLength = 12;

Map portletViewMap = CollectionFactory.getHashMap();

portletViewMap.put("access", new Boolean(access));
portletViewMap.put("active", new Boolean(portlet.isActive()));

portletViewMap.put("portletId", portlet.getPortletId());
portletViewMap.put("portletTitleLength", new Integer(portletTitleLength));

portletViewMap.put("restoreCurrentView", new Boolean(portlet.isRestoreCurrentView()));

renderRequest.setAttribute(WebKeys.PORTLET_VIEW_MAP, portletViewMap);

if ((cachePortlet != null) && cachePortlet.isStrutsPortlet()) {

	// Make sure the Tiles context is reset for the next portlet

	request.removeAttribute(org.apache.struts.taglib.tiles.ComponentConstants.COMPONENT_CONTEXT);
}

boolean portletException = false;

if (portlet.isActive() && access) {
	try {
		cachePortlet.render(renderRequest, renderResponse);
	}
	catch (UnavailableException ue) {
		portletException = true;

		PortalUtil.destroyPortletInstance(portlet);
	}
	catch (Exception e) {
		portletException = true;

		e.printStackTrace();
	}

	SessionMessages.clear(renderRequest);
	SessionErrors.clear(renderRequest);
}

boolean showPortletAccessDenied = portlet.isShowPortletAccessDenied();
boolean showPortletInactive = portlet.isShowPortletInactive();

	if ((cachePortlet != null) && cachePortlet.isStrutsPortlet()) {
		if (!access || portletException) {
			PortletRequestProcessor portletReqProcessor = (PortletRequestProcessor)portletCtx.getAttribute(WebKeys.PORTLET_STRUTS_PROCESSOR);

			ActionMapping actionMapping = portletReqProcessor.processMapping(request, response, (String)portlet.getInitParams().get("view-action"));

			ComponentDefinition definition = null;

			if (actionMapping != null) {

				// See action path /weather/view

				String definitionName = actionMapping.getForward();

				if (definitionName == null) {

					// See action path /journal/view_articles

					String[] definitionNames = actionMapping.findForwards();

					for (int definitionNamesPos = 0; definitionNamesPos < definitionNames.length; definitionNamesPos++) {
						if (definitionNames[definitionNamesPos].endsWith("view")) {
							definitionName = definitionNames[definitionNamesPos];

							break;
						}
					}

					if (definitionName == null) {
						definitionName = definitionNames[0];
					}
				}

				definition = TilesUtil.getDefinition(definitionName, request, application);
			}

			String templatePath = Constants.TEXT_HTML_DIR + "/portal/layout_portal.jsp";
			if (definition != null) {
				templatePath = Constants.TEXT_HTML_DIR + definition.getPath();
			}
	%>
		
<%@page import="com.dotmarketing.business.APILocator"%><jsp:include page="/html/portal/portlet_error.jsp"></jsp:include>
	<%
		}
		else {

				pageContext.getOut().print(stringServletRes.getString());

		}
	}
	else {
		renderRequest.setAttribute(WebKeys.PORTLET_CONTENT, stringServletRes.getString());

		String portletContent = StringPool.BLANK;
		if (portletException) {
			portletContent = "/portal/portlet_error.jsp";
		}
		
		
if(!statePopUp || portletException){%>
	<jsp:include page="/html/portal/portlet_error.jsp"></jsp:include>
<%}else{ %>
	<%= renderRequest.getAttribute(WebKeys.PORTLET_CONTENT) %>
<%}}%>


<style>
.dotcmsHelpButton{display:block;position:absolute;top:3px;right:50%;margin-left:200px;padding:3px 5px;border:1px solid #fff;font-size:11px;background: rgba(255, 255, 255, 0.3);color:#000;text-decoration:none;}
.dotcmsHelpButton:hover{background: rgba(255, 255, 255, 0.8);}
</style>

<script>
	function showHelp(){
		dijit.byId("_helpWindow").show();
		
		
		
		var jsonpArgs = {
			url: "http://dotcms.com/internal/jsonp.dot?id=<%=portlet.getPortletId() %>",
			callbackParamName: "dotcallback",
			load: function(data){
				var content = data.contentlets;
				var targetMenu = dojo.byId("_dotHelpMenu");
				var targetBody = dojo.byId("_dotHelpResults");
				
				targetMenu.innerHTML = "";
				targetBody.innerHTML = "";
				
				var i = 0;
				dojo.forEach(content, function(contentlets,i){
					targetMenu.innerHTML += "<a href='#" + i + "' class='helpMenuLink'>" + data.contentlets[i].headline + "</a><br>";
					i++;
				});
				
				var i = 0;
				dojo.forEach(content, function(contentlets,i){
					targetBody.innerHTML += "<h2 id='" + i + "'>" + data.contentlets[i].headline + "</h2>" 
					targetBody.innerHTML += data.contentlets[i].body;
					targetBody.innerHTML += "<div style='border-top:1px solid #ccc;margin-top:30px;padding-top:20px;'>&nbsp;<div>";
					i++;
				});
			},
			error: function(error){
				targetBody.innerHTML = "An unexpected error occurred: " + error;
			}
		};
		dojo.io.script.get(jsonpArgs);
    }
</script>



<a href="#" onclick="showHelp();" class="dotcmsHelpButton">
	Help for: <%=portlet.getPortletId() %>
</a>

<div id="_helpWindow" data-dojo-type="dijit.Dialog" data-dojo-props="title:'dotCMS Help'">
	<div style="height:600px;width:900px;overflow:auto;padding:20px;position:relative;">
		<div id="_dotHelpMenu" style="position:absolute;top:0;left:0;padding:20px 10px;width:210px;"> </div>
		<div id="_dotHelpResults"></div>
	</div>
</div>
