package com.dotcms.publishing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.dotcms.publisher.business.DotPublisherException;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import com.thoughtworks.xstream.security.ForbiddenClassException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * @author Jonathan Gamba 2019-01-08
 */
public class BundlerUtilUnitTest {

    @Test
    public void sanitize_bundle_name_no_changes() throws Exception {

        final String[] testBundleNames = {"bundle.zip", "bundle.tar.gz", "bundle", "bunDLE.zip",
                "Bundle.tar.gz", "bundle.bundle.bundle", "bundle.bundle.bundle.bundle"};

        for (final String testBundleName : testBundleNames) {
            String sanitizedName = BundlerUtil.sanitizeBundleName(testBundleName);
            assertNotNull(sanitizedName);
            assertEquals(sanitizedName, testBundleName);
        }
    }

    @Test
    public void sanitize_bundle_name_with_changes() throws Exception {

        final String bundleName = "bundle.zip";
        final String[] testBundleNames = {
                "/some/path/" + bundleName,
                "/another/random/path/" + bundleName,
                "another/random/path/" + bundleName};

        for (final String testBundleName : testBundleNames) {
            String sanitizedName = BundlerUtil.sanitizeBundleName(testBundleName);
            assertNotNull(sanitizedName);
            assertEquals(sanitizedName, bundleName);
        }
    }

    @Test(expected = DotPublisherException.class)
    public void sanitize_bundle_name_null_name() throws Exception {
        BundlerUtil.sanitizeBundleName(null);
    }

    @Test(expected = DotPublisherException.class)
    public void sanitize_bundle_name_empty_name() throws Exception {
        BundlerUtil.sanitizeBundleName("");
    }

    /**
     * A bundle file naming a type the trusted list does not cover must come back as that
     * refusal, with the file left exactly as it arrived.
     *
     * <p>The fallback below the first read prepends an XML 1.1 prologue and reads again, which
     * rewrites the received file on disk. For a refusal that is pointless: no prologue changes
     * whether a type is trusted, and it costs the message that names the offending type,
     * replacing it with a generic parse failure.</p>
     */
    @Test
    public void testXmlToObject_whenTypeOutsideTrustedList_shouldRefuseWithoutRewritingFile()
            throws IOException {
        final File file = File.createTempFile("trusted-list-probe", ".xml");
        file.deleteOnExit();
        final byte[] asReceived = "<java.beans.EventHandler/>".getBytes(StandardCharsets.UTF_8);
        Files.write(file.toPath(), asReceived);

        try {
            BundlerUtil.xmlToObject(file);
            fail("Expected java.beans.EventHandler to be refused by the trusted list");
        } catch (final ForbiddenClassException e) {
            assertTrue("The refusal should name the refused type, was: " + e.getMessage(),
                    e.getMessage().contains("java.beans.EventHandler"));
        }

        assertArrayEquals("A refused bundle file must be left as it arrived",
                asReceived, Files.readAllBytes(file.toPath()));
    }
}
