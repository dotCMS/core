package com.dotcms.security.apps;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class VersionOverrideObjectInputStreamTest {

    @Test
    public void testDeserializeAllowedClass() throws Exception {
        Map<String, List<AppSecrets>> secretsMap = new HashMap<>();
        List<AppSecrets> appSecretsList = new ArrayList<>();
        appSecretsList.add(AppSecrets.builder()
                .withKey("testApp")
                .withSecret("secret1", "value1")
                .build());
        secretsMap.put("site1", appSecretsList);
        AppsSecretsImportExport original = new AppsSecretsImportExport(secretsMap);

        byte[] serialized = serialize(original);
        try (VersionOverrideObjectInputStream voois = new VersionOverrideObjectInputStream(new ByteArrayInputStream(serialized))) {
            AppsSecretsImportExport deserialized = (AppsSecretsImportExport) voois.readObject();
            assertNotNull(deserialized);
            assertTrue(deserialized.getSecrets().containsKey("site1"));
        }
    }

    @Test(expected = InvalidClassException.class)
    public void testDeserializeBlockedClass() throws Exception {
        URL url = new URL("http://localhost");
        byte[] serialized = serialize(url);

        try (VersionOverrideObjectInputStream voois = new VersionOverrideObjectInputStream(new ByteArrayInputStream(serialized))) {
            voois.readObject();
        }
    }

    private byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        return baos.toByteArray();
    }
}
