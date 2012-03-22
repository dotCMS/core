<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<script type="text/javascript" src="/html/portlet/ext/workflows/schemes/workflow_js.jsp" ></script>

<style type="text/css">
	@import "/html/portlet/ext/workflows/schemes/workflow.css"; 
</style>	

<div class="portlet-wrapper">
	<div class="subNavCrumbTrail">
		<ul id="subNavCrumbUl">
			<li>
				<a href="javascript:schemeAdmin.show()"><%=LanguageUtil.get(pageContext, "Workflow")%></a>
			</li>
			<li class="lastCrumb"><span><%=LanguageUtil.get(pageContext, "Schemes")%></span></li>
		</ul>
		<div class="clear"></div>
	</div>
	
	<%if(LicenseUtil.getLevel() < 200){ %>
	
	
	
		<style>
			.wrapper{background:url(/html/images/skin/workflow-promo.png) no-repeat 0 0;height:992px;margin:0 auto;}
			.content{position:fixed;left:50%;top:50%;margin:-200px 0 0 -300px;width:600px;background:#333;opacity:.85;color:#fff;padding:20px 20px 35px 20px;-moz-border-radius: 15px;-webkit-border-radius: 15px;-moz-box-shadow:0px 0px 15px #666;-webkit-box-shadow:0px 0px 15px #666;}
			.content h2{font-size:200%;}
			.content p{margin:0;}
			.content ul{margin:5px 0 25px 15px;padding:0 0 0 10px;list-style-position:outside; list-style:decimal;}
			.content li{list-style-position:outside; list-style:disc;}
			.content a{color:#fff;}
		</style>
		<div class="greyBg"></div>
		<div class="wrapper">
			<div class="content">
				<h2><%= LanguageUtil.get(pageContext, "Workflows") %></h2>
				<p><%= LanguageUtil.get(pageContext, "Workflows-Not-Licensed") %></p>
			</div>
		</div>

	<%}else{ %>
		<div id="hangWorkflowMainHere">
		
		</div>
	<%} %>

</div>

<div dojoType="dijit.Dialog" style="display: none" id="addEditScheme">
	<div id="addEditSchemeHanger"></div>
</div>

