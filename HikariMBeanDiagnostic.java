package com.dotcms.rest.api.v1.temp;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.fasterxml.jackson.core.JsonProcessingException;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * Temporary diagnostic endpoint to check HikariCP MBean availability.
 * Remove this after debugging is complete.
 */
@Path("/v1/temp/hikari-debug")
public class HikariMBeanDiagnostic {

    private final WebResource webResource = new WebResource();

    @GET
    @Path("/mbeans")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public ResponseEntityView<Map<String, Object>> listHikariMBeans(
            @Context HttpServletRequest request,
            @Context HttpServletResponse response) throws JsonProcessingException {

        webResource.init(request, response, true);

        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            Map<String, Object> result = new HashMap<>();

            // Get all HikariCP MBeans
            Set<ObjectName> allHikari = mBeanServer.queryNames(new ObjectName("com.zaxxer.hikari:*"), null);
            List<Map<String, String>> mbeanList = new ArrayList<>();

            for (ObjectName mbean : allHikari) {
                Map<String, String> mbeanInfo = new HashMap<>();
                mbeanInfo.put("objectName", mbean.toString());
                mbeanInfo.put("type", mbean.getKeyProperty("type"));
                mbeanInfo.put("pool", mbean.getKeyProperty("Pool"));
                
                // Try to get some attributes to see if MBean is accessible
                try {
                    if ("Pool".equals(mbean.getKeyProperty("type"))) {
                        Object activeConnections = mBeanServer.getAttribute(mbean, "ActiveConnections");
                        Object totalConnections = mBeanServer.getAttribute(mbean, "TotalConnections");
                        mbeanInfo.put("activeConnections", String.valueOf(activeConnections));
                        mbeanInfo.put("totalConnections", String.valueOf(totalConnections));
                        mbeanInfo.put("accessible", "true");
                    }
                } catch (Exception e) {
                    mbeanInfo.put("accessible", "false");
                    mbeanInfo.put("error", e.getMessage());
                }
                
                mbeanList.add(mbeanInfo);
            }

            result.put("totalMBeans", allHikari.size());
            result.put("mbeans", mbeanList);
            
            // Specifically check for Pool MBeans
            Set<ObjectName> poolMBeans = mBeanServer.queryNames(new ObjectName("com.zaxxer.hikari:type=Pool*"), null);
            result.put("poolMBeansFound", poolMBeans.size());
            
            // Check for PoolConfig MBeans
            Set<ObjectName> configMBeans = mBeanServer.queryNames(new ObjectName("com.zaxxer.hikari:type=PoolConfig*"), null);
            result.put("configMBeansFound", configMBeans.size());

            return new ResponseEntityView<>(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("stackTrace", Arrays.toString(e.getStackTrace()));
            return new ResponseEntityView<>(error);
        }
    }
} 