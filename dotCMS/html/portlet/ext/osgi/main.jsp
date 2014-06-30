<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotcms.repackage.felix_4_2_1.org.osgi.framework.Bundle"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.listeners.OsgiFelixListener"%>
<%@page import="com.dotcms.repackage.felix_4_2_1.org.apache.felix.main.AutoProcessor"%>
<%@page import="com.dotcms.repackage.felix_4_2_1.org.osgi.framework.BundleContext"%>

<script type="text/javascript" src="/html/portlet/ext/osgi/js.jsp" ></script>
<div class="portlet-wrapper">
	
	<div class="subNavCrumbTrail">
		<ul id="subNavCrumbUl">

		
			<li>
				<a href="javascript:bundles.show()"><%=LanguageUtil.get(pageContext, "OSGI")%></a>
			</li>
			<li class="lastCrumb"><span><%=LanguageUtil.get(pageContext, "OSGI-MANAGER")%></span></li>
		</ul>
		<div class="clear"></div>
	</div>
	
	<div id="osgiMain">
	
	</div>
</div>

