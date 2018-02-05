<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="org.apache.struts.Globals"%>
<%@ page import="com.dotmarketing.util.*" %>
<%@ include file="/html/common/init.jsp" %>

<%
	request.setAttribute("SHOW_HOST_SELECTOR", new Boolean(true));

	String portletId1 = "rules";

	
	Portlet portlet1 = PortletManagerUtil.getPortletById(company.getCompanyId(), portletId1);
	String strutsAction = ParamUtil.get(request, "struts_action", null);
	List<CrumbTrailEntry> cTrail = new ArrayList<CrumbTrailEntry>();
	String _crumbHost = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
	String _crumbHostname = null;
	if(UtilMethods.isSet(_crumbHost) && !_crumbHost.equals("allHosts")) {
		Host currentHost = APILocator.getHostAPI().find(_crumbHost, user, false);
		if(currentHost != null){
			_crumbHostname = currentHost.getHostname();
			if(UtilMethods.isSet(_crumbHostname)){
				cTrail.add(new CrumbTrailEntry(_crumbHostname, "javascript:showHostPreview();"));
			}
		}
	}
	if(cTrail.size() <1){
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "No-Host-Permission"), "#"));
	}
	if (!UtilMethods.isSet(strutsAction) || strutsAction.equals(portlet1.getInitParams().get("view-action"))) {
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title." + portletId1), null));
		request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, cTrail);
	} else {
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title." + portletId1), "javascript: cancelEdit();"));
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Edit-Virtual-Link"), null));
		request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, cTrail);
	}
%>

	<div class="portlet-wrapper">
		<%@ include file="/html/portlet/ext/common/sub_nav_inc.jsp" %>

		<%if( LicenseUtil.getLevel() < LicenseLevel.STANDARD.level){ %>
		
			<jsp:include page="/WEB-INF/jsp/rules/not_licensed.jsp"></jsp:include>

		<%return;}%>
	</div>
	
	<div id="rules-engine-container" class="portlet-wrapper">
	  <iframe id="rulesIframe" name="rulesIframe" width="100%" height="100%" frameborder="0" style="width:100%;height:100%;min-height:400px;"></iframe>
	</div>





<script>
  var localeParam = '';
  //Trying to get ISO Code from session and format to standard.
  var langIsoCode = '<%= request.getSession().getAttribute(Globals.LOCALE_KEY) %>';
  langIsoCode = langIsoCode.replace("_", "-");

  if(langIsoCode){
    localeParam = "locale=" + langIsoCode;
  }

  var siteParam="realmId=<%=session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID)%>";
  
	
  //Add param to the rules engine iframe.
  document.getElementById("rulesIframe").src = "/<%=com.dotmarketing.util.PortletURLUtil.URL_ADMIN_PREFIX%>/#/fromCore/rules?" + localeParam + "&" + siteParam;

	function  resizeIframe(){

        var viewport = dojo.window.getBox();
	    var viewport_height = viewport.h;

	    var e =  dojo.byId("rules-engine-container");
	    dojo.style(e, "height", (viewport_height - 150) + "px");
	}
	// need the timeout for back buttons

	dojo.addOnLoad(function(){
		resizeIframe();
		setTimeout(resizeIframe, 100);
		setTimeout(resizeIframe, 500);
		setTimeout(resizeIframe, 5000);

		// deal with style funk
		dojo.style("subNavCrumbTrail", "margin", "0px -10px 10px -10px");
		dojo.style("dotAjaxMainHangerDiv", "margin-top", "-9px");

		
	});
	dojo.connect(window, "onresize", this, "resizeIframe");
</script>