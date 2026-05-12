package com.dotcms.security.apps;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class VersionOverrideObjectInputStreamTest {

    @Test
    public void testDeserializeAllowedClass() throws IOException, ClassNotFoundException {
        Map<String, List<AppSecrets>> secretsMap = new HashMap<>();
        List<AppSecrets> secretsList = new ArrayList<>();
        secretsList.add(AppSecrets.builder().withKey("test").build());
        secretsMap.put("site1", secretsList);
        AppsSecretsImportExport importExport = new AppsSecretsImportExport(secretsMap);

        byte[] serialized = serialize(importExport);

        try (ByteArrayInputStream bin = new ByteArrayInputStream(serialized);
                VersionOverrideObjectInputStream vOIS = new VersionOverrideObjectInputStream(bin)) {
            Object deserialized = vOIS.readObject();
            assertNotNull(deserialized);
            assertTrue(deserialized instanceof AppsSecretsImportExport);
        }
    }

    @Test
    public void testRejectUnauthorizedClass() throws IOException, ClassNotFoundException {
        java.util.BitSet unauthorized = new java.util.BitSet();
        byte[] serialized = serialize(unauthorized);

        try (ByteArrayInputStream bin = new ByteArrayInputStream(serialized);
                VersionOverrideObjectInputStream vOIS = new VersionOverrideObjectInputStream(bin)) {
            vOIS.readObject();
            fail("Should have thrown InvalidClassException");
        } catch (InvalidClassException e) {
            // expected
        }
    }

    @Test
    public void testRejectProxy() throws IOException, ClassNotFoundException {
        InvocationHandler handler = new MyInvocationHandler();
        Object proxy = Proxy.newProxyInstance(
                VersionOverrideObjectInputStreamTest.class.getClassLoader(),
                new Class[] { Runnable.class, Serializable.class },
                handler);
        byte[] serialized = serialize(proxy);

        try (ByteArrayInputStream bin = new ByteArrayInputStream(serialized);
                VersionOverrideObjectInputStream vOIS = new VersionOverrideObjectInputStream(bin)) {
            vOIS.readObject();
            fail("Should have thrown InvalidClassException for proxy");
        } catch (InvalidClassException e) {
            // expected
        }
    }

    @Test
    public void testRejectGadgetBeforeReadObject() throws IOException, ClassNotFoundException {
        GadgetClass gadget = new GadgetClass();
        GadgetClass.readObjectCalled = false;
        byte[] serialized = serialize(gadget);

        try (ByteArrayInputStream bin = new ByteArrayInputStream(serialized);
                VersionOverrideObjectInputStream vOIS = new VersionOverrideObjectInputStream(bin)) {
            vOIS.readObject();
            fail("Should have thrown InvalidClassException for gadget");
        } catch (InvalidClassException e) {
            // expected
        }
        assertFalse("readObject should not have been called", GadgetClass.readObjectCalled);
    }

    private byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (ObjectOutputStream oout = new ObjectOutputStream(bout)) {
            oout.writeObject(obj);
        }
        return bout.toByteArray();
    }

    public static class MyInvocationHandler implements InvocationHandler, Serializable {
        private static final long serialVersionUID = 1L;
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return null;
        }
    }

    public static class GadgetClass implements Serializable {
        private static final long serialVersionUID = 1L;
        public static boolean readObjectCalled = false;
        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            readObjectCalled = true;
            in.defaultReadObject();
        }
    }
}
