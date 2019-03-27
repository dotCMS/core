package com.dotcms.util;

import com.google.common.collect.ImmutableSet;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Encapsulates the logic to see if owner class is friend of one of the classes in the invoke stack trace.
 * Note: the callback should be in the samethread to work
 * @author jsanca
 */
public class FriendClass {

    private final Set<String> friendClasses;

    public FriendClass(final Class... friendClasses) {
        this.friendClasses = new ImmutableSet.Builder<String>()
                .addAll(Arrays.asList(friendClasses).stream()
                        .map(Class::getName).collect(Collectors.toList())).build();
    }

    public FriendClass(final String... friendClasses) {
        this.friendClasses = new ImmutableSet.Builder<String>()
                .add(friendClasses).build();
    }

    /**
     * Return true if the class calling is a friend
     * @return boolean
     */
    public boolean isFriend () {

        final StackTraceElement [] traceElements =
                Thread.currentThread().getStackTrace();

        if (null != traceElements) {

            for (final StackTraceElement element : traceElements) {

                if (this.friendClasses.contains(element.getClassName())) {

                    return true;
                }
            }
        }

        return false;
    } // isFriend
}
