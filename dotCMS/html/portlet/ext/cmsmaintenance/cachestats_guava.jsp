<%@page import="com.google.common.cache.Cache"%>
<%@page import="org.github.jamm.MemoryMeter"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.google.common.cache.CacheStats"%>
<%@page import="com.dotmarketing.business.DotGuavaCacheAdministratorImpl"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.dotmarketing.business.CacheLocator"%>
<%@page import="com.dotmarketing.business.DotJBCacheAdministratorImpl"%>
<%@page import="java.util.Map"%>
<%@ include file="/html/common/init.jsp"%>
<%@page import="java.util.List"%>
<%
boolean ADMIN_MODE = (session.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
if(!ADMIN_MODE || user == null){
	response.sendError(401);
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
        <% List<Map<String, Object>> stats = CacheLocator.getCacheAdministrator().getCacheStatsList();
        
        List<Map<String, Object>> liveWorking = new ArrayList<Map<String, Object>>();


        int totalMem = 0;
        int totalConfMem = 0;
        int totalDisk = 0;

        CacheStats metaStats = new CacheStats(0,0,0,0,0,0);
        NumberFormat nf = DecimalFormat.getInstance();
        for(Map<String, Object> s:stats){

        	CacheStats cacheStats = (CacheStats) s.get("CacheStats");
			Cache cache =(Cache) s.get("cache");
        	boolean isDefault =(Boolean) s.get("isDefault");
        	
        	String region = (String) s.get("region");
        	
        	
        	
			int configuredSize =(Integer) s.get("configuredSize");

        	
        	
        	boolean hasLoad = (cacheStats.loadCount() > 0);
        	
        	long memoryUsed =0;
        	String myMemoryUsed = "-";
        	String avgMemoryUsed = "-";
        	String sizeHighlightColor = "#aaaaaa";
			if(!isDefault){
				
				totalConfMem +=configuredSize;
				
	        	if(cacheStats !=null){
	        		metaStats = metaStats.plus(cacheStats);
	        	}
	        	
				try{
	    			int i = new Integer(s.get("memory").toString());
	            	totalMem +=i;
				}
				catch(Exception e){}
				
				try{
					int i = new Integer(s.get("disk").toString());
					if(i>0)totalDisk +=i;
				}
				catch(Exception e){}
				
				if(showSize){
					try{
						memoryUsed=meter.measureDeep(cache);
						myMemoryUsed =UtilMethods.prettyMemory( memoryUsed);
						avgMemoryUsed = myMemoryUsed;
						try{
							avgMemoryUsed=UtilMethods.prettyMemory( memoryUsed / new Integer(s.get("memory").toString()));
						}
						catch(Exception e){
							
						}
						if(myMemoryUsed.endsWith("G")){
							sizeHighlightColor="black";
						}
						else if(myMemoryUsed.endsWith("M")){
							
							sizeHighlightColor="#666666";
						}
					}
					catch(Exception e){
						sizeHighlightColor="silver";
						myMemoryUsed="Jamm not set as -javaagent";
						
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
		                <td <%=isDefault ? "style='color:silver;'":"" %>><%=configuredSize %> </td>
		                <td <%=isDefault ? "style='color:silver;'":"" %>><%= s.get("memory") %></td>
		                <td <%=isDefault ? "style='color:silver;'":"" %>><%=((Boolean) s.get("toDisk")) ?  s.get("disk") : "-" %></td>
		                <td <%=isDefault ? "style='color:silver;'":"" %>><%= nf.format(cacheStats.loadCount()) %></td>
		                <td <%=isDefault ? "style='color:silver;'":"" %>><%if(hasLoad){%><%= nf.format(cacheStats.hitRate() * 100) %>%<%}else{%>-<%} %>  </td>
		                <td <%=isDefault ? "style='color:silver;'":"" %>><%if(hasLoad){%><%= nf.format(cacheStats.averageLoadPenalty()/1000000) %> ms<%}else{%>-<%} %></td>
		                <td <%=isDefault ? "style='color:silver;'":"" %>><%if(hasLoad){%><%= cacheStats.evictionCount() %><%}else{%>-<%} %></td>
		                <%if(showSize){ %>
		                	<td style="color:<%=sizeHighlightColor%>">
		                		<%= myMemoryUsed %>  / <%=avgMemoryUsed %>
		                	</td>
		                <%} %>
		                
		        </tr>
        		
        <%}%>


        <tr>
        	<td style="border:1px solid white"><b><%= LanguageUtil.get(pageContext,"Total") %></b></td>
            <td style="border:1px solid white"><%=totalConfMem %></td>
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
	                		Set Jamm at statup, e.g. -javaagent:./dotCMS/WEB-INF/lib/jamm-0.2.5.jar
	                	<%} %>
                	 </td>
                <%} %>       
           
            

            
        </tr>
</table>
