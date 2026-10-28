import java.io.FileInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import sun.security.util.UntrustedCertificates;

/**
 * Build-time check for CVE-2026-91403: exits 1 unless this JRE's
 * lib/security/blocked.certs blocks every eMudhra CA certificate in the given PEM
 * file. Asks the same lookup the TLS and PKIX validators use, so it passes or fails
 * regardless of which roots the vendor's cacerts happens to contain.
 *
 * Usage: java --add-exports java.base/sun.security.util=ALL-UNNAMED VerifyEmudhraBlocked.java blocked.pem
 */
public class VerifyEmudhraBlocked {

    /** The 16 certificates upstream JDK-8395142 added to blocked.certs. */
    private static final int EXPECTED_CERTS = 16;

    public static void main(String[] args) throws Exception {
        List<X509Certificate> certs;
        try (FileInputStream in = new FileInputStream(args[0])) {
            certs = CertificateFactory.getInstance("X.509").generateCertificates(in)
                    .stream().map(X509Certificate.class::cast).toList();
        }
        long trusted = certs.stream().filter(c -> !UntrustedCertificates.isUntrusted(c))
                .peek(c -> System.err.println("NOT BLOCKED: " + c.getSubjectX500Principal()
                        + " <- " + c.getIssuerX500Principal()))
                .count();
        if (certs.size() != EXPECTED_CERTS || trusted > 0) {
            System.err.println("FAIL: " + trusted + " of " + certs.size()
                    + " eMudhra CA certificates are not in blocked.certs (CVE-2026-91403)");
            System.exit(1);
        }
        System.out.println("OK: all " + certs.size() + " eMudhra CA certificates are blocked");
    }
}
