<%@page import="com.dotcms.repackage.javax.portlet.WindowState"%>
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
<%@ page import="org.quartz.JobKey" %>
<%@ page import="org.quartz.impl.matchers.GroupMatcher" %>
<%
	java.util.Hashtable params2 = new java.util.Hashtable ();
	params2.put("struts_action", new String [] {"/ext/scheduler/view_schedulers"} );
	params2.put("pageNumber",new String[] { "1" });

	String referrer2 = com.dotmarketing.util.PortletURLUtil.getRenderURL(request, WindowState.MAXIMIZED.toString(), params2);
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

<%
	List<String> jobGroupNames = QuartzUtils.getScheduler().getJobGroupNames();

	for(String myGroup : jobGroupNames) {
		Set<JobKey> jobKeys = QuartzUtils.getScheduler().getJobKeys(GroupMatcher.jobGroupEquals(myGroup));
%>

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

	<%
		for(JobKey jobKey : jobKeys) {
			try {
				JobDetail jobDetail = QuartzUtils.getScheduler().getJobDetail(jobKey);
				List<? extends Trigger> triggers = QuartzUtils.getScheduler().getTriggersOfJob(jobKey);

				for (Trigger trigger : triggers) {
	%>

	<tr>
		<td><%= jobDetail.getJobClass().getName() %></td>
		<td><%= jobDetail.isDurable() %></td>
		<td><%= jobDetail.isConcurrentExectionDisallowed() %></td>
		<td><%= jobDetail.requestsRecovery() %></td>
		<td>
			<%= (trigger != null && trigger.getNextFireTime() != null)
					? new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z").format(trigger.getNextFireTime())
					: "" %>
		</td>
		<td>
			<% if(trigger != null) {
				if(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING == trigger.getMisfireInstruction()) { %>
			<%= LanguageUtil.get(pageContext, "scheduler.job.misfire.donothing") %>
			<% } else if(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW == trigger.getMisfireInstruction()) { %>
			<%= LanguageUtil.get(pageContext, "scheduler.job.misfire.fireOnce") %>
			<% }
			} %>
		</td>
	</tr>

	<%
		}
	} catch (Exception e) {
	%>
	<tr>
		<td><%= jobKey.getName() %></td>
		<td colspan="4" class="red"><%= LanguageUtil.get(pageContext, "an-unexpected-error-occurred") + "<br/>" + e.getMessage() %></td>
		<td colspan="2">
			<button dojoType="dijit.form.Button" onclick="deleteJobWithError('<%= jobKey.getName() %>','<%= myGroup %>')" iconClass="deleteIcon">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete")) %>
			</button>
		</td>
	</tr>
	<%
			}
		}
	%>

</table>
<html:form action="/ext/scheduler/edit_scheduler" styleId="deleteJobForm" style="display:hidden">
	<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="">
	<input name="<portlet:namespace />redirect" type="hidden" value="">
	<input name="referrer" type="hidden" value="">
	<input type="hidden" name="group" type="hidden" id="group" value="">
	<input type="hidden" name="name" type="hidden" id="name" value="">
</html:form>
<%
	}
%>