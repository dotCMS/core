package com.dotmarketing.osgi;

import org.apache.felix.framework.BundleWiringImpl;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Created by Jonathan Gamba
 * Date: 7/27/12
 */
public class CombinedLoader extends ClassLoader {

    private Set<ClassLoader> loaders = new HashSet<ClassLoader>();

    public CombinedLoader ( ClassLoader parent ) {
        super( parent );
    }

    public void addLoader ( ClassLoader loader ) {

        if ( !loaders.contains( loader ) ) {

            if ( loader instanceof BundleWiringImpl.BundleClassLoader ) {

                //If we already have a class loader of this bundle lets remove it an replace it with the new one it have
                BundleWiringImpl.BundleClassLoader bundleClassLoader = (BundleWiringImpl.BundleClassLoader) loader;
                removeByBundleName( bundleClassLoader.getBundle().getSymbolicName() );
            }

            loaders.add( loader );
        }
    }

    private void removeByBundleName ( String bundleName ) {

        Iterator<ClassLoader> iterator = loaders.iterator();
        while ( iterator.hasNext() ) {

            ClassLoader loader = iterator.next();
            if ( loader instanceof BundleWiringImpl.BundleClassLoader ) {

                BundleWiringImpl.BundleClassLoader bundleClassLoader = (BundleWiringImpl.BundleClassLoader) loader;
                String symbolicName = bundleClassLoader.getBundle().getSymbolicName();

                if ( bundleName.contains( symbolicName ) ) {
                    iterator.remove();
                }
            }
        }

    }

    public void addLoader ( Class clazz ) {
        addLoader( clazz.getClass().getClassLoader() );
    }

    public void removeLoader ( ClassLoader loader ) {
        loaders.remove( loader );
    }

    public void removeLoader ( Class clazz ) {
        loaders.remove( clazz.getClass().getClassLoader() );
    }

    public Class<?> findClass ( String name ) throws ClassNotFoundException {

        for ( ClassLoader loader : loaders ) {
            try {
                return loader.loadClass( name );
            } catch ( ClassNotFoundException cnfe ) {
                // Try next
            }
        }

        return super.findClass( name );
    }

    @Override
    public Class<?> loadClass ( String name ) throws ClassNotFoundException {

        for ( ClassLoader loader : loaders ) {
            try {
                return loader.loadClass( name );
            } catch ( ClassNotFoundException cnfe ) {
                // Try next
            }
        }

        return super.loadClass( name );
    }

    @Override
    public URL getResource ( String name ) {

        for ( ClassLoader loader : loaders ) {
            URL url = loader.getResource( name );
            if ( url != null ) {
                return url;
            }
        }

        return super.getResource( name );
    }

    @Override
    public Enumeration<URL> getResources ( String name ) throws IOException {

        for ( ClassLoader loader : loaders ) {

            Enumeration<URL> urls = loader.getResources( name );
            if ( urls != null && urls.hasMoreElements() ) {

                ArrayList<URL> finalURLs = new ArrayList<URL>();
                //Now we need to check what kind of resource we got
                while ( urls.hasMoreElements() ) {
                    URL url = urls.nextElement();

                    if ( loader instanceof BundleWiringImpl.BundleClassLoader ) {

                        /*
                        Some frameworks don't handle properly osgi bundles (e.g. Spring framework unless we are using equinox -> Explicit verification on the code)
                        and when are trying to load element from a folder and not asking for an specific file is better to return the
                        real location of this bundle in order to allow the caller to scan a known protocol for him.
                         */
                        if ( url.getProtocol().equals( "bundle" ) && name.endsWith( "/" ) ) {

                            //Using the loader in order to know more about the owner bundle
                            BundleWiringImpl.BundleClassLoader bundleClassLoader = (BundleWiringImpl.BundleClassLoader) loader;

                            //Get the real location of this bundle
                            String bundleLocation = bundleClassLoader.getBundle().getLocation();
                            //Create the proper URL object for a jar file, using a proper protocol and file paths for jars
                            url = new URL( "jar", null, bundleLocation + "!/" );
                        }
                    }

                    finalURLs.add( url );
                }
                return Collections.enumeration( finalURLs );
            }
        }

        return super.getResources( name );
    }

}