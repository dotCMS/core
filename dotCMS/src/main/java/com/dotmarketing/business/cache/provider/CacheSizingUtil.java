package com.dotmarketing.business.cache.provider;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.github.benmanes.caffeine.base.UnsafeAccess;
import net.bytebuddy.agent.ByteBuddyAgent;


public class CacheSizingUtil {


    final int standardSampleSize = Config.getIntProperty("CACHE_SIZER_SAMPLE_SIZE", 20);
    
    final int maxRecusionDepth = 10;
    
    final boolean useCompressedOops = true;

    /**
     * Authoritative object sizer, used when a byte-buddy {@code -javaagent} is present (the dotCMS
     * container preloads one; the dotcms-core surefire run does too). We only read an
     * ALREADY-installed agent via {@link ByteBuddyAgent#getInstrumentation()} and NEVER call
     * {@link ByteBuddyAgent#install()} — its self-attach fallback can hang on JDK 21+ in containers.
     * When no agent is present, sizing falls back to the {@code Unsafe} estimate below.
     */
    private static final Instrumentation INSTRUMENTATION = resolveInstrumentationOrNull();

    private static Instrumentation resolveInstrumentationOrNull() {
        try {
            return ByteBuddyAgent.getInstrumentation();
        } catch (Throwable noAgentPreloaded) { // IllegalStateException when not preloaded
            return null;
        }
    }

    public String averageSizePretty(final Map<String, Object> cacheMap) {

        return UtilMethods.prettyByteify(averageSize(cacheMap));

    }

    /**
     * Takes a map and pulls random values from it to calculate an average object size
     * 
     * @param cacheMap
     * @return
     */
    public long averageSize(final Map<String, Object> cacheMap) {

        if (cacheMap.size() == 0) {
            return 0;
        }

        long totalSize = 0;

        final int numberToSample = Math.min(cacheMap.size(), standardSampleSize);


        for (int i = 0; i < numberToSample; i++) {
            List<String> keysAsArray = new ArrayList<>(cacheMap.keySet());
            Random r = new Random();
            Object object = cacheMap.get(keysAsArray.get(r.nextInt(keysAsArray.size())));
            if (object instanceof Optional && ((Optional) object).isPresent()) {
                object = ((Optional) object).get();
            }
            if (object instanceof Optional && ((Optional)object).isEmpty()) {
                return 0;
            }
            totalSize += sizeOf(object);
        }
        return totalSize / numberToSample;

    }

    /**
     * Tries to use the Unsafe class to calculate an objects size. If this does not work, it will fall
     * back on the serialzied size (which less than accurate).
     * 
     * @param cacheMap
     * @return
     */
    public long sizeOf(Object object) {
        long retainedSize = 0;
        try {
            retainedSize = retainedSize(object);
        } catch (Throwable t) {
            Logger.infoEvery(this.getClass(),
                            "Unable to use the Unsafe.class to size cache objects, falling back to serialization:"
                                            + t.getMessage(),
                            60000);
        }

        return retainedSize > 0 ? retainedSize : sizeOfSerialized(object);

    }


    final ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();

