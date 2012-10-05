<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%
    long serversNumber=0;
    String nastyError = null;
    try{
    	serversNumber=1; //TODO Get the server number
    }catch(Exception e){
    	nastyError = e.getMessage();
    }
    if(serversNumber > 0){
%>
<table class="listingTable shadowBox">
		<tr>
			<th><%= LanguageUtil.get(pageContext, "publisher_instance") %></th>		
			<th><%= LanguageUtil.get(pageContext, "publisher_instance_link") %></th>			
		</tr>
<% try{
	for(int server=0; server < serversNumber; server++ ){
		String solrServerUrl = "same-url"; //TODO --- Just an example;
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
			<th><%= LanguageUtil.get(pageContext, "publisher_instance") %></th>		
			<th><%= LanguageUtil.get(pageContext, "publisher_instance_link") %></th>				
		</tr>
		<tr>
			<td colspan="2" class="solr_tcenter"><%= LanguageUtil.get(pageContext, "publisher_No_Results") %></td>
		</tr>
	</table>
<%}%>
<%if(UtilMethods.isSet(nastyError)){%>
		<dl>
			<dt style='color:red;'><%= LanguageUtil.get(pageContext, "publisher_Query_Error") %> </dt>
			<dd><%=nastyError %></dd>
		</dl>
<%}%>