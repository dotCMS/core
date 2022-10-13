package com.dotcms.mock.response;

import com.dotcms.UnitTestBase;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotmarketing.util.DateUtil;
import com.google.common.collect.ImmutableList;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class MockAsyncResponseTest extends UnitTestBase {

    @SuppressWarnings("unchecked")
    @Ignore
    @Test
    public void testDeleteScheme() throws Exception {

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicReference<Response> response = new AtomicReference<>();
        final AsyncResponse asyncResponse = new MockAsyncResponse((arg) -> {

            response.set ((Response)arg);
            countDownLatch.countDown();
            return true;
        }, arg -> {

            countDownLatch.countDown();
            fail("Error on deleting step");
            return true;
        });

        new TestResource().test(asyncResponse);

        try {

            countDownLatch.await(10, TimeUnit.MINUTES);
            assertNotNull(response);
            assertEquals(Response.Status.OK.getStatusCode(), response.get().getStatus());
            assertNotNull(response.get().getEntity());
            assertTrue(response.get().getEntity() instanceof ResponseEntityView);
            assertTrue(ResponseEntityView.class.cast(response.get().getEntity()).getEntity() instanceof List);
            assertEquals(220, List.class.cast(ResponseEntityView.class.cast(response.get().getEntity()).getEntity()).size());
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }

    static class TestResource {

        public final void test(@Suspended final AsyncResponse asyncResponse) {

            try {

                ResponseUtil.handleAsyncResponse(
                        this.veryExpensiveTask(), asyncResponse);
            } catch (Exception e) {
                asyncResponse.resume(ResponseUtil.mapExceptionResponse(e));
            }
        }

        private Future<Object> veryExpensiveTask() {

            final DotSubmitter dotSubmitter = DotConcurrentFactory.getInstance()
                    .getSubmitter(DotConcurrentFactory.DOT_SYSTEM_THREAD_POOL);

            return dotSubmitter.submit(this::fireTasks);
        }

        private List<String> fireTasks() {

            final CopyOnWriteArrayList<String> results = new CopyOnWriteArrayList<>();
            final List<Future> futures = new ArrayList<>();
            final DotSubmitter dotSubmitter = DotConcurrentFactory.getInstance()
                    .getSubmitter(DotConcurrentFactory.DOT_SYSTEM_THREAD_POOL);

            for (int i = 0; i <= 10; ++i) {

                IntStream.rangeClosed(1, 20).forEach(number ->
                        futures.add(dotSubmitter.submit(() -> {

                            DateUtil.sleep(100 * number);
                            results.add(String.valueOf(100 * number));
                        })));

                for (final Future future : futures) {

                    try {
                        future.get();
                    } catch (Exception e) {}
                }

                futures.clear();
            }

            return ImmutableList.copyOf(results);
        }
    }

}
