<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.plugin.business.PluginAPI"%>
<%
	PluginAPI pluginAPI = APILocator.getPluginAPI();
    long serversNumber=0;
    String nastyError = null;
    try{
    	serversNumber=Long.parseLong(pluginAPI.loadProperty("com.dotcms.solr","com.dotcms.solr.SOLR_SERVER_NUMBER"));
    }catch(Exception e){
    	nastyError = e.getMessage();
    }
    if(serversNumber > 0){
%>
<table class="listingTable shadowBox">
		<tr>
			<th><%= LanguageUtil.get(pageContext, "SOLR_instance") %></th>		
			<th><%= LanguageUtil.get(pageContext, "SOLR_instance_link") %></th>			
		</tr>
<% try{
	for(int server=0; server < serversNumber; server++ ){
		String solrServerUrl = pluginAPI.loadProperty("com.dotcms.solr", "com.dotcms.solr."+server+".SOLR_SERVER");
%>		
		<tr>
			<td class="solr_tcenter"><%= solrServerUrl %></td>
			<td class="solr_tcenter"><a target="new" href="<%= solrServerUrl+"/admin" %>"><%= solrServerUrl+"/admin" %></a></td>
		</tr>
	</table>
<%   }
	}catch(Exception e){
		nastyError = e.getMessage();
	}
   }else{%>	
<table class="listingTable shadowBox">
		<tr>
			<th><%= LanguageUtil.get(pageContext, "SOLR_instance") %></th>		
			<th><%= LanguageUtil.get(pageContext, "SOLR_instance_link") %></th>				
		</tr>
		<tr>
			<td colspan="2" class="solr_tcenter"><%= LanguageUtil.get(pageContext, "SOLR_No_Results") %></td>
		</tr>
	</table>
<%}%>
<%if(UtilMethods.isSet(nastyError)){%>
		<dl>
			<dt style='color:red;'><%= LanguageUtil.get(pageContext, "SOLR_Query_Error") %> </dt>
			<dd><%=nastyError %></dd>
		</dl>
<%}%>