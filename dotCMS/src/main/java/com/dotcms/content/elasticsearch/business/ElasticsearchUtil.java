package com.dotcms.content.elasticsearch.business;

import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.control.Try;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.cluster.settings.ClusterGetSettingsRequest;
import org.elasticsearch.action.admin.cluster.settings.ClusterGetSettingsResponse;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsResponse;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.settings.Settings;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;

/**
 * Provide util methods to set Elasticsearch index properties
 */
public final class ElasticsearchUtil {
    private final static String READ_ONLY_ALLOW_DELETE_SETTING = "index.blocks.read_only_allow_delete";
    private final static String READ_ONLY_SETTING = "index.blocks.read_only";
    public static final String ES_DEFAULT_VALUE = "30s";

    private ElasticsearchUtil(){}

    /**
     * Return true if any the the indices are read only, otherwise return false
     * @param indicesNames names of indices to check
     * @return
     */
    public static  boolean isAnyReadOnly(final String... indicesNames) throws IOException {
        final GetSettingsRequest request = new GetSettingsRequest().indices(indicesNames);

        Logger.debug(ElasticsearchUtil.class, () -> "Checking if current indices are read only");

        final GetSettingsResponse response = RestHighLevelClientProvider
                        .getInstance()
                        .getClient()
                        .indices()
                        .getSettings(request, RequestOptions.DEFAULT);

        Logger.debug(ElasticsearchUtil.class, "Response received on checking if any of the provided indices is read only");

        return Arrays.stream(indicesNames).anyMatch((indexName) -> {
            final String readOnlyAllowDelete = response.getSetting(indexName, READ_ONLY_ALLOW_DELETE_SETTING);
            final String readOnly = response.getSetting(indexName, READ_ONLY_SETTING);

            final boolean isReadOnly = Boolean.parseBoolean(readOnlyAllowDelete) || Boolean.parseBoolean(readOnly);
            Logger.debug(ElasticsearchUtil.class, String.format("Index %s read only: %s", indexName, isReadOnly));

            return isReadOnly;
        });
    }

    /**
     * Send a request to Elasticsearch to put the indices in read-only = false
     *
     * @param indicesNames names of indices to change read_only property
     * @return
     */
    public static AcknowledgedResponse putReadOnlyToFalse(final String... indicesNames) throws IOException {
        final UpdateSettingsRequest request = new UpdateSettingsRequest(indicesNames);

        final Settings.Builder settingBuilder = Settings.builder()
                .put(READ_ONLY_ALLOW_DELETE_SETTING, false)
                .put(READ_ONLY_SETTING, false);

        request.settings(settingBuilder);

        return  RestHighLevelClientProvider
                        .getInstance()
                        .getClient()
                        .indices()
                        .putSettings(request, RequestOptions.DEFAULT);
    }

    /**
     * Return true if the currnet LIVE or WORKING Index is read only, otherwise return false
     * @return
     */
    public static boolean isEitherLiveOrWorkingIndicesReadOnly() {
        final IndiciesInfo indiciesInfo = loadIndicesInfo();
        try {
            return ElasticsearchUtil.isAnyReadOnly(indiciesInfo.getLive(), indiciesInfo.getWorking());
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }
    }

    /***
     * Set to read only to false to the current LIVE and WORKING indices
     * @throws ElasticsearchResponseException
     */
    public static void setLiveAndWorkingIndicesToWriteMode() throws ElasticsearchResponseException {
        final IndiciesInfo indiciesInfo = loadIndicesInfo();

        try {
            final AcknowledgedResponse response = ElasticsearchUtil.putReadOnlyToFalse(indiciesInfo.getLive(), indiciesInfo.getWorking());

            if (!response.isAcknowledged()) {
                throw new ElasticsearchResponseException(response);
            }
        } catch (IOException e){
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Creates a request to get the value of the setting cluster.blocks.read_only, which returns
     * true if the Elastic Search cluster is in read only mode
     * @return boolean
     */
    public static boolean isClusterInReadOnlyMode(){
        final ClusterGetSettingsResponse response = getClusterSettings();

        return Boolean.parseBoolean(response.getSetting("cluster.blocks.read_only"))
                || Boolean.parseBoolean(response.getSetting("cluster.blocks.read_only_allow_delete"));
    }

    /**
     * Return how often Elasticsearch check on disk usage for each node in the cluster
     * @return boolean
     */
    public static long getClusterUpdateInterval(){
        final ClusterGetSettingsResponse response = getClusterSettings();
        final String intervalString = response.getSetting("cluster.info.update.interval");
        final long intervalInMillis = getIntervalInMillis(intervalString == null ? ES_DEFAULT_VALUE : intervalString);

        return intervalInMillis;
    }

    private static long getIntervalInMillis(final String intervalString) {
        final long interval = Long.parseLong(intervalString.substring(0, intervalString.length() - 1).trim());

        if (intervalString.endsWith("m")) {
            return Duration.ofMinutes(interval).toMillis();
        } else {
            return Duration.ofSeconds(interval).toMillis();
        }


    }


    private static ClusterGetSettingsResponse getClusterSettings() {
         try {
            return RestHighLevelClientProvider.getInstance()
                            .getClient().cluster()
                            .getSettings(new ClusterGetSettingsRequest(), RequestOptions.DEFAULT);
        } catch (IOException e) {
            Logger.warnAndDebug(ESIndexAPI.class, "Error getting ES cluster settings", e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Set the Elasticsearch cluster to read only = false
     * @return
     */
    public static void setClusterToWriteMode() throws ElasticsearchResponseException {

        final String[] properties = new String[]{"cluster.blocks.read_only", "cluster.blocks.read_only_allow_delete"};

        for (final String property : properties) {
            final ClusterUpdateSettingsRequest request = new ClusterUpdateSettingsRequest();

            final Settings.Builder settingBuilder = Settings.builder()
                    .put(property, false);

            request.persistentSettings(settingBuilder);

            try {
                final ClusterUpdateSettingsResponse response = RestHighLevelClientProvider.getInstance()
                        .getClient()
                        .cluster()
                        .putSettings(request, RequestOptions.DEFAULT);

                if (!response.isAcknowledged()) {
                    throw new ElasticsearchResponseException(response);
                }
            } catch (IOException e){
                throw new DotRuntimeException(e);
            }
        }

    }

    private static IndiciesInfo loadIndicesInfo() {
        try{
            return APILocator.getIndiciesAPI().loadIndicies();
        } catch (DotDataException e) {
            Logger.error(ESReadOnlyMonitor.class, e);
            throw new DotRuntimeException(e);
        }
    }
}
