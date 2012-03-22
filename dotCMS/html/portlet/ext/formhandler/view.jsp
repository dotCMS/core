<%@ page import="java.util.ArrayList" %>
<%@ page import="javax.portlet.WindowState" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.liferay.portal.util.Constants" %>
<%@ page import="java.util.List" %>
<%@ page import="com.dotmarketing.portlets.categories.model.Category" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure" %>
<%@ include file="/html/portlet/ext/formhandler/init.jsp" %>

<%
	java.util.Map params = new java.util.HashMap();
	String referer = com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params);	
	List structures = (List) request.getAttribute(com.dotmarketing.util.WebKeys.Structure.STRUCTURES);
	int STRUCTURE_TYPE_FORM = Structure.STRUCTURE_TYPE_FORM;
%>
<%@page import="com.dotmarketing.util.Config"%>
<style type="text/css">
<!--
input.text
{
  font-family: verdana, helvetica, arial;
  font-size: 11px;
  border: 1px silver solid;
  width: 200px;
  height: 15px;
}

input.btn
{
  font-family: verdana, helvetica, arial;
  font-size: 11px;
  width: 100px;
}

textarea
{
  font-family: verdana, helvetica, arial;
  font-size: 11px;
  border: 1px silver solid;
  width: 200px;
  height: 50px;
}

.mRed
{
	color: red;
}

fieldset
{
	border: 1px silver solid;
}

legend
{
	color: #666666;
}
-->
</style>
<script language="javascript">
function addNewForm()
{
	var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
	href = href + "<portlet:param name='struts_action' value='/ext/structure/edit_structure' />";
	href = href + "<portlet:param name='structureType' value='<%=String.valueOf(STRUCTURE_TYPE_FORM)%>' />";
	href = href + "<portlet:param name='referer' value='<%=referer%>' />";
	href = href + "</portlet:actionURL>";			
	document.location.href = href;
}

function allForms()
{
	var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
	href = href + "<portlet:param name='referer' value='<%=referer%>' />";
	href = href + "</portlet:actionURL>";			
	document.location.href = href;
}
function downloadToExcel(structureInode){
	
	var fieldsValues = "";
	var categoriesValues = "";
	var showDeleted = false;
	
	
	var href = "<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
	href += "<portlet:param name='struts_action' value='/ext/contentlet/edit_contentlet' />";
	href += "<portlet:param name='cmd' value='export' />";		
	href += "<portlet:param name='referer' value='<%=java.net.URLDecoder.decode(referer, "UTF-8")%>' />";		
	href += "</portlet:actionURL>";
	href += "&expStructureInode="+structureInode+"&expFieldsValues="+fieldsValues+"&expCategoriesValues="+categoriesValues+"&showDeleted="+showDeleted;
		
	window.location.href=href;	

}
</script>
<form action="" method="post" name="order">
<table border="0" cellpadding="4" cellspacing="0" width="100%" class="listingTable">
	<tr class="header">
		
		<td><%= LanguageUtil.get(pageContext, "Action") %></td>
		
		<td><a href="<portlet:actionURL>
		<portlet:param name='struts_action' value='/ext/formhandler/view_form' />
		<portlet:param name='orderBy' value='inode' />
		</portlet:actionURL>" ><%= LanguageUtil.get(pageContext, "Inode") %></a>
		</td>
		
		<td><a href="<portlet:actionURL>
		<portlet:param name='struts_action' value='/ext/formhandler/view_form' />
		<portlet:param name='orderBy' value='upper(name)' />
		</portlet:actionURL>" ><%= LanguageUtil.get(pageContext, "Structure-Name") %></a>
		</td>
		
		<td><a href="<portlet:actionURL>
		<portlet:param name='struts_action' value='/ext/formhandler/view_form' />
		<portlet:param name='orderBy' value='upper(description)' />
		</portlet:actionURL>" ><%= LanguageUtil.get(pageContext, "Description") %></a>
		</td>
		
		<td align="center">
		<%= LanguageUtil.get(pageContext, "Content") %>
		</td>
		<td align="center">
		<%= LanguageUtil.get(pageContext, "Download-to-Excel") %>
		</td>
	
	</tr>
	<% 
	if (structures.size() > 0)
	{
		for(int i = 0;i < 5 && i<structures.size();i++)
		{
			Structure structure = (Structure) structures.get(i);
			String str_style =(i % 2 == 0 ? "class=\"alternate_1\"" : "class=\"alternate_2\"");
		%>
  		<tr <%=str_style %> >
  			<td>
    			<a href="<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>
      			 	<portlet:param name='struts_action' value='/ext/structure/edit_structure' />
      			 	<portlet:param name='inode' value='<%=structure.getInode()%>' />
      			 	<portlet:param name='referer' value='<%=referer%>' />
      			    </portlet:actionURL>">
      			     <span class="editIcon"></span>
      			</a>
      		</td>
      		<td>
  					<a href="<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>
  						 	<portlet:param name='struts_action' value='/ext/structure/edit_structure' />
  						 	<portlet:param name='inode' value='<%=structure.getInode()%>' />
  						 	<portlet:param name='referer' value='<%=referer%>' />
  						    </portlet:actionURL>">
  						  	<%=structure.getInode()%></a>
  			</td>
  			<td>
  					<a href="<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>
  						 	<portlet:param name='struts_action' value='/ext/structure/edit_structure' />
  						 	<portlet:param name='inode' value='<%=structure.getInode()%>' />
  						 	<portlet:param name='referer' value='<%=referer%>' />
  						    </portlet:actionURL>">
  						  	<%=structure.getName()%></a>
                  &nbsp;&nbsp;<%if(structure.isDefaultStructure()){%>(default)<%}%>
  			</td>
  			<td>
  				<%=structure.getDescription()%>
  			</td>
  			<td align="center">
  				<a href="<portlet:renderURL>
			<portlet:param name='struts_action' value='/ext/contentlet/view_contentlets' />
			<portlet:param name='structure_id' value='<%=structure.getInode()%>' />
			</portlet:renderURL>">
			<%= LanguageUtil.get(pageContext, "view") %></a>
  			</td>
            <td align="center">
            <a href="javascript:downloadToExcel('<%=structure.getInode()%>')">
			<img src='/icon?i=csv.xls' border='0' alt='export results' align='absbottom'>
            </td>
            
  		</tr>
  		<%}%>
		<%}else{%>
  		<tr align="center">
  			<td colspan="3"><%= LanguageUtil.get(pageContext, "There-are-no-Forms-to-display") %></td>
  		</tr>
		<%}%>
</table>
<table border="0" cellpadding="4" cellspacing="0" width="100%">
	<tr>
		<td align="right">
			<a class="gamma" href="#" onCLick="allForms();"><%= LanguageUtil.get(pageContext, "All-Forms") %></a>  | <a class="gamma" href="#" onCLick="addNewForm();" ><%= LanguageUtil.get(pageContext, "New-Form") %></a>
			&nbsp; &nbsp;
		</td>
	</tr>
</table>
</form>
