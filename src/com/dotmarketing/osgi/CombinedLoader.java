package com.dotmarketing.osgi;

import org.apache.felix.framework.BundleWiringImpl;

import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
            if ( url != null )
                return url;
        }

        return super.getResource( name );
    }

}