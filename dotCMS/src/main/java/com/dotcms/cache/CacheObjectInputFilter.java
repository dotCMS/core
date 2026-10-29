package com.dotcms.cache;

import com.dotmarketing.util.Logger;
import io.vavr.Lazy;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class filter for cache values read back from Redis. Only the classes the caches store (dotCMS
 * classes and the JDK, Guava, Vavr and Jackson value types they hold) are deserialized; any other
 * class stops the read with an {@link java.io.InvalidClassException}, which the Redis cache handles
 * as a miss.
 * <p>
 * Patterns are package or class names: {@code pkg.**} matches the package and its subpackages,
 * {@code pkg.*} matches the classes directly in the package, anything else matches one class. More
 * patterns can be added with the {@value #ALLOWED_CLASSES_PROPERTY} property (comma separated), for
 * example for classes that a plugin puts in the cache.
 */
public final class CacheObjectInputFilter implements ObjectInputFilter {

    /** Comma separated patterns that are allowed on top of the default ones. */
    public static final String ALLOWED_CLASSES_PROPERTY = "CACHE_SERIALIZATION_ALLOWED_CLASSES";

    static final List<String> DEFAULT_PATTERNS = List.of(
            "com.dotcms.**",
            "com.dotmarketing.**",
            "com.liferay.**",
            "java.lang.*",
            "java.util.**",
            "java.time.**",
            "java.math.*",
            "java.sql.*",
            "java.text.*",
            "com.google.common.base.*",
            "com.google.common.collect.**",
            "io.vavr.**",
            "com.fasterxml.jackson.databind.node.*",
            // the TimeZone implementation a User carries
            "sun.util.calendar.ZoneInfo",
            // the keys held by the Apps secrets cache
            "javax.crypto.spec.SecretKeySpec");

    /** Third-party libraries shipped under the dotCMS namespace; they are not cache values. */
    static final List<String> EXCLUDED_PATTERNS = List.of("com.dotcms.repackage.**");

    // fully qualified: ObjectInputFilter declares its own nested Config class
    private static final Lazy<CacheObjectInputFilter> INSTANCE = Lazy.of(() -> new CacheObjectInputFilter(
            com.dotmarketing.util.Config.getStringArrayProperty(ALLOWED_CLASSES_PROPERTY, new String[0])));

    private final List<String> allowedPatterns;
    private final Set<String> reportedClasses = ConcurrentHashMap.newKeySet();

    /**
     * Creates a filter with the default patterns plus the given ones.
     *
     * @param extraPatterns patterns allowed on top of {@link #DEFAULT_PATTERNS}
     */
    public CacheObjectInputFilter(final String... extraPatterns) {
        this.allowedPatterns = Stream.concat(DEFAULT_PATTERNS.stream(), Stream.of(extraPatterns))
                .map(String::trim)
                .filter(pattern -> !pattern.isEmpty())
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns the shared filter, built from the default patterns and {@value #ALLOWED_CLASSES_PROPERTY}.
     *
     * @return the shared filter
     */
    public static CacheObjectInputFilter getInstance() {
        return INSTANCE.get();
    }

    /**
     * Opens an {@link ObjectInputStream} over the given stream with the shared filter set.
     *
     * @param in the serialized cache value
     * @return a stream that only reads the allowed classes
     * @throws IOException if the stream header cannot be read
     */
    public static ObjectInputStream newObjectInputStream(final InputStream in) throws IOException {
        final ObjectInputStream objectInputStream = new ObjectInputStream(in);
        objectInputStream.setObjectInputFilter(getInstance());
        return objectInputStream;
    }

    /**
     * Allows primitive types, arrays of allowed types and classes that match the allowed patterns;
     * rejects proxies and every other class.
     *
     * @param filterInfo the class, array length, depth and references of the current read
     * @return {@link Status#ALLOWED}, {@link Status#REJECTED}, or {@link Status#UNDECIDED} when there
     *         is no class to check
     */
    @Override
    public Status checkInput(final FilterInfo filterInfo) {
        Class<?> serialClass = filterInfo.serialClass();
        if (null == serialClass) {
            return Status.UNDECIDED;
        }
        while (serialClass.isArray()) {
            serialClass = serialClass.getComponentType();
        }
        if (serialClass.isPrimitive()) {
            return Status.ALLOWED;
        }
        if (!Proxy.isProxyClass(serialClass) && isAllowed(serialClass.getName())) {
            return Status.ALLOWED;
        }
        if (reportedClasses.add(serialClass.getName())) {
            Logger.warn(CacheObjectInputFilter.class, "Cache value of class " + serialClass.getName()
                    + " was not read back. If this class is a cache value, add it to "
                    + ALLOWED_CLASSES_PROPERTY);
        }
        return Status.REJECTED;
    }

    /**
     * Tells whether a class name matches the allowed patterns and none of the excluded ones.
     *
     * @param className the fully qualified class name
     * @return {@code true} if the class can be read back
     */
    boolean isAllowed(final String className) {
        return EXCLUDED_PATTERNS.stream().noneMatch(pattern -> matches(pattern, className))
                && allowedPatterns.stream().anyMatch(pattern -> matches(pattern, className));
    }

    private static boolean matches(final String pattern, final String className) {
        if (pattern.endsWith(".**")) {
            return className.startsWith(pattern.substring(0, pattern.length() - 2));
        }
        if (pattern.endsWith(".*")) {
            final String packageName = pattern.substring(0, pattern.length() - 1);
            return className.startsWith(packageName)
                    && className.indexOf('.', packageName.length()) < 0;
        }
        return className.equals(pattern);
    }
}
