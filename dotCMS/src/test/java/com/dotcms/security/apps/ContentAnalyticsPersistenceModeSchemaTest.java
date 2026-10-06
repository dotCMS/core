package com.dotcms.security.apps;

import org.junit.Test;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the {@code persistenceMode} {@code SELECT} param in
 * {@code dotContentAnalytics-config.yml} parses per the shape {@link AppDescriptorHelper}
 * already validates for {@code Type.SELECT}: exactly two options, exactly one marked
 * {@code selected}, with {@code readwrite} as that default (see
 * {@code specs/37521-content-analytics-mode/contracts/app-config-schema.md}).
 *
 * @author dotCMS
 * @since 2026
 */
public class ContentAnalyticsPersistenceModeSchemaTest {

    /**
     * Method to test: {@link AppDescriptorHelper#readAppFile(Path)}
     *
     * Given Scenario: {@code dotContentAnalytics-config.yml} defines a {@code persistenceMode}
     * param of type {@code SELECT} with two options ("Read & Write" / "Read Only"), following
     * the same shape {@link AppDescriptorHelper} already validates for {@code dotsaml-config.yml}'s
     * {@code signatureValidationType}.
     *
     * Expected Result: The parsed {@link AppSchema} exposes a {@code persistenceMode} param of
     * type {@code SELECT} with exactly two options, exactly one of them marked {@code selected},
     * and that default option is {@code readwrite}.
     */
    @Test
    public void persistenceModeParsesAsSelectWithReadWriteDefault() throws Exception {
        final AppSchema schema = readContentAnalyticsSchema();

        final ParamDescriptor persistenceMode = schema.getParams().get("persistenceMode");
        assertNotNull("persistenceMode param must be present in dotContentAnalytics-config.yml",
                persistenceMode);
        assertEquals(Type.SELECT, persistenceMode.getType());

        @SuppressWarnings("unchecked")
        final List<Map> options = persistenceMode.getList();
        assertEquals("persistenceMode must have exactly two options", 2, options.size());

        final long selectedCount = options.stream().filter(option -> option.containsKey("selected")).count();
        assertEquals("Exactly one option must be marked selected", 1, selectedCount);

        final Map readWrite = options.stream()
                .filter(option -> "readwrite".equals(option.get("value")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("'readwrite' option missing"));
        assertEquals(Boolean.TRUE, readWrite.get("selected"));

        final boolean hasReadOnly = options.stream().anyMatch(option -> "readonly".equals(option.get("value")));
        assertTrue("'readonly' option missing", hasReadOnly);
    }

    /**
     * Loads {@code dotContentAnalytics-config.yml} straight off the test classpath and parses
     * it via {@link AppDescriptorHelper#readAppFile(Path)}, bypassing
     * {@link AppDescriptorHelper#loadAppDescriptors()} entirely since that entry point resolves
     * the server's Apps directory through {@code APILocator}, which requires a live dotCMS
     * context this unit test does not have.
     *
     * @return the parsed {@link AppSchema} for the Content Analytics app descriptor.
     * @throws Exception if the resource cannot be found, read, or parsed.
     */
    private AppSchema readContentAnalyticsSchema() throws Exception {
        final URL resource = Thread.currentThread().getContextClassLoader()
                .getResource("apps/dotContentAnalytics-config.yml");
        assertNotNull("dotContentAnalytics-config.yml not found on test classpath", resource);
        final Path path = Paths.get(resource.toURI());
        return new AppDescriptorHelper().readAppFile(path);
    }
}
