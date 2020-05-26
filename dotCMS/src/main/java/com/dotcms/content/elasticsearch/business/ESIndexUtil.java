package com.dotcms.content.elasticsearch.business;

import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.settings.Settings;

import java.util.Arrays;

public class ESIndexUtil {
    private final static String READ_ONLY_ALLOW_DELETE_SETTING = "index.blocks.read_only_allow_delete";
    private final static String READ_ONLY_SETTING = "index.blocks.read_only";

    private ESIndexUtil(){}

    public static  boolean isReadOnly(final String... indexNames) {
        final GetSettingsRequest request = new GetSettingsRequest().indices(indexNames);
        request.names(READ_ONLY_ALLOW_DELETE_SETTING, READ_ONLY_SETTING);

        Logger.info(ESIndexUtil.class, "Checking if current index are read only");

        final GetSettingsResponse response = Sneaky.sneak(() ->
                RestHighLevelClientProvider.getInstance().getClient().indices()
                        .getSettings(request, RequestOptions.DEFAULT));

        Logger.info(ESIndexUtil.class, "Checking if current index are read only RESPONSE");

        return Arrays.stream(indexNames).anyMatch((indexName) -> {
            final String readOnlyAllowDelete = response.getSetting(indexName, READ_ONLY_ALLOW_DELETE_SETTING);
            final String readOnly = response.getSetting(indexName, READ_ONLY_SETTING);

            return Boolean.parseBoolean(readOnlyAllowDelete) || Boolean.parseBoolean(readOnly);
        });
    }

    public static AcknowledgedResponse putReadOnlyToFalse(final String... indexName) {
        final UpdateSettingsRequest request = new UpdateSettingsRequest(indexName);

        final Settings.Builder settingBuilder = Settings.builder()
                .put(READ_ONLY_ALLOW_DELETE_SETTING, false)
                .put(READ_ONLY_SETTING, false);

        request.settings(settingBuilder);

        return Sneaky.sneak(() ->
                RestHighLevelClientProvider.getInstance().getClient().indices()
                        .putSettings(request, RequestOptions.DEFAULT)
        );
    }

    public static boolean isAnyCurrentIndicesReadOnly() {
        final IndiciesInfo indiciesInfo = loadIndicesInfo();
        return ESIndexUtil.isReadOnly(indiciesInfo.getLive(), indiciesInfo.getWorking());
    }

    public static void putCurrentIndicesToWriteMode() throws ESResponseException {
        final IndiciesInfo indiciesInfo = loadIndicesInfo();
        final AcknowledgedResponse response = ESIndexUtil.putReadOnlyToFalse(indiciesInfo.getLive(), indiciesInfo.getWorking());

        if (!response.isAcknowledged()) {
            throw new ESResponseException(response);
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
