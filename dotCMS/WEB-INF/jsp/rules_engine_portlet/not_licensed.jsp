<%@page import="com.liferay.portal.language.LanguageUtil"%>
<style>
	#dotAjaxMainDiv {overflow: visible;}
	.wrapperCluster{background:url(/html/images/skin/es-search-promo.png) no-repeat 50% 0;height:757px;margin:0 auto;border:1px silver solid;padding:0px;overflow:hidden;}
	.content{position:fixed;left:50%;top:50%;margin:-200px 0 0 -300px;width:600px;background:#333;opacity:.75;color:#fff;padding:20px 20px 35px 20px;-moz-border-radius: 15px;-webkit-border-radius: 15px;-moz-box-shadow:0px 0px 15px #666;-webkit-box-shadow:0px 0px 15px #666;}
	.content h2{font-size:200%;}
	.content p{margin:0;}
	.content ul{margin:5px 0 25px 15px;padding:0 0 0 10px;list-style-position:outside; list-style:decimal;}
	.content li{list-style-position:outside; list-style:disc;}
	.content a{color:#fff;}
</style>

<div class="wrapperCluster">
	<div class="content">
		<h2><%=LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title.ES_SEARCH_PORTLET")%></h2>
		<p><%= LanguageUtil.get(pageContext, "ES-QUERY-NOT-LICENSED") %></p>
	</div>
</div>


