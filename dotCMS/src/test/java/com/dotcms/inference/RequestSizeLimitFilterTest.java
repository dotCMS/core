package com.dotcms.inference;

import com.dotcms.inference.model.InferenceLimits;
import com.dotcms.inference.rest.RequestSizeLimitFilter;
import com.dotcms.inference.rest.view.InferenceErrorView;
import com.dotmarketing.util.Config;
import org.junit.After;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the request-size ceiling, including the case that used to skip it entirely.
 *
 * <p>A body that declares its length is easy: the filter compares one number. A body that does
 * not — chunked, or simply streamed by a client that omits {@code Content-Length}, which several
 * HTTP clients do by default — has no number to compare, and an earlier version of the filter
 * returned at that point with a comment saying the resource enforced the ceiling as it read. No
 * resource did. The single control on body size could be skipped by leaving out one header, and
 * the comment is what kept anyone from noticing.</p>
 *
 * <p>These tests exist to keep that from coming back: the undeclared path is exercised here in
 * exactly the way the declared one is, so the two cannot drift apart again.</p>
 */
public class RequestSizeLimitFilterTest {

    private final RequestSizeLimitFilter filter = new RequestSizeLimitFilter();

    @After
    public void after() {
        Config.setProperty(InferenceLimits.MAX_REQUEST_BYTES_KEY, null);
    }

    /**
     * Given a request that declares a length over the ceiling
     * When the filter runs
     * Then it is refused with a typed 413 before the body is read at all
     */
    @Test
    public void test_declaredOverSizeBody_isRefusedBeforeItIsRead() throws IOException {
        Config.setProperty(InferenceLimits.MAX_REQUEST_BYTES_KEY, "10");

        final ContainerRequestContext context = mock(ContainerRequestContext.class);
        when(context.getLength()).thenReturn(64);
        final AtomicReference<Response> aborted = new AtomicReference<>();
        doAnswer(invocation -> {
            aborted.set(invocation.getArgument(0));
            return null;
        }).when(context).abortWith(any());

        filter.filter(context);

        assertNotNull("A body that announces it is too large must be refused on the announcement, "
                + "not after it has been read into memory", aborted.get());
        assertEquals(413, aborted.get().getStatus());
        assertTrue(aborted.get().getEntity() instanceof InferenceErrorView);
    }

    /**
     * Given a request that declares no length and stays under the ceiling
     * When its body is read
     * Then it is delivered untouched
     *
     * <p>The ordinary case, and the one a wrapper most easily breaks: every streamed request with
     * no {@code Content-Length} now passes through this stream, so a bug here would refuse valid
     * traffic rather than merely fail to refuse invalid traffic.</p>
     */
    @Test
    public void test_undeclaredLengthUnderTheCeiling_passesThroughIntact() throws IOException {
        Config.setProperty(InferenceLimits.MAX_REQUEST_BYTES_KEY, "1024");

        final byte[] body = "{\"model\":\"gpt-4o\"}".getBytes();
        final InputStream wrapped = wrapUndeclared(new ByteArrayInputStream(body));

        assertEquals("The body must arrive byte for byte", new String(body),
                new String(wrapped.readAllBytes()));
    }

    /**
     * Given a request that declares no length and runs past the ceiling
     * When its body is read
     * Then it is refused mid-read with the same typed 413 the declared path returns
     *
     * <p>This is the hole. Before the fix the same request was accepted in full, because the
     * filter had no length to check and nothing downstream checked either. Refusing mid-read
     * rather than measuring first is deliberate: buffering the body to size it would hand over
     * exactly the memory the ceiling exists to bound.</p>
     */
    @Test
    public void test_undeclaredLengthOverTheCeiling_isRefusedMidRead() throws IOException {
        Config.setProperty(InferenceLimits.MAX_REQUEST_BYTES_KEY, "16");

        final InputStream wrapped = wrapUndeclared(new ByteArrayInputStream(new byte[4096]));

        final WebApplicationException refused =
                assertThrows("A body with no declared length must still be bounded",
                        WebApplicationException.class, wrapped::readAllBytes);

        assertEquals("The refusal must be the same 413 a declared over-size body gets, so a "
                        + "client cannot tell which route refused it",
                413, refused.getResponse().getStatus());
        assertTrue(refused.getResponse().getEntity() instanceof InferenceErrorView);
    }

    /**
     * Given a request that declares no length
     * When the filter runs
     * Then the request is not aborted outright — it is allowed to proceed under a counted stream
     *
     * <p>Refusing every undeclared-length body would also close the hole, and would break every
     * client that streams its request. The fix has to bound the body without rejecting the shape.</p>
     */
    @Test
    public void test_undeclaredLength_isNotRefusedOutright() throws IOException {
        Config.setProperty(InferenceLimits.MAX_REQUEST_BYTES_KEY, "1024");

        final ContainerRequestContext context = mock(ContainerRequestContext.class);
        when(context.getLength()).thenReturn(-1);
        when(context.getEntityStream()).thenReturn(new ByteArrayInputStream(new byte[8]));

        filter.filter(context);

        verify(context, never()).abortWith(any());
        verify(context).setEntityStream(any());
    }

    /**
     * @param body the raw body an undeclared-length request would carry
     * @return the stream the filter installs in place of it
     */
    private InputStream wrapUndeclared(final InputStream body) throws IOException {
        final ContainerRequestContext context = mock(ContainerRequestContext.class);
        when(context.getLength()).thenReturn(-1);
        when(context.getEntityStream()).thenReturn(body);

        final AtomicReference<InputStream> installed = new AtomicReference<>();
        doAnswer(invocation -> {
            installed.set(invocation.getArgument(0));
            return null;
        }).when(context).setEntityStream(any());

        filter.filter(context);

        assertNotNull("The filter must install a counting stream when no length is declared",
                installed.get());
        return installed.get();
    }
}
