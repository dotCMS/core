package com.dotmarketing.business.cache.provider;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.openjdk.jol.info.ClassLayout;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.github.benmanes.caffeine.base.UnsafeAccess;
import io.vavr.control.Try;


public class CacheSizingUtil {


    final int standardSampleSize = Config.getIntProperty("CACHE_SIZER_SAMPLE_SIZE", 20);

    public String averageSizePretty(final Map<String, Object> cacheMap) {

        return UtilMethods.prettyByteify(averageSize(cacheMap));

    }


    public long averageSize(final Map<String, Object> cacheMap) {

        if (cacheMap.size() == 0) {
            return 0;
        }

        long totalSize = 0;

        final int numberToSample = Math.min(cacheMap.size(), standardSampleSize);


        for (int i = 0; i < numberToSample; i++) {
            List<String> keysAsArray = new ArrayList<String>(cacheMap.keySet());
            Random r = new Random();
            Object object = cacheMap.get(keysAsArray.get(r.nextInt(keysAsArray.size())));
            if (object instanceof Optional && ((Optional) object).isPresent()) {
                object = ((Optional) object).get();
            }
            if (object instanceof Optional && !((Optional) object).isPresent()) {
                return 0;
            }
            totalSize += sizeOf(object);
        }
        return totalSize / numberToSample;

    }

    public long sizeOf(Object object) {
        long retainedSize = Try.of(()->retainedSize(object)).getOrElse(0);
        
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

        if(object==null) {
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
            objectOutputStream.flush();
            objectOutputStream.close();

            return byteOutputStream.toByteArray().length;
        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), "failed writing:" + object.getClass() + " : " + e.getMessage(), e);
            return 0;
        }

    }

    /**
     * returns the size of an objects class, based on the classes properties
     * 
     * @param object
     * @return
     */
    long sizeOfClass(Object object) {

        return ClassLayout.parseClass(object.getClass()).instanceSize();
    }

    public static boolean useCompressedOops = true;

    public static int retainedSize(Object obj) {
        return retainedSize(obj, new HashMap<>());
    }

    @SuppressWarnings("restriction")
    private static int retainedSize(Object obj, HashMap<Object, Object> calculated) {
        Object ref = null;
        try {
            if (obj == null)
                throw new NullPointerException();
            calculated.put(obj, obj);
            Class<?> cls = obj.getClass();
            if (cls.isArray()) {
                int arraysize = UnsafeAccess.UNSAFE.arrayBaseOffset(cls)
                                + UnsafeAccess.UNSAFE.arrayIndexScale(cls) * Array.getLength(obj);
                if (!cls.getComponentType().isPrimitive()) {
                    Object[] arr = (Object[]) obj;
                    for (Object comp : arr) {
                        if (comp != null && !isCalculated(calculated, comp))
                            arraysize += retainedSize(comp, calculated);
                    }
                }
                return arraysize;
            } else {
                int objectsize = sizeof(cls);
                for (Field f : getAllNonStaticFields(obj.getClass())) {
                    Class<?> fcls = f.getType();
                    if (fcls.isPrimitive())
                        continue;
                    f.setAccessible(true);
                     ref = f.get(obj);
                    if (ref != null && !isCalculated(calculated, ref)) {
                        calculated.put(ref, ref);
                        int referentSize = retainedSize(ref, calculated);
                        objectsize += referentSize;
                    }
                }
                return objectsize;
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("restriction")
    public static int sizeof(Class<?> cls) {

        if (cls == null)
            throw new NullPointerException();

        if (cls.isArray())
            throw new IllegalArgumentException();

        if (cls.isPrimitive())
            return primsize(cls);

        int lastOffset = Integer.MIN_VALUE;
        Class<?> lastClass = null;

        for (Field f : getAllNonStaticFields(cls)) {
            if (Modifier.isStatic(f.getModifiers()))
                continue;

            int offset = (int) UnsafeAccess.UNSAFE.objectFieldOffset(f);
            if (offset > lastOffset) {
                lastOffset = offset;
                lastClass = f.getClass();
            }
        }
        if (lastOffset > 0)
            return modulo8(lastOffset + primsize(lastClass));
        else
            return 16;
    }

    private static Field[] getAllNonStaticFields(Class<?> cls) {
        if (cls == null)
            throw new NullPointerException();

        List<Field> fieldList = new ArrayList<Field>();
        while (cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()))
                    fieldList.add(f);
            }
            cls = cls.getSuperclass();
        }
        Field[] fs = new Field[fieldList.size()];
        fieldList.toArray(fs);
        return fs;
    }

    private static boolean isCalculated(HashMap<Object, Object> calculated, Object test) {
        Object that = calculated.get(test);
        return that != null && that == test;
    }

    private static int primsize(Class<?> cls) {
        if (cls == byte.class)
            return 1;
        if (cls == boolean.class)
            return 1;
        if (cls == char.class)
            return 2;
        if (cls == short.class)
            return 2;
        if (cls == int.class)
            return 4;
        if (cls == float.class)
            return 4;
        if (cls == long.class)
            return 8;
        if (cls == double.class)
            return 8;
        else
            return useCompressedOops ? 4 : 8;
    }

    private static int modulo8(int value) {
        return (value & 0x7) > 0 ? (value & ~0x7) + 8 : value;
    }


}

