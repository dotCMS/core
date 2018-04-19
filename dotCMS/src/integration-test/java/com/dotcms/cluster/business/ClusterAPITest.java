package com.dotcms.cluster.business;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import static com.dotcms.cluster.business.ReplicasMode.AUTOWIRE;
import static com.dotcms.cluster.business.ReplicasMode.BOUNDED;
import static com.dotcms.cluster.business.ReplicasMode.STATIC;
import static org.junit.Assert.assertEquals;

@RunWith(DataProviderRunner.class)
public class ClusterAPITest {

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @DataProvider
    public static Object[][] testGetReplicasCountDataProvider() {
        return new Object[][] {{null, true, AUTOWIRE, 0, "false"}, {null, false, STATIC, 1, "false"},
            {"0-all", false, BOUNDED, -1, "0-all"},  {"5", true, STATIC, 5, "false"}, {"5", false, STATIC, 5, "false"},
            };
    }


    @Test
    @UseDataProvider("testGetReplicasCountDataProvider")
    public void testGetReplicasMode(final String esIndexReplicas,
                                    final boolean autowire, final ReplicasMode expectedMode, int expectedReplicas,
                                    final String expectedAutoExpandReplicas) {
        final String oldEsIndexReplicasValue = Config.getStringProperty("ES_INDEX_REPLICAS", null);

        try {
            if (UtilMethods.isSet(esIndexReplicas)) {
                Config.setProperty("ES_INDEX_REPLICAS", esIndexReplicas);
            }

            ClusterAPI clusterAPI = Mockito.spy(APILocator.getClusterAPI());
            Mockito.doReturn(autowire).when(clusterAPI).isESAutoWire();

            ReplicasMode replicasMode = clusterAPI.getReplicasMode();
            assertEquals(expectedMode, replicasMode);
            assertEquals(expectedReplicas, expectedMode.getNumberOfReplicas());
            assertEquals(expectedAutoExpandReplicas, expectedMode.getAutoExpandReplicas());

        } finally {
            if(UtilMethods.isSet(esIndexReplicas)) {
                Config.setProperty("ES_INDEX_REPLICAS", oldEsIndexReplicasValue);
            }
        }
    }
}
