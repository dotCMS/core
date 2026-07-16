package com.dotcms.content.index.opensearch;

import static org.junit.Assert.assertEquals;

import com.dotcms.content.index.opensearch.OSIndexAPIImpl.ConnectionFailureKind;
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
