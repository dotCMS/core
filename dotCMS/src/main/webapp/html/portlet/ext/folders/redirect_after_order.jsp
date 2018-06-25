<%
String orderReturnPath = session.getAttribute("orderReturnPath") != null ? (String) session.getAttribute("orderReturnPath") : "/";
session.removeAttribute("orderReturnPath");
%>
<script>



parent.location.href= '/dotAdmin/#/edit-page/content?url=<%=orderReturnPath%>&r='+new Date().getTime();



</script>