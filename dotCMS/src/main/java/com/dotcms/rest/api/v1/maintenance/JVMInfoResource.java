package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.business.SystemTable;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.util.ReleaseInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Try;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.text.NumberFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

@Tag(name = "Administration")
@Path("/v1/jvm")
@SuppressWarnings("serial")
public class JVMInfoResource implements Serializable {

    private static final String DEFAULT_OBFUSCATE_PATTERN = "passw|pass|passwd|secret|key|token";

    public static final Pattern obfuscateBasePattern = Pattern.compile(DEFAULT_OBFUSCATE_PATTERN,
            Pattern.CASE_INSENSITIVE);

    public static final Pattern obfuscatePattern = Pattern.compile(
            Config.getStringProperty("OBFUSCATE_SYSTEM_ENVIRONMENTAL_VARIABLES", DEFAULT_OBFUSCATE_PATTERN),
            Pattern.CASE_INSENSITIVE);

    @Path("/")
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response getJvmInfo(@Context final HttpServletRequest request, @Context final HttpServletResponse response)
                    throws IOException {

        final InitDataObject initData = new WebResource.InitBuilder(request, response)
                        .requiredBackendUser(true)
                        .requestAndResponse(request, response)
                        .rejectWhenNoUser(true)
                        .requiredPortlet("maintenance")
                        .init();


        final LinkedHashMap<String,Object> resultMap=new LinkedHashMap<>();
        resultMap.put("release", this.getReleaseInfo());
        resultMap.put("host", getHostInfo());
        resultMap.put("jvm", getJVMInfo());
        resultMap.put("environment", getEnvironmentalVars());
        resultMap.put("configOverrides", getDBOverrides());
        resultMap.put("system", getSystemProps());

        
        
        return Response.ok(resultMap).build();



    }


    @SuppressWarnings("restriction")
    private Map<String,Object> getHostInfo(){
        
        final Map<String,Object> resultMap=new LinkedHashMap<>();
        resultMap.put("cores", Runtime.getRuntime().availableProcessors());

        NumberFormat percentage = NumberFormat.getPercentInstance();
        percentage.setMinimumFractionDigits(1);
        
        
        com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean)
                        java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        
        
        
        resultMap.put("physicalMemory",   UtilMethods.prettyByteify(os.getTotalPhysicalMemorySize()));
        resultMap.put("processors",  os.getAvailableProcessors());
        resultMap.put("committedMemory",  UtilMethods.prettyByteify(os.getCommittedVirtualMemorySize()));
        resultMap.put("freePhysicalMemory",  UtilMethods.prettyByteify(os.getFreePhysicalMemorySize()));
        resultMap.put("cpuLoadJava",  percentage.format(os.getProcessCpuLoad()));
        resultMap.put("arch",  os.getArch());
        resultMap.put("cpuLoadSystem",  percentage.format(os.getSystemCpuLoad()));
        resultMap.put("os",  os.getName());
        resultMap.put("osVersion",  os.getVersion());
        resultMap.put("hostname", Try.of(()->InetAddress.getLocalHost().getHostName()).getOrElse("ukn") );
        
        
        return resultMap;
        
    }
    

    private Map<String, Object> getDBOverrides(){
        final Map<String,Object> resultMap=new LinkedHashMap<>();

        SystemTable systemTable =APILocator.getSystemAPI().getSystemTable();

        resultMap.putAll(systemTable.all());




        return resultMap;
    }



    private Map<String,Object> getJVMInfo(){
        
        final Map<String,Object> resultMap=new LinkedHashMap<>();
        
        
        long jvmUpTime = ManagementFactory.getRuntimeMXBean().getUptime();
        long jvmStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        
        String duration = DateUtil.prettyDateSince(new Date(jvmStartTime));
        resultMap.put("maxMemory",UtilMethods.prettyByteify( Runtime.getRuntime().maxMemory() ));
        resultMap.put("allocatedMemory",UtilMethods.prettyByteify( Runtime.getRuntime().totalMemory() ));
        resultMap.put("freeMemory", UtilMethods.prettyByteify( Runtime.getRuntime().freeMemory()));
       
        resultMap.put("vmName", ManagementFactory.getRuntimeMXBean().getVmName());
        resultMap.put("vmVendor", ManagementFactory.getRuntimeMXBean().getVmVendor());
        resultMap.put("vmVersion", ManagementFactory.getRuntimeMXBean().getVmVersion());
        resultMap.put("started", duration);
        resultMap.put("startUpTime", new Date(jvmStartTime).toString());
        
        return resultMap;
        
    }
    
    private Map<String,Object> getSystemProps(){
        
        final Map<String,Object> resultMap=new LinkedHashMap<>();
        Properties props = System.getProperties();
        for(Object keyObject : props.keySet()) {
            final String key = (String) keyObject;
            resultMap.put(key, obfuscateIfNeeded(key,props.getProperty(key)));
        }
        
        return resultMap;
        
    }
    
    
    private Map<String,Object> getEnvironmentalVars(){
        
        final Map<String,Object> resultMap=new LinkedHashMap<>();
        Map<String,String> vars = System.getenv();
        for(String key : vars.keySet()) {
            resultMap.put(key, obfuscateIfNeeded(key,vars.get(key)));
        }
        
        return resultMap;
        
    }
    private Map<String,Object> getReleaseInfo(){
        
        final Map<String,Object> resultMap=new LinkedHashMap<>();
        resultMap.put("version",ReleaseInfo.getVersion());
        resultMap.put("buildDate",UtilMethods.htmlDateToHTMLTime (ReleaseInfo.getBuildDate()));

        resultMap.put("name",ReleaseInfo.getName());
        resultMap.put("buildNumber",ReleaseInfo.getBuildNumber());
        resultMap.put("serverInfo",ReleaseInfo.getServerInfo());
        resultMap.put("releaseInfo",ReleaseInfo.getReleaseInfo());

        return resultMap;
        
    }
    
    
    
    public static String obfuscateIfNeeded(final String key, final Object valueObject) {
        final String value = (String) valueObject;
        if(UtilMethods.isEmpty(value)) return "";
        return obfuscateBasePattern.matcher(key).find() || obfuscatePattern.matcher(key).find()
                ? obfuscate(value)
                : value;
    }
    
    private static String obfuscate(final String value) {
        return value.charAt(0)
                + "*********"
                + value.charAt(value.length() - 1);
    }
    

}
