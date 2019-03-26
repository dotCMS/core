package com.dotcms.content.elasticsearch.util;

import static org.junit.Assert.assertEquals;

import com.dotcms.cluster.bean.Server;
import com.liferay.util.StringPool;
import org.junit.Test;

public class ESClientTest {

  @Test
  public void testgetServerAddress_ValidIpAndValidPort_ShouldGetAddressSemiColonPort() {
    final String ipAddress = "192.168.1.186";
    final int port = 9300;
    final Server server =
        Server.builder().withIpAddress(ipAddress).withEsTransportTcpPort(port).build();
    final ESClient esClient = new ESClient();
    final String fullAddress = esClient.getServerAddress(server);
    final String expectedAddress = ipAddress + StringPool.COLON + port;

    assertEquals(expectedAddress, fullAddress);
  }
}
