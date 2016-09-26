<%@ include file="/html/portlet/ext/campaigns/init.jsp" %>
<%@ page import="com.dotmarketing.util.Config" %>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.dotmarketing.util.Config" %>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%
	com.dotmarketing.portlets.campaigns.model.Campaign c = (com.dotmarketing.portlets.campaigns.model.Campaign)  request.getAttribute(com.dotmarketing.util.WebKeys.CAMPAIGN_EDIT);
%>
<script language="Javascript">
	function deleteCampaign() {
		var form = document.getElementById('fm');
		if(confirm("<%= LanguageUtil.get(pageContext,"Are-you-sure-you-want-to-delete-this-campaign-(this-cannot-be-undone)") %>")){
			form.<portlet:namespace />cmd.value = '<%=Constants.DELETE%>';
			form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/campaigns/edit_campaign" /></portlet:actionURL>';
			submitForm(form);
		}
	}
</script>
<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Pending-Campaign")) %>' />
	
	<html:form action='/ext/campaigns/edit_campaign' styleId="fm">	
		<input type="hidden" name="inode" value="<%= String.valueOf(c.getInode()) %>">
		<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="resend">
		<input type="hidden" name="<portlet:namespace />redirect" value="<portlet:renderURL><portlet:param name="struts_action" value="/ext/campaigns/view_campaigns" /></portlet:renderURL>">
		
		<%= LanguageUtil.get(pageContext,"This-campaign-was-not-delivered-when-it-was-suppose-to") %>
		<br><br><br>

       <button dojoType="dijit.form.Button" onclick="deleteCampaign();" >
          <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "delete-campaign")) %>     
       </button>
        
	</html:form>
</liferay:box>