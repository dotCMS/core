package com.dotcms.util;

import com.dotmarketing.util.Logger;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for class metadata scanning using Jandex for improved performance.
 * This provides a faster alternative to reflection-based class analysis including:
 * - Annotation scanning
 * - Class hierarchy analysis (subclasses, implementations)
 * - Method and field metadata
 * - Interface implementation discovery
 * 
 * @author dotCMS
 */
public class JandexClassMetadataScanner {
    
    private static volatile Index jandexIndex;
    private static final Object jandexIndexLock = new Object();
    
    /**
     * Initialize and get the Jandex index for fast annotation scanning
     * Uses double-checked locking pattern for thread-safe lazy initialization
     * 
     * @return The Jandex index, or null if not available
     */
    public static Index getJandexIndex() {
        Index index = jandexIndex; // First check (volatile read)
        if (index == null) {
            synchronized (jandexIndexLock) {
                index = jandexIndex; // Second check (double-checked locking)
                if (index == null) {
                    index = initializeJandexIndex();
                    jandexIndex = index; // Volatile write
                }
            }
        }
        return index;
    }
    
    /**
     * Initialize the Jandex index with proper resource management
     * 
     * @return The initialized Jandex index, or null if not available
     */
    private static Index initializeJandexIndex() {
        try {
            // Try to load from META-INF/jandex.idx first (most common location)
            URL indexUrl = JandexClassMetadataScanner.class.getClassLoader()
                .getResource("META-INF/jandex.idx");
            
            if (indexUrl != null) {
                try (var inputStream = indexUrl.openStream()) {
                    Index index = new IndexReader(inputStream).read();
                    Logger.info(JandexClassMetadataScanner.class, "Loaded Jandex index from META-INF/jandex.idx");
                    return index;
                }
            } else {
                // Try alternative locations
                indexUrl = JandexClassMetadataScanner.class.getClassLoader()
                    .getResource("META-INF/classes.idx");
                
                if (indexUrl != null) {
                    try (var inputStream = indexUrl.openStream()) {
                        Index index = new IndexReader(inputStream).read();
                        Logger.info(JandexClassMetadataScanner.class, "Loaded Jandex index from META-INF/classes.idx");
                        return index;
                    }
                } else {
                    Logger.warn(JandexClassMetadataScanner.class, "No Jandex index found, falling back to reflection-based scanning");
                    return null;
                }
            }
        } catch (Exception e) {
            Logger.warn(JandexClassMetadataScanner.class, "Failed to load Jandex index: " + e.getMessage() + ", falling back to reflection", e);
            return null;
        }
    }
    
