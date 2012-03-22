<%@ include file="/html/portlet/ext/templates/init.jsp" %>

<%@ page import="com.dotmarketing.util.Config" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%
	String referer = request.getParameter("referer");
	String[] publishInodes = request.getParameterValues("publishInode");
%>

<script language="Javascript">
function submitfmPublish() {
	form = document.getElementById('fm');
	form.cmd.value = "publish";
	form.action = '<%=CTX_PATH%>/ext/templates/publish_templates';
	submitForm(form);
}
function cancel() {
	window.location.href = "<%=referer%>";
}
</script>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Template-Related-Assets")) %>' />

	<table border="0" cellpadding="2" cellspacing="2" width="100%">
		<tr>
			<td>
				<form id="fm" method="post">
				<input type="hidden" name="referer" value="<%=referer%>">
				<input type="hidden" name="cmd" value=<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "publish")) %>>
				<% 
					for (int i=0;i<publishInodes.length;i++) {
				%>
					<input type="hidden" name="publishInode" value="<%= publishInodes[i] %>">
				<% } %>
				
				<table border="0" cellpadding="2" cellspacing="2" width="100%">
					<tr class="beta">
						<Td>
						<B><font class="beta" size="2">
						<%= LanguageUtil.get(pageContext, "Title") %>
						</font></B>
						</td>
						<Td>
						<B><font class="beta" size="2">
						<%= LanguageUtil.get(pageContext, "Description") %>
						</font></B>
						</td>
					</tr>
					<% 
						java.util.List relatedAssets = (java.util.List) request.getAttribute(com.dotmarketing.util.WebKeys.TEMPLATE_RELATED_ASSETS);
						java.util.Iterator relatedAssetsIter = relatedAssets.iterator();
						int k=0;						
						while (relatedAssetsIter.hasNext()) {

							com.dotmarketing.portlets.containers.model.Container container = (com.dotmarketing.portlets.containers.model.Container) relatedAssetsIter.next();
							k++;
							String str_style = "";
							if ((k%2)==0) {
								str_style = "class=gamma";
							}
						%>
						<tr>
							<td <%=str_style%> valign="bottom">
								<table border=0 cellpadding=0 cellspacing=0 <%=str_style%>>
								<tr>
									<td width="16">
										<font class="gamma" size="2">
										<%= com.dotmarketing.util.UtilHTML.getAssetIcon(container) %>&nbsp;
										</font>
									</td>
									<td>
										<font class="gamma" size="2">
										<%=container.getTitle()%>
										</font>
									</td>
								</tr>
								</table>
							</td>
							<td <%=str_style%> >
								<font class="gamma" size="2">
								<%=container.getFriendlyName()%>
								</font>
							</td>
						</tr>
					<%}%>
					<% if (relatedAssets.size() ==0) { %>
					<tr>
						<td colspan="3" align=center>
						<font class="bg" size="2"><%= LanguageUtil.get(pageContext, "There-are-no-Related-Assets-to-show") %></font>
						</td>
					</tr>
					<% } %>
					
					<tr>
						<td colspan="3" align="center">
                        <button dojoType="dijit.form.Button" onClick="submitfmPublish()">
                           <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "publish-all-related-assets")) %>
                        </button>
                        
                        <button dojoType="dijit.form.Button" onClick="cancel()">
                           <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
                        </button>
						</td>
					</tr>
				</table>
				</form>
			</td>
		</tr>
	</table>
</liferay:box>

