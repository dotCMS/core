package com.dotcms.cost;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/** Failure-boundary checks that do not need a live database or production endpoint. */
public class RequestCostOutboxTest {

    @Test
    public void test_retryDelayIsBoundedAndPermanentRejectionsAreSlow() {
        assertEquals(5, RequestCostOutbox.retryDelaySeconds(0, 503));
        assertEquals(10, RequestCostOutbox.retryDelaySeconds(1, -1));
        assertEquals(900, RequestCostOutbox.retryDelaySeconds(Integer.MAX_VALUE, 503));
        for (final int status : List.of(400, 401, 403, 404)) {
            assertEquals(900, RequestCostOutbox.retryDelaySeconds(0, status));
        }
        assertEquals(5, RequestCostOutbox.retryDelaySeconds(0, 429));
        assertEquals(5, RequestCostOutbox.retryDelaySeconds(0, 408));
    }

    @Test
    public void test_onlyCompletionStatusAcknowledges() {
        assertTrue(RequestCostOutbox.isCompletionAck(201));
        for (final int status : List.of(-1, 200, 202, 204, 301, 307, 400, 401, 429, 503)) {
            assertFalse(RequestCostOutbox.isCompletionAck(status));
        }
    }

    @Test
    public void test_oversizedPayloadRefusesAdmissionBeforeDatabaseAccess() throws Exception {
        assertFalse(new RequestCostOutbox().add("https://example.com/cost",
                "x".repeat(RequestCostOutbox.MAX_PAYLOAD_BYTES + 1)));
        assertFalse(new RequestCostOutbox().add("https://example.com/cost",
                "é".repeat(RequestCostOutbox.MAX_PAYLOAD_BYTES)));
    }
}