    /**
     * Find all classes annotated with a specific annotation using Jandex
     * 
     * @param annotationName The fully qualified name of the annotation to search for
     * @param packagePrefixes Optional package prefixes to filter results
     * @return List of class names that have the annotation
     */
    public static List<String> findClassesWithAnnotation(String annotationName, String... packagePrefixes) {
        Index index = getJandexIndex();
        if (index == null) {
            return new ArrayList<>();
        }
        
        DotName annotationDotName = DotName.createSimple(annotationName);
        List<ClassInfo> annotatedClasses = index.getAnnotations(annotationDotName).stream()
            .map(AnnotationInstance::target)
            .filter(target -> target.kind() == org.jboss.jandex.AnnotationTarget.Kind.CLASS)
            .map(target -> target.asClass())
            .collect(Collectors.toList());
        
        return annotatedClasses.stream()
            .map(classInfo -> classInfo.name().toString())
            .filter(className -> {
                if (packagePrefixes.length == 0) {
                    return true;
                }
                for (String prefix : packagePrefixes) {
                    if (className.startsWith(prefix)) {
                        return true;
                    }
                }
                return false;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Find all classes annotated with a specific annotation and load them
     * 
     * @param annotationClass The annotation class to search for
     * @param packagePrefixes Optional package prefixes to filter results
     * @return List of loaded classes that have the annotation
     */
    public static List<Class<?>> findClassesWithAnnotation(Class<?> annotationClass, String... packagePrefixes) {
        List<String> classNames = findClassesWithAnnotation(annotationClass.getName(), packagePrefixes);
        List<Class<?>> classes = new ArrayList<>();
        
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                classes.add(clazz);
            } catch (ClassNotFoundException | NoClassDefFoundError | ExceptionInInitializerError e) {
                // Skip classes that can't be loaded or initialized
                Logger.warn(JandexClassMetadataScanner.class, "Could not load class " + className + ": " + e.getMessage());
            }
        }
        
        return classes;
    }
    
    /**
     * Check if a class has a specific annotation using Jandex
     * 
     * @param className The fully qualified class name
     * @param annotationName The fully qualified annotation name
     * @return true if the class has the annotation
     */
    public static boolean hasClassAnnotation(String className, String annotationName) {
        Index index = getJandexIndex();
        if (index == null) {
            return false;
        }
        
        DotName classDotName = DotName.createSimple(className);
        DotName annotationDotName = DotName.createSimple(annotationName);
        
        ClassInfo classInfo = index.getClassByName(classDotName);
        return classInfo != null && classInfo.classAnnotation(annotationDotName) != null;
    }
    
    /**
     * Get annotation value from a class using Jandex
     * 
     * @param className The fully qualified class name
     * @param annotationName The fully qualified annotation name
     * @param valueName The name of the annotation value to retrieve
     * @return The annotation value as a string, or null if not found
     */
    public static String getClassAnnotationValue(String className, String annotationName, String valueName) {
        Index index = getJandexIndex();
        if (index == null) {
            return null;
        }
        
        DotName classDotName = DotName.createSimple(className);
        DotName annotationDotName = DotName.createSimple(annotationName);
        
        ClassInfo classInfo = index.getClassByName(classDotName);
        if (classInfo != null) {
            AnnotationInstance annotation = classInfo.classAnnotation(annotationDotName);
            if (annotation != null) {
                AnnotationValue value = annotation.value(valueName);
                if (value != null) {
                    return value.asString();
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get annotation value as integer from a class using Jandex
     * 
     * @param className The fully qualified class name
     * @param annotationName The fully qualified annotation name
     * @param valueName The name of the annotation value to retrieve
     * @return The annotation value as an integer, or null if not found
     */
    public static Integer getClassAnnotationIntValue(String className, String annotationName, String valueName) {
        Index index = getJandexIndex();
        if (index == null) {
            return null;
        }
        
        DotName classDotName = DotName.createSimple(className);
        DotName annotationDotName = DotName.createSimple(annotationName);
        
        ClassInfo classInfo = index.getClassByName(classDotName);
        if (classInfo != null) {
            AnnotationInstance annotation = classInfo.classAnnotation(annotationDotName);
            if (annotation != null) {
                AnnotationValue value = annotation.value(valueName);
                if (value != null) {
                    return value.asInt();
                }
            }
        }
        
        return null;
    }
    
    /**
     * Check if Jandex is available and working
     * 
     * @return true if Jandex index is available
     */
    public static boolean isJandexAvailable() {
        return getJandexIndex() != null;
    }
    
    // ===============================================================================================
    // CLASS HIERARCHY AND METADATA METHODS
    // ===============================================================================================
    
    /**
     * Find all known implementations of an interface using Jandex
     * 
     * @param interfaceName The fully qualified interface name
     * @param packagePrefixes Optional package prefixes to filter results
     * @return List of class names that implement the interface
     */
    public static List<String> findImplementationsOf(String interfaceName, String... packagePrefixes) {
        Index index = getJandexIndex();
        if (index == null) {
            return new ArrayList<>();
        }
        
        DotName interfaceDotName = DotName.createSimple(interfaceName);
        Collection<ClassInfo> implementations = index.getAllKnownImplementors(interfaceDotName);
        
        return implementations.stream()
            .map(classInfo -> classInfo.name().toString())
            .filter(className -> {
                if (packagePrefixes.length == 0) {
                    return true;
                }
                for (String prefix : packagePrefixes) {
                    if (className.startsWith(prefix)) {
                        return true;
                    }
                }
                return false;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Find all known implementations of an interface and load them
     * 
     * @param interfaceClass The interface class to find implementations for
     * @param packagePrefixes Optional package prefixes to filter results
     * @return List of loaded classes that implement the interface
     */
    public static List<Class<?>> findImplementationsOf(Class<?> interfaceClass, String... packagePrefixes) {
        List<String> classNames = findImplementationsOf(interfaceClass.getName(), packagePrefixes);
        List<Class<?>> classes = new ArrayList<>();
        
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                classes.add(clazz);
            } catch (ClassNotFoundException | NoClassDefFoundError | ExceptionInInitializerError e) {
                Logger.warn(JandexClassMetadataScanner.class, "Could not load implementation class " + className + ": " + e.getMessage());
            }
        }
        
        return classes;
    }
    
    /**
     * Find all known subclasses of a class using Jandex
     * 
     * @param className The fully qualified class name
     * @param packagePrefixes Optional package prefixes to filter results
     * @return List of class names that extend the class
     */
    public static List<String> findSubclassesOf(String className, String... packagePrefixes) {
        Index index = getJandexIndex();
        if (index == null) {
            return new ArrayList<>();
        }
        
        DotName classDotName = DotName.createSimple(className);
        Collection<ClassInfo> subclasses = index.getAllKnownSubclasses(classDotName);
        
        return subclasses.stream()
            .map(classInfo -> classInfo.name().toString())
            .filter(subclassName -> {
                if (packagePrefixes.length == 0) {
                    return true;
                }
                for (String prefix : packagePrefixes) {
                    if (subclassName.startsWith(prefix)) {
                        return true;
                    }
                }
                return false;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Find all known subclasses of a class and load them
     * 
     * @param parentClass The parent class to find subclasses for
     * @param packagePrefixes Optional package prefixes to filter results
     * @return List of loaded classes that extend the parent class
     */
    public static List<Class<?>> findSubclassesOf(Class<?> parentClass, String... packagePrefixes) {
        List<String> classNames = findSubclassesOf(parentClass.getName(), packagePrefixes);
        List<Class<?>> classes = new ArrayList<>();
        
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                classes.add(clazz);
            } catch (ClassNotFoundException | NoClassDefFoundError | ExceptionInInitializerError e) {
                Logger.warn(JandexClassMetadataScanner.class, "Could not load subclass " + className + ": " + e.getMessage());
            }
        }
        
        return classes;
    }
    
    /**
     * Get detailed class information using Jandex
     * 
     * @param className The fully qualified class name
     * @return ClassInfo object or null if not found
     */
    public static ClassInfo getClassInfo(String className) {
        Index index = getJandexIndex();
        if (index == null) {
            return null;
        }
        
        DotName classDotName = DotName.createSimple(className);
        return index.getClassByName(classDotName);
    }
    
    /**
     * Get methods of a class that have a specific annotation
     * 
     * @param className The fully qualified class name
     * @param annotationName The fully qualified annotation name
     * @return List of method names that have the annotation
     */
    public static List<String> getMethodsWithAnnotation(String className, String annotationName) {
        ClassInfo classInfo = getClassInfo(className);
        if (classInfo == null) {
            return new ArrayList<>();
        }
        
        DotName annotationDotName = DotName.createSimple(annotationName);
        return classInfo.methods().stream()
            .filter(method -> method.hasAnnotation(annotationDotName))
            .map(method -> method.name())
            .collect(Collectors.toList());
    }
    
    /**
     * Get fields of a class that have a specific annotation
     * 
     * @param className The fully qualified class name
     * @param annotationName The fully qualified annotation name
     * @return List of field names that have the annotation
     */
    public static List<String> getFieldsWithAnnotation(String className, String annotationName) {
        ClassInfo classInfo = getClassInfo(className);
        if (classInfo == null) {
            return new ArrayList<>();
        }
        
        DotName annotationDotName = DotName.createSimple(annotationName);
        return classInfo.fields().stream()
            .filter(field -> field.hasAnnotation(annotationDotName))
            .map(field -> field.name())
            .collect(Collectors.toList());
    }
    
    /**
     * Get the superclass name of a class
     * 
     * @param className The fully qualified class name
     * @return The superclass name or null if not found or no superclass
     */
    public static String getSuperclassName(String className) {
        ClassInfo classInfo = getClassInfo(className);
        if (classInfo == null || classInfo.superName() == null) {
            return null;
        }
        
        return classInfo.superName().toString();
    }
    
    /**
     * Get the interface names implemented by a class
     * 
     * @param className The fully qualified class name
     * @return List of interface names implemented by the class
     */
    public static List<String> getInterfaceNames(String className) {
        ClassInfo classInfo = getClassInfo(className);
        if (classInfo == null) {
            return new ArrayList<>();
        }
        
        return classInfo.interfaceNames().stream()
            .map(DotName::toString)
            .collect(Collectors.toList());
    }
    
    /**
     * Check if a class implements a specific interface
     * 
     * @param className The fully qualified class name
     * @param interfaceName The fully qualified interface name
     * @return true if the class implements the interface
     */
    public static boolean implementsInterface(String className, String interfaceName) {
        List<String> interfaces = getInterfaceNames(className);
        return interfaces.contains(interfaceName);
    }
    
    /**
     * Check if a class extends a specific superclass
     * 
     * @param className The fully qualified class name
     * @param superclassName The fully qualified superclass name
     * @return true if the class extends the superclass
     */
    public static boolean extendsSuperclass(String className, String superclassName) {
        String actualSuperclass = getSuperclassName(className);
        return superclassName.equals(actualSuperclass);
    }
}