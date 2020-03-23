package com.dotcms.content.elasticsearch.util;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import okhttp3.Authenticator;
import okhttp3.ConnectionPool;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;

/**
 * Custom ES client that connects to the ES server using Http2
 * @author nollymar
 */
public class NativeHttpRestClient {

    private static OkHttpClient client;
    private static String url;

    public NativeHttpRestClient(final SSLContext sslContext) {

        final String protocol = sslContext != null ? "https://" : "http://";

        Builder builder = new OkHttpClient.Builder()
                .authenticator(new Authenticator() {
                    @Override public Request authenticate(Route route, Response response) {
                        if (response.request().header("Authorization") != null) {
                            return null; // Give up, we've already attempted to authenticate.
                        }

                        final String credential = Credentials.basic(
                                Config.getStringProperty("ES_AUTH_BASIC_USER", null),
                                Config.getStringProperty("ES_AUTH_BASIC_PASSWORD", null));

                        return response.request().newBuilder()
                                .header("Authorization", credential)
                                .build();
                    }
                })
                .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES));


        if (sslContext != null){
            builder.sslSocketFactory(sslContext.getSocketFactory());
        }
        client = builder.build();

        NativeHttpRestClient.url =
                protocol + Config.getStringProperty("ES_HOSTNAME", "127.0.0.1") + ":" + Config
                        .getIntProperty("ES_PORT", 9200);


    }

    /**
     * Performs searches over the ES server
     * @param searchRequest
     * @param options
     * @return
     */
    public SearchResponse search(final SearchRequest searchRequest, final RequestOptions options) {

        final MediaType MEDIA_TYPE_JSON
                = MediaType.parse("application/json; charset=utf-8");

        final Request request = new Request.Builder()
                .url(url  + "/" + searchRequest.indices()[0] + "/_search")
                .post(RequestBody.create(MEDIA_TYPE_JSON, searchRequest.source().toString()))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            return SearchResponseConverter.parseEntity(response.body(), SearchResponse::fromXContent);

        } catch (Exception e) {
            Logger.warnAndDebug(NativeHttpRestClient.class, e.getCause().getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }
}
