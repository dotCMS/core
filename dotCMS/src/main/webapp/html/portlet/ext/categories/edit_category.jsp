<%@ page import="com.dotmarketing.portlets.categories.struts.CategoryForm" %>
<%@ page import="com.dotmarketing.portlets.categories.model.Category"%>
<%@ include file="/html/portlet/ext/categories/init.jsp" %>
<%@page import="com.dotmarketing.business.UserAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.dotmarketing.util.InodeUtils"%>

<%
	UserAPI userAPI = APILocator.getUserAPI();
	Category category = (Category) request.getAttribute(com.dotmarketing.util.WebKeys.CATEGORY_EDIT);
	Category parentCategory = null;
	String parentId = request.getParameter("parent");
	if(UtilMethods.isSet(parentId))
		parentCategory = APILocator.getCategoryAPI().find(parentId, userAPI.getSystemUser(), false);
	
	CategoryForm categoryForm = (CategoryForm) request.getAttribute("CategoryForm");
%>



<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext,\"edit-category\") %>" />

	<script>

	function cancel()
	{
		var href = "<portlet:renderURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
		href += "<portlet:param name='struts_action' value='/ext/categories/view_category' />";
		href += "</portlet:renderURL>";
		document.location.href = href;
	}

	function doSubmit() {					
		form = document.getElementById("fm");
		if(form.categoryVelocityVarName.value==""){
			fillVelocityVarName();
	    }
		form.action = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/categories/edit_category" /></portlet:actionURL>';
		submitForm(form);
	}

	function fillVelocityVarNameIfNew(){
		<%if (!InodeUtils.isSet(category.getInode())) { %>
			fillVelocityVarName();
		<%}%>
	}
	
	function fillVelocityVarName()
	{
		
		var form = document.getElementById("fm");
		var relation = form.categoryName.value;
		var upperCase = false;
		var newString = "";
		for(i=0;i < relation.length ; i++){
			var c = relation.charAt(i);
			if(upperCase){
				c=c.toUpperCase();
			}
			else{
				c=c.toLowerCase();
			}
			if(c == ' '){
				upperCase = true;
			}
			else{
				upperCase = false;
				newString+=c;
			}
		}
		var re = /[^a-zA-Z0-9]+/g;
		newString = newString.replace(re, "");
		
		form.categoryVelocityVarName.value = newString;
		
		if(newString.length > 0){
			document.getElementById("VariableIdTitle").style.display = "";
		}	
	}
	function hideEditButtonsRow() {
		
		dojo.style('editCategoryButtonRow', { display: 'none' });
	}
	
	function showEditButtonsRow() {
		if( typeof changesMadeToPermissions!= "undefined"){
			if(changesMadeToPermissions == true){
				dijit.byId('applyPermissionsChangesDialog').show();
			}
		}
		dojo.style('editCategoryButtonRow', { display: '' });
		changesMadeToPermissions = false;
	}
	</script>
	
	
	
	
	
	
	<html:form action='/ext/categories/edit_category' styleId="fm">
	<input type="hidden" name="cmd" value="<%= Constants.ADD %>">
	<html:hidden property="inode"  />
	<html:hidden property="parent"  />
	<html:hidden property="sortOrder"  />
	<html:hidden property="active" value="true" />
	<% if (request.getParameter("parent") != null) { %>
		<input type="hidden" name="redirect" value="<portlet:actionURL>
		<portlet:param name="struts_action" value="/ext/categories/view_category" />
		<portlet:param name="inode" value='<%=request.getParameter("parent")%>' />
		</portlet:actionURL>">
	<% } %>


	<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">
		<div id="TabOne" dojoType="dijit.layout.ContentPane" onShow="showEditButtonsRow()" title="<%= LanguageUtil.get(pageContext, "properties") %>">
			<dl>
			    <dt>
		       		<span id="VariableIdTitle" <%if(category.getCategoryVelocityVarName() ==null){%> style="display:none"<%}%>>
			    	<%= LanguageUtil.get(pageContext, "Variable-ID") %>:
					</span>
				</dt>
				<dd style="clear: none;">
					<html:text property="categoryVelocityVarName" readonly="true" style="width:250px;border:0;" />	
				</dd>
				<dt>
					<%= LanguageUtil.get(pageContext, "category-name") %>:
				</dt>
				<dd>
					<input type="text" dojoType="dijit.form.TextBox" name="categoryName" maxlength="50" size="30" onblur="fillVelocityVarNameIfNew()" value="<%= UtilMethods.isSet(categoryForm.getCategoryName()) ?categoryForm.getCategoryName() : "" %>" />
				</dd>
     			<dt>
					<%= LanguageUtil.get(pageContext, "category-unique-key") %>:
				</dt>
				<dd>
					<input type="text" dojoType="dijit.form.TextBox" name="key" size="30" maxlength="255" value="<%= UtilMethods.isSet(categoryForm.getKey()) ? categoryForm.getKey() : "" %>" />
				</dd>
				
				<dt>
					<%= LanguageUtil.get(pageContext, "keywords") %>:
				</dt>
				<dd>
					<textarea dojoType="dijit.form.Textarea" name="keywords" style="width:250px; min-height:40px;"><%= UtilMethods.isSet(categoryForm.getKeywords()) ? categoryForm.getKeywords() : "" %></textarea>
				</dd>
			</dl>	

			
		</div>
<%
	PermissionAPI perAPI = APILocator.getPermissionAPI();
	boolean canEditAsset = perAPI.doesUserHavePermission(category, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user);
	if (canEditAsset) {
%>
		<div id="TabTwo" dojoType="dijit.layout.ContentPane" onShow="hideEditButtonsRow()" title="<%= LanguageUtil.get(pageContext, "permissions") %>">
			<div style="padding: 15px;">
				<%
					request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, category);
					request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT_BASE, parentCategory);
				%>
				<%@ include file="/html/portlet/ext/common/edit_permissions_tab_inc.jsp" %>
			</div>
		</div>
<%
	}
%>
</div>
<div class="clear"></div>
	<div class="buttonRow" id="editCategoryButtonRow">
	
	   <button dojoType="dijit.form.Button" onClick="doSubmit();" iconClass="saveIcon" type="button">
	      <%= LanguageUtil.get(pageContext, "save") %>
	   </button>
	
	   <button dojoType="dijit.form.Button" onClick="cancel();" iconClass="cancelIcon" type="button">
	       <%= LanguageUtil.get(pageContext, "cancel") %>
	   </button>
	</div>
	</html:form>
	
</liferay:box>









