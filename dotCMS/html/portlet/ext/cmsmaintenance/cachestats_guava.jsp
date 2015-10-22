<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotmarketing.exception.DotSecurityException"%>
<%@page import="com.dotcms.repackage.com.google.common.cache.Cache"%>
<%@page import="com.dotcms.repackage.org.github.jamm.MemoryMeter"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotcms.repackage.com.google.common.cache.CacheStats"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.dotmarketing.business.CacheLocator"%>
<%@page import="java.util.Map"%>
<%@ include file="/html/common/init.jsp"%>
<%@page import="java.util.List"%>
<%
try {
	user = com.liferay.portal.util.PortalUtil.getUser(request);
	if(user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", user)){
		throw new DotSecurityException("Invalid user accessing cachestats_guava.jsp - is user '" + user + "' logged in?");
	}
} catch (Exception e) {
	Logger.error(this.getClass(), e.getMessage());
	%>
	
		<div class="callOutBox2" style="text-align:center;margin:40px;padding:20px;">
		<%= LanguageUtil.get(pageContext,"you-have-been-logged-off-because-you-signed-on-with-this-account-using-a-different-session") %><br>&nbsp;<br>
		<a href="/admin"><%= LanguageUtil.get(pageContext,"Click-here-to-login-to-your-account") %></a>
		</div>
		
	<% 
	//response.sendError(403);
	return;
}
boolean showSize = (request.getParameter("showSize") != null);

MemoryMeter meter = new MemoryMeter();

%>
	<div style="padding-bottom:30px;">
		<table class="listingTable shadowBox" style="width:400px">
			<tr>
				<th><%= LanguageUtil.get(pageContext, "Total-Memory-Available") %></th>
				<td align="right"><%=UtilMethods.prettyByteify( Runtime.getRuntime().maxMemory())%> </td>
			</tr>
			<tr>
				<th><%= LanguageUtil.get(pageContext, "Memory-Allocated") %></th>
				<td align="right"><%= UtilMethods.prettyByteify( Runtime.getRuntime().totalMemory())%></td>
			</tr>
			<tr>
				<th><%= LanguageUtil.get(pageContext, "Filled-Memory") %></th>
				<td align="right"><%= UtilMethods.prettyByteify( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())%></td>
			</tr>
			<tr>
				<th><%= LanguageUtil.get(pageContext, "Free-Memory") %></th>
				<td align="right"><%= UtilMethods.prettyByteify( Runtime.getRuntime().freeMemory())%></td>
			</tr>
		</table>
		<div class="clear"></div>
	</div>

