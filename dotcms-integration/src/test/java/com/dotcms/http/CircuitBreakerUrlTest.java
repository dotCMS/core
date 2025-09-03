package com.dotcms.http;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.util.network.IPUtils;
import com.dotmarketing.exception.DotRuntimeException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import com.dotmarketing.util.DateUtil;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.dotcms.http.CircuitBreakerUrl.Method;
import com.dotmarketing.util.UUIDUtil;
import com.google.common.collect.ImmutableMap;

import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.CircuitBreakerOpenException;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CircuitBreakerUrlTest {

    private static final String GOOD_URL = "https://www.dotcms.com";
    private static final String REDIRECT_URL = "http://www.dotcms.com"; // this will redirect to https
    private static final String BAD_URL = "https://localhost:9999/test";
    private static final String HEADER = "X-MY-HEADER";
    private static final String HEADER_VALUE = "SEEMS TO BE WORKING";
    private static final String PARAM = "X-MY-PARAM";
    private static final String PARAM_VALUE = "PARAM SEEMS TO BE WORKING";

    private CircuitBreakerUrl.Response<?> response;

    @Before
    public void setup() {
        response = mock(CircuitBreakerUrl.Response.class);
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_OK);
    }

    @Test()
    public void test_circuitBreakerConnectionControl() {

        final DotSubmitter dotSubmitter = DotConcurrentFactory.getInstance().getSubmitter();
        final CircuitBreakerUrl.CircuitBreakerConnectionControl circuitBreakerConnectionControl =
                new CircuitBreakerUrl.CircuitBreakerConnectionControl(3);
        final List<Future<Boolean>> threads = new ArrayList<>();

        for (int i = 0; i < 10; ++i) {

            threads.add(dotSubmitter.submit(()-> {

                circuitBreakerConnectionControl.check("test");
                try {

                    circuitBreakerConnectionControl.start(Thread.currentThread().getId());
                    DateUtil.sleep(1000);
                    return true;
                }  finally {
                    circuitBreakerConnectionControl.end(Thread.currentThread().getId());
                }
            }));
        }

        try {
            for (Future<Boolean> future : threads) {

                future.get();
            }
        }catch (Exception e) {

            Assert.assertTrue(ExceptionUtil.causedBy(e, RejectedExecutionException.class));
            return;
        }

        Assert.fail("Not reject when reach the max");
    }

    @Test
    public void testGoodBreaker() throws Exception {

        final NullOutputStream nos = new NullOutputStream();

        final String key = "testBreaker";
        final int timeout = 2000;

        CircuitBreaker breaker = CircuitBreakerPool.getBreaker(key);

        assert (breaker.isClosed());

        for (int i = 0; i < 10; i++) {

            CircuitBreakerUrl cburl = CircuitBreakerUrl.builder().setUrl(GOOD_URL).setTimeout(timeout).setCircuitBreaker(breaker).build();

            cburl.doOut(nos);
        }
        breaker = CircuitBreakerPool.getBreaker(key);
        assert (breaker.isClosed());
    }

    @Test
    public void testBadBreaker() {

        try {
            final NullOutputStream nos = new NullOutputStream();

            final String key = "testBadBreaker";
            final int timeout = 2000;

            CircuitBreaker breaker = CircuitBreakerPool.getBreaker(key);
            assert (breaker.isClosed());

            IPUtils.disabledIpPrivateSubnet(true);

            for (int i = 0; i < 10; i++) {
                try {
                    new CircuitBreakerUrl(BAD_URL, timeout, breaker).doOut(nos);
                } catch (Exception e) {
                    assert (e instanceof CircuitBreakerOpenException);
                }
            }
            breaker = CircuitBreakerPool.getBreaker(key);

            assert (breaker.isOpen());
        }finally {
            IPUtils.disabledIpPrivateSubnet(false);

        }
    }

    @Test
    public void test_breaker_url_using_private_ip_throws_an_exception()  {
            final NullOutputStream nos = new NullOutputStream();

            final String key = "testPrivateIP";
            final int timeout = 2000;

            CircuitBreaker breaker = CircuitBreakerPool.getBreaker(key);
            assert (breaker.isClosed());

            try {
                new CircuitBreakerUrl(BAD_URL, timeout, breaker).doOut(nos);
            } catch (Exception e) {
                assert (e instanceof DotRuntimeException);
                assert (e.getMessage().contains("Remote HttpRequests cannot access private subnets"));
            }
    }

    /*
     * This requires 
     * http://httpbin.org
     * which can be run via docker
     * docker run -p 80:80 kennethreitz/httpbin
     */
    //@Test
    public void testHeaders() throws CircuitBreakerOpenException, IOException {
        Map<String, String> headers = ImmutableMap.of(HEADER, HEADER_VALUE);

        CircuitBreakerUrl cburl = CircuitBreakerUrl.builder().setMethod(Method.GET).setUrl("http://localhost/get").setTimeout(1000)
                .setHeaders(headers).build();

        for (int i = 0; i < 10; i++) {
            String x = cburl.doString();
            assert (x.contains(HEADER_VALUE));
        }
    }
    
    /*
     * This requires 
     * http://httpbin.org
     * which can be run via docker
     * docker run -p 80:80 kennethreitz/httpbin
     */
    //@Test
    public void testPost() throws CircuitBreakerOpenException, IOException {
        Map<String, String> params = ImmutableMap.of(PARAM, PARAM_VALUE);

        CircuitBreakerUrl cburl = CircuitBreakerUrl.builder().setMethod(Method.POST).setUrl("http://localhost/post").setTimeout(1000)
                .setParams(params).build();


        for (int i = 0; i < 10; i++) {
            String x = cburl.doString();
            assert (x.contains(PARAM_VALUE));
        }
    }

    /*
     * @BeforeClass public static void prepare() throws Exception{ //Setting web app environment
     * IntegrationTestInitService.getInstance().init(); }
     */
    @Test
    public void testRecovery() throws  InterruptedException, IOException {

        IPUtils.disabledIpPrivateSubnet(true);

        try {
            final NullOutputStream nos = new NullOutputStream();

            final String key = "testRecoveryBreaker";
            final int timeout = 2000;

            CircuitBreaker breaker = CircuitBreakerPool.getBreaker(key);
            breaker.withDelay(5, TimeUnit.SECONDS);
            assert (breaker.isClosed());

            for (int i = 0; i < breaker.getSuccessThreshold().denominator; i++) {
                try {
                    new CircuitBreakerUrl(GOOD_URL, timeout, breaker).doOut(nos);
                } catch (Exception e) {
                    // shoud not be here
                    assert (false);
                }
            }

            assert (breaker.isClosed());

            for (int i = 0; i < breaker.getFailureThreshold().denominator; i++) {
                try {
                    new CircuitBreakerUrl(BAD_URL, timeout, breaker).doOut(nos);
                } catch (Exception e) {
                    assert (e instanceof CircuitBreakerOpenException);
                }
            }
            assert (breaker.isOpen());
            for (int i = 0; i < breaker.getFailureThreshold().denominator; i++) {
                try {
                    new CircuitBreakerUrl(BAD_URL, timeout, breaker).doOut(nos);
                } catch (CircuitBreakerOpenException e) {
                    assert (e instanceof CircuitBreakerOpenException);
                }
            }
            Thread.sleep(breaker.getDelay().toMillis() + 1000);

            try {
                new CircuitBreakerUrl(GOOD_URL, timeout, breaker).doOut(nos);
            } catch (Exception e) {
                // shoud not be here
                assert (false);
            }

            assert (breaker.isHalfOpen());

            for (int i = 0; i < breaker.getSuccessThreshold().denominator; i++) {
                try {
                    new CircuitBreakerUrl(GOOD_URL, timeout, breaker).doOut(nos);
                } catch (Exception e) {
                    // shoud not be here
                    assert (false);
                }
            }

            assert (breaker.isClosed());
        }finally {
            IPUtils.disabledIpPrivateSubnet(false);
        }
    }

    @Test
    public void testBreakerPool() {

        final String key = UUIDUtil.uuid();
        final int success = 500;

        CircuitBreaker breaker = CircuitBreakerPool.getBreaker(key);
        assert (breaker.isClosed());
        breaker.withSuccessThreshold(success);
        assert (breaker.getSuccessThreshold().denominator == success);

        CircuitBreaker breaker2 = CircuitBreakerPool.getBreaker(key);

        assert (breaker.equals(breaker2));
        assert (breaker2.getSuccessThreshold().denominator == success);
    }

    @Test
    public void testToString() throws Exception {

        final String key = "testBreaker";
        final int timeout = 2000;

        CircuitBreaker breaker = CircuitBreakerPool.getBreaker(key);
        assert (breaker.isClosed());

        for (int i = 0; i < 10; i++) {
            String x = new CircuitBreakerUrl(GOOD_URL, timeout, breaker).doString();
            assert (x.length() > 100);
        }
    }

    @Test(expected = BadRequestException.class)
    public void disallowRedirects() throws Exception {

        final String key = "testBreaker";
        final int timeout = 2000;

        CircuitBreaker breaker = CircuitBreakerPool.getBreaker(key);
        assert (breaker.isClosed());


        new CircuitBreakerUrl(REDIRECT_URL, timeout, breaker).doString();
    }

    @Test
    public void testGet() {
        for (int i = 0; i < 10; i++) {
            try {
                String x = new CircuitBreakerUrl(GOOD_URL, 2000).doString();
                assert (x.contains("content=\"dotCMS\""));

            } catch (Exception e) {
                assert (e instanceof CircuitBreakerOpenException);
            }
        }
    }

    /**
     * Method to test: {@link CircuitBreakerUrl#doOut(OutputStream)}
     * Given scenario: Invoke {@link CircuitBreakerUrl#doOut(OutputStream)} using a bad request
     * Expected Result: {@link BadRequestException}
     */
    @Test(expected = BadRequestException.class)
    public void testBadRequest() throws Exception {
        final NullOutputStream nos = new NullOutputStream();

        final String key = "testBreaker";
        final int timeout = 10000;
        CircuitBreaker breaker = CircuitBreakerPool.getBreaker(key);

        assert (breaker.isClosed());

        CircuitBreakerUrl cburl =
                CircuitBreakerUrl.builder()
                        // Returns 400 Bad Request
                        .setUrl("https://httpbin.org/status/400")
                        .setMethod(Method.GET)
                        .setTimeout(timeout)
                        .setCircuitBreaker(breaker)
                        .build();
        cburl.doOut(nos);
    }

    /**
     * Method to test: {@link CircuitBreakerUrl#doOut(OutputStream)}
     * Given scenario: Invoke {@link CircuitBreakerUrl#doOut(OutputStream)} using a bad request
     * Expected Result: http status should be evaluated
     */
    @Test
    @Ignore("sdsfsf.com is an unknown host and will not return a bad request, need to refactor")
    public void testBadRequest_dontThrow() throws Exception {
        final NullOutputStream nos = new NullOutputStream();

        final String key = "testBreaker";
        final int timeout = 2000;

        CircuitBreaker breaker = CircuitBreakerPool.getBreaker(key);

        assert (breaker.isClosed());

        CircuitBreakerUrl cburl = CircuitBreakerUrl.builder()
                // Returns 400 Bad Request
            .setUrl("https://run.mocky.io/v3/434b4b0e-9a8b-4895-88e3-beab1b8d0a0d")
            .setMethod(Method.POST)
            .setTimeout(timeout).setCircuitBreaker(breaker)
            .setThrowWhenError(false)
            .build();
        cburl.doOut(nos);

        assert (cburl.response() >= 400 && cburl.response() <= 499);
    }

    /**
     * Given an int status code
     * Then evaluate it does have a SUCCESS http status
     */
    @Test
    public void test_isSuccessStatusCode() {
        assertTrue(CircuitBreakerUrl.isSuccessResponse(HttpStatus.SC_ACCEPTED));
        assertTrue(CircuitBreakerUrl.isSuccessResponse(HttpStatus.SC_OK));
        assertFalse(CircuitBreakerUrl.isSuccessResponse(HttpStatus.SC_BAD_REQUEST));
        assertFalse(CircuitBreakerUrl.isSuccessResponse(HttpStatus.SC_FORBIDDEN));
        assertFalse(CircuitBreakerUrl.isSuccessResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR));
    }

    /**
     * Given a {@link Response}
     * Then evaluate it does have a SUCCESS http status
     */
    @Test
    public void test_isSuccessResponse() {
        assertTrue(CircuitBreakerUrl.isSuccessResponse(response));
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_ACCEPTED);
        assertTrue(CircuitBreakerUrl.isSuccessResponse(response));
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        assertFalse(CircuitBreakerUrl.isSuccessResponse(response));
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_FORBIDDEN);
        assertFalse(CircuitBreakerUrl.isSuccessResponse(response));
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        assertFalse(CircuitBreakerUrl.isSuccessResponse(response));
    }
}
