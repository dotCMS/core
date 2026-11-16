package com.dotcms.security.apps;

import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * This class is used to override the default ObjectInputStream class to handle version mismatch
 * in case of any mismatch in the serialVersionUID of the class we override the class descriptor with the local class descriptor
 */
public class VersionOverrideObjectInputStream extends ObjectInputStream {

    /**
     * Constructor
     * @param in InputStream
     * @throws IOException IOException
     */
    public VersionOverrideObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

    /**
     * This method is used to read the class descriptor and override the class descriptor in case of any mismatch in the serialVersionUID.
     * The local class is looked up without being initialized, and the stream's
     * {@link ObjectInputFilter}, when there is one, is asked about it before its local descriptor is
     * read, because reading that descriptor initializes the class. A class the filter rejects is
     * refused here, so its static code never runs.
     * @return ObjectStreamClass
     * @throws IOException IOException
     * @throws ClassNotFoundException ClassNotFoundException
     */
    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        ObjectStreamClass resultClassDescriptor = super.readClassDescriptor(); // initially streams descriptor
        Class<?> localClass; // the class in the local JVM that this descriptor represents.
        try {
            localClass = Class.forName(resultClassDescriptor.getName(), false,
                    VersionOverrideObjectInputStream.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            Logger.error("No local class for " + resultClassDescriptor.getName(), e);
            return resultClassDescriptor;
        }
        rejectIfFiltered(localClass);
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

    /**
     * Asks the stream's {@link ObjectInputFilter}, if one is set, whether the given class may be
     * read, and refuses it when the filter rejects it. Only the class is checked here; the limits a
     * filter may also enforce (depth, references, size) are still checked by
     * {@link ObjectInputStream} as the object is read.
     *
     * @param localClass the local class a descriptor in the stream names
     * @throws InvalidClassException if the filter rejects the class
     */
    private void rejectIfFiltered(final Class<?> localClass) throws InvalidClassException {
        final ObjectInputFilter filter = getObjectInputFilter();
        if (null == filter) {
            return;
        }
        final ObjectInputFilter.Status status = filter.checkInput(new ObjectInputFilter.FilterInfo() {
            @Override
            public Class<?> serialClass() {
                return localClass;
            }

            @Override
            public long arrayLength() {
                return -1;
            }

            @Override
            public long depth() {
                return 0;
            }

            @Override
            public long references() {
                return 0;
            }

            @Override
            public long streamBytes() {
                return 0;
            }
        });
        if (ObjectInputFilter.Status.REJECTED == status) {
            throw new InvalidClassException(localClass.getName(), "filter status: REJECTED");
        }
    }

}
