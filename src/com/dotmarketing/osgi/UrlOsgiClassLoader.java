package com.dotmarketing.osgi;

import org.github.jamm.MemoryMeter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by Jonathan Gamba
 * Date: 1/31/13
 */
public class UrlOsgiClassLoader extends URLClassLoader {

    Instrumentation instrumentation;
    OSGIClassTransformer transformer;

    private Collection<URL> urls;

    public UrlOsgiClassLoader ( URL url ) throws Exception {

        super( new URL[]{url}, null );

        urls = new ArrayList<URL>();
        urls.add( url );

        //Find and set the instrumentation object
        instrumentation = findInstrumentation();
        //Creates our transformer, a class that will allows to redefine a class content
        transformer = new OSGIClassTransformer();

        //Adding the transformer
        instrumentation.removeTransformer( transformer );//Just to be sure we don't have two instances of the same transformer
        instrumentation.addTransformer( transformer, true );
    }

    @Override
    public Class<?> loadClass ( String name ) throws ClassNotFoundException {
        try {
            return super.loadClass( name );
        } catch ( ClassNotFoundException e ) {
            return ClassLoader.getSystemClassLoader().loadClass( name );
        }
    }

    public Class<?> findClass ( String name ) throws ClassNotFoundException {
        return super.findClass( name );
    }

    @Override
    public synchronized Class loadClass ( String name, boolean resolve ) throws ClassNotFoundException {
        return super.loadClass( name, resolve );
    }

    @Override
    protected void addURL ( URL url ) {

        if ( !urls.contains( url ) ) {
            urls.add( url );
            super.addURL( url );
        }
    }

    public Boolean contains ( URL url ) {
        return urls.contains( url );
    }

    /**
     * Reload all the loaded classes of this custom class loader, by reload we mean to redefine those classes.
     *
     * @throws Exception
     */
    public void reload () throws Exception {

        for ( URL url : urls ) {
            reload( url );
        }
    }

    /**
     * Reload all the loaded classes of this custom class loader of a given url, by reload we mean to redefine those classes.
     * <br>USE THIS METHOD IF YOU DON'T WANT TO RELOAD ALL THE CLASSES FOR ALL THE URLS ADDED TO THIS CLASSLOADER
     *
     * @throws Exception
     */
    public void reload ( URL url ) throws Exception {

        File jarFile = new File( url.toURI() );
        JarFile jar = new JarFile( jarFile );

        Enumeration resources = jar.entries();
        while ( resources.hasMoreElements() ) {

            //We will try to redefine class by class
            JarEntry entry = (JarEntry) resources.nextElement();
            if ( !entry.isDirectory() && entry.getName().contains( ".class" ) ) {

                String className = entry.getName().replace( "/", "." ).replace( ".class", "" );

                //We just want to redefine loaded classes, we don't want to load something that will not be use it
                Class currentClass = findLoadedClass( className );
                if ( currentClass != null ) {

                    InputStream in = null;
                    ByteArrayOutputStream out = null;
                    try {
                        in = jar.getInputStream( entry );
                        out = new ByteArrayOutputStream();

                        byte[] buffer = new byte[1024];
                        int length;
                        while ( (length = in.read( buffer )) > 0 ) {
                            out.write( buffer, 0, length );
                        }
                        byte[] byteCode = out.toByteArray();

                        try {
                            //And finally redefine the class
                            ClassDefinition classDefinition = new ClassDefinition( currentClass, byteCode );
                            instrumentation.redefineClasses( classDefinition );
                        } catch ( ClassNotFoundException e ) {
                            //If the class has not been loaded we don't need to redefine it
                        }
                    } finally {
                        if ( in != null ) {
                            in.close();
                        }
                        if ( out != null ) {
                            out.close();
                        }
                    }
                }

            }
        }
    }

    /**
     * We need to access to the instrumentation (This class provides services needed to instrument Java programming language code.
     * Instrumentation is the addition of byte-codes to methods for the purpose of gathering data to be
     * utilized by tools, or on this case to modify on run time classes content in order to reload them.)
     *
     * @return an Instrumentation instance
     * @throws Exception
     */
    public Instrumentation findInstrumentation () throws Exception {

        //Find the instrumentation object
        MemoryMeter mm = new MemoryMeter();

        Field instrumentationField = MemoryMeter.class.getDeclaredField( "instrumentation" );//We need to access it using reflection
        instrumentationField.setAccessible( true );

        Instrumentation instrumentation = (Instrumentation) instrumentationField.get( mm );

        instrumentationField.setAccessible( false );

        return instrumentation;
    }

}