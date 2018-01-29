<%@ include file="/html/portlet/ext/folders/init.jsp" %>
<%@ page import="com.dotmarketing.util.Config" %>
<%@ page import="com.dotmarketing.beans.Inode" %>
<%@ page import="com.dotmarketing.beans.WebAsset" %>
<%@ page import="com.dotmarketing.portlets.contentlet.model.Contentlet" %>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%
	String referer = (request.getParameter("referer") != null ) ? request.getParameter("referer") : "";
%>
<%@page import="com.dotmarketing.portlets.folders.business.FolderFactory"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="org.apache.commons.beanutils.PropertyUtils"%>
<%@page import="com.dotmarketing.portlets.structure.model.Field"%>
<%@page import="com.dotmarketing.portlets.structure.factories.FieldFactory"%>
<script src="/html/js/scriptaculous/prototype.js" type="text/javascript"></script>
<script src="/html/js/scriptaculous/scriptaculous.js" type="text/javascript"></script>
<script language="Javascript">
function submitfm() {
	form = document.getElementById('fm');
	form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/contentlet/order_contentlets" /></portlet:actionURL>';
	submitForm(form);
}
function savechanges() {
	form = document.getElementById('fm');
	form.cmd.value = "generatemenu";
	form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/contentlet/order_contentlets" /></portlet:actionURL>';
	form.action = form.action + serialize();
	submitForm(form);
}
function moveMenuItemDown(menuItem){
 	document.getElementById('item').value=menuItem;
 	//document.getElementById('folderParent').value=parentFolder;
 	document.getElementById('move').value="down";
	submitfm();
}

function moveMenuItemUp(menuItem){
 	document.getElementById('item').value=menuItem;
 	//document.getElementById('folderParent').value=parentFolder;
 	document.getElementById('move').value="up";
	submitfm();
}
function goBack() 
{
	<% if (!referer.equals("")) { %>
		window.location.href = "<%=java.net.URLDecoder.decode(referer,"UTF-8")%>";
	<% } else { %>
		window.location.href = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/contentlet/order_contentlets" /></portlet:actionURL>';
	<% } %>
}

</script>

<%
	String pageId = request.getParameter("pageId");
	String containerId = request.getParameter("containerId");

%>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="Order Contentlet Items" />

	<form id="fm" method="post">
		<input type="hidden" name="referer" value="<%=referer%>">
		<input type="hidden" name="cmd" value="reorder">
		<input type="hidden" name="item" id="item" value="">
		<input type="hidden" name="pageId" id="pageId" value="<%=request.getParameter("pageId")%>">
		<input type="hidden" name="containerId" id="containerId" value="<%=request.getParameter("containerId")%>">
		<input type="hidden" name="move" id="move" value="">
	<table border="0" cellpadding="2" cellspacing="2" align="center">
		<tr>
			<td><B><%= LanguageUtil.get(pageContext, "Drag-and-drop-the-items-to-the-desired-position-and-then-save-your-changes") %></B></td>
		</tr>
		<tr>
			<td></td>
		</tr>
		<tr>
			<td align="center">
				<table><tr><td>
					<% 
						java.util.List<Contentlet> items = (java.util.List) request.getAttribute(com.dotmarketing.util.WebKeys.MENU_ITEMS);
						//String htmlTree = ContentletFactory.getContentletTree(items);
						
						//All this was moved here becuase it us to be in teh ContentletFactory and was placed here while it was being refactored.  This also need to be refactored.
						
						int level = 0;
						List<Integer> ids = new ArrayList<Integer>();						
						StringBuffer sb = new StringBuffer();
						int internalCounter = 0;
						String className = "class" + internalCounter;
						String id = "list" + internalCounter;
						ids.add(internalCounter);						

						sb.append("<ul id='" + id + "' >\n");		
						Iterator<Contentlet> itemsIter = items.iterator();		
						Field theField = null;
						while (itemsIter.hasNext()) {
							Contentlet contentlet = itemsIter.next();			
							String title = "";
							String inode = "";

							if(theField == null){
								List<Field> fields = FieldFactory.getFieldsByStructure(contentlet.getStructureInode());
								for(Field field : fields){
									if(field.isRequired()){

										theField = field;
										break;

									}
								}
							}

							try {
								title = contentlet.getTitle();
							} catch (Exception e) {
								Logger.warn(Contentlet.class,e.getMessage());
							}
							inode = contentlet.getInode();
							sb.append("<li class=\"" + className + "\" id=\"inode_" + inode + "\" >" + title + "</li>\n");

						}
						sb.append("</ul>\n");
						
						//StringBuffer sb = getContentletTree(items,ids,level,counter);

						sb.append("<script language='javascript'>\n");
						for(int i = ids.size() - 1;i >= 0;i--)
						{
							int internalCounter1 = (Integer) ids.get(i);
							String id1 = "list" + internalCounter1;
							String className1 = "class" + internalCounter1;
							String sortCreate = "Sortable.create(\"" + id1 + "\",{dropOnEmpty:true,tree:true,constraint:false,only:\"" + className1 + "\"});\n";			
							sb.append(sortCreate);
						}

						sb.append("\n");
						sb.append("function serialize(){\n");
						sb.append("var values = \"\";\n");
						for(int i = 0;i < ids.size();i++)
						{
							int internalCounter1 = (Integer) ids.get(i);
							String id1 = "list" + internalCounter1;		
							String sortCreate = "values += \"&\" + Sortable.serialize('" + id1 + "');\n";			
							sb.append(sortCreate);
						}
						sb.append("return values;\n");
						sb.append("}\n");

						sb.append("</script>\n");

						sb.append("<style>\n");
						for(int i = 0;i < ids.size();i++)
						{
							int internalCounter1 = (Integer) ids.get(i);
							String className1 = "class" + internalCounter1;	
							String style = "li." + className1 + " { cursor: move;}\n";			
							sb.append(style);
						}
						sb.append("</style>\n");
						
						String htmlTree = sb.toString();
					%>
					<%= htmlTree %>
						<br/>
                        <button dojoType="dijit.form.Button" onClick="savechanges()">
                            <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save-changes")) %>
                        </button>   
						<% if (referer!=null && referer.length()>0) { %>
                        <button dojoType="dijit.form.Button" onClick="window.location.href='<%=referer%>'">
                            <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
                        </button>
						<% } %>
				</td></tr></table>
			</td>
		</tr>
	</table>
	</form>
</liferay:box>
