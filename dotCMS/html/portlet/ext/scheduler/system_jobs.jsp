<%@page import="javax.portlet.WindowState"%>
<%@page import="com.liferay.portal.util.Constants"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="org.quartz.CronTrigger"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.text.DateFormat"%>
<%@page import="org.quartz.Trigger"%>
<%@page import="org.quartz.JobDetail"%>
<%@page import="java.util.Set"%>
<%@page import="com.dotmarketing.quartz.ScheduledTask"%>
<%@page import="com.dotmarketing.quartz.QuartzUtils"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Arrays"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="com.dotmarketing.business.web.WebAPILocator"%>
<%
	java.util.Hashtable params2 = new java.util.Hashtable ();
	params2.put("struts_action", new String [] {"/ext/scheduler/view_schedulers"} );
	params2.put("pageNumber",new String[] { "1" });
	
	String referrer2 = com.dotmarketing.util.PortletURLUtil.getRenderURL(request, javax.portlet.WindowState.MAXIMIZED.toString(), params2);
%>
<%@ include file="/html/common/uservalidation.jsp"%>
<script type="text/javascript">
function deleteJobWithError(name,group) {
	if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.Scheduler.confirm.delete")) %>')){
		var form = document.getElementById('deleteJobForm');
		form.<portlet:namespace />cmd.value = '<%=Constants.DELETE%>';
		form.<portlet:namespace />redirect.value = '<%= referrer2 %>';
		form.referrer.value = '<%= referrer2 %>';
		form.name.value = name;
		form.group.value = group;
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/scheduler/edit_scheduler" /></portlet:actionURL>';
		submitForm(form);
	}
}
</script>
<%String[] groups = QuartzUtils.getSequentialScheduler().getJobGroupNames(); %>

<%for(String myGroup : groups) {%>
	<%String[] tasks =  QuartzUtils.getSequentialScheduler().getJobNames(myGroup);%>
	<div style="padding:10px;">
		<h3><%= LanguageUtil.get(pageContext, "Group") %>: <%=myGroup %></h3>
	</div>
	<table class="listingTable">
	<tr>
	<th><%= LanguageUtil.get(pageContext, "scheduler.job.class") %></th>
	<th><%= LanguageUtil.get(pageContext, "scheduler.job.durable") %></th>
	<th><%= LanguageUtil.get(pageContext, "scheduler.job.stateful") %></th>
	<th><%= LanguageUtil.get(pageContext, "scheduler.job.sequential") %></th>
	<th><%= LanguageUtil.get(pageContext, "scheduler.job.runsAt") %></th>
	<th><%= LanguageUtil.get(pageContext, "scheduler.job.misfire") %></th>
	</tr>



   
	<%for(String t : tasks){%>
	<%try{ %>
		<%JobDetail d = QuartzUtils.getSequentialScheduler().getJobDetail(t, myGroup);  %>
		<% Trigger trig  =null;%>
		<%for(Trigger x :  QuartzUtils.getSequentialScheduler().getTriggersOfJob(t, myGroup) ) {trig=x;break;}%>





		<tr>
			<td>

				<%=d.getJobClass() %>
			</td>
			<td>
				<%=d.isDurable() %>
			</td>
			<td>
				<%=d.isStateful() %>
			</td>
			<td>
				<%=d.isVolatile()%>
			</td>

			<td align="right">
				<%if(trig !=null && trig.getNextFireTime()!=null){ %>
					<%=new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss  z").format( trig.getNextFireTime()) %>
				<%} %>
			</td>
			<td>
				<%if(trig !=null){ %>
					<%if(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING == trig.getMisfireInstruction()){ %>
						<%= LanguageUtil.get(pageContext, "scheduler.job.misfire.donothing") %>
					<%}else if(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW == trig.getMisfireInstruction()){ %>
						<%= LanguageUtil.get(pageContext, "scheduler.job.misfire.fireOnce") %>
					<%}%>
				<%} %>
			</td>




		</tr>
		<%}catch(Exception e){%>
		<tr><td><%=t%></td><td colspan="4" class="red"><%=LanguageUtil.get(pageContext, "an-unexpected-error-occurred")+"<br/>"+e.getMessage() %></td>
		<td colspan="2"><button dojoType="dijit.form.Button" onclick="deleteJobWithError('<%=t%>','<%=myGroup%>')" iconClass="deleteIcon">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete")) %>
						</button>
		</td></tr>
		<%} %>
	<%} %>
	</table>
	<html:form action="/ext/scheduler/edit_scheduler" styleId="deleteJobForm" style="display:hidden">
	  		<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="">
			<input name="<portlet:namespace />redirect" type="hidden" value="">
			<input name="referrer" type="hidden" value="">
			<input type="hidden" name="group" type="hidden" id="group" value="">
			<input type="hidden" name="name" type="hidden" id="name" value="">
	</html:form>
<%}%>