package com.dotcms.content.elasticsearch.util;

import com.dotcms.IntegrationTestBase;
import com.dotcms.cluster.bean.Server;
import com.dotcms.util.IntegrationTestInitService;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.dotcms.content.elasticsearch.util.ESClient.ES_TRANSPORT_HOST;
import static org.elasticsearch.common.settings.Settings.*;
import static org.junit.Assert.assertEquals;

public class ESClientTest extends IntegrationTestBase {

    private final ESClient client = new ESClient();

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    // TODO: Let's convert ClusterFactory into an API with instance methods and then write these tests.
    @Test
    public void testSetUpTransportConf_ValidTransportHostInSettings_ShouldBeKeptInSettings() {
//        final Builder settings = builder();
//        final String myTestAddress = "127.0.0.1";
//        settings.put(ES_TRANSPORT_HOST, myTestAddress);
//
//        Server server = Server.builder().build();
//        client.setUpTransportConf(server, settings);
//
//        assertEquals(myTestAddress, settings.get(ES_TRANSPORT_HOST));

    }


}
