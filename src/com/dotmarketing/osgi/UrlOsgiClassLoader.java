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

    private ClassLoader topClassLoader;
    private ClassLoader mainClassLoader;

    private Collection<URL> urls;

    public UrlOsgiClassLoader ( URL url, ClassLoader mainClassLoader ) throws Exception {

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

        //Set our main class loader in order to use it in case we don't find a reference to a class we need to load
        this.mainClassLoader = mainClassLoader;

        //Search for the top class loader in the dotCMS class loaders hierarchy
        this.topClassLoader = findTopLoader( ClassLoader.getSystemClassLoader() );
    }

    @Override
    public Class<?> loadClass ( String name ) throws ClassNotFoundException {
        try {
            return super.loadClass( name );
        } catch ( ClassNotFoundException e ) {
            return mainClassLoader.loadClass( name );
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

        /*
         Remove our custom class loader from the dotCMS class loaders hierarchy in order to avoid problems redefining
         a class that should not even be loaded like the Activator or any class that should live just in the context
         of the OSGI plugin.
          */
        unlinkClassLoaders();

        File jarFile = new File( url.toURI() );
        JarFile jar = new JarFile( jarFile );

        Enumeration resources = jar.entries();
        while ( resources.hasMoreElements() ) {

            //We will try to redefine class by class
            JarEntry entry = (JarEntry) resources.nextElement();
            if ( !entry.isDirectory() && entry.getName().contains( ".class" ) ) {

                String className = entry.getName().replace( "/", "." ).replace( ".class", "" );

                //We just want to redefine loaded classes, we don't want to load something that will not be use it
                Class currentClass = searchClassForReloading( className );
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

        //Link again the class loaders
        linkClassLoaders();
    }

    /**
     * Search for a given class. This method will be call it when a class needs to be reload it, in order to do that
     * we will search first on the main classLoader (dotCMS ClassLoader) and if the class is not loaded and exist in dotCMS we
     * want to load it to redefine it with our implementation or changes.
     *
     * @param className
     * @return
     */
    private Class searchClassForReloading ( String className ) {

        Class foundClass;
        try {
            //First let search this class in the main dotCMS class loader
            foundClass = mainClassLoader.loadClass( className );
        } catch ( ClassNotFoundException e ) {
            foundClass = findLoadedClass( className );
        }

        return foundClass;
    }

    /**
     * In order to inject an OSGI bundle class code inside dotCMS context it is required to insert our
     * custom class loader with the OSGI bundle classes inside the dotCMS class loaders hierarchy.
     *
     * @throws Exception
     */
    public void linkClassLoaders () throws Exception {

        //Inject our custom class loader
        Field parentLoaderField = ClassLoader.class.getDeclaredField( "parent" );
        parentLoaderField.setAccessible( true );
        parentLoaderField.set( topClassLoader, this );
        parentLoaderField.setAccessible( false );
    }

    /**
     * Remove our custom class loader from the dotCMS class loaders hierarchy.
     *
     * @throws Exception
     */
    public void unlinkClassLoaders () throws Exception {

        //Remove our custom class loader
        Field parentLoaderField = ClassLoader.class.getDeclaredField( "parent" );
        parentLoaderField.setAccessible( true );
        parentLoaderField.set( topClassLoader, null );
        parentLoaderField.setAccessible( false );
    }

    /**
     * Search for the top class loader in the dotCMS class loaders hierarchy
     *
     * @param loader base class loader
     * @return The class loader at the top of the dotCMS class loaders hierarchy
     */
    private ClassLoader findTopLoader ( ClassLoader loader ) {

        if ( loader.getParent() == null ) {
            return loader;
        } else {
            return findTopLoader( loader.getParent() );
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

    @Override
    public URL[] getURLs () {
        return new URL[]{};
    }

}