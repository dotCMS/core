<%@ page import="com.liferay.portal.util.PortalUtil"%>
<%@ page import="com.liferay.util.ParamUtil"%>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.util.Config" %>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.db.DbConnectionFactory"%>




<%if((DbConnectionFactory.isOracle() ||  DbConnectionFactory.isMsSql()) 
		&& "100".equals(System.getProperty("dotcms_level")) 
		&& session.getAttribute("db-community-edition-warning") ==null){ %>
		<%session.setAttribute("db-community-edition-warning", "1");  %>
	<script>
		function closeCotDbWarningDialog(){
			dijit.byId('dotDbWarningDialog').hide();
			<%if(request.getAttribute("licenseManagerPortletUrl") != null){ %>
				window.location='<%=request.getAttribute("licenseManagerPortletUrl") %>';
			<%}%>
		}
	</script>
		
		
	<div id="dotDbWarningDialog" dojoType="dijit.Dialog" style="display:none" title="<%= LanguageUtil.get(pageContext, "db-community-edition-warning-title") %>">
		<div dojoType="dijit.layout.ContentPane" style="width:400px;height:150px;" class="box" hasShadow="true" id="dotDbWarningDialogCP">
			<%= LanguageUtil.get(pageContext, "db-community-edition-warning-text") %>
			<br>&nbsp;<br>
			<div class="buttonRow">
				<button dojoType="dijit.form.Button" onClick="closeCotDbWarningDialog()" iconClass="cancelIcon"><%= LanguageUtil.get(pageContext, "close") %></button>
			</div>
		</div>
	</div>
	<script>
		dojo.addOnLoad (function(){
			dojo.style(dijit.byId("dotDbWarningDialog").closeButtonNode, "visibility", "hidden"); 
			dijit.byId("dotDbWarningDialog").show();
		});
	</script>
	
<%} %>


<iframe name="hidden_iframe" id="hidden_iframe" style="position:absolute;top:-100px;width:0px; height:0px; border: 0px;"></iframe>
<script>
	function setKeepAlive(){
		var myId=document.getElementById("hidden_iframe");
		myId.src ="/html/common/keep_alive.jsp?r=<%=System.currentTimeMillis()%>";
	}
	// 15 minutes
	setTimeout("setKeepAlive()", 60000 * 15);
</script>

	
	