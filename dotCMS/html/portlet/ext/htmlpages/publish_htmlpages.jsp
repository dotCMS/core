<%@ include file="/html/portlet/ext/htmlpages/init.jsp" %>

<%@ page import="com.dotmarketing.util.Config" %>
<%@ page import="com.dotmarketing.portlets.links.model.Link" %>
<%@ page import="com.dotmarketing.portlets.templates.model.Template" %>
<%@ page import="com.dotmarketing.portlets.files.model.File" %>
<%@ page import="com.dotmarketing.portlets.containers.model.Container" %>
<%@ page import="com.dotmarketing.portlets.contentlet.model.Contentlet" %>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="java.util.ArrayList"%>
<%@ page import="com.dotmarketing.util.*" %>
<%
	String referer = request.getParameter("referer");
	String[] publishInodes = request.getParameterValues("publishInode");
%>

<script language="Javascript">
function submitfmPublish() {
	form = document.getElementById('fm');
	form.<portlet:namespace />cmd.value = "publish";
	form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/htmlpages/publish_htmlpages" /></portlet:actionURL>';
	submitForm(form);
}
function cancel() {
	window.location.href = "<%=referer%>";
}
</script>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "HTML-Page-Related-Assets")) %>'/>


<form id="fm" method="post">
<input type="hidden" name="referer" value="<%=referer%>">
<input type="hidden" name="<portlet:namespace />cmd" value="publish">

