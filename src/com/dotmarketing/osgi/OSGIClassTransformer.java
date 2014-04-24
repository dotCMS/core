package com.dotmarketing.osgi;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * Created by Jonathan Gamba
 * Date: 2/1/13
 */
public class OSGIClassTransformer implements ClassFileTransformer {

    /**
     * The internal form class name of the class to transform
     */
    protected String className;
    /**
     * The class loader of the class
     */
    protected ClassLoader classLoader;

    public OSGIClassTransformer ( String className, ClassLoader classLoader ) {
        this.className = className.replace( '.', '/' );
        this.classLoader = classLoader;
    }

    public byte[] transform ( ClassLoader loader, String className, Class redefiningClass, ProtectionDomain domain, byte[] bytes ) throws IllegalClassFormatException {

        if ( className.equals( this.className ) && loader.equals( classLoader ) ) {
            return bytes;
        }

        return null;
    }

    public ClassLoader getClassLoader () {
        return classLoader;
    }

    public void setClassLoader ( ClassLoader classLoader ) {
        this.classLoader = classLoader;
    }

    public String getClassName () {
        return className;
    }

    public void setClassName ( String className ) {
        this.className = className;
    }

}