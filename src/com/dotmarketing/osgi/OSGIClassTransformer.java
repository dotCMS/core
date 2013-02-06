package com.dotmarketing.osgi;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * Created by Jonathan Gamba
 * Date: 2/1/13
 */
public class OSGIClassTransformer implements ClassFileTransformer {

    public OSGIClassTransformer () {
        super();
    }

    public byte[] transform ( ClassLoader loader, String className, Class redefiningClass, ProtectionDomain domain, byte[] bytes ) throws IllegalClassFormatException {

        if ( !(loader instanceof UrlOsgiClassLoader) ) {
            return null;
        }

        return bytes;
    }

}