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

<%@ include file="/html/common/uservalidation.jsp"%>

<script type="text/javascript">
function deleteJobWithError(name,group) {
	if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.Scheduler.confirm.delete")) %>')){
		form = document.getElementById('cmsMaintenanceForm');
        var action = "<portlet:actionURL>";
        action += "<portlet:param name='struts_action' value='/ext/cmsmaintenance/view_cms_maintenance' />";
        action += "</portlet:actionURL>";
        form.cmd.value="deleteSystemJob";
        form.systemJobName.value = name;
        form.systemJobGroup.value = group;
        form.action = action
        form.submit();
	}
}
</script>

<%String[] groups = QuartzUtils.getScheduler().getJobGroupNames(); %>
<table class="listingTable">
    <tr>
        <th><%= LanguageUtil.get(pageContext, "scheduler.job.class") %></th>
        <th><%= LanguageUtil.get(pageContext, "group") %></th>
        <th><%= LanguageUtil.get(pageContext, "scheduler.job.durable") %></th>
        <th><%= LanguageUtil.get(pageContext, "scheduler.job.stateful") %></th>
        <th><%= LanguageUtil.get(pageContext, "scheduler.job.sequential") %></th>
        <th><%= LanguageUtil.get(pageContext, "scheduler.job.runsAt") %></th>
        <th><%= LanguageUtil.get(pageContext, "scheduler.job.misfire") %></th>
    </tr>

<%for(String myGroup : groups) {%>
	<%String[] tasks =  QuartzUtils.getScheduler().getJobNames(myGroup);%>
    <%if(tasks.length==0){continue;}%>


	<%for(String t : tasks){%>
      	<%try{ %>
      		<% final JobDetail detail = QuartzUtils.getScheduler().getJobDetail(t, myGroup);  %>
      		<% Trigger[] triggers  =QuartzUtils.getScheduler().getTriggersOfJob(t, myGroup);%>
            <% Trigger trig  =triggers!=null && triggers.length>0? triggers[0] : null;%>
      		<%if(trig==null)continue; %>
      		<tr>
      			<td>
      				<%=detail.getJobClass().getSimpleName() %>
      			</td>
               <td>
                    <%=myGroup %>
               </td>
      			<td>
      				<%=detail.isDurable() %>
      			</td>
      			<td>
      				<%=detail.isStateful() %>
      			</td>
      			<td>
      				<%=detail.isVolatile()%>
      			</td>
      			<td>
                    <%if(QuartzUtils.isJobRunning(detail.getName(), detail.getGroup())){ %>
                        Running 
      				<%}else if(trig !=null && trig.getNextFireTime()!=null){ %>
      					<%=new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss  z").format( trig.getNextFireTime()) %>
      				<%}%>
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
            <tr>
                <td><%=t%></td>
                <td colspan="5" class="red"><%=LanguageUtil.get(pageContext, "an-unexpected-error-occurred")+"<br/>"+e.getMessage() %></td>
                <td colspan="1">
                    <button dojoType="dijit.form.Button" onclick="deleteJobWithError('<%=t%>','<%=myGroup%>')" iconClass="deleteIcon">
                        <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete")) %>
                    </button>
                 </td>
            </tr>
		<%} %>
	<%} %>

<%}%>
</table>