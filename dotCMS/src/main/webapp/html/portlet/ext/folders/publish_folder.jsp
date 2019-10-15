<%@ include file="/html/portlet/ext/folders/init.jsp" %>

<%@ page import="com.dotmarketing.portlets.links.model.Link" %>
<%@ page import="com.dotmarketing.portlets.contentlet.model.Contentlet" %>
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
    window.location.href = "<%=referer%>" + "&p_p_id=site-browser";
}
</script>



				<form id="fm" method="post">
				<input type="hidden" name="referer" value="<%=referer%>">
				<input type="hidden" name="cmd" value="publish">
				<input type="hidden" name="inode" value="<%= request.getParameter("inode") %>">
		<div style="margin:auto;width:90%;padding:20px;">
                 <table class="listingTable" style="border:1px solid silver;">
					<tr class="header">
                        <th></th>
						<th>&nbsp;<%= LanguageUtil.get(pageContext, "Title") %></th>
						<th><%= LanguageUtil.get(pageContext, "Description") %></th>
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
							

	
						%>
						<tr  >

									<td width="20">
										<% if(con != null && con.getTitleImage().isPresent()){ %>
                                            <img src="/dA/<%=con.getInode()%>/<%=con.getTitleImage().get().variable()%>/75h/25q" >
                                        <%}else if(con !=null){ %>
											<%= com.dotmarketing.util.UtilHTML.getAssetIcon(con) %>&nbsp;
										<%}else{ %>
											<%= com.dotmarketing.util.UtilHTML.getAssetIcon(webasset) %>&nbsp;
										<%} %>
									</td>
									<td>
										<%
											String title = "";
											if(con != null){
												title = con.getTitle();
											}
											else if (webasset instanceof Link) {
												title = ((Link)webasset).getProtocal() + ((Link)webasset).getUrl();
											}
											else {
												title = webasset.getTitle();
											}
										%>
										<%=title%>
									</td>
							<td>
								<% if(con != null){ %>
								<%=con.getContentType().name()%>
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

				</table>
            
            <div style="margin:auto;width:400px;">
                                   
                        <button dojoType="dijit.form.Button" onClick="submitfmPublish()">
                           <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "publish-all-related-assets"  )) %>
                        </button>&nbsp; &nbsp; &nbsp;
                        <button dojoType="dijit.form.Button" onClick="cancel()" class="dijitButtonFlat">
                           <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel" )) %>
                        </button>
                 
            
            </div>
            </div>
            
            
            
				</form>


