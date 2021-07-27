package com.dotcms.http;

import com.dotcms.rest.exception.BadRequestException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.output.NullOutputStream;
import org.junit.Test;

import com.dotcms.http.CircuitBreakerUrl.Method;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;

import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.CircuitBreakerOpenException;


public class CircuitBreakerUrlTest {

    final static String goodUrl = "https://dotcms.com";
    final static String badUrl = "https://localhost:9999/test";

    final static String HEADER="X-MY-HEADER";
    final static String HEADER_VALUE="SEEMS TO BE WORKING";
    final static String PARAM="X-MY-PARAM";
    final static String PARAM_VALUE="PARAM SEEMS TO BE WORKING";
    
    @Test
    public void testGoodBreaker() throws Exception {


        final NullOutputStream nos = new NullOutputStream();

        final String key = "testBreaker";
        final int timeout = 2000;

        CircuitBreaker breaker = CurcuitBreakerPool.getBreaker(key);

        assert (breaker.isClosed());

        for (int i = 0; i < 10; i++) {

            CircuitBreakerUrl cburl = CircuitBreakerUrl.builder().setUrl(goodUrl).setTimeout(timeout).setCircuitBreaker(breaker).build();

            cburl.doOut(nos);
        }
        breaker = CurcuitBreakerPool.getBreaker(key);
        assert (breaker.isClosed());
    }


    @Test
    public void testBadBreaker() throws ExecutionException {


        final NullOutputStream nos = new NullOutputStream();

        final String key = "testBadBreaker";
        final int timeout = 2000;

        CircuitBreaker breaker = CurcuitBreakerPool.getBreaker(key);
        assert (breaker.isClosed());

        for (int i = 0; i < 10; i++) {
            try {
                new CircuitBreakerUrl(badUrl, timeout, breaker).doOut(nos);
            } catch (Exception e) {
                assert (e instanceof CircuitBreakerOpenException);
            }
        }
        breaker = CurcuitBreakerPool.getBreaker(key);

        assert (breaker.isOpen());
    }

    /*
     * This requires 
     * http://httpbin.org
     * which can be run via docker
     * docker run -p 80:80 kennethreitz/httpbin
     */


    //@Test
    public void testHeaders() throws ExecutionException, CircuitBreakerOpenException, IOException {
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
    public void testPost() throws ExecutionException, CircuitBreakerOpenException, IOException {
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


        final NullOutputStream nos = new NullOutputStream();


        final String key = "testRecoveryBreaker";
        final int timeout = 2000;

        CircuitBreaker breaker = CurcuitBreakerPool.getBreaker(key);
        breaker.withDelay(5, TimeUnit.SECONDS);
        assert (breaker.isClosed());


        for (int i = 0; i < breaker.getSuccessThreshold().denominator; i++) {
            try {
                new CircuitBreakerUrl(goodUrl, timeout, breaker).doOut(nos);
            } catch (Exception e) {
                // shoud not be here
                assert (false);
            }
        }

        assert (breaker.isClosed());

        for (int i = 0; i < breaker.getFailureThreshold().denominator; i++) {
            try {
                new CircuitBreakerUrl(badUrl, timeout, breaker).doOut(nos);
            } catch (Exception e) {
                assert (e instanceof CircuitBreakerOpenException);
            }
        }
        assert (breaker.isOpen());
        for (int i = 0; i < breaker.getFailureThreshold().denominator; i++) {
            try {
                new CircuitBreakerUrl(badUrl, timeout, breaker).doOut(nos);
            } catch (CircuitBreakerOpenException e) {
                assert (e instanceof CircuitBreakerOpenException);
            }
        }
        Thread.sleep(breaker.getDelay().toMillis() + 1000);

        try {
            new CircuitBreakerUrl(goodUrl, timeout, breaker).doOut(nos);
        } catch (Exception e) {
            // shoud not be here
            assert (false);
        }

        assert (breaker.isHalfOpen());

        for (int i = 0; i < breaker.getSuccessThreshold().denominator; i++) {
            try {
                new CircuitBreakerUrl(goodUrl, timeout, breaker).doOut(nos);
            } catch (Exception e) {
                // shoud not be here
                assert (false);
            }
        }

        assert (breaker.isClosed());
    }

    @Test
    public void testBreakerPool() {


        final String key = UUIDUtil.uuid();
        final int success = 500;

        CircuitBreaker breaker = CurcuitBreakerPool.getBreaker(key);
        assert (breaker.isClosed());
        breaker.withSuccessThreshold(success);
        assert (breaker.getSuccessThreshold().denominator == success);

        CircuitBreaker breaker2 = CurcuitBreakerPool.getBreaker(key);

        assert (breaker.equals(breaker2));
        assert (breaker2.getSuccessThreshold().denominator == success);

    }

    @Test
    public void testToString() throws Exception {

        final String key = "testBreaker";
        final int timeout = 2000;

        CircuitBreaker breaker = CurcuitBreakerPool.getBreaker(key);
        assert (breaker.isClosed());

        for (int i = 0; i < 10; i++) {
            String x = new CircuitBreakerUrl(goodUrl, timeout, breaker).doString();
            assert (x.length() > 100);
        }


    }

    public void testMemory() throws Exception {
        System.gc();

        Thread.sleep(3000);


        long startMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        final NullOutputStream nos = new NullOutputStream();


        final String key = "testBreaker";
        final int timeout = 2000;

        CircuitBreaker breaker = CurcuitBreakerPool.getBreaker(key);
        assert (breaker.isClosed());

        for (int i = 0; i < 1000; i++) {
            new CircuitBreakerUrl(goodUrl, timeout, breaker).doOut(nos);
        }
        breaker = CurcuitBreakerPool.getBreaker(key);
        assert (breaker.isClosed());
        CurcuitBreakerPool.flushPool();
        System.gc();
        Thread.sleep(3000);
        long endMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        System.out.println("start:" + UtilMethods.prettyMemory(startMem));
        System.out.println("end  :" + UtilMethods.prettyMemory(endMem));
        System.out.println("diff :" + UtilMethods.prettyMemory(endMem - startMem));
    }

    @Test
    public void testGet() {
        for (int i = 0; i < 10; i++) {
            try {
                String x = new CircuitBreakerUrl(goodUrl, 2000).doString();
                assert (x.contains("Java"));
                assert (x.contains("/application/themes/dotcms/js/bootstrap.min.js"));

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
        final int timeout = 2000;

        CircuitBreaker breaker = CurcuitBreakerPool.getBreaker(key);

        assert (breaker.isClosed());

        CircuitBreakerUrl cburl = CircuitBreakerUrl.builder().setUrl("http://sdsfsf.com")
                .setMethod(Method.POST).setTimeout(timeout).setCircuitBreaker(breaker).build();
        cburl.doOut(nos);
    }

}
