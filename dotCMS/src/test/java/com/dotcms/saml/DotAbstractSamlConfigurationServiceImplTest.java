package com.dotcms.saml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DotAbstractSamlConfigurationServiceImplTest {

    @Test
    public void defaults_file_can_not_turn_signature_checks_off(@TempDir final Path tempDir) throws IOException {
        final Path defaults = tempDir.resolve("dotcms-saml-default.properties");
        Files.write(defaults, String.join("\n",
                SamlName.DOT_SAML_VERIFY_SIGNATURE_CREDENTIALS.getPropertyName() + "=false",
                SamlName.DOT_SAML_VERIFY_SIGNATURE_PROFILE.getPropertyName() + "=false",
                SamlName.DOT_SAML_CLOCK_SKEW.getPropertyName() + "=5000",
                "not.a.saml.property=whatever").getBytes(StandardCharsets.UTF_8));

        final TestConfigurationService service = new TestConfigurationService();
        service.initService(Map.of(SamlConfigurationService.DOT_SAML_DEFAULT_PROPERTIES_CONTEXT_MAP_KEY,
                defaults.toString()));

        assertTrue(service.getDefaultBooleanParameter(SamlName.DOT_SAML_VERIFY_SIGNATURE_CREDENTIALS));
        assertTrue(service.getDefaultBooleanParameter(SamlName.DOT_SAML_VERIFY_SIGNATURE_PROFILE));
        // other defaults from the file still apply
        assertEquals("5000", service.getDefaultStringParameter(SamlName.DOT_SAML_CLOCK_SKEW));
    }

    private static final class TestConfigurationService extends DotAbstractSamlConfigurationServiceImpl {

        @Override
        public Map<String, String> createInitialMap() {
            final Map<String, String> map = new HashMap<>();
            map.put(SamlName.DOT_SAML_VERIFY_SIGNATURE_CREDENTIALS.getPropertyName(), "true");
            map.put(SamlName.DOT_SAML_VERIFY_SIGNATURE_PROFILE.getPropertyName(), "true");
            map.put(SamlName.DOT_SAML_CLOCK_SKEW.getPropertyName(), "10000");
            return map;
        }
    }
}
