
<%@page import="com.dotcms.rest.config.RestServiceUtil"%>
<%@page import="com.dotcms.rest.config.DotRestApplication"%>
<div class="portlet-wrapper">

<h2>This is the TESTING JSP</h2>

	<div style="padding:100px;">
		<a href="javascript:dotAjaxNav.show('/api/portlet/<%=request.getAttribute("PORTLET_ID") %>/');">show defualt (render) jsp</a> <br>&nbsp;<br>
		<a href="javascript:dotAjaxNav.show('/api/portlet/<%=request.getAttribute("PORTLET_ID") %>/testing');">show testing jsp</a><br>&nbsp;<br>
		<a href="javascript:dotAjaxNav.show('/api/restexample/test/');">get me some json</a>
	</div>

</div>