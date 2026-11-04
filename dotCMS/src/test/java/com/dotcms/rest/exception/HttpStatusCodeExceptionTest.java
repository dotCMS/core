package com.dotcms.rest.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The message of a status-code exception is often built by the caller and may carry
 * user-supplied text such as a URL; it must reach the response untouched.
 */
public class HttpStatusCodeExceptionTest {

    /**
     * A message with percent-encoded characters and no format arguments is kept as is.
     */
    @Test
    public void messageWithPercentSigns_isNotFormatted() {
        final String message = "Got invalid response for url: [https://a.test/r?u=http%3A%2F%2Fb.test] response: [302]";

        final BadRequestException exception = new BadRequestException(message);

        assertEquals(400, exception.getResponse().getStatus());
        assertTrue(String.valueOf(exception.getResponse().getEntity()).contains("http%3A%2F%2Fb.test"));
    }

    /**
     * A message with format arguments is still formatted.
     */
    @Test
    public void messageWithArguments_isFormatted() {
        final BadRequestException exception = new BadRequestException("Missing %s", "name");

        assertTrue(String.valueOf(exception.getResponse().getEntity()).contains("Missing name"));
    }
}
