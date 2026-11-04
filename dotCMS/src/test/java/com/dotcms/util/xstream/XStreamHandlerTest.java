package com.dotcms.util.xstream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import com.acme.publishing.AcmeProbe;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotcms.repackage.probe.RepackagedProbe;
import com.dotcms.publishing.XMLSerializerUtil;
import com.dotcms.publishing.manifest.CSVManifestBuilder;
import com.thoughtworks.xstream.XStream;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.SecurityLogger;
import com.thoughtworks.xstream.security.ForbiddenClassException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Verifies that the {@link XStream} instances handed out by {@link XStreamHandler} only
 * reconstruct types covered by the trusted list, and still reconstruct the types a bundle
 * legitimately carries.
 */
public class XStreamHandlerTest {

    /**
     * Builds the smallest XML document that asks XStream to reconstruct the given type.
     *
     * @param className fully qualified name of the type the document asks for
     * @return an XML document whose root element names that type
     */
    private String xmlFor(final String className) {
        return "<" + className + "/>";
    }

    /**
     * Asserts that the handler's instance refuses to reconstruct the given type.
     *
     * @param className fully qualified name of the type that must be refused
     */
    private void assertRefused(final String className) {
        final XStream xstream = XStreamHandler.newXStreamInstance();
        try {
            xstream.fromXML(xmlFor(className));
            fail("Expected " + className + " to be refused by the trusted list");
        } catch (ForbiddenClassException e) {
            assertTrue(e.getMessage().contains(className));
        }
    }

    /**
     * The baseline case: a type that belongs to none of the trusted packages must not be
     * reconstructed, even though it is on the classpath and XStream could build it.
     */
    @Test
    public void testFromXML_whenTypeOutsideTrustedList_shouldRefuse() {
        assertRefused("java.beans.EventHandler");
    }

    /**
     * The repackaged libraries live under {@code com.dotcms.repackage}, which is nested inside
     * the {@code com.dotcms} namespace a bundle legitimately uses. Trusting that namespace as a
     * whole would carry them along, so the trusted list has to leave them out.
     */
    @Test
    public void testFromXML_whenRepackagedScriptingType_shouldRefuse() {
        assertRefused("com.dotcms.repackage.bsh.XThis");
    }

    /**
     * Same nesting problem as the repackaged scripting library, from a different shaded
     * dependency — a bundle has no reason to carry a database connection pool.
     */
    @Test
    public void testFromXML_whenRepackagedDataSourceType_shouldRefuse() {
        assertRefused("com.dotcms.repackage.org.apache.commons.dbcp.BasicDataSource");
    }

    /**
     * {@code java.lang} holds the string and number types a bundle does carry, and also types
     * that start operating-system processes. Trusting the package wholesale takes both, so the
     * trusted list has to name what it needs instead of the package.
     */
    @Test
    public void testFromXML_whenProcessLaunchingType_shouldRefuse() {
        assertRefused("java.lang.ProcessBuilder");
    }

    /**
     * The second {@code java.lang} entry point to the operating system, kept as its own case so
     * that narrowing the package is verified by more than one example.
     */
    @Test
    public void testFromXML_whenRuntimeType_shouldRefuse() {
        assertRefused("java.lang.Runtime");
    }

    /**
     * A bundle document may ask XStream to build a dynamic proxy rather than a named class.
     * The marker type that enables that path lives under {@code com.thoughtworks.xstream.mapper},
     * so a trusted-list entry for that whole package would keep the path open.
     */
    @Test
    public void testFromXML_whenDynamicProxyRequested_shouldRefuse() {
        final XStream xstream = XStreamHandler.newXStreamInstance();
        try {
            xstream.fromXML("<dynamic-proxy><interface>java.util.Map</interface>"
                    + "<handler class=\"java.beans.EventHandler\"/></dynamic-proxy>");
            fail("Expected the dynamic proxy path to be refused by the trusted list");
        } catch (ForbiddenClassException e) {
            // expected
        }
    }

