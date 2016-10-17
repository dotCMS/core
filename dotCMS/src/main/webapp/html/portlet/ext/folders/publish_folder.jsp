<%@ include file="/html/portlet/ext/folders/init.jsp" %>

<%@ page import="com.dotmarketing.util.Config" %>
<%@ page import="com.dotmarketing.portlets.links.model.Link" %>
<%@ page import="com.dotmarketing.portlets.templates.model.Template" %>
<%@ page import="com.dotmarketing.portlets.files.model.File" %>
<%@ page import="com.dotmarketing.portlets.containers.model.Container" %>
<%@ page import="com.dotmarketing.portlets.contentlet.model.Contentlet" %>
<%@ page import="com.dotmarketing.portlets.htmlpages.model.HTMLPage" %>
<%@page import="com.dotmarketing.util.UtilMethods"%>

<%
	String referer = request.getParameter("referer");
%>

<script language="Javascript">
function submitfmPublish() {
	form = document.getElementById('fm');
	form.cmd.value = "publish";
	form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/folders/publish_folder" /></portlet:actionURL>';
	submitForm(form);
}
function cancel() {
	window.location.href = "<%=referer%>";
}
</script>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Folder-Related-Assets" )) %>'/>

	<table border="0" cellpadding="0" cellspacing="0" width="100%">
		<tr>
			<td>
				<form id="fm" method="post">
				<input type="hidden" name="referer" value="<%=referer%>">
				<input type="hidden" name="cmd" value="publish">
				<input type="hidden" name="inode" value="<%= request.getParameter("inode") %>">
				
				<table border="0" cellpadding="0" cellspacing="0" width="100%" class="listingTable">
					<tr class="header">
						<td>&nbsp;<%= LanguageUtil.get(pageContext, "Title") %></td>
						<td><%= LanguageUtil.get(pageContext, "Description") %></td>
					</tr>
					<% 
						java.util.List relatedAssets = (java.util.List) request.getAttribute(com.dotmarketing.util.WebKeys.FOLDER_RELATED_ASSETS);
						java.util.Iterator relatedAssetsIter = relatedAssets.iterator();
						int k=0;						
						while (relatedAssetsIter.hasNext()) {

							com.dotmarketing.beans.WebAsset webasset =  null;
							Contentlet con = null;
							Object o = relatedAssetsIter.next(); 
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
							<td valign="bottom">
								<table border="0" cellpadding="0" cellspacing="0" width="100%" >
								<tr>
									<td width="20">
										<% if(con != null){ %>
											<%= com.dotmarketing.util.UtilHTML.getAssetIcon(con) %>&nbsp;
										<%}else{ %>
											<%= com.dotmarketing.util.UtilHTML.getAssetIcon(webasset) %>&nbsp;
										<%} %>
									</td>
									<td>
										<%
											String title = "";
											if(con != null){
												title = "Contentlet Title";
											}
											else if (webasset instanceof Link) {
												title = ((Link)webasset).getProtocal() + ((Link)webasset).getUrl();
											}
											else if (webasset instanceof File) {
												title = ((File)webasset).getFileName();
											}
											else if (webasset instanceof HTMLPage) {
												title = ((HTMLPage)webasset).getPageUrl();
											}
											else {
												title = webasset.getTitle();
											}
										%>
										<%=title%>
									</td>
								</tr>
								</table>
							</td>
							<td>
								<% if(con != null){ %>
								<%=con.getTitle()%>
								<% } else { %>
								<%=webasset.getFriendlyName()%>
								<% } %>
							</td>
						</tr>
					<%}%>
					<% if (relatedAssets.size() ==0) { %>
					<tr>
						<td colspan="3" align=center>
						<%= LanguageUtil.get(pageContext, "There-are-no-Related-Assets-to-show") %>
						</td>
					</tr>
					<% } %>
					
					<tr><td colspan="3">&nbsp;</td></tr>
					<tr>
						<td colspan="3" align="center">
                        <button dojoType="dijit.form.Button" onClick="submitfmPublish()">
                           <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "publish-all-related-assets"  )) %>
                        </button>
                        <button dojoType="dijit.form.Button" onClick="cancel()">
                           <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel" )) %>
                        </button>
						</td>
					</tr>
				</table>
				</form>
			</td>
		</tr>
	</table>
</liferay:box>

