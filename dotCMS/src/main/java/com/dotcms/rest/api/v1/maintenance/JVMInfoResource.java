package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.business.SystemTable;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.system.SystemTableResource;
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
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Tag(name = "Administration")
@Path("/v1/jvm")
@SuppressWarnings("serial")
public class JVMInfoResource implements Serializable {

    private static final String DEFAULT_OBFUSCATE_PATTERN = "passw|pass|passwd|secret|key|token";

    /**
     * Words that hide a value only as a whole word of the name: bounded by the start or end of the name, an
     * underscore, a dot or a dash ({@code SMTP_AUTH_USER}, {@code dotcms.cluster.salt}, {@code SENTRY_DSN}), so that
     * {@code AUTHOR_NAME} keeps its value. {@code pwd} also needs a separator before it ({@code TRUSTSTORE_PWD}), so
     * that the shell's working directory ({@code PWD}) stays visible.
     */
    private static final String WHOLE_WORD_OBFUSCATE_PATTERN =
            "(^|[_.-])(salt|credentials?|auth|authorization|jdbc|dsn|bearer)($|[_.-])|[_.-]pwd($|[_.-])";

    private static final String MASK = "*****";

    public static final Pattern obfuscateBasePattern = Pattern.compile(
            DEFAULT_OBFUSCATE_PATTERN + "|" + WHOLE_WORD_OBFUSCATE_PATTERN, Pattern.CASE_INSENSITIVE);

    /**
     * The same words as {@link #WHOLE_WORD_OBFUSCATE_PATTERN} written as a word of a camel-case name
     * ({@code jdbcUrl}, {@code spring.datasource.jdbcUrl}, {@code dbPwd}, {@code smtpAuth}); case-sensitive, so that
     * {@code authorName} keeps its value.
     */
    private static final Pattern CAMEL_CASE_OBFUSCATE_PATTERN = Pattern.compile(
            "(^|[a-z0-9_.-])(Salt|Pwd|Credentials?|Auth|Authorization|Jdbc|Dsn|Bearer)($|[A-Z0-9_.-])"
                    + "|(^|[_.-])(salt|pwd|credentials?|auth|authorization|jdbc|dsn|bearer)[A-Z]");

    public static final Pattern obfuscatePattern = Pattern.compile(
            Config.getStringProperty("OBFUSCATE_SYSTEM_ENVIRONMENTAL_VARIABLES", DEFAULT_OBFUSCATE_PATTERN),
            Pattern.CASE_INSENSITIVE);

    /**
     * The user information of a URL inside a value, {@code scheme://user:password@host}, up to the last {@code @}
     * before the path, so that a password holding an {@code @} is hidden whole.
     */
    private static final Pattern URL_USER_INFO = Pattern.compile("://[^/\\s]*@");

    /**
     * The name of a {@code name=value} pair inside a value, such as a JVM option or a URL query parameter. The name
     * starts after a character that cannot be part of it and is read possessively, so a long run of name characters
     * is scanned once.
     */
    private static final Pattern PAIR_NAME = Pattern.compile("(?<![A-Za-z0-9_.-])([A-Za-z0-9_.-]++)=");

    /**
     * The value of a pair: an authorization scheme and its token ({@code Basic abc}, {@code Bearer abc}), a double- or
     * single-quoted string, or the text up to a space, {@code &}, {@code ;} or {@code ,}.
     */
    private static final Pattern PAIR_VALUE = Pattern.compile(
            "(?i:basic|bearer|digest|token)\\s+[^\\s&;,]+|\"[^\"]*\"|'[^']*'|[^\\s&;,]+");

    /** The {@code -} or {@code --} and the {@code D} that start a JVM option such as {@code -Dname=value}. */
    private static final Pattern OPTION_PREFIX = Pattern.compile("^-+(D(?=[A-Za-z]))?");

