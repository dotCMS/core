<%@page import="com.dotcms.publisher.ajax.RemotePublishAjaxAction"%>
<%@page import="com.dotmarketing.portlets.workflows.actionlet.PushPublishActionlet"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowActionClass"%>
<%@ include file="/html/common/init.jsp" %>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowAction"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.business.Role"%>
<%@page import="java.util.Set"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%
String inode=request.getParameter("inode");// DOTCMS-7085
GregorianCalendar cal = new GregorianCalendar();

%>


<script type="text/javascript">


alert("test");
function togglePublishExpireDivs(){
	
	var x = "publish" ;
	if(dijit.byId("iwtExpire").isChecked()){
		x = "expire" ;
		
	}
	else 	if(dijit.byId("iwtPublishExpire").isChecked()){
		x = "publishexpire" ;
	}
	alert(x);
}

</script>

<!--  DOTCMS-7085 -->
<input name="assetIdentifier" id="assetIdentifier" type="hidden" value="<%=inode%>"> 

<div style="width:430px;" dojoType="dijit.form.Form" id="publishForm">
		
		
		
		
		
		<div class="fieldWrapper">
			<div class="fieldName" style="width:80px">
				<%= LanguageUtil.get(pageContext, "I-want-to") %>:
			</div>
			<div class="fieldValue">
				<input type="radio" dojoType="dijit.form.RadioButton" checked="true" onChange="pushHandler.togglePublishExpireDivs()" value="<%= RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH %>" name="wfIWantTo" id="iwtPublish" ><label for="iwtPublish"><%= LanguageUtil.get(pageContext, "publish") %></label>&nbsp;
				<input type="radio" dojoType="dijit.form.RadioButton" onChange="pushHandler.togglePublishExpireDivs()" value="<%= RemotePublishAjaxAction.DIALOG_ACTION_EXPIRE %>" name="wfIWantTo" id="iwtExpire" ><label for="iwtExpire"><%= LanguageUtil.get(pageContext, "delete") %></label>&nbsp;
				<input type="radio" dojoType="dijit.form.RadioButton" onChange="pushHandler.togglePublishExpireDivs()" value="<%= RemotePublishAjaxAction.DIALOG_ACTION_PUBLISH_AND_EXPIRE %>" name="wfIWantTo" id="iwtPublishExpire" ><label for="iwtPublishExpire"><%= LanguageUtil.get(pageContext, "publish") %> &amp; <%= LanguageUtil.get(pageContext, "delete") %></label>
			</div>
			<div class="clear"></div>
		</div>
		

		<%
			String hour = (cal.get(GregorianCalendar.HOUR_OF_DAY) < 10) ? "0"+cal.get(GregorianCalendar.HOUR_OF_DAY) : ""+cal.get(GregorianCalendar.HOUR_OF_DAY);
			String min = (cal.get(GregorianCalendar.MINUTE) < 10) ? "0"+cal.get(GregorianCalendar.MINUTE) : ""+cal.get(GregorianCalendar.MINUTE);
		%>
		<br>
		<div class="fieldWrapper" id="publishTimeDiv">
			
			<div class="fieldName" style="width:80px">
				<%= LanguageUtil.get(pageContext, "Publish") %>:
			</div>
			<div class="fieldValue">
				<input 
					type="text" 
					dojoType="dijit.form.DateTextBox" 
					validate="return false;" 
					invalidMessage=""  
					id="wfPublishDateAux"
					name="wfPublishDateAux" value="now" style="width: 110px;">
								
									
				<input type="text" name="wfPublishTimeAux" id="wfPublishTimeAux" value="now"
				 	data-dojo-type="dijit.form.TimeTextBox"
					required="true" style="width: 100px;"/>
			</div>
			<div class="clear"></div>
		</div>
		
		<div class="fieldWrapper" id="expireTimeDiv" style="display:none">
			<div class="fieldName" style="width:80px"><%= LanguageUtil.get(pageContext, "publisher_Expire") %> :
			</div>
			<div class="fieldValue">
			<input 
				type="text" 
				dojoType="dijit.form.DateTextBox" 
				validate="return false;"   
				id="wfExpireDateAux" name="wfExpireDateAux" value="now" style="width: 110px;">
							
							
			<input type="text" name="wfExpireTimeAux" id="wfExpireTimeAux" value="now"
			    data-dojo-type="dijit.form.TimeTextBox"	
				style="width: 100px;" />
			</div>
			<div class="clear"></div>
		</div>
		
		<div class="buttonRow">
			<button dojoType="dijit.form.Button" iconClass="saveAssignIcon" onClick="pushHandler.remotePublish()" type="button">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save")) %>
			</button>
			<button dojoType="dijit.form.Button" iconClass="cancelIcon" onClick="dijit.byId('remotePublisherDia').hide()" type="button">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
			</button>
		
		</div>
</div>
