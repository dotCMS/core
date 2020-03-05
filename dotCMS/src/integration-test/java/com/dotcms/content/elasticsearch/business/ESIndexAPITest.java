package com.dotcms.content.elasticsearch.business;

import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import java.io.IOException;
import java.util.UUID;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ESIndexAPITest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void test_createIndex_newIndexShouldHaveProperReplicasSetting() throws IOException {
        final String newIndexName = "mynewindex"+ UUID.randomUUID().toString().toLowerCase();
        try {
            APILocator.getESIndexAPI().createIndex(newIndexName);
            final String fullNewIndexName = APILocator.getESIndexAPI()
                    .getNameWithClusterIDPrefix(newIndexName);
            GetSettingsRequest request = new GetSettingsRequest().indices(fullNewIndexName);
            GetSettingsResponse getSettingsResponse = RestHighLevelClientProvider.getInstance()
                    .getClient().indices().getSettings(request, RequestOptions.DEFAULT);

            String replicasSetting = getSettingsResponse
                    .getSetting(fullNewIndexName, "index.auto_expand_replicas");

            Assert.assertEquals("0-all", replicasSetting);
        } finally {
            APILocator.getESIndexAPI().delete(APILocator.getESIndexAPI()
                    .getNameWithClusterIDPrefix(newIndexName));
        }
    }

}
