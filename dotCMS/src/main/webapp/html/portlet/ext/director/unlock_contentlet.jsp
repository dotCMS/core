<%@ include file="/html/portlet/ext/director/init.jsp" %>
<!-- JSP Imports --> 
<%@ page import="com.dotmarketing.portlets.contentlet.model.Contentlet" %>
<%@ page import="com.dotmarketing.portlets.containers.model.Container" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>

<% 
Contentlet contentlet;
if (request.getAttribute(com.dotmarketing.util.WebKeys.CONTENTLET_EDIT)!=null) {
	contentlet = (Contentlet) request.getAttribute(com.dotmarketing.util.WebKeys.CONTENTLET_EDIT);
}
else {
	ContentletAPI contentletAPI = APILocator.getContentletAPI();
	contentlet = (Contentlet) contentletAPI.find(request.getParameter("inode"), user, true);
	
}
String assetName ="Contentlet";
try{
Structure s = contentlet.getStructure();
	assetName = s.getName();
}
catch(Exception e){
	if(contentlet != null){
		Logger.debug(this, "Contentlet "+ contentlet.getInode() + " does not know its structure");
	}
	else{
		Logger.error(this, "Contentlet is null");
	}
}
String referer = (request.getParameter("referer") != null ) ? java.net.URLDecoder.decode(request.getParameter("referer"),"UTF-8") : "";

%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="com.dotmarketing.business.UserAPI"%>
<%@page import="com.dotmarketing.business.RoleAPI"%>
<%@page import="com.dotmarketing.business.VersionableAPI"%>
<%@page import="com.dotmarketing.util.DateUtil"%>
<%@page import="java.util.HashMap"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.util.Logger"%>
<form action="<portlet:actionURL><portlet:param name="struts_action" value="/ext/director/direct" /></portlet:actionURL>" method="post" id="fm">
<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="unlock">
<input name="<portlet:namespace />referer" type="hidden" value="<%=referer%>">
<input type="hidden" name="htmlPage" value="<%= (String) request.getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_INODE) %>">
<input type="hidden" name="container" value="<%= (String) request.getAttribute(com.dotmarketing.util.WebKeys.CONTAINER_INODE) %>">
<input type="hidden" name="contentlet" value="<%= contentlet.getInode() %>">
<input type="hidden" name="language" value="<%= contentlet.getLanguageId() %>">


<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"unlock-contentlet\") %>" />

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
	String userName= "unknown";
	String userFullName= "unknown";
	try{
		com.liferay.portal.model.User userMod = com.liferay.portal.ejb.UserManagerUtil.getUserById(contentlet.getModUser());
		userName = userMod.getEmailAddress();
		userFullName = userMod.getFullName();
	}
	catch(Exception e){

	}
	
	SimpleDateFormat sdf = new SimpleDateFormat("h:mm aa, MMMM d?, yyyy",LanguageUtil.getLocale(pageContext));
	String modDate = sdf.format(contentlet.getModDate());
	modDate = modDate.replaceAll("\\?", "th");
	String sinceMessage = "";
	HashMap<String, Long> diffDates = DateUtil.diffDates(contentlet.getModDate(), new Date());
	if (0 < diffDates.get(DateUtil.DIFF_YEARS)) {
		sinceMessage = LanguageUtil.get(pageContext, "more-than-a-year-ago");
	} else if (48 < diffDates.get(DateUtil.DIFF_HOURS)) {
		sinceMessage = LanguageUtil.get(pageContext, "2-days-ago");
	} else if (24 < diffDates.get(DateUtil.DIFF_HOURS)) {
		sinceMessage = LanguageUtil.get(pageContext, "1-day-ago--") + modDate + ").";
	} else if (8 < diffDates.get(DateUtil.DIFF_HOURS)) {
		sinceMessage = "" + diffDates.get(DateUtil.DIFF_HOURS) +" " +LanguageUtil.get(pageContext, "hours-ago--") + modDate + ").";
	} else if (1 <= diffDates.get(DateUtil.DIFF_HOURS)) {
		sinceMessage = "" + diffDates.get(DateUtil.DIFF_HOURS) +" "+ LanguageUtil.get(pageContext, "hour-s--and") + diffDates.get(DateUtil.DIFF_MINUTES) + " " +LanguageUtil.get(pageContext, "minute-s--ago--")  + modDate + ").";
	} else if (diffDates.get(DateUtil.DIFF_HOURS) < 1) {
		sinceMessage = "" + diffDates.get(DateUtil.DIFF_MINUTES)+" " + LanguageUtil.get(pageContext, "minute-s--ago--") + modDate + ").";
	}
	
	User systemUser = APILocator.getUserAPI().getSystemUser();
	boolean isUserCMSAdmin = APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole());
	String lockedUserId = APILocator.getVersionableAPI().getLockedBy(contentlet);
%>

<div class="shadowBox headerBox" style="width:600px;margin:auto;padding:10px 20px;margin-bottom:20px;">
	<h4 style="margin-bottom:20px;"><span class="exclamation"></span> <%= LanguageUtil.get(pageContext, "Alert") %></h4>
	<h4 style="margin-bottom:20px;margin-"><%= LanguageUtil.format(pageContext, "locked-asset-message", new LanguageWrapper[]{new LanguageWrapper("", "<span style='color:black;font-size:16px'>" + assetName + "</span>", ""), new LanguageWrapper("", userName, ""), new LanguageWrapper("", userFullName, ""), new LanguageWrapper("", "<br>" + sinceMessage, "")}) %></h4> 
	<%if(user.getUserId().equals(systemUser.getUserId()) || isUserCMSAdmin || user.getUserId().equals(lockedUserId)){ %>
	  <h3><%= LanguageUtil.get(pageContext, "Would-you-like-to-unlock-it-to-proceed") %></h3>
	<%} %>  
	<div style="padding:10px 20px;">
		<b><%= LanguageUtil.get(pageContext, "Title") %>:</b>
		<%=contentlet.getTitle()%><br/>

	</div>
	<div class="clear">&nbsp;</div>
	<div class="buttonRow">
	<%if(user.getUserId().equals(systemUser.getUserId()) || isUserCMSAdmin || user.getUserId().equals(lockedUserId)){ %>
        <button dojoType="dijit.form.Button" onClick="submitfm(document.getElementById('fm'))" iconClass="unlockIcon" >
           <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "unlock1")) %>
        </button>
    <%} %>    
        <button dojoType="dijit.form.Button" onClick="cancelUnlock()" iconClass="cancelIcon">
           <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
        </button>
	</div>
</div>

</liferay:box>

</form>









