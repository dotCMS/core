package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.client.RequestOptions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ESIndexAPITest {

    private static ESIndexAPI esIndexAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        esIndexAPI = APILocator.getESIndexAPI();
    }

    @Test
    public void test_createIndex_newIndexShouldHaveProperReplicasSetting() throws IOException {
        final String newIndexName = "mynewindex"+ UUID.randomUUID().toString().toLowerCase();
        try {
            esIndexAPI.createIndex(newIndexName);
            final String fullNewIndexName = esIndexAPI.getNameWithClusterIDPrefix(newIndexName);
            GetSettingsRequest request = new GetSettingsRequest().indices(fullNewIndexName);
            GetSettingsResponse getSettingsResponse = RestHighLevelClientProvider.getInstance()
                    .getClient().indices().getSettings(request, RequestOptions.DEFAULT);

            String replicasSetting = getSettingsResponse
                    .getSetting(fullNewIndexName, "index.auto_expand_replicas");

            Assert.assertEquals("0-all", replicasSetting);
        } finally {
            esIndexAPI.delete(esIndexAPI.getNameWithClusterIDPrefix(newIndexName));
        }
    }

    @Test
    public void testGetIndicesStatsWhenStatsTypeIsLongShouldPass(){

        final Map<String, Object> jsonMap = new HashMap<>();

        final Map<String, Object> indices = new HashMap<>();
        final Map<String, Object> indexDetails = new HashMap<>();
        final Map<String, Object> statsMap = new HashMap<>();
        final Map<String, Object> countMap = new HashMap<>();
        final Map<String, Object> sizeMap = new HashMap<>();

        countMap.put("count", Long.valueOf(5));
        sizeMap.put("size_in_bytes", Long.valueOf(2000));

        statsMap.put("docs", countMap);
        statsMap.put("store", sizeMap);

        indexDetails.put("primaries", statsMap);
        indices.put("myDummyIndex", indexDetails);
        jsonMap.put("indices", indices);

        final ESIndexAPI indexAPI = spy(ESIndexAPI.class);
        doReturn(jsonMap).when(indexAPI).performLowLevelRequest(any());
        when(indexAPI.hasClusterPrefix(anyString())).thenReturn(true);

        final  Map<String, IndexStats> result = indexAPI.getIndicesStats();

        assertEquals(1, result.size());

        final IndexStats stats = result.get("myDummyIndex");
        assertNotNull(stats);
        assertEquals(5, stats.getDocumentCount());
        assertEquals(2000, stats.getSizeRaw());
    }

    @Test
    public void testGetIndicesStatsWhenStatsTypeIsIntegerShouldPass(){

        final Map<String, Object> jsonMap = new HashMap<>();

        final Map<String, Object> indices = new HashMap<>();
        final Map<String, Object> indexDetails = new HashMap<>();
        final Map<String, Object> statsMap = new HashMap<>();
        final Map<String, Object> countMap = new HashMap<>();
        final Map<String, Object> sizeMap = new HashMap<>();

        countMap.put("count", 5);
        sizeMap.put("size_in_bytes", 2000);

        statsMap.put("docs", countMap);
        statsMap.put("store", sizeMap);

        indexDetails.put("primaries", statsMap);
        indices.put("myDummyIndex", indexDetails);
        jsonMap.put("indices", indices);

        final ESIndexAPI indexAPI = spy(ESIndexAPI.class);
        doReturn(jsonMap).when(indexAPI).performLowLevelRequest(any());
        when(indexAPI.hasClusterPrefix(anyString())).thenReturn(true);

        final  Map<String, IndexStats> result = indexAPI.getIndicesStats();

        assertEquals(1, result.size());

        final IndexStats stats = result.get("myDummyIndex");
        assertNotNull(stats);
        assertEquals(5, stats.getDocumentCount());
        assertEquals(2000, stats.getSizeRaw());
    }
}
