<%@page import="com.dotmarketing.business.ChainableCacheAdministratorImpl"%>
<%@page import="com.dotmarketing.business.CacheLocator"%>
<%
((ChainableCacheAdministratorImpl)CacheLocator.getCacheAdministrator().getImplementationObject()).testCluster();
%>
