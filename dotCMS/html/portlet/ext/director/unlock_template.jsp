<%@ include file="/html/portlet/ext/director/init.jsp" %>
<%@ page import="com.dotmarketing.portlets.templates.model.Template" %>
<%@ page import="com.dotmarketing.factories.InodeFactory" %>
<%@ page import="com.dotmarketing.portlets.htmlpages.model.HTMLPage" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.util.InodeUtils" %>

<% 
HTMLPage htmlPage = (HTMLPage) request.getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_EDIT);
Template template =(Template) request.getAttribute(com.dotmarketing.util.WebKeys.TEMPLATE_EDIT);

String referer = (request.getParameter("referer") != null ) ? java.net.URLDecoder.decode(request.getParameter("referer"),"UTF-8") : "";
%>
<%@page import="com.dotmarketing.util.DateUtil"%>
<%@page import="java.util.HashMap"%>
<form action="<portlet:actionURL><portlet:param name="struts_action" value="/ext/director/direct" /></portlet:actionURL>" method="post" id="fm">
<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="editTemplate">
<input name="<portlet:namespace />subcmd" type="hidden" value="unlockTemplate">
<input name="referer" type="hidden" value="<%=referer%>">
<% if (InodeUtils.isSet(htmlPage.getInode())) { %>
	<input type="hidden" name="htmlPage" value="<%= htmlPage.getInode() %>">
<% } else { %>
	<input type="hidden" name="template" value="<%= template.getInode() %>">
<% } %>


<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"unlock-template\") %>" />

<script language="Javascript">
var myForm = document.getElementById('fm');


function submitfm(form) {
	submitForm(form);
}

function cancelUnlock(){
	window.location = "<%=referer%>";

}


</script>
<%
	String userName = "unknown";
	String userFullName= "unknown";
	
	try{
		com.liferay.portal.model.User userMod = com.liferay.portal.ejb.UserManagerUtil.getUserById(template.getModUser());
		userName = userMod.getEmailAddress();
		userFullName = userMod.getFullName();
	}
	catch(Exception e){
		
	}
	
	SimpleDateFormat sdf = new SimpleDateFormat("h:mm aa, MMMM d?, yyyy");
	String modDate = sdf.format(template.getModDate());
	modDate = modDate.replaceAll("\\?", "th");
	String sinceMessage = "";
	HashMap<String, Long> diffDates = DateUtil.diffDates(template.getModDate(), new Date());
	if (0 < diffDates.get(DateUtil.DIFF_YEARS)) {
		sinceMessage = LanguageUtil.get(pageContext, "more-than-a-year-ago");
	} else if (48 < diffDates.get(DateUtil.DIFF_HOURS)) {
		sinceMessage = LanguageUtil.get(pageContext, "2-days-ago");
	} else if (24 < diffDates.get(DateUtil.DIFF_HOURS)) {
		sinceMessage = LanguageUtil.get(pageContext, "1-day-ago--") + modDate + ").";
	} else if (8 < diffDates.get(DateUtil.DIFF_HOURS)) {
		sinceMessage = "" + diffDates.get(DateUtil.DIFF_HOURS) +" " +LanguageUtil.get(pageContext, "hours-ago--") + modDate + ").";
	} else if (1 <= diffDates.get(DateUtil.DIFF_HOURS)) {
		sinceMessage = "" + diffDates.get(DateUtil.DIFF_HOURS) +" "+ LanguageUtil.get(pageContext, "hour-s--and") + diffDates.get(DateUtil.DIFF_MINUTES) +" " +LanguageUtil.get(pageContext, "minute-s--ago--") + modDate + ").";
	} else if (diffDates.get(DateUtil.DIFF_HOURS) < 1) {
		sinceMessage = "" + diffDates.get(DateUtil.DIFF_MINUTES)+" " + LanguageUtil.get(pageContext, "minute-s--ago--") + modDate + ").";
	}
%>

<div class="shadowBox headerBox" style="width:600px;margin:auto;padding:10px 20px;margin-bottom:20px;">
	<h4 style="margin-bottom:20px;"><span class="exclamation"></span> <%= LanguageUtil.get(pageContext, "Alert") %></h4>
	<h4 style="margin-bottom:20px;"><%= LanguageUtil.format(pageContext, "locked-asset-message", new LanguageWrapper[]{new LanguageWrapper("", "Template", ""), new LanguageWrapper("", userName, ""), new LanguageWrapper("", userFullName, ""), new LanguageWrapper("", sinceMessage, "")}) %></h4> 
	<h3><%= LanguageUtil.get(pageContext, "Would-you-like-to-unlock-it-to-proceed") %></h3>
	<div style="padding:10px 20px;">
		<b><%= LanguageUtil.get(pageContext, "Title") %>:</b>
		<%=template.getTitle()%><br/>
		
		<b><%= LanguageUtil.get(pageContext, "Description") %>:</b>
		<%=template.getFriendlyName()%>
	</div>
	<div class="clear">&nbsp;</div>
	<div class="buttonRow">
		<button dojoType="dijit.form.Button"  onClick="submitfm(document.getElementById('fm'))" iconClass="unlockIcon">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "unlock1")) %>
		</button>
		<button dojoType="dijit.form.Button" onClick="cancelUnlock()" iconClass="cancelIcon">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
		</button>
		
	</div>
</div>

</liferay:box>

</form>









