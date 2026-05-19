package com.dotcms.security.apps;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

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
    public void testDeserializationAllowlist() throws IOException, ClassNotFoundException {
        Map<String, List<AppSecrets>> secretsMap = new HashMap<>();
        List<AppSecrets> secretsList = new ArrayList<>();
        secretsList.add(AppSecrets.builder()
                .withKey("test-app")
                .withSecret("secret1", "value1")
                .build());
        secretsMap.put("host1", secretsList);
        AppsSecretsImportExport original = new AppsSecretsImportExport(secretsMap);

        byte[] serialized;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
            serialized = baos.toByteArray();
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(serialized);
                VersionOverrideObjectInputStream voois = new VersionOverrideObjectInputStream(bais)) {
            AppsSecretsImportExport deserialized = (AppsSecretsImportExport) voois.readObject();
            assertNotNull(deserialized);
            assertNotNull(deserialized.getSecrets().get("host1"));
        }
    }

    @Test
    public void testDeserializationBlocked() throws IOException {
        URL unauthorizedObject = new URL("http://localhost");
        byte[] serialized;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(unauthorizedObject);
            serialized = baos.toByteArray();
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(serialized);
                VersionOverrideObjectInputStream voois = new VersionOverrideObjectInputStream(bais)) {
            assertThrows(InvalidClassException.class, voois::readObject);
        }
    }
}
