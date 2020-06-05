package com.dotcms.content.elasticsearch.business;

import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.settings.Settings;

import java.util.Arrays;

/**
 * Provide util methods to set Elasticsearch index properties
 */
public final class ESIndexUtil {
    private final static String READ_ONLY_ALLOW_DELETE_SETTING = "index.blocks.read_only_allow_delete";
    private final static String READ_ONLY_SETTING = "index.blocks.read_only";

    private ESIndexUtil(){}

    /**
     * Return true if any the the indices are read only, otherwise return false
     * @param indicesNames names of indices to check
     * @return
     */
    public static  boolean isAnyReadOnly(final String... indicesNames) {
        final GetSettingsRequest request = new GetSettingsRequest().indices(indicesNames);

        Logger.debug(ESIndexUtil.class, () -> "Checking if current indices are read only");

        final GetSettingsResponse response = Try.of(() ->
                RestHighLevelClientProvider
                        .getInstance()
                        .getClient()
                        .indices()
                        .getSettings(request, RequestOptions.DEFAULT))
                .getOrElseThrow(DotRuntimeException::new);

        Logger.debug(ESIndexUtil.class, "Response received on checking if any of the provided indices is read only");

        return Arrays.stream(indicesNames).anyMatch((indexName) -> {
            final String readOnlyAllowDelete = response.getSetting(indexName, READ_ONLY_ALLOW_DELETE_SETTING);
            final String readOnly = response.getSetting(indexName, READ_ONLY_SETTING);

            final boolean isReadOnly = Boolean.parseBoolean(readOnlyAllowDelete) || Boolean.parseBoolean(readOnly);
            Logger.debug(ESIndexUtil.class, String.format("Index %s read only: %s", indexName, isReadOnly));

            return isReadOnly;
        });
    }

    /**
     * Send a request to Elasticsearch to put the indices in read-only = false
     *
     * @param indicesNames names of indices to change read_only property
     * @return
     */
    public static AcknowledgedResponse putReadOnlyToFalse(final String... indicesNames) {
        final UpdateSettingsRequest request = new UpdateSettingsRequest(indicesNames);

        final Settings.Builder settingBuilder = Settings.builder()
                .put(READ_ONLY_ALLOW_DELETE_SETTING, false)
                .put(READ_ONLY_SETTING, false);

        request.settings(settingBuilder);

        return Try.of(() ->
                RestHighLevelClientProvider
                        .getInstance()
                        .getClient()
                        .indices()
                        .putSettings(request, RequestOptions.DEFAULT)
        ).getOrElseThrow(DotRuntimeException::new);
    }

    /**
     * Return true if the currnet LIVE or WORKING Index is read only, otherwise return false
     * @return
     */
    public static boolean isEitherLiveOrWokingIndicesReadOnly() {
        final IndiciesInfo indiciesInfo = loadIndicesInfo();
        return ESIndexUtil.isAnyReadOnly(indiciesInfo.getLive(), indiciesInfo.getWorking());
    }

    /***
     * Set to read only to false to the current LIVE and WORKING indices
     * @throws ElasticsearchResponseException
     */
    public static void setLiveAndWorkingIndicesToWriteMode() throws ElasticsearchResponseException {
        final IndiciesInfo indiciesInfo = loadIndicesInfo();
        final AcknowledgedResponse response = ESIndexUtil.putReadOnlyToFalse(indiciesInfo.getLive(), indiciesInfo.getWorking());

        if (!response.isAcknowledged()) {
            throw new ElasticsearchResponseException(response);
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
