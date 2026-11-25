package com.dotcms.saml.utils;

import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.opensaml.xml.util.Base64;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

/** Checks that adapting the legacy decoder's diagnostics does not change its contract. */
public class TestBase64Compatibility {

    /** Valid payloads retain the existing encode/decode round-trip behavior. */
    @Test
    public void validPayloadRoundTrips() {
        final byte[] payload = "SAML decoder compatibility".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(payload, Base64.decode(Base64.encodeBytes(payload)));
    }

    /** Invalid input remains rejected rather than being accepted by the diagnostic change. */
    @Test
    public void invalidCharacterIsRejected() {
        final byte[] invalid = "!AAA".getBytes(StandardCharsets.US_ASCII);
        assertNull(Base64.decode(invalid, 0, invalid.length));
    }
}
