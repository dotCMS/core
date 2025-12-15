package com.dotcms.content.elasticsearch.business;

import static com.dotcms.content.elasticsearch.business.LegacyIndicesInfo.CLUSTER_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.UtilMethods;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.client.RequestOptions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class ESIndexAPITest {

    private static ESIndexAPI esIndexAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        esIndexAPI = APILocator.getESIndexAPI();
    }

    @Test
    public void test_createIndex_newIndexShouldHaveProperReplicasSetting() throws IOException {
        final String newIndexName = "mynewindex" + UUID.randomUUID().toString().toLowerCase();
        try {
            esIndexAPI.createIndex(newIndexName);
            final String fullNewIndexName = esIndexAPI.getNameWithClusterIDPrefix(newIndexName);
            GetSettingsRequest request = new GetSettingsRequest().indices(fullNewIndexName);
            GetSettingsResponse getSettingsResponse = RestHighLevelClientProvider.getInstance().getClient().indices()
                            .getSettings(request, RequestOptions.DEFAULT);

            String replicasSetting = getSettingsResponse.getSetting(fullNewIndexName, "index.auto_expand_replicas");

            Assert.assertEquals("0-all", replicasSetting);
        } finally {
            esIndexAPI.delete(esIndexAPI.getNameWithClusterIDPrefix(newIndexName));
        }
    }


    @Test
    public void index_exists_should_resolve_even_with_cluster_id() throws Exception {
        final String indexName = "live_esindexapitest_index_exists_" + System.currentTimeMillis();
        final String clusterIndexName = esIndexAPI.getNameWithClusterIDPrefix(indexName);
        assertFalse(esIndexAPI.indexExists(indexName));
        assertFalse(esIndexAPI.indexExists(clusterIndexName));

        esIndexAPI.createIndex(indexName);

        assertTrue(esIndexAPI.indexExists(clusterIndexName));
        assertTrue(esIndexAPI.indexExists(clusterIndexName));

        esIndexAPI.delete(indexName);

        assertFalse(esIndexAPI.indexExists(indexName));
        assertFalse(esIndexAPI.indexExists(clusterIndexName));
    }



    @Test
    public void testGetIndicesStatsWhenStatsTypeIsLongShouldPass() {

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

        final Map<String, IndexStats> result = indexAPI.getIndicesStats();

        assertEquals(1, result.size());

        final IndexStats stats = result.get("myDummyIndex");
        assertNotNull(stats);
        assertEquals(5, stats.getDocumentCount());
        assertEquals(2000, stats.getSizeRaw());
    }

    @Test
    public void testGetIndicesStatsWhenStatsTypeIsIntegerShouldPass() {

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

        final Map<String, IndexStats> result = indexAPI.getIndicesStats();

        assertEquals(1, result.size());

        final IndexStats stats = result.get("myDummyIndex");
        assertNotNull(stats);
        assertEquals(5, stats.getDocumentCount());
        assertEquals(2000, stats.getSizeRaw());
    }

    @Test
    public void testGetClusterStatsWhenStatsTypeIsLongShouldPass() {

        final Map<String, Object> jsonMap = new HashMap<>();
        final Map<String, Object> nodes = new HashMap<>();
        final Map<String, Object> indices = new HashMap<>();
        final Map<String, Object> countMap = new HashMap<>();
        final Map<String, Object> sizeMap = new HashMap<>();

        countMap.put("count", Long.valueOf(5));
        sizeMap.put("size_in_bytes", Long.valueOf(2000));
        indices.put("docs", countMap);
        indices.put("store", sizeMap);
        nodes.put("indices", indices);
        nodes.put("roles", new ArrayList<String>());
        jsonMap.put("nodes", Map.of("node1", nodes));
        jsonMap.put("cluster_name", "dummyCluster");

        final ESIndexAPI indexAPI = spy(ESIndexAPI.class);
        doReturn(jsonMap).when(indexAPI).performLowLevelRequest(any());

        final ClusterStats result = indexAPI.getClusterStats();

        assertNotNull(result);
        assertTrue(UtilMethods.isSet(result.getNodeStats()));

        final NodeStats nodeStats = result.getNodeStats().get(0);
        assertEquals(5, nodeStats.getDocCount());
        assertEquals(2000, nodeStats.getSizeRaw());
    }

    @Test
    public void testGetClusterStatsWhenStatsTypeIsIntegerShouldPass() {

        final Map<String, Object> jsonMap = new HashMap<>();
        final Map<String, Object> nodes = new HashMap<>();
        final Map<String, Object> indices = new HashMap<>();
        final Map<String, Object> countMap = new HashMap<>();
        final Map<String, Object> sizeMap = new HashMap<>();

        countMap.put("count", 5);
        sizeMap.put("size_in_bytes", 2000);
        indices.put("docs", countMap);
        indices.put("store", sizeMap);
        nodes.put("indices", indices);
        nodes.put("roles", new ArrayList<String>());
        jsonMap.put("nodes", Map.of("node1", nodes));
        jsonMap.put("cluster_name", "dummyCluster");


        final ESIndexAPI indexAPI = spy(ESIndexAPI.class);
        doReturn(jsonMap).when(indexAPI).performLowLevelRequest(any());

        final ClusterStats result = indexAPI.getClusterStats();

        assertNotNull(result);
        assertTrue(UtilMethods.isSet(result.getNodeStats()));

        final NodeStats nodeStats = result.getNodeStats().get(0);
        assertEquals(5, nodeStats.getDocCount());
        assertEquals(2000, nodeStats.getSizeRaw());
    }


    String[] testClusterNames = {"testing_cluster_name", "testing.cluster-names", "cluster.123.ABC", "12368689060",
            "__THIS_CLUSTER_"};


    @Test
    public void test_allowed_cluster_names_in_indexes() {

        final String indexName = "liveindex_20210322183037";


        for (final String clusterName : testClusterNames) {

            final ESIndexAPI indexAPI = new ESIndexAPI(Lazy.of(() -> CLUSTER_PREFIX + clusterName + "."));

            final String testIndexName = indexAPI.getNameWithClusterIDPrefix(indexName);

            assert (indexAPI.hasClusterPrefix(testIndexName));

            final String testIndexNameRecursive = indexAPI.getNameWithClusterIDPrefix(testIndexName);

            assertEquals(testIndexNameRecursive, testIndexName);

            final String showableIndexName = indexAPI.removeClusterIdFromName(testIndexName);

            assert (!indexAPI.hasClusterPrefix(showableIndexName));

            assertEquals(showableIndexName, indexName);


        }


    }

    @DataProvider
    public static Object[] testDeleteOldIndicesDP() {
        return new Integer[]{2, 0, 5, 50};
    }

    /**
     * Method to test: {@link ESIndexAPI#deleteInactiveLiveWorkingIndices(int)}
     * Given scenario: different numbers for the live/working sets to be kept (not deleted)
     * Expected result: indices older than the live/working index-set indicated to be kept are successfully deleted
     */
    @Test
    @UseDataProvider("testDeleteOldIndicesDP")
    public void testDeleteOldIndices(final int inactiveLiveWorkingSetsToKeep) throws DotIndexException, IOException, InterruptedException {
        // get live and working active indices
        final IndicesInfo info = Try.of(()->APILocator.getIndiciesAPI().loadLegacyIndices())
                .getOrNull();

        final String liveIndex = info.getLive();
        final String workingIndex = info.getWorking();
        final int LIVE_AND_WORKING_COUNT = 2;

        // create a few working/live indices
        final ContentletIndexAPI contentletIndexAPI = APILocator.getContentletIndexAPI();

        List<String> indicesThatShouldStay = new ArrayList<>();

        for(int i=0; i<6; i++) {
            info.createNewIndicesName(IndexType.REINDEX_WORKING, IndexType.REINDEX_LIVE);
            contentletIndexAPI.createContentIndex(info.getReindexWorking(), 0);
            contentletIndexAPI.createContentIndex(info.getReindexLive(), 0);

            if(i>=6-inactiveLiveWorkingSetsToKeep) {
                indicesThatShouldStay.add(esIndexAPI.removeClusterIdFromName(info.getReindexWorking()));
                indicesThatShouldStay.add(esIndexAPI.removeClusterIdFromName(info.getReindexLive()));
            }
            Thread.sleep(1000);
        }

        esIndexAPI.deleteInactiveLiveWorkingIndices(inactiveLiveWorkingSetsToKeep);

        List<String> indicesAfterDeletion = esIndexAPI.getLiveWorkingIndicesSortedByCreationDateDesc();
        // assert active live index wasn't removed
        assertTrue(indicesAfterDeletion.contains(esIndexAPI.removeClusterIdFromName(liveIndex)));
        // assert active working index wasn't removed
        assertTrue(indicesAfterDeletion.contains(esIndexAPI.removeClusterIdFromName(workingIndex)));
        // assert proper resulting size
        assertTrue(LIVE_AND_WORKING_COUNT + inactiveLiveWorkingSetsToKeep*2 >= indicesAfterDeletion.size());
        // assert expected indices to stay
        for (String indexThatShouldStay : indicesThatShouldStay) {
            assertTrue(indicesAfterDeletion.contains(indexThatShouldStay));
        }

    }

}
