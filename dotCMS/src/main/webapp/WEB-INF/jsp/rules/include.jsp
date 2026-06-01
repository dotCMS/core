<%@ page import="com.dotmarketing.util.Config" %>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotcms.repackage.org.apache.struts.Globals"%>
<%@ page import="com.dotmarketing.util.PortletURLUtil" %>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.util.UUIDUtil"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.liferay.util.Xss"%>
<%@page import="javax.ws.rs.WebApplicationException"%>
<%@page import="javax.ws.rs.core.Response"%>
<%@ include file="/html/common/init.jsp" %>

<%
    final String id = request.getParameter("id");
    if (!UtilMethods.isSet(id)) {
        Logger.debug(this, "rules/include called with missing or empty 'id' parameter");
        throw new WebApplicationException(
            Response.status(Response.Status.BAD_REQUEST)
                .entity("Missing or empty required parameter: id")
                .build()
        );
    }

    if (!UUIDUtil.isUUID(id)) {
        Logger.debug(this, "rules/include called with invalid id format");
        throw new WebApplicationException(
            Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid id format: " + Xss.encodeForHTML(id))
                .build()
        );
    }

    Contentlet contentlet;
    try {
        contentlet = APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(id);
    } catch (final Exception e) {
        Logger.debug(this, "rules/include - findContentletByIdentifierAnyLanguage failed for id=" + id, e);
        throw new WebApplicationException(
            Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid id parameter: " + Xss.encodeForHTML(id))
                .build()
        );
    }

    if (contentlet == null || !UtilMethods.isSet(contentlet.getIdentifier())) {
        Logger.debug(this, "rules/include - no contentlet found for id=" + id);
        throw new WebApplicationException(
            Response.status(Response.Status.NOT_FOUND)
                .entity("No content found for id: " + Xss.encodeForHTML(id))
                .build()
        );
    }

    if (user == null || !APILocator.getPermissionAPI().doesUserHavePermission(
            contentlet, PermissionAPI.PERMISSION_READ, user, false)) {
        Logger.debug(this, "rules/include - user lacks READ on id=" + id);
        throw new WebApplicationException(
            Response.status(Response.Status.FORBIDDEN)
                .entity("Access denied")
                .build()
        );
    }
%>


	
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

  var siteParam="realmId=<%=Xss.encodeForJavaScript(id)%>";
  var hideFireOnParam = "hideFireOn=true";
  var hideRulePushOptions = "hideRulePushOptions=<%=Xss.encodeForJavaScript(request.getParameter("hideRulePushOptions"))%>";
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