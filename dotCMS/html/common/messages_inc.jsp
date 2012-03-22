<%@ page import="org.apache.struts.action.ActionErrors" %>
<%@ page import="java.util.HashSet"%>
<%@ page import="java.util.Set"%>
<%@ page import="org.apache.struts.action.ActionErrors" %>
<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<%@ page import="java.util.List"%>
<%@ page import="java.util.ArrayList"%>
<%@ page import="org.apache.struts.action.ActionMessage"%>
<%@ page import="java.util.Iterator"%>
<%@ page import="com.liferay.util.servlet.SessionMessages"%>
<%@ page import="org.apache.struts.action.ActionMessages"%>
<%@page import="org.apache.struts.Globals"%>
<%
if(request.getSession().getAttribute(ActionErrors.GLOBAL_ERROR) != null){
	request.setAttribute(ActionErrors.GLOBAL_ERROR, request.getSession().getAttribute(ActionErrors.GLOBAL_ERROR));

}


Set<String> messages = new HashSet<String>();
Set<String> errors = new HashSet<String>();

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

if(SessionMessages.contains(request, "message")){
	messages.add((String) SessionMessages.get(request, "message"));
}
if(SessionMessages.contains(request, "error")){
	errors.add((String) SessionMessages.get(request, "error"));
}
if(SessionMessages.contains(request, "custommessage")){
	messages.add((String) SessionMessages.get(request, "custommessage"));
}



SessionMessages.clear(session);
SessionMessages.clear(request);
request.getSession().removeAttribute("org.apache.struts.action.MESSAGE");
request.getSession().removeAttribute("org.apache.struts.action.ERROR");



%>




	
<%@page import="com.dotmarketing.util.UtilMethods"%>

<script>
	dojo.require("dojo.fx");
	dojo.require("dijit.layout.ContentPane");


	var messagesCount = 0;
	var messageYIncrement = 60;
	var occupiedPositions = new Array();

	
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


	
	
		function showDotCMSSystemMessage(message){
			showDotCMSSystemMessage(message, false);
		}
	
		function showDotCMSSystemMessage(message, isError){

			var position = 40;

			if(occupiedPositions.length > 0)
				position = occupiedPositions[occupiedPositions.length - 1] + messageYIncrement;	
				occupiedPositions.push(position);
				

			var className = isError? 'systemErrorsHolder':'systemMessagesHolder';
			var holdingDiv = dojo.create("div", { 	
				id : "systemMessagesWrapper" + messagesCount, 
				className : className,
				style: { top: position + '%' }
			}, dojo.body());

			var className = isError? 'errorMessages':'systemMessages';
			var systemMessages = dojo.create("div", { 	
				id: "systemMessages" + messagesCount,
				className: className
			}, holdingDiv);

			systemMessages.innerHTML = message;
		
			dojo.connect(dijit.byId("systemMessages"), "onClick", hideDotCMSSystemMessage);
				
			var hideFn = dojo.partial(hideDotCMSSystemMessage, messagesCount);
			dojo.connect(holdingDiv, 'onclick', hideFn);	
	
			var hideFn = dojo.partial(hideDotCMSSystemMessage, messagesCount);
			var fadeOutFn = dojo.fadeOut({node: "systemMessages" + messagesCount, delay: 10, duration: 0, onEnd: hideFn }).play;
			
			var fadeIn = dojo.fadeIn({node: "systemMessages" + messagesCount, duration: 2000, onEnd: fadeOutFn });
			fadeIn.play();
	
			var ttl = message.split(" ").length;
			ttl = ttl * 200;
			if(ttl < 1000){
				ttl = 1000;
			}
			
			
		
			
			hideMessagesHandler = setTimeout(hideFn,ttl);
	
			messagesCount++;
		
		}
	
		function hideDotCMSSystemMessage(messageId){
	
			var currentY = parseInt(dojo.byId("systemMessagesWrapper" + messageId).style.top);
			occupiedPositions = dojo.filter(occupiedPositions, function (x) {
				return x != currentY;
			});

	
			dojo.fadeOut({node: "systemMessagesWrapper" + messageId}).play();
			dojo.destroy("systemMessagesWrapper" + messageId);
	
	
		}
	
		var hideErrorsHandler;
	
		function showDotCMSErrorMessage(message){
			showDotCMSSystemMessage(message, true);
		}

</script>
