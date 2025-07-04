package com.dotcms.rest.config;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe registry that maintains WeakReferences to Class objects.
 * This allows classes to be garbage collected when no longer referenced elsewhere,
 * preventing memory leaks in dynamic class loading scenarios.
 */
public class WeakClassRegistry {

    private final Set<WeakReference<Class<?>>> weakReferences =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Adds a class to the registry if not already present.
     * @param clazz the class to add
     * @return true if the class was added (new) or already exists, false if clazz is null
     */
    public boolean addIfAbsent(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        // Check if already exists
        if (contains(clazz)) {
            return true;
        }

        // Add new weak reference
        weakReferences.add(new WeakReference<>(clazz));
        return true;
    }

    /**
     * Checks if the registry contains the specified class.
     * @param clazz the class to check
     * @return true if the class is present and not garbage collected
     */
    public boolean contains(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        for (WeakReference<Class<?>> ref : weakReferences) {
            Class<?> referencedClass = ref.get();
            if (clazz.equals(referencedClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes a class from the registry.
     * @param clazz the class to remove
     * @return true if the class was found and removed
     */
    public boolean remove(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        Iterator<WeakReference<Class<?>>> iterator = weakReferences.iterator();
        while (iterator.hasNext()) {
            WeakReference<Class<?>> ref = iterator.next();
            Class<?> referencedClass = ref.get();

            if (referencedClass == null) {
                // Clean up dead reference
                iterator.remove();
            } else if (clazz.equals(referencedClass)) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a Set of all viable (non-garbage collected) classes.
     * This method also performs cleanup of dead references.
     * @return Set of Class objects that are still viable
     */
    public Set<Class<?>> getViableClasses() {
        Set<Class<?>> viableClasses = new HashSet<>();
        Iterator<WeakReference<Class<?>>> iterator = weakReferences.iterator();

        while (iterator.hasNext()) {
            WeakReference<Class<?>> ref = iterator.next();
            Class<?> clazz = ref.get();

            if (clazz != null) {
                viableClasses.add(clazz);
            } else {
                // Remove dead references while iterating
                iterator.remove();
            }
        }

        return viableClasses;
    }

    /**
     * Manually triggers cleanup of dead references.
     * This is automatically performed by getViableClasses() but can be called explicitly.
     */
    public void cleanup() {
        weakReferences.removeIf(ref -> ref.get() == null);
    }

    /**
     * Returns the number of weak references (including potentially dead ones).
     * For accurate count of viable classes, use getViableClasses().size()
     * @return number of weak references stored
     */
    public int size() {
        return weakReferences.size();
    }

    /**
     * Checks if the registry is empty (no references at all).
     * @return true if no references are stored
     */
    public boolean isEmpty() {
        return weakReferences.isEmpty();
    }

    /**
     * Clears all references from the registry.
     */
    public void clear() {
        weakReferences.clear();
    }
}
