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

<!--  DOTCMS-7085 -->
<input name="assetIdentifier" id="assetIdentifier" type="hidden" value="<%=inode%>"> 

<div style="width:430px;">
		

		<%
			String hour = (cal.get(GregorianCalendar.HOUR_OF_DAY) < 10) ? "0"+cal.get(GregorianCalendar.HOUR_OF_DAY) : ""+cal.get(GregorianCalendar.HOUR_OF_DAY);
			String min = (cal.get(GregorianCalendar.MINUTE) < 10) ? "0"+cal.get(GregorianCalendar.MINUTE) : ""+cal.get(GregorianCalendar.MINUTE);
		%>
		
		<div class="fieldWrapper">
			<br>
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
		
		<div class="fieldWrapper">
			<div class="fieldName" style="width:80px"><%= LanguageUtil.get(pageContext, "publisher_Expire") %> :
			</div>
			<div class="fieldValue">
			<input 
				type="text" 
				dojoType="dijit.form.DateTextBox" 
				validate="return false;"   
				id="wfExpireDateAux" name="wfExpireDateAux" value="" style="width: 110px;">
							
							
			<input type="text" name="wfExpireTimeAux" id="wfExpireTimeAux" value=""
			    data-dojo-type="dijit.form.TimeTextBox"	
				style="width: 100px;" />
				
			&nbsp;&nbsp;<input type="checkbox" dojoType="dijit.form.CheckBox" checked="checked" name="wfNeverExpire" id="wfNeverExpire" > <%= LanguageUtil.get(pageContext, "publisher_Never_Expire") %>
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
