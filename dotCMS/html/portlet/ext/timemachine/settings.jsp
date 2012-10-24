<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.quartz.ScheduledTask"%>
<%@include file="/html/common/init.jsp"%>
<%@include file="/html/common/top_inc.jsp"%>

<%
String cronExp="";
List<String> hosts=new ArrayList<String>();
Boolean allhosts=false;
List<String> langs=new ArrayList<String>(); 

ScheduledTask task=APILocator.getTimeMachineAPI().getQuartzJob();
if(task!=null) {
    allhosts=(Boolean) task.getProperties().get("allhosts");
    hosts=(List<String>) task.getProperties().get("hosts");
    langs=(List<String>) task.getProperties().get("langs");
}
%>

<form dojoType="dijit.form.Form" >
        
</form>