<table class="listingTable ">
        <thead>
                <th><%= LanguageUtil.get(pageContext,"Cache-Region") %></th>
				<th><%= LanguageUtil.get(pageContext,"Cache-Provider-Name") %></th>
				<th><%= LanguageUtil.get(pageContext,"Configured") %></th>
                <th><%= LanguageUtil.get(pageContext,"In-Memory") %></th>
                <th><%= LanguageUtil.get(pageContext,"On-Disk") %></th>
                <th><%= LanguageUtil.get(pageContext,"Load") %></th>
                <th><%= LanguageUtil.get(pageContext,"Hit-Rate") %></th>
                <th><%= LanguageUtil.get(pageContext,"Avg-Load-Time") %></th>
                
                <th><%= LanguageUtil.get(pageContext,"Evictions") %></th>
                <%if(showSize){ %>
                	<th><%= LanguageUtil.get(pageContext,"Size") %> / <%= LanguageUtil.get(pageContext,"Avg") %></th>
                <%} %>
        </thead>
        <%

			List<Map<String, Object>> stats = CacheLocator.getCacheAdministrator().getCacheStatsList();

			int totalMem = 0;
			int totalConfMem = 0;
			int totalDisk = 0;

			CacheStats metaStats = new CacheStats(0, 0, 0, 0, 0, 0);
			NumberFormat nf = DecimalFormat.getInstance();

			for ( Map<String, Object> s : stats ) {

				Boolean toDisk = false;
				if ( s.get("toDisk") != null ) {
					toDisk = (Boolean) s.get("toDisk");
				}

				boolean hasLoad = false;
				CacheStats cacheStats = null;
				if ( s.get("CacheStats") != null ) {
					cacheStats = (CacheStats) s.get("CacheStats");
					hasLoad = (cacheStats.loadCount() > 0);
				}
				Cache cache = null;
				if ( s.get("cache") != null ) {
					cache = (Cache) s.get("cache");
				}

				String name = ((String) s.get("name")).toLowerCase();
				String key = (String) s.get("key");
				boolean isDefault = (Boolean) s.get("isDefault");
				String region = ((String) s.get("region")).toLowerCase();
				int configuredSize = (Integer) s.get("configuredSize");

				String memory = "-";
				if ( s.get("memory") != null && !toDisk ) {
					memory = s.get("memory").toString();
				}

				String disk = "-";
				if ( s.get("disk") != null && toDisk ) {
					disk = s.get("disk").toString();
				}

				long memoryUsed;
				String myMemoryUsed = "-";
				String avgMemoryUsed = "-";
				String sizeHighlightColor = "#aaaaaa";
				if ( !isDefault ) {

					if ( configuredSize > 0 ) {
						totalConfMem += configuredSize;
					}

					if ( cacheStats != null ) {
						metaStats = metaStats.plus(cacheStats);
					}

					try {
						int i = new Integer(s.get("memory").toString());
						if ( i > 0 ) {
							totalMem += i;
						}
					} catch ( Exception e ) {
					}

					try {
						int i = new Integer(s.get("disk").toString());
						if ( i > 0 ) totalDisk += i;
					} catch ( Exception e ) {
					}

					if ( showSize ) {
						try {
							memoryUsed = meter.measureDeep(cache);
							myMemoryUsed = UtilMethods.prettyMemory(memoryUsed);
							avgMemoryUsed = myMemoryUsed;
							try {
								avgMemoryUsed = UtilMethods.prettyMemory(memoryUsed / new Integer(s.get("memory").toString()));
							} catch ( Exception e ) {

							}
							if ( myMemoryUsed.endsWith("G") ) {
								sizeHighlightColor = "black";
							} else if ( myMemoryUsed.endsWith("M") ) {

								sizeHighlightColor = "#666666";
							}
						} catch ( Exception e ) {
							sizeHighlightColor = "silver";
							myMemoryUsed = "Jamm not set as -javaagent";

						}
					}
				}

        	%>

		        <tr>
		                <td align="left">
						<%=!isDefault ? "<b>":"" %>
						<%= region %>
						<%=!isDefault ? "</b>":"" %>
						<%=isDefault ? "(default)":"" %></td>
						<td <%=isDefault ? "style='color:silver;'":"" %>><%=name%></td>
						<td <%=isDefault ? "style='color:silver;'":"" %>><%=configuredSize%></td>
		                <td <%=isDefault ? "style='color:silver;'":"" %>><%=memory%></td>
		                <td <%=isDefault ? "style='color:silver;'":"" %>><%=disk%></td>
		                <td <%=isDefault ? "style='color:silver;'":"" %>><%if(hasLoad){%><%= nf.format(cacheStats.loadCount()) %><%}else{%>-<%}%></td>
		                <td <%=isDefault ? "style='color:silver;'":"" %>><%if(hasLoad){%><%= nf.format(cacheStats.hitRate() * 100) %>%<%}else{%>-<%}%>  </td>
		                <td <%=isDefault ? "style='color:silver;'":"" %>><%if(hasLoad){%><%= nf.format(cacheStats.averageLoadPenalty()/1000000) %> ms<%}else{%>-<%}%></td>
		                <td <%=isDefault ? "style='color:silver;'":"" %>><%if(hasLoad){%><%= cacheStats.evictionCount() %><%}else{%>-<%}%></td>
		                <%if(showSize){ %>
		                	<td style="color:<%=sizeHighlightColor%>">
		                		<%= myMemoryUsed %>  / <%=avgMemoryUsed %>
		                	</td>
		                <%} %>
		                
		        </tr>
        		
        <%}%>


        <tr>
        	<td style="border:1px solid white"><b><%= LanguageUtil.get(pageContext,"Total") %></b></td>
            <td style="border:1px solid white"></td>
            <td style="border:1px solid white"><%= totalConfMem %></td>
            <td style="border:1px solid white"><%= totalMem %></td>
            <td style="border:1px solid white"><%= totalDisk %></td>
            <td style="border:1px solid white"><%= nf.format(metaStats.loadCount()) %> </td>
            <td style="border:1px solid white"><%= nf.format(metaStats.hitRate() * 100) %> </td>
            <td style="border:1px solid white"><%= nf.format(metaStats.averageLoadPenalty()/1000000) %> ms</td>
            <td style="border:1px solid white"><%= metaStats.evictionCount() %></td>
		    
                <%if(showSize){ %>
	                <td style="border:1px solid white;">
	                	<%try{ %>
	          				<%= UtilMethods.prettyMemory( meter.measureDeep(stats)) %> 
	         
	                	<%}catch(Exception e){ %>
	                		Set Jamm at statup, e.g. -javaagent:./dotCMS/WEB-INF/lib/dot.jamm-0.2.5_2.jar
	                	<%} %>
                	 </td>
                <%} %>       
           
            

            
        </tr>
</table>