<% 
	for (int i=0;i<publishInodes.length;i++) {
%>
	<input type="hidden" name="publishInode" value="<%= publishInodes[i] %>">
<% } %>
<% 
	PermissionAPI perAPI = APILocator.getPermissionAPI();
	java.util.List relatedAssets = (java.util.List) request.getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_RELATED_ASSETS);				
	if(relatedAssets.size()>0){ 
		java.util.List relatedAssetsWithPerm = new ArrayList();
		java.util.List relatedAssetsWithOutPerm = new ArrayList();
		java.util.Iterator relatedAssetsIter = relatedAssets.iterator();
		while (relatedAssetsIter.hasNext()) {
			Object o = relatedAssetsIter.next();
			if(o instanceof Contentlet){
				Contentlet con = (Contentlet)o;
				if (perAPI.doesUserHavePermission(con,PermissionAPI.PERMISSION_PUBLISH, user)) {
					relatedAssetsWithPerm.add (con);
				} else {
					relatedAssetsWithOutPerm.add (con);
				}
			}else if(o instanceof WebAsset){
				com.dotmarketing.beans.WebAsset webasset =  (com.dotmarketing.beans.WebAsset) o;
				if (perAPI.doesUserHavePermission(webasset,PermissionAPI.PERMISSION_PUBLISH, user)) {
					relatedAssetsWithPerm.add (webasset);
				} else {
					relatedAssetsWithOutPerm.add (webasset);
				}
			}
		}
	%>
<% if (relatedAssetsWithPerm.size() > 0) { %>
 
	<h2 style="margin:10px;"><%= LanguageUtil.get(pageContext, "Related-Dependencies-that-also-need-to-be-published") %></h2>
	<table class="listingTable">
		<tr>
			<th colspan="2"><%= LanguageUtil.get(pageContext, "Title") %></th>
			<th><%= LanguageUtil.get(pageContext, "Comments") %></th>
		</tr>
		<% 
			java.util.Iterator relatedWithPermAssetsIter = relatedAssetsWithPerm.iterator();
			int k=0;						
			while (relatedWithPermAssetsIter.hasNext()) {
				com.dotmarketing.beans.WebAsset webasset = null;
				Contentlet con = null;
				Object o = relatedWithPermAssetsIter.next();
				if(o instanceof Contentlet){
					con = (Contentlet)o;
				}else{
					webasset =  (com.dotmarketing.beans.WebAsset) o;
				}
				k++;
				String str_style = "";
				if ((k%2)==0) {
					str_style = "class=\"alternate_2\"";
				}
				else{
				    str_style = "class=\"alternate_1\"";
                }
			%>
			<tr <%=str_style%> >
				<td width="20">
					<% if(con != null){ %>
						<%= com.dotmarketing.util.UtilHTML.getAssetIcon(con) %>
					<% }else{ %>
						<%= com.dotmarketing.util.UtilHTML.getAssetIcon(webasset) %>
					<% } %>
				</td>
				<td>
					<%
						String title = "";
						if(con != null){
							title = com.dotmarketing.util.UtilMethods.isSet(con.getTitle())?con.getTitle():"Contentlet Title";
						}
						else if (webasset instanceof Link) {
							title = ((Link)webasset).getProtocal() + ((Link)webasset).getUrl();
						}
						else if (webasset instanceof File) {
							title = ((File)webasset).getFileName();
						}
						else {
							title = webasset.getTitle();
						}
					%>
					<%=title%>
				</td>
				<td width="30%">
					<%if(con != null && APILocator.getWorkflowAPI().findSchemeForStruct(con.getStructure()).isMandatory()){ %>
						<span style="color:red"><%= LanguageUtil.get(pageContext, "Cannot-Publish-In-A-Workflow") %></span>
					
					<%} else if(con != null && !APILocator.getPermissionAPI().doesUserHavePermission(con, APILocator.getPermissionAPI().PERMISSION_PUBLISH, user)){%>
						<span style="color:red"><%= LanguageUtil.get(pageContext, "you-do-not-have-the-required-permissions") %></span>
					
					<%}else{ %>
						<%= LanguageUtil.get(pageContext, "Will-be-Published") %>
					
					<%} %>

				</td>
				
			</tr>
		<%	}	%>
	</table>
<% } %>
				

<% if (relatedAssetsWithOutPerm.size() > 0) { %>
	<h2 style="margin:10px;"><%= LanguageUtil.get(pageContext, "Related-Dependencies-that-also-need-to-be-published-but-you-dont-have-permissions-to-publish-them") %></h2>
	<table class="listingTable">
		<tr>
			<th colspan="2"><%= LanguageUtil.get(pageContext, "Title") %></th>
			<th><%= LanguageUtil.get(pageContext, "Description") %></th>
		</tr>
		<% 
			java.util.Iterator relatedWithOutPermAssetsIter = relatedAssetsWithOutPerm.iterator();
			int k=0;						
			while (relatedWithOutPermAssetsIter.hasNext()) {
				com.dotmarketing.beans.WebAsset webasset = null;
				Contentlet con = null;
				Object o = relatedWithOutPermAssetsIter.next();
				if(o instanceof Contentlet){
					con = (Contentlet)o;
				}else{
					webasset =  (com.dotmarketing.beans.WebAsset) o;
				}
				
				k++;
				String str_style = "";
				if ((k%2)==0) {
					str_style = "class=\"alternate_1\"";
				}
				else{
					str_style = "class=\"alternate_2\"";
				}
			%>
			<tr <%=str_style%>>
				<td width="20">
					<% if(con != null){ %>
						<%= com.dotmarketing.util.UtilHTML.getAssetIcon(con) %>
					<% }else{ %>
						<%= com.dotmarketing.util.UtilHTML.getAssetIcon(webasset) %>
					<% } %>
				</td>
				<td>
					<%
						String title = "";
						if(con != null){
							title = com.dotmarketing.util.UtilMethods.isSet(con.getTitle())?con.getTitle():"Contentlet Title";
						}
						else if (webasset instanceof Link) {
							title = ((Link)webasset).getProtocal() + ((Link)webasset).getUrl();
						}
						else if (webasset instanceof File) {
							title = ((File)webasset).getFileName();
						}
						else {
							title = webasset.getTitle();
						}
					%>
					<%=title%>
				</td>
				<td>
					<% if(con != null){%>
					    <%=com.dotmarketing.util.UtilMethods.isSet(con.getTitle())?con.getTitle():"&nbsp;"%>
					<%}else {%>    
						<%=webasset != null ? webasset.getFriendlyName():"Title"%>
					<%} %>
				</td>
			</tr>
		<% } %>
	</table>
	<% } %>
<% } %>
				
<div class="buttonRow">
	<button dojoType="dijit.form.Button" onClick="submitfmPublish()" iconClass="publishIcon">
	    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "publish-all-related-assets")) %>
    </button>&nbsp;
    <button dojoType="dijit.form.Button" onClick="cancel()" iconClass="cancelIcon">
	    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
    </button>
</div>

</form>
</liferay:box>

