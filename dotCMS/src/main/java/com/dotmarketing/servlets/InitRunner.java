package com.dotmarketing.servlets;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.ULID;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.util.ReleaseInfo;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class InitRunner implements Runnable {


    static final String TOTAL_WORKFLOWS = "select count(*) as test_value from workflow_scheme";

    static final String USER_EXCLUDE = "companyid<>'default' and userid<> 'system' and userid<>'anonymous'";

    static final String ACTIVE_USERS =
            "select count(userid) as test_value from user_ where " + USER_EXCLUDE
                    + " and lastlogindate > now() - interval '1 month'";

    static final String TOTAL_USERS =
            "select count(userid) as test_value from user_ where " + USER_EXCLUDE;

    static final String TOTAL_LANGUAGES = "select count(*) as test_value from language";


    static final String TOTAL_TYPES = "select count(*) as test_value from structure";

    static final String TOTAL_SITES =
            "select count(id) as test_value from identifier where asset_subtype='Host' and id <> 'SYSTEM_HOST' ";

    static final String ACTIVE_SITES =
            "select count(id) as test_value from identifier,contentlet_version_info "
                    + " where asset_subtype='Host' and "
                    + " id <> 'SYSTEM_HOST' and "
                    + " identifier.id = contentlet_version_info.identifier and "
                    + " contentlet_version_info.live_inode is not null";

    static final String TOTAL_FOLDERS = "select count(*) as test_value from identifier where asset_type='folder'";

    static final String NUMBER_OF_CONTENTS =
            "select count(working_inode) as test_value from contentlet_version_info";

    static final String RECENTLY_EDITED_CONTENT =
            "select count(working_inode) as test_value from contentlet_version_info, contentlet where contentlet.inode =contentlet_version_info.working_inode and contentlet.mod_date  > now() - interval '1 month'";

    static final String LAST_CONTENT_EDIT =
            "select max(version_ts) as test_value from contentlet_version_info;";

    static final String FIND_ALL_HOSTS_SQL =
            "select \n"
                    + "contentlet_as_json->'fields'->'aliases'->>'value' as aliases,\n"
                    + "contentlet_as_json->'fields'->'hostName'->>'value' as hostname,\n"
                    + "contentlet_as_json->'fields'->'isDefault'->>'value' as default,\n"
                    + "working_inode, \n"
                    + "identifier.id as id \n"
                    + "from contentlet_version_info cvi, \n"
                    + "identifier, \n"
                    + "contentlet \n"
                    + "where \n"
                    + "id = cvi.identifier and \n"
                    + "identifier.asset_subtype='Host'  and \n"
                    + "cvi.working_inode = contentlet.inode and\n"
                    + "id <> 'SYSTEM_HOST' "
                    + "order by contentlet_as_json->'fields'->'isDefault'->>'value' desc "
                    + "limit 100";

    private final Lazy<List<Map<String, Object>>> findAllHostNames = Lazy.of(() -> {
        try (Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
            return new DotConnect(FIND_ALL_HOSTS_SQL).loadObjectResults(conn);


        } catch (DotDataException | SQLException e) {
            logError(e);
            return List.of();
        }
    });

    private String getDefaultHostname() {
        return findAllHostNames.get()
                .stream()
                .filter(map -> "true".equals(map.get("default")))
                .findFirst()
                .map(map -> (String) map.get("hostname")).orElse("unknown");
    }

    private List<String> getHostnames() {
        Set<String> hostNames = new HashSet<>();
        for (Map<String, Object> map : findAllHostNames.get()) {
            hostNames.add((String) map.get("hostname"));
            String aliases = (String) map.get("aliases");
            if (UtilMethods.isEmpty(aliases)) {
                continue;
            }
            String[] x = aliases.split("[\\r\\n,]+");
            for (String y : x) {
                y = y.trim();
                if (UtilMethods.isSet(y) && !y.contains("dotcms.com") || !"host".equals(y)) {
                    hostNames.add(y);
                }
            }
        }
        return List.copyOf(hostNames);
    }


    private boolean sendData() throws Exception {
        if (!Config.getBooleanProperty("dotcms.pingbacks.enabled", true)) {
            return false;
        }
        String endpoint = Config.getStringProperty("dotcms.pingbacks.endpoint", "https://hola.dotcms.site/ping");
        if (!UtilMethods.isSet(endpoint)) {
            logInfo("No pingback endpoint configured");
            return false;
        }

        Map<String, Object> stats = getStats();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String json = objectMapper.writeValueAsString(stats);

        logInfo("Sending pingback to " + endpoint);
        logInfo(String.format("data:%s", json));

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        logInfo("Pingback response: " + response.statusCode());

        return response.statusCode() >= 200 && response.statusCode() < 300;


    }


    @Override
    public void run() {
        try {
            sendData();
        } catch (Throwable e) {
            logError(e);
        }
    }


    /*
    id              UUID NOT NULL,           -- ULID generated by dotCMS, time-sortable
    default_host    TEXT NOT NULL,            -- primary hostname (also in hostnames array)
    hostnames       TEXT[] NOT NULL,          -- all hostnames including default_host
    cluster_id      VARCHAR,                 -- dotCMS cluster identifier
    total_users     INTEGER,
    active_users    INTEGER,
    total_types     INTEGER,
    total_content   BIGINT,                  -- bigint for large enterprise instances
    dotcms_version  TEXT,
    total_languages INTEGER,
    total_sites     INTEGER,
    active_sites    INTEGER,
    cluster_nodes   INTEGER,
    num_folders     INTEGER,
    workflows       INTEGER,
    uve_enabled     BOOLEAN,
    push_publishing BOOLEAN,
    ip_address      INET,                    -- client IP from request headers
    geo_lat         DECIMAL(9,7),
    geo_long        DECIMAL(10,7),
    geo_data        JSONB,                   -- full raw MaxMind GeoLite2-City response
    extra           JSONB,                   -- catch-all for unknown POST body fields
    created_date    TIMESTAMPTZ NOT NULL DEFAULT now(),
     */

    private Map<String, Object> getStats() {

        long startTime = System.currentTimeMillis();
        final ImmutableMap.Builder<String, Object> resultMapBuilder = new ImmutableMap.Builder<>();
        Map<String, Object> resultMap = new ImmutableMap.Builder<String, Object>()
                .put("id", new ULID().nextULID())
                .put("hostnames", getHostnames())
                .put("dotcmsVersion", ReleaseInfo.getVersion())
                .put("buildDate", ReleaseInfo.getBuildDate())
                .put("lastContentEdit", getDate(LAST_CONTENT_EDIT))
                .put("defaultHost", getDefaultHostname())
                .put("clusterId", ClusterFactory.getClusterId())
                .put("totalUsers", getInt(TOTAL_USERS))
                .put("activeUsers", getInt(ACTIVE_USERS))
                .put("totalSites", getInt(TOTAL_SITES))
                .put("totalTypes", getInt(TOTAL_TYPES))
                .put("activeSites", getInt(ACTIVE_SITES))
                .put("totalContent", getInt(NUMBER_OF_CONTENTS))
                .put("recentlyEditedContent", getInt(RECENTLY_EDITED_CONTENT))
                .put("totalLanguages", getInt(TOTAL_LANGUAGES))
                .put("numFolders", getInt(TOTAL_FOLDERS))
                .put("clusterNodes", Try.of(() -> APILocator.getServerAPI().getAliveServers().size()).getOrElse(-1))
                .put("workflows", getInt(TOTAL_WORKFLOWS))
                .put("uveEnabled", isUveEnabled())
                .put("jvmInfo", getJVMInfo())
                .put("createdDate", new Date())
                .put("pushPublishing", Try.of(this::countPushPublishing).getOrElse(0))
                .put("collectionTime", (System.currentTimeMillis() - startTime))
                .build();

        return resultMap;

    }

    private static Optional<Object> getObject(String sql) {
        try (Connection conn = DbConnectionFactory.getDataSource().getConnection()) {
            return Optional.ofNullable(new DotConnect(sql).loadObjectResults(conn).get(0).get("test_value"));

        } catch (DotDataException | SQLException e) {
            throw new DotRuntimeException(e);
        }
    }


    private static long getInt(String sql) {
        Optional<Object> result = getObject(sql);
        return (Long) result.orElse(-1);
    }

    private static Date getDate(String sql) {
        Optional<Object> result = getObject(sql);
        return (Date) result.orElse(new Date(0));
    }

    private static String getString(String sql) {
        Optional<Object> result = getObject(sql);
        return (String) result.orElse(null);
    }

    boolean isUveEnabled() {

        List<String> identifiers = findAllHostNames.get().stream().map(m -> (String) m.get("id"))
                .collect(Collectors.toList());

        return !(APILocator.getAppsAPI().filterSitesForAppKey("dotema-config-v2", identifiers, APILocator.systemUser())
                .isEmpty());


    }

    private int countPushPublishing() throws DotDataException {
        return APILocator.getPublisherEndPointAPI().getAllEndPoints().size();
    }


    private Map<String, Object> getJVMInfo() {

        final Map<String, Object> resultMap = new LinkedHashMap<>();

        long jvmStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();

        String duration = DateUtil.prettyDateSince(new Date(jvmStartTime));
        resultMap.put("maxMemory", UtilMethods.prettyByteify(Runtime.getRuntime().maxMemory()));
        resultMap.put("allocatedMemory", UtilMethods.prettyByteify(Runtime.getRuntime().totalMemory()));
        resultMap.put("freeMemory", UtilMethods.prettyByteify(Runtime.getRuntime().freeMemory()));

        resultMap.put("vmName", ManagementFactory.getRuntimeMXBean().getVmName());
        resultMap.put("vmVendor", ManagementFactory.getRuntimeMXBean().getVmVendor());
        resultMap.put("vmVersion", ManagementFactory.getRuntimeMXBean().getVmVersion());
        resultMap.put("started", duration);
        resultMap.put("startUpTime", new Date(jvmStartTime).toString());

        return resultMap;

    }

    private static void logInfo(String msg) {
        if (Config.getBooleanProperty("dotcms.pingbacks.log", false)) {
            Logger.info(InitRunner.class, msg);
        }

    }


    private static void logError(Throwable e) {
        if (Config.getBooleanProperty("dotcms.pingbacks.log", false)) {
            Logger.error(InitRunner.class, e.getMessage(), e);
        }
    }


}
