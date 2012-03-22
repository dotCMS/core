<%@ include file="/html/portlet/admin/init.jsp" %>

<form action="<portlet:actionURL><portlet:param name="struts_action" value="/admin/upload_logo" /></portlet:actionURL>" enctype="multipart/form-data" method="post" name="<portlet:namespace />fm" onSubmit="submitForm(this); return false;">
<input name="<portlet:namespace />redirect" type="hidden" value="<portlet:renderURL><portlet:param name="struts_action" value="/admin/edit_company" /></portlet:renderURL>">

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"logo\") %>" />

<c:if test="<%= SessionErrors.contains(renderRequest, UploadException.class.getName()) %>">
	<div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "an-unexpected-error-occurred-while-uploading-your-file") %></div>
</c:if>

<dl>
	<dt>&nbsp;</dt>
	<dd><input class="form-text" name="<portlet:namespace />file_name" size="50" type="file"></dd>
	<dd class="inputCaption"><%= LanguageUtil.format(pageContext, "upload-a-gif-or-jpeg-that-is-x-pixels-tall-and-x-pixels-wide", new Object[] {"50", "250"}, false) %></dd>
</dl>
	
<div class="buttonRow">
	<button dojoType="dijit.form.Button" type="submit" id="submitButton" iconClass="saveIcon"><%= LanguageUtil.get(pageContext, "upload-image") %></button>
</div>

</liferay:box>

</form>

<script language="JavaScript">
	dojo.addOnLoad (function(){		
		setTimeout("document.<portlet:namespace />fm.<portlet:namespace />file_name.focus();",500);		
	});
</script>