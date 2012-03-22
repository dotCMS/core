<%@page import="com.liferay.portal.language.LanguageUtil"%>
<div style="min-height: 500px;">
	<div style="float:left;width:40%;padding:50px">
		<h2><%= LanguageUtil.get(pageContext, "Forms-and-Form-Builder") %></h2>
		<br />
		<p>
			<%= LanguageUtil.get(pageContext, "Forms-and-Form-Builder-in-Enterprise") %>
		</p>
		<ul>
			<li style="list-style: square;padding-bottom:5px;">
				<a href="http://dotcms.com/products/editions/" target="_blank">
					<%= LanguageUtil.get(pageContext, "Learn-more-about-dotCMS-Enterprise") %>
				</a>
			</li>
			<%if(request.getAttribute("licenseManagerPortletUrl") != null){ %>
				<li style="list-style: square;padding-bottom:5px;">
					<a href="<%=request.getAttribute("licenseManagerPortletUrl") %>">
						<%=LanguageUtil.get(pageContext, "Request-a-free-trial-license") %>
					</a>
				</li>
			<%}%>
			<li style="list-style: square;padding-bottom:5px;">
				<a href="http://www.dotcms.com/contact-us" target="_blank">
					<%=LanguageUtil.get(pageContext, "Contact-Us-for-more-Information") %>
				</a>
			</li>
			<li style="list-style: square;padding-bottom:5px;">
				<a href="/html/dotCMS_form_builder/form_builder_2_7.html" target="_blank">
					<%= LanguageUtil.get(pageContext, "Still-use-legacy-form-builder") %>
				</a>
			</li>
		</ul>
	</div>
	<div style="float:left;text-align:center;">




			<div id="formHandlerDemoDiv" style="width:480px;height:300px;background:#eee;border:1px solid gray;margin:10px; padding:0px;">
				<embed id="formHandlerDemo" src="http://blip.tv/play/he9%2BgffpbgA%2Em4v" type="application/x-shockwave-flash" width="480" height="300" allowscriptaccess="always" allowfullscreen="true"></embed>
			</div>

			<%=LanguageUtil.get(pageContext, "watch-easy-form-creation") %>
			

	</div>
</div>
<script type="text/javascript">
dojo.addOnUnload (function(){
	if(dojo.isIE){//DOTCMS-5302
		document.getElementById('formHandlerDemoDiv').innerHTML="";
	}	
});
</script>

