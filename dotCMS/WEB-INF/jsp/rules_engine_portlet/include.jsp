<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@ page import="com.dotmarketing.util.Config" %>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotcms.repackage.org.apache.struts.Globals"%>
<%@ include file="/html/common/init.jsp" %>





<%if( LicenseUtil.getLevel() < 200){ %>
	<div class="portlet-wrapper">
		<div class="subNavCrumbTrail">
			<ul id="subNavCrumbUl">
				<li class="lastCrumb">
					<a href="#" ><%=LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title.RULES_ENGINE_PORTLET")%></a>
				</li>

			</ul>
			<div class="clear"></div>
		</div>
		<jsp:include page="/WEB-INF/jsp/rules_engine_portlet/not_licensed.jsp"></jsp:include>

	</div>
<%return;}%>

	
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

	
  //Add param to the rules engine iframe.
  document.getElementById("rulesIframe").src = "/html/js/_rulesengine?" + localeParam + "&" + siteParam + "&" + hideFireOnParam+ "&" +hideRulePushOptions;
  


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