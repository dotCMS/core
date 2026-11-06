package com.dotcms.content.elasticsearch.util;

import static org.junit.Assert.*;

import com.dotcms.content.index.SearchAdmission;
import com.dotcms.content.index.SearchUnavailableException;
import com.dotmarketing.util.Config;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Uses loopback HTTP, not an external cluster, to test cancellation during execution and leasing. */
public class SearchTransportTest {
    private final Map<String, Object> previous = new HashMap<>();
    private final CountDownLatch entered = new CountDownLatch(1);
    private final CountDownLatch release = new CountDownLatch(1);
    private final ExecutorService serverThreads = Executors.newFixedThreadPool(2);
    private HttpServer server;
    private RestHighLevelClient client;

    /** Holds one HTTP response open so the configured one-connection pool is genuinely exhausted. */
    @Before
    public void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setExecutor(serverThreads);
        server.createContext("/", exchange -> {
            entered.countDown();
            try {
                release.await(10, TimeUnit.SECONDS);
                final byte[] body = "{\"count\":0,\"_shards\":{\"total\":1,\"successful\":1,\"failed\":0}}".getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        property("ES_ENDPOINTS", "http://localhost:" + server.getAddress().getPort());
        property("ES_TLS_ENABLED", false);
        property("ES_AUTH_TYPE", "BASIC");
        property("ES_MAX_CONNECTIONS", 1);
        property("ES_MAX_CONNECTIONS_PER_ROUTE", 1);
        property("ES_CONNECTION_REQUEST_TIMEOUT", 25);
        property("ES_SOCKET_TIMEOUT", 5000);
        client = new DotRestHighLevelClientProvider().getClient();
    }

    /** Always releases loopback handlers, closes I/O reactors and restores global config. */
    @After
    public void cleanup() throws Exception {
        release.countDown();
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.stop(0);
        }
        serverThreads.shutdownNow();
        assertTrue(serverThreads.awaitTermination(5, TimeUnit.SECONDS));
        previous.forEach(Config::setProperty);
    }

    /** The hard deadline also bounds a request queued behind an occupied transport connection. */
    @Test(timeout = 15000)
    public void deadlineAlsoBoundsConnectionAcquisition() throws Exception {
        final CompletableFuture<CountResponse> first = count();
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        final SimpleMeterRegistry registry = new SimpleMeterRegistry();
        try {
            final SearchAdmission admission = new SearchAdmission(1, 200, "test", registry);
            assertEquals(SearchUnavailableException.Reason.DEADLINE,
                    assertThrows(SearchUnavailableException.class,
                            () -> admission.<CountResponse>execute(completion ->
                                    client.countAsync(new CountRequest("working"), RequestOptions.DEFAULT,
                                            ActionListener.wrap(completion::complete,
                                                    completion::completeExceptionally))::cancel)).reason());
            assertFalse(first.isDone());
            org.awaitility.Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() ->
                    registry.get("dotcms.search.admission.active").gauge().value() == 0);
        } finally {
            registry.close();
        }
    }

    /** Caller timeout must cancel real HTTP I/O and eventually return its admission permit. */
    @Test(timeout = 15000)
    public void deadlineCancelsRealTransport() throws Exception {
        final SimpleMeterRegistry registry = new SimpleMeterRegistry();
        try {
            final SearchAdmission admission = new SearchAdmission(1, 200, "test", registry);
            final CountDownLatch transportCompleted = new CountDownLatch(1);
            assertEquals(SearchUnavailableException.Reason.DEADLINE,
                    assertThrows(SearchUnavailableException.class,
                            () -> admission.<CountResponse>execute(completion -> {
                                completion.whenComplete((result, failure) -> transportCompleted.countDown());
                                return client.countAsync(new CountRequest("working"), RequestOptions.DEFAULT,
                                        ActionListener.wrap(completion::complete,
                                                completion::completeExceptionally))::cancel;
                            })).reason());
            assertTrue(transportCompleted.await(5, TimeUnit.SECONDS));
            org.awaitility.Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() ->
                    registry.get("dotcms.search.admission.active").gauge().value() == 0);
            assertEquals(Integer.valueOf(1), admission.execute(completion -> {
                completion.complete(1);
                return () -> { };
            }));
        } finally {
            registry.close();
        }
    }

    /** Starts a real request using the configured provider without going through admission. */
    private CompletableFuture<CountResponse> count() {
        final CompletableFuture<CountResponse> result = new CompletableFuture<>();
        client.countAsync(new CountRequest("working"), RequestOptions.DEFAULT,
                ActionListener.wrap(result::complete, result::completeExceptionally));
        return result;
    }

    /** Captures original values so tests never leave their transport settings behind. */
    private void property(final String name, final Object value) {
        previous.put(name, Config.getStringProperty(name, null));
        Config.setProperty(name, value);
    }
}
