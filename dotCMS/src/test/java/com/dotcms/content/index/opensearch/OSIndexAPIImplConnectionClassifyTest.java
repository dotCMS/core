package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.dotcms.content.index.opensearch.OSIndexAPIImpl.ConnectionFailureKind;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.json.JSONException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;
import org.junit.Test;

/**
 * Unit tests for {@link OSIndexAPIImpl#classifyConnectionError(Throwable)} — the OpenSearch
 * connection-error classification hardening (issue #36244 follow-up).
 *
 * <p>Pure function, no container: the classifier walks the exception chain and matches on exception
 * simple-name and message text, so synthetic exception chains are sufficient to exercise every
 * branch. Same package as {@link OSIndexAPIImpl} to reach the package-private classifier and
 * {@link ConnectionFailureKind}.</p>
 *
 * @author Fabrizzio Araya
 */
public class OSIndexAPIImplConnectionClassifyTest {

    /** Synthetic type whose simple name matches Apache HttpClient's transport exception. */
    private static final class ConnectionClosedException extends IOException {
        ConnectionClosedException(final String message) {
            super(message);
        }
    }

    private static ConnectionFailureKind classify(final Throwable t) {
        return OSIndexAPIImpl.classifyConnectionError(t);
    }

    // ---- TLS / scheme mismatch ------------------------------------------------------------------

    @Test
    public void sslException_isTlsSchemeMismatch() {
        assertEquals(ConnectionFailureKind.TLS_SCHEME_MISMATCH,
                classify(new SSLException("Unrecognized SSL message, plaintext connection?")));
    }

    @Test
    public void connectionClosed_httpAgainstHttpsPort_isTlsSchemeMismatch() {
        // The classic symptom of speaking http:// to an https-only OS 3.x port.
        assertEquals(ConnectionFailureKind.TLS_SCHEME_MISMATCH,
                classify(new ConnectionClosedException("Connection is closed")));
    }

    @Test
    public void certificateMessage_isTlsSchemeMismatch() {
        assertEquals(ConnectionFailureKind.TLS_SCHEME_MISMATCH,
                classify(new IOException("unable to find valid certification path to requested target")));
    }

    // ---- Auth -----------------------------------------------------------------------------------

    @Test
    public void forbidden403_isAuthForbidden() {
        assertEquals(ConnectionFailureKind.AUTH_FORBIDDEN,
                classify(new RuntimeException("method [HEAD], status line [HTTP/1.1 403 Forbidden]")));
    }

    @Test
    public void unauthorized401_isAuthForbidden() {
        assertEquals(ConnectionFailureKind.AUTH_FORBIDDEN,
                classify(new RuntimeException("401 Unauthorized")));
    }

    @Test
    public void securityExceptionStatus403_isAuthForbidden() {
        // The shape OpenSearch returns when a role does not cover the requested index names — the
        // operation-failure case this classifier is also used for (issue #36222).
        assertEquals(ConnectionFailureKind.AUTH_FORBIDDEN,
                classify(new DotStateException(
                        "Failed to create index: cluster_acme.working_20260101000000.os",
                        new RuntimeException("OpenSearch exception [type=security_exception,"
                                + " reason=no permissions for [indices:admin/create]] status: 403"))));
    }

    /**
     * A dotCMS wrapper message embeds the physical index name, and a {@code _yyyyMMddHHmmss}
     * timestamp regularly contains the digits 403 or 401 (here 12:04:03). Matching the status code
     * as a bare substring reported such a failure as a permission problem and told the operator to
     * change {@code DOT_DOTCMS_CLUSTER_ID} for something that has nothing to do with permissions.
     */
    @Test
    public void statusCodeDigitsInsideIndexTimestamp_isNotAuthForbidden() {
        assertNotEquals(ConnectionFailureKind.AUTH_FORBIDDEN,
                classify(new DotStateException(
                        "Failed to parse index settings for: cluster_acme.working_20260728120403.os",
                        new JSONException("Unexpected character in settings JSON"))));
        assertNotEquals(ConnectionFailureKind.AUTH_FORBIDDEN,
                classify(new IOException("Empty orphaned OS index"
                        + " cluster_acme.working_20260728120401.os could not be deleted")));
    }

    // ---- Unreachable ----------------------------------------------------------------------------

    @Test
    public void connectException_isUnreachable() {
        assertEquals(ConnectionFailureKind.UNREACHABLE,
                classify(new ConnectException("Connection refused")));
    }

    @Test
    public void unknownHost_isUnreachable() {
        assertEquals(ConnectionFailureKind.UNREACHABLE,
                classify(new UnknownHostException("no-such-host")));
    }

    @Test
    public void socketTimeout_isUnreachable() {
        assertEquals(ConnectionFailureKind.UNREACHABLE,
                classify(new SocketTimeoutException("connect timed out")));
    }

    // ---- Cause-chain walking & fallback ---------------------------------------------------------

    @Test
    public void walksCauseChain_wrappedSslException_isTlsSchemeMismatch() {
        final Throwable wrapped = new RuntimeException("io error",
                new IOException("handshake failed", new SSLException("bad_certificate")));
        assertEquals(ConnectionFailureKind.TLS_SCHEME_MISMATCH, classify(wrapped));
    }

    @Test
    public void nullError_isUnknown() {
        assertEquals(ConnectionFailureKind.UNKNOWN, classify(null));
    }

    @Test
    public void unrecognizedError_isUnknown() {
        assertEquals(ConnectionFailureKind.UNKNOWN,
                classify(new IllegalStateException("something else entirely")));
    }
}
