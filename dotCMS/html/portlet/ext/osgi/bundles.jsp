<%@page import="java.io.File"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="com.dotmarketing.listeners.OsgiFelixListener"%>
<%@page import="org.osgi.framework.Bundle"%>
<%
Bundle[] ba = OsgiFelixListener.m_fwk.getBundleContext().getBundles();
Map<Integer,String> states = new HashMap<Integer,String>();
states.put(Bundle.ACTIVE, LanguageUtil.get(pageContext, "OSGI-Bundles-State-Active"));
states.put(Bundle.INSTALLED, LanguageUtil.get(pageContext, "OSGI-Bundles-State-Installed"));
states.put(Bundle.RESOLVED, LanguageUtil.get(pageContext, "OSGI-Bundles-State-Resolved"));
states.put(Bundle.STARTING, LanguageUtil.get(pageContext, "OSGI-Bundles-State-Starting"));
states.put(Bundle.STOPPING, LanguageUtil.get(pageContext, "OSGI-Bundles-State-Stopping"));
states.put(Bundle.UNINSTALLED, LanguageUtil.get(pageContext, "OSGI-Bundles-State-Uninstalled"));
states.put(Bundle.START_TRANSIENT, LanguageUtil.get(pageContext, "OSGI-Bundles-State-StartTransient"));
states.put(Bundle.STOP_TRANSIENT, LanguageUtil.get(pageContext, "OSGI-Bundles-State-StopTransient"));
%>

<table class="listingTable" style="margin:0 0 25px 0;" id="bundlesTable">
	<tr>
		<th>Name</th>
		<th>State</th>
		<th>Jar</th>
		<th></th>		
	</tr>
	<% for(Bundle b : ba){ %>
	<tr>
		<td><%=b.getSymbolicName()%></td>
		<td><%=states.get(b.getState())%></td>
		<td><%=b.getLocation().contains(File.separator)?b.getLocation().substring(b.getLocation().lastIndexOf(File.separator) + 1):"System"%></td>
		<td><%if(b.getLocation().contains(File.separator) && b.getLocation().contains(File.separator + "load" + File.separator)){ %><a href="/DotAjaxDirector/com.dotmarketing.portlets.osgi.AJAX.OSGIAJAX?cmd=undeploy&jar=<%=b.getLocation().substring(b.getLocation().lastIndexOf(File.separatorChar) + 1)%>">Undeploy</a><%} %></td>
	</tr>
	<%}%>
</table>