    /**
     * A plain map still survives the round trip. This is the smoke test for the collection types
     * every bundle document is built out of.
     */
    @Test
    public void testFromXML_whenTrustedType_shouldDeserialize() {
        final XStream xstream = XStreamHandler.newXStreamInstance();
        final Map<String, String> original = new HashMap<>();
        original.put("bundleId", "abc-123");

        final String xml = xstream.toXML(original);
        final Object restored = xstream.fromXML(xml);

        assertEquals(original, restored);
    }

    /**
     * A bundle document does not only carry data: it carries {@link Class} objects, which
     * XStream has to resolve by name on the way back in. {@link PushPublisher} stands in for
     * the publisher list a real bundle configuration holds, so narrowing the trusted list too
     * far around the dotCMS namespace fails here.
     *
     * <p>The full {@link com.dotcms.publishing.PublisherConfig} round trip belongs in the
     * integration suite: its constructor resolves the default language through
     * {@code APILocator}, so it cannot be built without a running instance.</p>
     */
    @Test
    public void testFromXML_whenClassReference_shouldRoundTrip() {
        final XStream xstream = XStreamHandler.newXStreamInstance();
        final List<Class<?>> original = new ArrayList<>();
        original.add(PushPublisher.class);

        final String xml = xstream.toXML(original);
        @SuppressWarnings("unchecked")
        final List<Class<?>> restored = (List<Class<?>>) xstream.fromXML(xml);

        assertEquals(1, restored.size());
        assertEquals(PushPublisher.class, restored.get(0));
    }


    /**
     * Clears the configured extension so one test's patterns cannot leak into the next.
     */
    @After
    public void clearExtraPatterns() {
        Config.setProperty(XStreamHandler.TRUSTED_LIST_EXTRA_PATTERNS_PROPERTY, null);
    }

    /**
     * Without the extension configured, a type from a customer's own namespace is refused —
     * the starting point the extension has to change.
     */
    @Test
    public void testFromXML_whenCustomerTypeAndNoExtension_shouldRefuse() {
        assertRefused(AcmeProbe.class.getName());
    }

    /**
     * The supported way out for a customer whose bundles carry their own classes: name the
     * package in the configuration property and the type is read back again.
     */
    @Test
    public void testFromXML_whenCustomerPackageConfigured_shouldDeserialize() {
        Config.setProperty(XStreamHandler.TRUSTED_LIST_EXTRA_PATTERNS_PROPERTY,
                "com.acme.**");

        final XStream xstream = XStreamHandler.newXStreamInstance();
        final AcmeProbe original = new AcmeProbe();
        original.setName("from-a-plugin");

        final AcmeProbe restored = (AcmeProbe) xstream.fromXML(xstream.toXML(original));

        assertEquals("from-a-plugin", restored.getName());
    }

    /**
     * More than one pattern can be configured at once, separated by commas, and the whitespace
     * someone leaves around them while editing a properties file is ignored.
     */
    @Test
    public void testFromXML_whenSeveralPatternsConfigured_shouldDeserialize() {
        Config.setProperty(XStreamHandler.TRUSTED_LIST_EXTRA_PATTERNS_PROPERTY,
                "com.example.**, com.acme.**");

        final XStream xstream = XStreamHandler.newXStreamInstance();
        final AcmeProbe original = new AcmeProbe();
        original.setName("second-in-the-list");

        final AcmeProbe restored = (AcmeProbe) xstream.fromXML(xstream.toXML(original));

        assertEquals("second-in-the-list", restored.getName());
    }

    /**
     * The extension adds to the trusted list, it does not replace the exclusions: a package
     * that was taken out on purpose stays out even if someone names it in the property.
     */
    @Test
    public void testFromXML_whenExtensionNamesExcludedPackage_shouldStillRefuse() {
        Config.setProperty(XStreamHandler.TRUSTED_LIST_EXTRA_PATTERNS_PROPERTY,
                "com.dotcms.repackage.**");

        assertRefused("com.dotcms.repackage.bsh.XThis");
    }

