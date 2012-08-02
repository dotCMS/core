<%@ include file="/html/portlet/ext/virtuallinks/init.jsp" %>

<%@ page import="com.dotmarketing.portlets.virtuallinks.struts.VirtualLinkForm" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="javax.servlet.jsp.PageContext" %>
<%@ page import="com.dotmarketing.util.*" %>
<%@page import="com.dotmarketing.business.APILocator"%>

<%
    List<Host> allHosts = APILocator.getHostAPI().findAll(APILocator.getUserAPI().getSystemUser(),true);
	com.dotmarketing.portlets.virtuallinks.model.VirtualLink vl;
	if (request.getAttribute(com.dotmarketing.util.WebKeys.VIRTUAL_LINK_EDIT)!=null) {
		vl = (com.dotmarketing.portlets.virtuallinks.model.VirtualLink) request.getAttribute(com.dotmarketing.util.WebKeys.VIRTUAL_LINK_EDIT);
	}
	else {
		vl =	com.dotmarketing.portlets.virtuallinks.factories.VirtualLinkFactory.newInstance(); 
	}
	VirtualLinkForm vfm = new VirtualLinkForm();
	
	if (request.getAttribute("VirtualLinkForm")!=null) {
		vfm = (VirtualLinkForm) request.getAttribute("VirtualLinkForm");
	}
	String uri = "";
	if (InodeUtils.isSet(request.getParameter("htmlinode"))) {
		com.dotmarketing.portlets.htmlpages.model.HTMLPage htmlPage = (com.dotmarketing.portlets.htmlpages.model.HTMLPage) com.dotmarketing.factories.InodeFactory.getInode(request.getParameter("htmlinode"), com.dotmarketing.portlets.htmlpages.model.HTMLPage.class);
		if (InodeUtils.isSet(htmlPage.getInode())) {
	com.dotmarketing.beans.Identifier i = com.dotmarketing.business.APILocator.getIdentifierAPI().find(htmlPage);
	uri = i.getURI();
	Host host = APILocator.getHostAPI().findParentHost(htmlPage, APILocator.getUserAPI().getSystemUser(), false);
	
	
	if (request.getAttribute("VirtualLinkForm")!=null) {
		vfm = (VirtualLinkForm) request.getAttribute("VirtualLinkForm");
		
		if (!uri.equals("")) {
			if (!InodeUtils.isSet(vfm.getHostId())) {
				if (uri.startsWith("http://")) {
					uri = uri.replace("http://", "");
				} else {
					if (uri.startsWith("https://")) {
						uri = uri.replace("https://", "");
					}
				}
				
				int indexOf = uri.indexOf('/');
				if (-1 < indexOf) {
					uri = uri.substring(indexOf);
				}
				
				vfm.setUri(uri);
			} else {
				if (vfm.getHostId().equalsIgnoreCase(host.getIdentifier()))
					vfm.setUri("http://" + host.getHostname() + uri);
				else
					vfm.setUri(uri);
			}
		}
	}
		}
	}
    List hosts=(List) request.getAttribute("host_list");
    hosts.remove(APILocator.getHostAPI().findSystemHost(user, false));
    
	String hostId = "";
	if(vfm.getHostId()!=null) 
		hostId = vfm.getHostId();
	if(!UtilMethods.isSet(hostId) && !UtilMethods.isSet(vfm.getInode())) {
		if(request.getParameter("host_id") != null) {
	hostId = request.getParameter("host_id");
		} else {
	hostId = (String)session.getAttribute(com.dotmarketing.util.WebKeys.SEARCH_HOST_ID);
		}	
	}
	
	if(!InodeUtils.isSet(vfm.getInode())){
		if (!UtilMethods.isSet(hostId) || hostId.equals("0")) {
			hostId = (String)session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
		}
	}
	
	Host host = null;
	if(UtilMethods.isSet(hostId)) {
		host = APILocator.getHostAPI().find(hostId, APILocator.getUserAPI().getSystemUser(), false);
	}
%>


<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit-Virtual-Link")) %>' />

<script language="Javascript">

dojo.require('dotcms.dijit.FileBrowserDialog');

function submitfm(form) {
	form.<portlet:namespace />cmd.value = '<%=Constants.ADD%>';
	form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/virtuallinks/edit_virtuallink" /></portlet:actionURL>';
	form.<portlet:namespace />redirect.value = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/virtuallinks/view_virtuallinks" /></portlet:renderURL>';
	submitForm(form);
}

function cancelEdit() {
	self.location = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/virtuallinks/view_virtuallinks" /></portlet:renderURL>';
}

