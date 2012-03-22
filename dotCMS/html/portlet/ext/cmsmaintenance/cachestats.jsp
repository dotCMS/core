<%@page import="org.jboss.cache.Cache"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.dotmarketing.business.CacheLocator"%>
<%@page import="com.dotmarketing.business.DotJBCacheAdministratorImpl"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.List"%>


<table class="listingTable shadowBox">
        <thead>
                <th>Region</th>
                <th>Memory</th>
                <th>Disk</th>
                <th>Eviction Algorithim</th>
                <th>Eviction Queue Capacity</th>
                <th>Eviction Queue Size</th>
        </thead>
        <% List<Map<String, Object>> stats = ((DotJBCacheAdministratorImpl)CacheLocator.getCacheAdministrator()).getCacheStatsList();

        List<Map<String, Object>> liveWorking = new ArrayList<Map<String, Object>>();


        int totalMem = 0;
        int totalDisk = 0;
        for(Map<String, Object> s:stats){
                if(((String) s.get("region")).startsWith("LIVECACHE") || ((String)s.get("region")).startsWith("WORKINGCACHE")){
                        liveWorking.add(s);
                        continue;
                }
        %>
        <tr>
                <td><%= s.get("region") %></td>
                <td><%= s.get("memory") %></td>
                <td><%= s.get("disk") %></td>
                <td><%= s.get("evictionAlgorithm") %></td>
                <td><%= s.get("evictionQueueCapacity") %></td>
                <td><%= s.get("evictionQueueSize") %></td>
        </tr>

        <%
                int i = new Integer(s.get("memory").toString());
                if(i> 0){
                        totalMem +=i;
                }
                i = new Integer(s.get("disk").toString());
                if(i> 0){
                        totalDisk +=i;
                }
        }


        for(Map<String, Object> s:liveWorking){ %>
        <tr>
                <td><%= s.get("region") %></td>
                <td><%= s.get("memory") %></td>
                <td><%= s.get("disk") %></td>

                <td><%= s.get("evictionAlgorithm") %></td>
                <td><%= s.get("evictionQueueCapacity") %></td>
                <td><%= s.get("evictionQueueSize") %></td>
        </tr>

        <%
                int i = new Integer(s.get("memory").toString());
                if(i> 0){
                        totalMem +=i;
                }
                i = new Integer(s.get("disk").toString());
                if(i> 0){
                        totalDisk +=i;
                }
        }
        %>

        <tr>
                <td>TOTAL</td>
                <td><%= totalMem %></td>
                <td><%= totalDisk %></td>
        </tr>
</table>