    /**
     * Serializes an object to attempt to find an object's size
     * 
     * @param object
     * @return
     */
    long sizeOfSerialized(Object object) {

        if (object == null) {
            return 0;
        }

        if (!(object instanceof Serializable)) {
            Logger.warn(this.getClass(), object.getClass() + " not serializable: " + object);
            return -1;
        }

        if (object instanceof String) {
            return ((String) object).length() * 8;

        }

        byteOutputStream.reset();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOutputStream)) {
            objectOutputStream.writeObject(object);
            objectOutputStream.close();

            return byteOutputStream.toByteArray().length;
        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), "failed writing:" + object.getClass() + " : " + e.getMessage(), e);
            return 0;
        }

    }




    public long retainedSize(Object obj) {
        return retainedSize(obj, new HashMap<>(), 0);
    }

    /**
     * Shallow size of a single object: the authoritative {@link Instrumentation#getObjectSize(Object)}
     * when a byte-buddy agent is present, otherwise an {@code Unsafe}-based estimate from live field
     * offsets. Both reflect the actual object layout (compact object headers, compressed oops), so the
     * recursive retained size adapts automatically.
     */
    private long shallowSize(final Object obj, final Class<?> cls) {
        if (INSTRUMENTATION != null) {
            return INSTRUMENTATION.getObjectSize(obj);
        }
        if (cls.isArray()) {
            return (long) UnsafeAccess.UNSAFE.arrayBaseOffset(cls)
                    + (long) UnsafeAccess.UNSAFE.arrayIndexScale(cls) * Array.getLength(obj);
        }
        return sizeof(cls);
    }

    @SuppressWarnings("restriction")
    private long retainedSize(Object obj, HashMap<Object, Object> calculated, final int depth) {
        try {
            if (obj == null) {
                throw new NullPointerException();
            }
            calculated.put(obj, obj);
            final Class<?> cls = obj.getClass();
            long size = shallowSize(obj, cls);
            if (cls.isArray()) {
                if (!cls.getComponentType().isPrimitive()) {
                    for (Object comp : (Object[]) obj) {
                        if (comp != null && !isCalculated(calculated, comp) && depth < maxRecusionDepth) {
                            size += retainedSize(comp, calculated, depth + 1);
                        }
                    }
                }
                return size;
            }
            for (Field f : getAllNonStaticFields(cls)) {
                if (f.getType().isPrimitive()) {
                    continue;
                }
                f.setAccessible(true);
                final Object ref = f.get(obj);
                if (ref != null && !isCalculated(calculated, ref) && depth < maxRecusionDepth) {
                    calculated.put(ref, ref);
                    size += retainedSize(ref, calculated, depth + 1);
                }
            }
            return size;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("restriction")
    public int sizeof(Class<?> cls) {

        if (cls == null) {
            throw new NullPointerException();
        }

        if (cls.isArray()) {
            throw new IllegalArgumentException();
        }

        if (cls.isPrimitive()) {
            return primsize(cls);
        }

        int lastOffset = Integer.MIN_VALUE;
        Class<?> lastClass = null;

        for (Field f : getAllNonStaticFields(cls)) {
            if (Modifier.isStatic(f.getModifiers())) {
                continue;
            }

            int offset = (int) UnsafeAccess.UNSAFE.objectFieldOffset(f);
            if (offset > lastOffset) {
                lastOffset = offset;
                lastClass = f.getType();
            }
        }
        if (lastOffset > 0) {
            return modulo8(lastOffset + primsize(lastClass));
        }

        return 16;
    }

    private Field[] getAllNonStaticFields(Class<?> cls) {
        if (cls == null) {
            throw new NullPointerException();
        }

        List<Field> fieldList = new ArrayList<>();
        while (cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    fieldList.add(f);
                }
            }
            cls = cls.getSuperclass();
        }
        Field[] fs = new Field[fieldList.size()];
        fieldList.toArray(fs);
        return fs;
    }

    private boolean isCalculated(HashMap<Object, Object> calculated, Object test) {
        Object that = calculated.get(test);
        return that != null && that == test;
    }

    private int primsize(Class<?> cls) {
        if (cls == byte.class) {
            return 1;
        }
        if (cls == boolean.class) {
            return 1;
        }
        if (cls == char.class) {
            return 2;
        }
        if (cls == short.class) {
            return 2;
        }
        if (cls == int.class) {
            return 4;
        }
        if (cls == float.class) {
            return 4;
        }
        if (cls == long.class) {
            return 8;
        }
        if (cls == double.class) {
            return 8;
        }
        else {
            return useCompressedOops ? 4 : 8;
        }
    }

    private int modulo8(int value) {
        return (value & 0x7) > 0 ? (value & ~0x7) + 8 : value;
    }


}

