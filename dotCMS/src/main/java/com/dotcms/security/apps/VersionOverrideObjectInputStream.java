package com.dotcms.security.apps;

import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * This class is used to override the default ObjectInputStream class to handle version mismatch
 * in case of any mismatch in the serialVersionUID of the class we override the class descriptor with the local class descriptor.
 * It also implements a strict allowlist for class deserialization to prevent insecure deserialization attacks.
 */
public class VersionOverrideObjectInputStream extends ObjectInputStream {

    private static final Set<String> ALLOWED_CLASSES = Set.of(
            AppsSecretsImportExport.class.getName(),
            AppSecrets.class.getName(),
            Secret.class.getName(),
            AbstractProperty.class.getName(),
            Type.class.getName(),
            HashMap.class.getName(),
            ArrayList.class.getName(),
            String.class.getName(),
            Boolean.class.getName(),
            Enum.class.getName(),
            "com.google.common.collect.ImmutableMap$SerializedForm",
            "com.google.common.collect.ImmutableBiMap$SerializedForm",
            "com.google.common.collect.ImmutableList$SerializedForm",
            "com.google.common.collect.ImmutableSet$SerializedForm",
            "[C", // char[]
            "[Ljava.lang.Object;" // Object[]
    );

    /**
     * Constructor
     * @param in InputStream
     * @throws IOException IOException
     */
    public VersionOverrideObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        final String name = desc.getName();
        if (!ALLOWED_CLASSES.contains(name)) {
            throw new InvalidClassException("Unauthorized deserialization attempt", name);
        }
        return super.resolveClass(desc);
    }

    @Override
    protected Class<?> resolveProxyClass(String[] interfaces) throws IOException, ClassNotFoundException {
        throw new InvalidClassException("Dynamic Proxy Deserialization is not allowed");
    }

    /**
     * This method is used to read the class descriptor and override the class descriptor in case of any mismatch in the serialVersionUID
     * @return ObjectStreamClass
     * @throws IOException IOException
     * @throws ClassNotFoundException ClassNotFoundException
     */
    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        ObjectStreamClass resultClassDescriptor = super.readClassDescriptor(); // initially streams descriptor
        Class<?> localClass; // the class in the local JVM that this descriptor represents.
        try {
            localClass = Class.forName(resultClassDescriptor.getName());
        } catch (ClassNotFoundException e) {
            Logger.error("No local class for " + resultClassDescriptor.getName(), e);
            return resultClassDescriptor;
        }
        final ObjectStreamClass localClassDescriptor = ObjectStreamClass.lookup(localClass);
        if (localClassDescriptor != null) { // only if class implements serializable
            final long localSUID = localClassDescriptor.getSerialVersionUID();
            final long streamSUID = resultClassDescriptor.getSerialVersionUID();
            if (streamSUID != localSUID) { // check for serialVersionUID mismatch.
                final String overrideMessage = String.format(
                        "Overriding serialized class version mismatch: " +
                        "local serialVersionUID = %d stream serialVersionUID = %d",
                        localSUID, streamSUID);
                final Exception e = new InvalidClassException(overrideMessage);
                // Report the error and override the class descriptor
                Logger.debug(this,"Version mismatch on serialized objects, Will still attempting deserialization .", e);
                resultClassDescriptor = localClassDescriptor; // Use local class descriptor for deserialization
            }
        }
        return resultClassDescriptor;
    }


}