    /**
     * A configured pattern changes what is trusted from the next instance onwards, without a
     * restart: support has to be able to unblock a customer while their publishing is down.
     */
    @Test
    public void testFromXML_whenPatternAddedAfterFirstUse_shouldTakeEffect() {
        assertRefused(AcmeProbe.class.getName());

        Config.setProperty(XStreamHandler.TRUSTED_LIST_EXTRA_PATTERNS_PROPERTY,
                "com.acme.**");

        final XStream xstream = XStreamHandler.newXStreamInstance();
        final AcmeProbe original = new AcmeProbe();
        original.setName("after-the-change");

        final AcmeProbe restored = (AcmeProbe) xstream.fromXML(xstream.toXML(original));

        assertEquals("after-the-change", restored.getName());
    }

    /**
     * The reader the receiver actually uses for {@code bundle.xml} is held by a singleton for
     * the life of the JVM. A pattern configured while an installation's publishing is failing
     * has to reach that held instance too, otherwise the only way out would be a restart.
     */
    @Test
    public void testSharedSerializer_whenPatternAddedAfterFirstUse_shouldTakeEffect() {
        final XStream before = XMLSerializerUtil.getInstance().getXmlSerializer();
        try {
            before.fromXML(xmlFor(AcmeProbe.class.getName()));
            fail("Expected the probe to be refused before the pattern is configured");
        } catch (ForbiddenClassException expected) {
            // the starting point
        }

        Config.setProperty(XStreamHandler.TRUSTED_LIST_EXTRA_PATTERNS_PROPERTY, "com.acme.**");

        final XStream after = XMLSerializerUtil.getInstance().getXmlSerializer();
        final AcmeProbe original = new AcmeProbe();
        original.setName("without-a-restart");

        final AcmeProbe restored = (AcmeProbe) after.fromXML(after.toXML(original));

        assertEquals("without-a-restart", restored.getName());
    }

    /**
     * A refusal is the one thing an administrator has to act on, and the place they see it is
     * the failed bundle in the Push Publishing portlet, which shows the exception's message.
     * That message therefore has to name both the type that was refused and the property that
     * would let it through, not just the type.
     */
    @Test
    public void testFromXML_whenTypeRefused_messageShouldNameTypeAndProperty() {
        final XStream xstream = XStreamHandler.newXStreamInstance();
        try {
            xstream.fromXML(xmlFor("java.beans.EventHandler"));
            fail("Expected java.beans.EventHandler to be refused by the trusted list");
        } catch (Exception e) {
            assertTrue("The message should name the refused type, was: " + e.getMessage(),
                    e.getMessage().contains("java.beans.EventHandler"));
            assertTrue("The message should name the property to set, was: " + e.getMessage(),
                    e.getMessage().contains(XStreamHandler.TRUSTED_LIST_EXTRA_PATTERNS_PROPERTY));
        }
    }

    /**
     * The exclusion has to reach anonymous types too. XStream's wildcard skips them by default,
     * which keeps the allow list conservative but leaves the exclusion short: an anonymous type
     * inside a shaded library would match neither side and land on XStream's own defaults,
     * where anything that is a map, a collection, a number, a throwable or a reflective member
     * is allowed unconditionally — and the probe here is a map.
     *
     * <p>The element name is written the way XStream writes it, with {@code $} escaped as
     * {@code _-}, because {@code $} is not legal in an XML name.</p>
     */
    @Test
    public void testFromXML_whenAnonymousTypeInExcludedPackage_shouldRefuse() {
        final String className = RepackagedProbe.anonymousType().getName();
        final XStream xstream = XStreamHandler.newXStreamInstance();
        try {
            xstream.fromXML(xmlFor(className.replace("$", "_-")));
            fail("Expected " + className + " to be refused: it sits inside an excluded package");
        } catch (ForbiddenClassException e) {
            assertTrue(e.getMessage().contains(className));
        }
    }

