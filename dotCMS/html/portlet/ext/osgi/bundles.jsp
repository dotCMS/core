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

<div dojoType="dojo.data.ItemFileReadStore" jsId="test" url="/html/portlet/ext/osgi/available_bundles_json.jsp"></div>
<div>
<%= LanguageUtil.get(pageContext,"OSGI-AVAIL-BUNDLES") %> : <input dojoType="dijit.form.ComboBox" store="test" searchAttr="label" name="availBundlesCombo" id="availBundlesCombo"> 
<button dojoType="dijit.form.Button" iconClass="addIcon" onclick="javascript:bundles.deploy()"><%=LanguageUtil.get(pageContext, "OSGI-Load-Bundle")%></button>
</div>
<form id="addBundle" name="addBundle" encType="multipart/form-data">
	<input type="hidden" name="cmd" value="add">
	<div>
		<!-- <input name="bundleUpload" multiple="false" type="file" data-dojo-type="dojox.form.Uploader" label="Select Bundle" id="bundleUpload" showProgress="true"/>&nbsp;&nbsp;&nbsp; -->
		<input type="file" name="bundleUpload" size="40">
		<span id="uploadFileName"></span>
		<button dojoType="dijit.form.Button" onClick='bundles.add()' iconClass="uploadIcon" type="button"><%=LanguageUtil.get(pageContext, "OSGI-Load-Bundle")%></button>
	</div>
</form>
<div class="clear"></div>
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
		<td><%if(b.getLocation().contains(File.separator) && b.getLocation().contains(File.separator + "load" + File.separator)){ %><a href="javascript:bundles.undeploy('<%=b.getLocation().substring(b.getLocation().lastIndexOf(File.separatorChar) + 1)%>')">Undeploy</a><%} %></td>
	</tr>
	<%}%>
</table>
<div align="center"><a href="javascript:mainAdmin.refresh();">Refresh</a></div>