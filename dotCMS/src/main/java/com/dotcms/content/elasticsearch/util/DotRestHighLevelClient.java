package com.dotcms.content.elasticsearch.util;

import java.io.IOException;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public class DotRestHighLevelClient {

    private DotRestHighLevelClient() {}

    private static class LazyHolder {
        static final RestHighLevelClient INSTANCE = new RestHighLevelClient(
                RestClient.builder(
                new HttpHost("localhost", 9201, "http")));
    }

    public static RestHighLevelClient getClient() {
        return LazyHolder.INSTANCE;
    }

    public static void close() throws IOException {
        LazyHolder.INSTANCE.close();
    }

}
