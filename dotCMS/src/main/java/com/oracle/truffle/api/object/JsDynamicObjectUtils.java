package com.oracle.truffle.api.object;

/**
 * This class is mostly to access to package sealed methods on the {@link DynamicObject}
 * @author jsanca
 */
public class JsDynamicObjectUtils {
    private JsDynamicObjectUtils() {
    }

    public static final Object[] getObjectArray(final DynamicObject object) {
        return object.getObjectStore();
    }
}
