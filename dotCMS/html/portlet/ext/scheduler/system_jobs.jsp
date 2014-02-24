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

<%@ include file="/html/common/uservalidation.jsp"%>

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
	<%}%>
	</table>
<%}%>
