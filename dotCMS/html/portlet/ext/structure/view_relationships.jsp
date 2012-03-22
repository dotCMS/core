<%@ page import="java.util.ArrayList" %>
<%@ page import="javax.portlet.WindowState" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.liferay.portal.util.Constants" %>
<%@ page import="java.util.List" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Relationship" %>

<%@ include file="/html/portlet/ext/structure/init.jsp" %>

<%
	java.util.Map params = new java.util.HashMap();
	params.put("struts_action", new String[] {"/ext/structure/view_relationships"});
	String referer = com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params);
	List<Relationship> relationships = (List<Relationship>) request.getAttribute(com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIPS);
%>

<script language="javascript">
function addNewRelationship()
{
	var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
 	href = href + "<portlet:param name='referer' value='<%=referer%>' />";
	href = href + "<portlet:param name='struts_action' value='/ext/structure/edit_relationship' />";
	href = href + "</portlet:actionURL>";
	document.location.href = href;
}
function deleteRelationship(inode) {
	if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.structure.delete.relationship")) %>')) {
		var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
		href = href + "<portlet:param name='struts_action' value='/ext/structure/edit_relationship' />";
	 	href = href + "<portlet:param name='referer' value='<%=referer%>' />";
	  	href = href + "<portlet:param name='cmd' value='<%=Constants.DELETE%>' />";
		href = href + "</portlet:actionURL>";
		href = href + "&inode=" + inode;
		document.location.href = href;
	}
}

</script>
<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Structures-Relationships")) %>' />
<form action="" method="post" name="order">

<div class="yui-g portlet-toolbar">
	<div class="yui-u" style="text-align:right;">
	
	        <button dojoType="dijit.form.Button" onCLick="addNewRelationship();" iconClass="plusIcon">
	           <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-New-Relationship")) %>
	        </button>
	</div>
</div>


<table class="listingTable" >
	<tr>
		<th style="text-align:center; white-space:nowrap;"><%= LanguageUtil.get(pageContext, "Action") %></th>		
		<th width="33%" ><a href="<portlet:actionURL><portlet:param name='struts_action' value='/ext/structure/view_relationships' /><portlet:param name='orderBy' value='relation_type_value' /></portlet:actionURL>" ><%= LanguageUtil.get(pageContext, "Relationship-Name") %></a></th>		
		<th width="33%" ><a href="<portlet:actionURL><portlet:param name='struts_action' value='/ext/structure/view_relationships' /><portlet:param name='orderBy' value='parent_relation_name' /></portlet:actionURL>" ><%= LanguageUtil.get(pageContext, "Parent-Structure") %></a></th>
		<th width="33%" ><a href="<portlet:actionURL><portlet:param name='struts_action' value='/ext/structure/view_relationships' /><portlet:param name='orderBy' value='child_relation_name' /></portlet:actionURL>" ><%= LanguageUtil.get(pageContext, "Child-Structure") %></a></th>
		<th style="text-align:center; white-space:nowrap;"><%= LanguageUtil.get(pageContext, "Cardinality") %></th>
		<th style="text-align:center; white-space:nowrap;"><%= LanguageUtil.get(pageContext, "Parent-Required") %></th>
		<th style="text-align:center; white-space:nowrap;"><%= LanguageUtil.get(pageContext, "Child-Required") %></th>
	</tr>
	<%
	if (relationships.size() > 0)
	{
		int i = 0;
		for(Relationship relationship : relationships)
		{
			Structure parentStructure = relationship.getParentStructure ();
			Structure childStructure = relationship.getChildStructure ();
			String classN = (i++ % 2 == 0 ? "alternate_1" : "alternate_2");
		%>

		<tr class="<%=classN%>">
			<td align="center">
                <a href="<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>
                        <portlet:param name='struts_action' value='/ext/structure/edit_relationship' />
                        <portlet:param name='inode' value='<%=relationship.getInode()%>' />
                        <portlet:param name='referer' value='<%=referer%>' />
                        </portlet:actionURL>">
                        <span class="editIcon"></span>
                </a>

                <a href="javascript:deleteRelationship('<%=relationship.getInode()%>')">
                	<span class="deleteIcon"></span>
				</a>  
			</td>
			<td>
					<a class="gamma" href="<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>
						 	<portlet:param name='struts_action' value='/ext/structure/edit_relationship' />
						 	<portlet:param name='inode' value='<%=relationship.getInode()%>' />
						 	<portlet:param name='referer' value='<%=referer%>' />
						    </portlet:actionURL>">
					  	<%=relationship.getRelationTypeValue()%>
					</a>
			</td>
			<td>
					<a  class="gamma" href="<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>
						 	<portlet:param name='struts_action' value='/ext/structure/edit_structure' />
						 	<portlet:param name='inode' value='<%=parentStructure.getInode()%>' />
						 	<portlet:param name='referer' value='<%=referer%>' />
						    </portlet:actionURL>">
						  	<%=parentStructure.getName()%>
					</a>
			</td>
			<td>
					<a  class="gamma" href="<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>
						 	<portlet:param name='struts_action' value='/ext/structure/edit_structure' />
						 	<portlet:param name='inode' value='<%=childStructure.getInode()%>' />
						 	<portlet:param name='referer' value='<%=referer%>' />
						    </portlet:actionURL>">
						  	<%=childStructure.getName()%>
					</a>
			</td>
			<td align="center">
				<%=relationship.getCardinality() == com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()?"1-N":"M-N"%>
			</td>
			<td align="center"><%=(relationship.isParentRequired()) ? LanguageUtil.get(pageContext, "Yes") : LanguageUtil.get(pageContext, "No") %></td>
			<td align="center"><%=(relationship.isChildRequired()) ? LanguageUtil.get(pageContext, "Yes") : LanguageUtil.get(pageContext, "No")%></td>
		</tr>
		<%}
		}else{%>
			<tr>
				<td colspan="7">
					<div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "There-are-not-content-relationships-to-display") %></div>
				</td>
			</tr>
		<%}%>

</table>

</form>
</liferay:box>