function deleteVirtualLink(form) {
	if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.virtuallink.confirm.delete")) %>')){
		form.<portlet:namespace />cmd.value = '<%=Constants.DELETE%>';
		form.<portlet:namespace />redirect.value = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/virtuallinks/view_virtuallinks" /></portlet:renderURL>';
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/virtuallinks/edit_virtuallink" /></portlet:actionURL>';
		submitForm(form);
	}
}
function browsePage() {
	pageSelector.show();
}

function pageSelected(page) {
	var path = page.pageURI;
	if(!path){
		path = page.path + page.fileName;
	}
	dojo.byId('vlUri').value = path;
}

</script>


<html:form action="/ext/virtuallinks/edit_virtuallink" styleId="fm">
	<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="">
	<input name="<portlet:namespace />redirect" type="hidden" value="">
	<input name="su" type="hidden" value="add">
	<input type="hidden" name="inode" value="<%=vl.getInode()%>">
	
	<dl class="shadowBoxLine">
		<dt>
			<span class="required"></span>
			<%= LanguageUtil.get(pageContext, "Title") %>:
		</dt>
		<dd>
			<input id="vlTitle" type="text" name="title"  value="<%=UtilMethods.webifyString(vfm.getTitle()) %>"
			dojoType="dijit.form.TextBox"
			trim="true"
			required="true"
			invalidMessage="<%=LanguageUtil.get(pageContext, "required") %>" />
		</dd>
		
		<dt><%= LanguageUtil.get(pageContext, "Applies-to") %>:</dt>
		<dd>	
			<% if(UtilMethods.isSet(vfm.getInode())) { %>
				<%= host != null?host.getHostname():"All Hosts" %>
				<input type="hidden" name="hostId" value="<%= hostId %>" />
			<% } else { %>
				<select  dojoType="dijit.form.FilteringSelect" id="hostId" name="hostId" autoComplete="true" value="<%=vfm.getHostId()%>">
				<% if((Boolean)request.getAttribute("isCMSAdministrator")) { %>
					<option value="0"><%= LanguageUtil.get(pageContext, "All-Hosts") %></OPTION>
				<% } %>
				<% for(Host h: allHosts){
				     if(!h.getIdentifier().equals(Host.SYSTEM_HOST) && h.isLive())
				%>     <option value="<%=h.getIdentifier() %>" ><%=h.getHostname() %></option>
				<%
				   }
				%>
								 
				</select>
			<% } %>
		</dd>
		
		<dt>
			<span class="required"></span>
			<%= LanguageUtil.get(pageContext, "URL") %>:
		</dt>
		<dd>
			<%String showUrl = UtilMethods.webifyString((vl.getUrl() != null && vl.getUrl().split(":").length>1) ? vl.getUrl().split(":")[1] : vl.getUrl());%>
			<input id="url" type="text" name="url"  value="<%=showUrl %>"
		    dojoType="dijit.form.TextBox"
		    trim="true"
		    required="true"
		    invalidMessage="<%=LanguageUtil.get(pageContext, "required") %>" />
		</dd>
		
		<dt>
			<span class="required"></span>
			<%= LanguageUtil.get(pageContext, "Enter-URL-to-Redirect-To") %>:
		</dt>
		<dd>
			<input id="vlUri" type="text" name="uri"  value="<%=UtilMethods.webifyString(vl.getUri()) %>"
			dojoType="dijit.form.TextBox"
			trim="true"
			required="true" style="width: 250px;"
			invalidMessage="<%=LanguageUtil.get(pageContext, "required") %>" />
			<button dojoType="dijit.form.Button" onClick="browsePage();return false;" iconClass="browseIcon">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "click-here-to-browse")) %>
			</button>
		</dd>
	</dl>
<div class="clear"></div>
<!-- START Button Roww -->
	<div class="buttonRow">
		<% if(InodeUtils.isSet(vl.getInode()) && APILocator.getVirtualLinkAPI().checkVirtualLinkForEditPermissions(vl, user)!=null){ %>
			<button iconClass="deleteIcon" dojoType="dijit.form.Button" onClick="deleteVirtualLink(document.getElementById('fm'))">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "delete")) %>
			</button>
		<%} %>
		<% if(!InodeUtils.isSet(vl.getInode()) || (InodeUtils.isSet(vl.getInode()) && 
				APILocator.getVirtualLinkAPI().checkVirtualLinkForEditPermissions(vl, user)!=null)){ %>
		<button iconClass="saveIcon" dojoType="dijit.form.Button" onClick="submitfm(document.getElementById('fm'))">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save")) %>
		</button>
		<% } %>
		<button  iconClass="cancelIcon" dojoType="dijit.form.Button" onClick="cancelEdit();return false;">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
		</button>
	</div>
<!-- ENDButton Roww -->

</html:form>
</liferay:box>

<div dojoAttachPoint="fileBrowser" jsId="pageSelector" onFileSelected="pageSelected" dojoType="dotcms.dijit.FileBrowserDialog">
</div>
