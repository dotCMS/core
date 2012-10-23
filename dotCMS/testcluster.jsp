<%@page import="com.dotmarketing.business.DotGuavaCacheAdministratorImpl"%>
<%@page import="com.dotmarketing.business.CacheLocator"%>
<%
((DotGuavaCacheAdministratorImpl)CacheLocator.getCacheAdministrator().getImplementationObject()).testCluster();
%>
