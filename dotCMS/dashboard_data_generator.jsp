<%@page import="com.dotcms.enterprise.DashboardProxy" %>
<%@page import="com.dotmarketing.portlets.dashboard.business.DashboardDataGenerator" %>
<%@page import="java.util.Calendar" %>
<%@page import="java.util.Date" %>
<%
	Calendar cal = Calendar.getInstance();
cal.setTime(new Date());
cal.add(Calendar.MONTH, -1);
Calendar cal2 = Calendar.getInstance();
cal2.setTime(new Date());
cal2.add(Calendar.MONTH, 1);

Integer monthFrom = 0;
Integer monthTo = 0; 
Integer yearFrom = 0;
Integer yearTo = 0;

try{
	monthFrom =request.getParameter("monthFrom")==null?cal.get(Calendar.MONTH)+1:Integer.valueOf(request.getParameter("monthFrom"));
	monthTo = request.getParameter("monthTo")==null?cal2.get(Calendar.MONTH)+1:Integer.valueOf(request.getParameter("monthTo"));
	yearFrom = request.getParameter("yearFrom")==null?cal.get(Calendar.YEAR):Integer.valueOf(request.getParameter("yearFrom"));
	yearTo =request.getParameter("yearTo")==null?cal2.get(Calendar.YEAR):Integer.valueOf(request.getParameter("yearTo"));
	
}catch(Exception e){}


boolean stop = request.getParameter("stop")==null?false:true;
DashboardDataGenerator generator = null;
String message = "";
if((yearTo>yearFrom) || (yearTo==yearFrom && monthTo>monthFrom)){
if(session.getAttribute("dashboardDataGenerator")==null){

	try{
	   generator = DashboardProxy.getDashboardDataGenerator(monthFrom, yearFrom, monthTo, yearTo);
	   generator.start();
	   session.setAttribute("dashboardDataGenerator", generator);
	}catch(Exception e){
		session.removeAttribute("dashboardDataGenerator");
		message = e.getMessage();
	}

}else{
	generator = (DashboardDataGenerator)session.getAttribute("dashboardDataGenerator");
	if(stop){
		generator.setFlag(false);
	}
}
}else{
	
	message = "Please provide a valid date interval";
}
%>
<html>
<body>

<form>
<div>
<% if(message!=null && !message.equals("")) {%>
  
  <b><%= message %></b>

<%}else{ %>

  <% if(!stop && !generator.isFinished()){ %>
     Generating sample data for dashboard from: <b> <%= generator.getMonthFrom()%> - <%= generator.getYearFrom()%> </b>  to  <b><%= generator.getMonthTo()%> - <%= generator.getYearTo()%></b>
     <%= generator.getProgress()%> %
  <%}else if(stop){ %>
     Process Stopped!
      <%session.removeAttribute("dashboardDataGenerator"); %>
  <%}else { %>
     Process Finished!
     <% if(!generator.getErrors().isEmpty()){ %>
     <b> Errors were found : </b>
     <br />
       <% for(String error : generator.getErrors()){ %>
           <b> <%= error %></b> <br />
       <%}%>
     <%} %>
     <b><%= generator.getRowCount() %></b> records generated
     <%session.removeAttribute("dashboardDataGenerator"); %>
     
  <%} %>
<%} %> 
</div>

</form>

</body>

</html>