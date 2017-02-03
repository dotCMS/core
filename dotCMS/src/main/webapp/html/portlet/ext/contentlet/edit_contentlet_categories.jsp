<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.portlets.categories.model.Category"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.categories.business.CategoryAPI"%>

<%@ include file="/html/portlet/ext/contentlet/init.jsp"%>

	<br/>
<%

	CategoryAPI catAPI = APILocator.getCategoryAPI();

	String[] selectedCategories = (String[])request.getAttribute("selectedCategories");
	List<Category> cats = (List<Category>) request.getAttribute("entityCategories");

	if (cats.size()>0) {
		for (Category category : cats) {
			List<Category> children = catAPI.getChildren(category, user, false);
			if (children.size() >= 1 && catAPI.canUseCategory(category, user, false)) {
				String catOptions = com.dotmarketing.util.UtilHTML.getSelectCategories(category,1,selectedCategories, user, false); 
				if(catOptions.length() > 0) {
%>
	<fieldset>
		<dl>
			<dt>
				<%= category.getCategoryName()%>:
			</dt>
			<dd>
				<select name="categories" class="selectMulti" size='9' multiple="multiple">
					<%= catOptions %>
				</select>
			</dd>
		</dl>
	</fieldset>
<%
				}
			}			
		}
	} else { 
%>
	<%= LanguageUtil.get(pageContext, "There-are-no-categories-associated-with-this-Content-Type") %>
<%
	}
%>