    /**
     * Returns release, host, JVM, environment, configuration override and system property
     * information for this node. Requires a CMS Administrator.
     *
     * @param request  the current {@link HttpServletRequest}
     * @param response the current {@link HttpServletResponse}
     * @return a map with the {@code release}, {@code host}, {@code jvm}, {@code environment},
     *         {@code configOverrides} and {@code system} sections
     */
    @Path("/")
    @GET
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    public final Response getJvmInfo(@Context final HttpServletRequest request, @Context final HttpServletResponse response)
                    throws IOException {

        final InitDataObject initData = new WebResource.InitBuilder(request, response)
                        .requiredBackendUser(true)
                        .requireAdmin(true)
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

        systemTable.all().forEach((key, value) ->
                resultMap.put(key, obfuscateIfNeeded(key, value)));

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
    
    
    
    /**
     * Returns the value to show for a configuration, environment or system property entry. The whole value is
     * replaced by {@code *****} when the name is a masked name ({@link #isMaskedName(String, Predicate)}). Otherwise
     * the value is shown with the value of any {@code name=value} pair whose name is a masked name, and the user
     * information of any URL, replaced by {@code *****}. The scan inside values covers {@code name=value} pairs (also
     * nested in another pair's value, with a quoted value, or with an authorization scheme such as
     * {@code Basic <token>}) and URL user information; other forms, such as JSON or YAML text, a quote that is not
     * closed or is preceded by a backslash, or a password holding a {@code /}, {@code ,} or {@code ;} that is not
     * percent-encoded, are not recognised.
     *
     * @param key         the name of the entry
     * @param valueObject the value of the entry, a {@link String} or {@code null}
     * @return an empty string for a missing value, otherwise the value to show
     */
    public static String obfuscateIfNeeded(final String key, final Object valueObject) {
        final String value = (String) valueObject;
        if(UtilMethods.isEmpty(value)) return "";
        final Predicate<String> refusedKeys = SystemTableResource.refusedKeys();
        return isMaskedName(key, refusedKeys) ? MASK : obfuscateInsideValue(value, refusedKeys);
    }

    /**
     * Tells whether the value of a name is hidden: the name matches the fixed pattern, its camel-case form, the
     * configurable {@code OBFUSCATE_SYSTEM_ENVIRONMENTAL_VARIABLES} pattern, or is a key the system table refuses,
     * read as is, in the form of an environment variable (upper case, {@code .} and {@code -} as {@code _}, without
     * the {@code DOT_} prefix that environment variables use for configuration properties) or in the form of a
     * property (lower case, {@code _} as {@code .}), so that {@code DOT_MY_PRIVATE_VALUE} matches a refused
     * {@code my.private.value}.
     */
    private static boolean isMaskedName(final String name, final Predicate<String> refusedKeys) {
        if (obfuscateBasePattern.matcher(name).find()
                || CAMEL_CASE_OBFUSCATE_PATTERN.matcher(name).find()
                || obfuscatePattern.matcher(name).find()) {
            return true;
        }
        final String upperCase = name.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
        final String environmentForm = upperCase.startsWith("DOT_") ? upperCase.substring(4) : upperCase;
        final String propertyForm = environmentForm.toLowerCase(Locale.ROOT).replace('_', '.');
        return refusedKeys.test(name) || refusedKeys.test(environmentForm) || refusedKeys.test(propertyForm);
    }

    /**
     * Tells whether the value of a {@code name=value} pair found inside a value is hidden. The dashes and the
     * {@code D} of a JVM option ({@code -Dname}, {@code --name}) are not part of the name; the dash counts as the
     * separator before its first word.
     */
    private static boolean isMaskedPairName(final String pairName, final Predicate<String> refusedKeys) {
        final String name = OPTION_PREFIX.matcher(pairName).replaceFirst("");
        return isMaskedName(name, refusedKeys) || (!name.equals(pairName) && isMaskedName("-" + name, refusedKeys));
    }

    /**
     * Replaces the values of {@code name=value} pairs whose name is a masked name, then the user information of
     * URLs, and keeps the rest of the value. After a pair whose name is not masked the scan goes on inside its value,
     * so a pair nested in it ({@code -Ddb.url=jdbc:postgresql://h/db?password=x}) is found too.
     */
    private static String obfuscateInsideValue(final String value, final Predicate<String> refusedKeys) {
        final Matcher name = PAIR_NAME.matcher(value);
        final Matcher pairValue = PAIR_VALUE.matcher(value);
        final StringBuilder result = new StringBuilder();
        int copied = 0;
        int from = 0;
        while (from < value.length() && name.find(from)) {
            from = name.end();
            if (isMaskedPairName(name.group(1), refusedKeys)
                    && pairValue.region(from, value.length()).lookingAt()) {
                result.append(value, copied, from).append(MASK);
                copied = pairValue.end();
                from = copied;
            }
        }
        result.append(value, copied, value.length());
        return URL_USER_INFO.matcher(result).replaceAll("://" + MASK + "@");
    }
    

}
