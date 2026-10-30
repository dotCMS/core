package com.dotcms.cache;

import com.dotmarketing.util.Logger;
import io.vavr.Lazy;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
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
 * <p>
 * Besides the class, a read also stops when the object graph is nested deeper than
 * {@value #MAX_DEPTH_PROPERTY}, holds more objects than {@value #MAX_REFERENCES_PROPERTY}, or
 * declares an array longer than the value itself (see {@link #newObjectInputStream(byte[])}).
 * <p>
 * The properties are read once, the first time a value is read back, so changing them needs a
 * restart.
 */
public final class CacheObjectInputFilter implements ObjectInputFilter {

    /** Comma separated patterns that are allowed on top of the default ones. */
    public static final String ALLOWED_CLASSES_PROPERTY = "CACHE_SERIALIZATION_ALLOWED_CLASSES";

    /** How deep the object graph of one cache value can be nested. */
    public static final String MAX_DEPTH_PROPERTY = "CACHE_SERIALIZATION_MAX_DEPTH";

    /** How many objects and back-references one cache value can hold. */
    public static final String MAX_REFERENCES_PROPERTY = "CACHE_SERIALIZATION_MAX_REFERENCES";

    static final long DEFAULT_MAX_DEPTH = 200;
    static final long DEFAULT_MAX_REFERENCES = 10_000_000;

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
            com.dotmarketing.util.Config.getLongProperty(MAX_DEPTH_PROPERTY, DEFAULT_MAX_DEPTH),
            com.dotmarketing.util.Config.getLongProperty(MAX_REFERENCES_PROPERTY, DEFAULT_MAX_REFERENCES),
            com.dotmarketing.util.Config.getStringArrayProperty(ALLOWED_CLASSES_PROPERTY, new String[0])));

    private final long maxDepth;
    private final long maxReferences;
    private final Patterns allowedPatterns;
    private final Patterns excludedPatterns = new Patterns(EXCLUDED_PATTERNS);
    private final Set<String> reportedLimits = ConcurrentHashMap.newKeySet();

    // the answer per class is computed once and kept on the class itself
    private final ClassValue<Boolean> allowedClasses = new ClassValue<>() {
        @Override
        protected Boolean computeValue(final Class<?> type) {
            final boolean allowed = isAllowed(type);
            if (!allowed) {
                Logger.warn(CacheObjectInputFilter.class, "Cache value of class " + type.getName()
                        + " was not read back. If this class is a cache value, add it to "
                        + ALLOWED_CLASSES_PROPERTY);
            }
            return allowed;
        }
    };

    /**
     * Creates a filter with the default patterns and limits plus the given patterns.
     *
     * @param extraPatterns patterns allowed on top of {@link #DEFAULT_PATTERNS}
     */
    public CacheObjectInputFilter(final String... extraPatterns) {
        this(DEFAULT_MAX_DEPTH, DEFAULT_MAX_REFERENCES, extraPatterns);
    }

    /**
     * Creates a filter with the given limits, the default patterns and the given patterns.
     *
     * @param maxDepth      how deep the object graph can be nested
     * @param maxReferences how many objects and back-references the value can hold
     * @param extraPatterns patterns allowed on top of {@link #DEFAULT_PATTERNS}
     */
    CacheObjectInputFilter(final long maxDepth, final long maxReferences, final String... extraPatterns) {
        this.maxDepth = maxDepth;
        this.maxReferences = maxReferences;
        this.allowedPatterns = new Patterns(Stream.concat(DEFAULT_PATTERNS.stream(), Stream.of(extraPatterns))
                .map(String::trim)
                .filter(pattern -> !pattern.isEmpty())
                .collect(Collectors.toList()));
    }

    /**
     * Returns the shared filter, built from the default patterns and the configuration properties.
     *
     * @return the shared filter
     */
    public static CacheObjectInputFilter getInstance() {
        return INSTANCE.get();
    }

    /**
     * Opens an {@link ObjectInputStream} over a serialized cache value with the shared filter set.
     * The stream also stops at an array that declares more elements than the value has bytes: every
     * element takes at least one byte, so such a length cannot come from a real value, and checking
     * it here keeps the array from being allocated.
     *
     * @param data the serialized cache value
     * @return a stream that only reads the allowed classes
     * @throws IOException if the stream header cannot be read
     */
    public static ObjectInputStream newObjectInputStream(final byte[] data) throws IOException {
        final ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(data));
        objectInputStream.setObjectInputFilter(getInstance().forValueOfLength(data.length));
        return objectInputStream;
    }

    /**
     * Returns this filter plus the array length check for a value of the given size.
     *
     * @param valueLength the size of the serialized value, in bytes
     * @return the filter to set on the stream that reads the value
     */
    ObjectInputFilter forValueOfLength(final long valueLength) {
        return filterInfo -> filterInfo.arrayLength() > valueLength
                ? overLimit("array length", filterInfo.arrayLength(), valueLength, "the value size")
                : checkInput(filterInfo);
    }

    /**
     * Allows primitive types, arrays of allowed types and classes that match the allowed patterns;
     * rejects proxies, every other class, and graphs over the depth or reference limits.
     *
     * @param filterInfo the class, array length, depth and references of the current read
     * @return {@link Status#ALLOWED}, {@link Status#REJECTED}, or {@link Status#UNDECIDED} when there
     *         is no class to check
     */
    @Override
    public Status checkInput(final FilterInfo filterInfo) {
        if (filterInfo.depth() > maxDepth) {
            return overLimit("depth", filterInfo.depth(), maxDepth, MAX_DEPTH_PROPERTY);
        }
        if (filterInfo.references() > maxReferences) {
            return overLimit("references", filterInfo.references(), maxReferences, MAX_REFERENCES_PROPERTY);
        }
        final Class<?> serialClass = filterInfo.serialClass();
        if (null == serialClass) {
            return Status.UNDECIDED;
        }
        return allowedClasses.get(serialClass) ? Status.ALLOWED : Status.REJECTED;
    }

    private Status overLimit(final String limit, final long value, final long max, final String setBy) {
        if (reportedLimits.add(limit)) {
            Logger.warn(CacheObjectInputFilter.class, "Cache value was not read back: its " + limit + " ("
                    + value + ") is over " + max + ", set by " + setBy);
        }
        return Status.REJECTED;
    }

    private boolean isAllowed(final Class<?> type) {
        Class<?> componentType = type;
        while (componentType.isArray()) {
            componentType = componentType.getComponentType();
        }
        return componentType.isPrimitive()
                || (!Proxy.isProxyClass(componentType) && isAllowed(componentType.getName()));
    }

    /**
     * Tells whether a class name matches the allowed patterns and none of the excluded ones.
     *
     * @param className the fully qualified class name
     * @return {@code true} if the class can be read back
     */
    boolean isAllowed(final String className) {
        return !excludedPatterns.matches(className) && allowedPatterns.matches(className);
    }

    /** Patterns split by kind up front, so matching a class name allocates nothing. */
    private static final class Patterns {

        private final Set<String> classNames;
        private final String[] packagesAndSubpackages;
        private final String[] packages;

        Patterns(final List<String> patterns) {
            final List<String> deep = new ArrayList<>();
            final List<String> direct = new ArrayList<>();
            final List<String> exact = new ArrayList<>();
            for (final String pattern : patterns) {
                if (pattern.endsWith(".**")) {
                    deep.add(pattern.substring(0, pattern.length() - 2));
                } else if (pattern.endsWith(".*")) {
                    direct.add(pattern.substring(0, pattern.length() - 1));
                } else {
                    exact.add(pattern);
                }
            }
            this.classNames = Set.copyOf(exact);
            this.packagesAndSubpackages = deep.toArray(new String[0]);
            this.packages = direct.toArray(new String[0]);
        }

        boolean matches(final String className) {
            if (classNames.contains(className)) {
                return true;
            }
            for (final String prefix : packagesAndSubpackages) {
                if (className.startsWith(prefix)) {
                    return true;
                }
            }
            for (final String prefix : packages) {
                if (className.startsWith(prefix) && className.indexOf('.', prefix.length()) < 0) {
                    return true;
                }
            }
            return false;
        }
    }
}
