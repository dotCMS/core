<%@ page import="com.dotcms.repackage.org.apache.struts.action.ActionErrors" %>
<%@ page import="java.util.HashSet"%>
<%@ page import="java.util.Set"%>
<%@ page import="com.dotcms.repackage.org.apache.struts.action.ActionErrors" %>
<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<%@ page import="java.util.List"%>
<%@ page import="java.util.ArrayList"%>
<%@ page import="com.dotcms.repackage.org.apache.struts.action.ActionMessage"%>
<%@ page import="java.util.Iterator"%>
<%@ page import="com.liferay.util.servlet.SessionMessages"%>
<%@ page import="com.dotcms.repackage.org.apache.struts.action.ActionMessages"%>
<%@page import="com.dotcms.repackage.org.apache.struts.Globals"%>
<%
if(request.getSession().getAttribute(ActionErrors.GLOBAL_ERROR) != null){
	request.setAttribute(ActionErrors.GLOBAL_ERROR, request.getSession().getAttribute(ActionErrors.GLOBAL_ERROR));

}


Set<String> messages = new HashSet<String>();
Set<String> errors = new HashSet<String>();
SessionDialogMessage dialogMessage = null;

if(request.getAttribute(ActionErrors.GLOBAL_ERROR) !=null){
	ActionErrors aes = (ActionErrors) request.getAttribute(ActionErrors.GLOBAL_ERROR);
	Iterator it = aes.get();
	while(it.hasNext()){
		ActionMessage am = (ActionMessage) it.next();
		String m = LanguageUtil.get(pageContext, am.getKey());
		if(am.getValues() != null){
			for(int i=0;i<am.getValues().length;i++){
				m = UtilMethods.replace(m, "{" + i + "}", (String) am.getValues()[i]);
			}
		}
		errors.add(m);
	}
}

if(request.getAttribute(Globals.ERROR_KEY) != null){
	ActionErrors aes = (ActionErrors) request.getAttribute(Globals.ERROR_KEY);
	Iterator it = aes.get();
	while(it.hasNext()){
		ActionMessage am = (ActionMessage) it.next();
		String m = LanguageUtil.get(pageContext, am.getKey());
		if(am.getValues() != null){
			for(int i=0;i<am.getValues().length;i++){
				m = UtilMethods.replace(m, "{" + i + "}", (String) am.getValues()[i]);
			}
		}
		errors.add(m);
	}
}




if(request.getAttribute(ActionMessages.GLOBAL_MESSAGE) !=null){
	ActionMessages aes = (ActionMessages) request.getAttribute(ActionMessages.GLOBAL_MESSAGE);
	Iterator it = aes.get();
	while(it.hasNext()){
		ActionMessage am = (ActionMessage) it.next();
		messages.add(am.getKey());
	}
}



if(SessionMessages.contains(session, "message")){
	messages.add((String) SessionMessages.get(session, "message"));
}

if(SessionMessages.contains(session, "error")){
	errors.add((String) SessionMessages.get(session, "error"));
}
if(SessionMessages.contains(session, "custommessage")){
	messages.add((String) SessionMessages.get(session, "custommessage"));
}

if (SessionMessages.contains(session, "dialogMessage")){
	dialogMessage = (SessionDialogMessage) SessionMessages.get(session, "dialogMessage");
}

//Support multiple messages
int i = 0;
do {
	if (SessionMessages.contains(request, "message" + i)) {
		messages.add((String) SessionMessages.get(request, "message" + i));
		i++;
	}
} while (SessionMessages.contains(request, "message" + i));

i = 0;
do {
	if(SessionMessages.contains(request, "error" + i)){
		errors.add((String) SessionMessages.get(request, "error" + i));
		i++;
	}
} while (SessionMessages.contains(request, "error" + i));

i = 0;
do {
	if(SessionMessages.contains(request, "custommessage" + i)){
		messages.add((String) SessionMessages.get(request, "custommessage" + i));
		i++;
	}
} while (SessionMessages.contains(request, "custommessage" + i));



SessionMessages.clear(session);
SessionMessages.clear(request);
request.getSession().removeAttribute("com.dotcms.repackage.org.apache.struts.action.MESSAGE");
request.getSession().removeAttribute("com.dotcms.repackage.org.apache.struts.action.ERROR");



%>

<script type='text/javascript' src='/html/js/messages.js'></script>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.util.servlet.SessionDialogMessage" %>
<%@ page import="java.util.Map.Entry" %>

<script>
	dojo.require("dojo.fx");
	dojo.require("dijit.layout.ContentPane");

		<%if(errors.size() > 0){%>
		   dojo.addOnLoad(
				   function () {
		   				showDotCMSErrorMessage("<ul><%for(String x : errors){%><li><%=UtilMethods.replace(LanguageUtil.get(pageContext, x), "\"", "\\\"") %></li><%} %></ul>")
				   }
		   		);
		<%}%>
		
	
		<%if(messages.size() > 0){%>
		   dojo.addOnLoad(
				   function () {
		   				showDotCMSSystemMessage("<div class=\"messageIcon resolveIcon\"></div>" + "<%for(String x : messages){%> <%=UtilMethods.replace(LanguageUtil.get(pageContext, x), "\"", "\\\"") %><%} %>")
				   }
		   		);
		<%}%>

    	<%if(dialogMessage != null){%>
			dojo.addOnLoad(function() {
                dijit.byId("messageDialog").set("title", "<%= dialogMessage.getTitle() %>");
                dojo.byId("messageDialogError").innerHTML = "<%= dialogMessage.getError() %>";

                <%
                String messageHTML = "";
                for (Entry<String, List<String>> entry : dialogMessage.getMessages().entrySet()) {
					messageHTML += "<table class='listingTable' style='margin-bottom: 0px'><thead><tr><th>"+ entry.getKey() +"</th></tr></thead><tbody>";
					for (String item : entry.getValue()) {
						messageHTML += "<tr><td>" + item + "</td></tr>";
					}
					messageHTML += "</tbody></table>";
                }%>
				dojo.byId("messageDialogContent").innerHTML = "<%= messageHTML %>";
                dojo.byId("messageDialogFooter").innerHTML = "<%= dialogMessage.getFooter() %>";
				dijit.byId("messageDialog").show();
    		}) ;
    	<%}%>
</script>

<div id="messageDialog" dojoType="dijit.Dialog" style="display:none;width:630px;height:auto;vertical-align: middle; "
	 draggable="true" title="Title" >

	<span id="messageDialogError" style="color: red; font-weight: bold">Error</span>
	<div id="messageDialogContent" style="overflow: auto;height:auto; margin-top: 10px; margin-bottom: 20px">HTML List of Elements</div>
	<span id="messageDialogFooter" style="margin-bottom: 10px; color: #888888;font-size: smaller"></span>
</div>
