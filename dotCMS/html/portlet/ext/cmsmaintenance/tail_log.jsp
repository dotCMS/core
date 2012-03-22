
<%@page import="java.util.regex.Matcher"%>
<%@page import="java.util.regex.Pattern"%>
<%@page import="com.dotmarketing.exception.DotDataException"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@ include file="/html/common/init.jsp"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%

	String regex = Config.getStringProperty("TAIL_LOG_FILE_REGEX");
	if(!UtilMethods.isSet(regex)){
		regex=".*";
	}
	String[] files = FileUtil.listFiles(Config.CONTEXT.getRealPath(Config.getStringProperty("TAIL_LOG_LOG_FOLDER")));
	Pattern p = Pattern.compile(regex);  
	List<String> l = new ArrayList<String>();
	for(String x : files){
		if(p.matcher(x).matches()){  
			l.add(x);   
		}
	}
	// http://jira.dotmarketing.net/browse/DOTCMS-6271
	// put matched files set to an array with exact size and then sort them
	files = l.toArray(new String[l.size()]);
	Arrays.sort(files);
	


	
	
	try {
		user = com.liferay.portal.util.PortalUtil.getUser(request);
	} catch (Exception e) {
		response.sendError(403);
		return;
	}
	try {
		if (!APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", user)) {
			response.sendError(403);
			return;
		}
	} catch (Exception e2) {
		Logger.error(this.getClass(), e2.getMessage(), e2);
		response.sendError(403);
		return;
	}

	
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">


<%request.setAttribute("popup", "true"); %>
<%@ include file="/html/common/top_inc.jsp" %>

<style>
	#tailingFrame{
		border:1px solid silver;
		overflow: auto;
		height:100%;
		height:100%;
		width:100%;
	
	}
	#tailContainer {
		margin-top:10px;
		margin-bottom:30px;
		height:80%;
		width:94%;
		position: absolute;
		top: 40px;; 
	    left: 3%; 
	}
	#headerContainer{
		position: absolute;
		width:94%;
	   	left: 3%; 
		border:0px solid silver;
		padding-top:10px;
		padding-left:10px;
	}
	
	#popMeUp{
		float:right;
		display:none;
	}

</style>

<script>

	function reloadTail(){
		var x = dijit.byId("fileName").getValue();
		dojo.byId("tailingFrame").src='/dotTailLogServlet/?fileName='+x;

	}

	function doPopup(){
			var x = dijit.byId("fileName").getValue();
			dijit.byId("fileName").setValue("");
			var newwin = window.open("/html/portlet/ext/cmsmaintenance/tail_log.jsp?fileName=" + x, "tailwin", "status=1,toolbars=1,resizable=1,scrollbars=1,height=600,width=800");
			newwin.focus();
	}

	dojo.ready(function(){
		if(self != top){
			dojo.style(dojo.byId("popMeUp"), "display", "block");
		}

			
		<%if(request.getParameter("fileName")!= null){%>
			dijit.byId("fileName").setValue("<%=UtilMethods.xmlEscape(request.getParameter("fileName"))%>");
		<%}%>
		
	});
	
	
	
	
</script>




	<div id="headerContainer">
		<%=LanguageUtil.get(pageContext, "Tail")%>: 
		<select name="fileName" dojoType="dijit.form.FilteringSelect" ignoreCase="true" id="fileName" style="width:250px;" onchange="reloadTail();">
			<option value=""></option>
			<%for(String f: files){ %>

					<option value="<%= f%>"><%= f%></option>

			<%} %>
		</select>
		&nbsp; &nbsp; 
		<%=LanguageUtil.get(pageContext, "Follow") %> <input type='checkbox' id='scrollMe' dojoType="dijit.form.CheckBox" value=1 checked="true" />
		
		<div id="popMeUp">
			<input type="button" value="popup" name="popup" label="popup" onclick="doPopup()" dojoType="dijit.form.Button" />
		</div>
		
	</div>
	
	<div id="tailContainer">
		<iframe id="tailingFrame" src="/html/blank.jsp"></iframe>
	</div>
	

</body>



</html>