    /**
     * A document can also name a type as the value of a {@code <java-class>} element rather
     * than as an element name, which is how a {@link Class} held in a field is written. That
     * path has to be checked against the trusted list as well, otherwise the list governs only
     * half of the ways a document can name a type.
     */
    @Test
    public void testFromXML_whenClassReferenceOutsideTrustedList_shouldRefuse() {
        final XStream xstream = XStreamHandler.newXStreamInstance();
        try {
            xstream.fromXML("<java-class>java.lang.Runtime</java-class>");
            fail("Expected the java.lang.Runtime reference to be refused by the trusted list");
        } catch (ForbiddenClassException e) {
            assertTrue(e.getMessage().contains("java.lang.Runtime"));
        }
    }

    /**
     * The same path must keep working for a type the trusted list does cover: the bundle's
     * configuration holds the publishers as a list of {@link Class}, written exactly this way.
     */
    @Test
    public void testFromXML_whenClassReferenceInsideTrustedList_shouldResolve() {
        final XStream xstream = XStreamHandler.newXStreamInstance();

        final Object resolved =
                xstream.fromXML("<java-class>" + PushPublisher.class.getName() + "</java-class>");

        assertEquals(PushPublisher.class, resolved);
    }

    /**
     * Reads a document naming a type the trusted list does not cover, and swallows the refusal
     * that read is expected to raise.
     *
     * @param xstream the instance to read with, reused across calls on purpose
     * @param className fully qualified name of the type the document asks for
     */
    private void refuse(final XStream xstream, final String className) {
        try {
            xstream.fromXML(xmlFor(className));
            fail("Expected " + className + " to be refused by the trusted list");
        } catch (ForbiddenClassException e) {
            //Expected. What this helper's callers assert on is what the refusal recorded.
        }
    }

    /**
     * XStream remembers a refusal by element name, so the second document naming the same type
     * never reaches the mapper this handler wraps. The audit entry has to be written anyway: a
     * serializer is held for the life of the JVM, so recording only the first refusal would mean
     * an import failing this afternoon left its trace this morning and nowhere else.
     */
    @Test
    public void testFromXML_whenSameTypeRefusedTwice_shouldRecordBothRefusals() {
        final XStream xstream = XStreamHandler.newXStreamInstance();

        try (MockedStatic<SecurityLogger> securityLogger = mockStatic(SecurityLogger.class)) {
            refuse(xstream, "java.beans.EventHandler");
            refuse(xstream, "java.beans.EventHandler");

            securityLogger.verify(() -> SecurityLogger.logWarn(eq(XStreamHandler.class),
                    contains("java.beans.EventHandler")), times(2));
        }
    }

    /**
     * The bundle's configuration holds the manifest builder in a field that is not transient, so
     * whatever state that builder is in when {@code bundle.xml} is written ends up in the
     * document the receiver reads back.
     *
     * <p>The only state it can be in there is this one. {@code bundle.xml} is written on the
     * branch taken when the builder has no manifest file yet, and the file handle and the file
     * are set together, so the open writer the builder holds later is always still null. The
     * document therefore carries the metadata map and nothing else, and the trusted list has to
     * cover it.</p>
     */
    @Test
    public void testFromXML_whenConfigCarriesManifestBuilder_shouldResolve() {
        final XStream xstream = XStreamHandler.newXStreamInstance();
        final CSVManifestBuilder builder = new CSVManifestBuilder();
        builder.addMetadata(CSVManifestBuilder.BUNDLE_ID_METADATA_NAME, "a-bundle-id");
        builder.addMetadata(CSVManifestBuilder.OPERATION_METADATA_NAME, "PUBLISH");

        final Object resolved = xstream.fromXML(xstream.toXML(builder));

        assertEquals(CSVManifestBuilder.class, resolved.getClass());
    }
}
