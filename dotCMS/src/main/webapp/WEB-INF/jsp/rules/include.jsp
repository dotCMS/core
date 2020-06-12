<%@ page import="com.dotmarketing.util.Config" %>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotcms.repackage.org.apache.struts.Globals"%>
<%@ page import="com.dotmarketing.util.PortletURLUtil" %>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@ include file="/html/common/init.jsp" %>

<% Contentlet contentlet =  (Contentlet) APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(request.getParameter("id"));  %>


	
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

  var siteParam="realmId=<%=request.getParameter("id")%>";
  var hideFireOnParam = "hideFireOn=true"; 
  var hideRulePushOptions = "hideRulePushOptions=<%=request.getParameter("hideRulePushOptions")%>";
  var isContentletHost = "isContentletHost=<%=contentlet.isHost()%>"
	
  //Add param to the rules engine iframe.
  document.getElementById("rulesIframe").src = "/<%=PortletURLUtil.URL_ADMIN_PREFIX%>/index.html#/fromCore/rules?" + localeParam + "&" + siteParam + "&" + hideFireOnParam+ "&" +hideRulePushOptions + "&" + isContentletHost ;
  
  

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
	});
	dojo.connect(window, "onresize", this, "resizeIframe");
	
</script>