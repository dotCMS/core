package org.apache.velocity.runtime.resource.loader;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

import java.io.InputStream;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;

import org.apache.velocity.util.StringUtils;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.commons.collections.ExtendedProperties;

import com.dotmarketing.util.Logger;

/**
 * <p>
 * ResourceLoader to load templates from multiple Jar files.
 * </p>
 * <p>
 * The configuration of the JarResourceLoader is straightforward -
 * You simply add the JarResourceLoader to the configuration via
 * </p>
 * <p><pre>
 *    resource.loader = jar
 *    jar.resource.loader.class = org.apache.velocity.runtime.resource.loader.JarResourceLoader
 *    jar.resource.loader.path = list of JAR &lt;URL&gt;s
 * </pre></p>
 *
 * <p> So for example, if you had a jar file on your local filesystem, you could simply do
 *    <pre>
 *    jar.resource.loader.path = jar:file:/opt/myfiles/jar1.jar
 *    </pre>
 * </p>
 * <p> Note that jar specification for the <code>.path</code> configuration property
 * conforms to the same rules for the java.net.JarUrlConnection class.
 * </p>
 *
 * <p> For a working example, see the unit test case,
 *  org.apache.velocity.test.MultiLoaderTestCase class
 * </p>
 *
 * @author <a href="mailto:mailmur@yahoo.com">Aki Nieminen</a>
 * @author <a href="mailto:daveb@miceda-data.com">Dave Bryson</a>
 * @version $Id: JarResourceLoader.java 691884 2008-09-04 06:46:51Z nbubna $
 */
public class JarResourceLoader extends ResourceLoader
{
    /**
     * Maps entries to the parent JAR File
     * Key = the entry *excluding* plain directories
     * Value = the JAR URL
     */
    private Map entryDirectory = new HashMap(559);

    /**
     * Maps JAR URLs to the actual JAR
     * Key = the JAR URL
     * Value = the JAR
     */
    private Map jarfiles = new HashMap(89);

    /**
     * Called by Velocity to initialize the loader
     * @param configuration
     */
    public void init( ExtendedProperties configuration)
    {
        Logger.debug(this,"JarResourceLoader : initialization starting.");

        // rest of Velocity engine still use legacy Vector
        // and Hashtable classes. Classes are implicitly
        // synchronized even if we don't need it.
        Vector paths = configuration.getVector("path");
        StringUtils.trimStrings(paths);

        /*
         *  support the old version but deprecate with a log message
         */

        if( paths == null || paths.size() == 0)
        {
            paths = configuration.getVector("resource.path");
            StringUtils.trimStrings(paths);

            if (paths != null && paths.size() > 0)
            {
                Logger.debug(this,"JarResourceLoader : you are using a deprecated configuration"
                          + " property for the JarResourceLoader -> '<name>.resource.loader.resource.path'."
                          + " Please change to the conventional '<name>.resource.loader.path'.");
            }
        }

        if (paths != null)
        {
            Logger.debug(this,"JarResourceLoader # of paths : " + paths.size() );

            for ( int i=0; i<paths.size(); i++ )
            {
                loadJar( (String)paths.get(i) );
            }
        }

        Logger.debug(this,"JarResourceLoader : initialization complete.");
    }

    private void loadJar( String path )
    {
        if (Logger.isDebugEnabled(this.getClass()))
        {
            Logger.debug(this,"JarResourceLoader : trying to load \"" + path + "\"");
        }

        // Check path information
        if ( path == null )
        {
            String msg = "JarResourceLoader : can not load JAR - JAR path is null";
            Logger.error(this,msg);
            throw new RuntimeException(msg);
        }
        if ( !path.startsWith("jar:") )
        {
            String msg = "JarResourceLoader : JAR path must start with jar: -> see java.net.JarURLConnection for information";
            Logger.error(this,msg);
            throw new RuntimeException(msg);
        }
        if ( path.indexOf("!/") < 0 )
        {
            path += "!/";
        }

        // Close the jar if it's already open
        // this is useful for a reload
        closeJar( path );

        // Create a new JarHolder
        JarHolder temp = new JarHolder( rsvc,  path );
        // Add it's entries to the entryCollection
        addEntries( temp.getEntries() );
        // Add it to the Jar table
        jarfiles.put( temp.getUrlPath(), temp );
    }

    /**
     * Closes a Jar file and set its URLConnection
     * to null.
     */
    private void closeJar( String path )
    {
        if ( jarfiles.containsKey(path) )
        {
            JarHolder theJar = (JarHolder)jarfiles.get(path);
            theJar.close();
        }
    }

    /**
     * Copy all the entries into the entryDirectory
     * It will overwrite any duplicate keys.
     */
    private void addEntries( Hashtable entries )
    {
        entryDirectory.putAll( entries );
    }

    /**
     * Get an InputStream so that the Runtime can build a
     * template with it.
     *
     * @param source name of template to get
     * @return InputStream containing the template
     * @throws ResourceNotFoundException if template not found
     *         in the file template path.
     */
    public InputStream getResourceStream( String source )
        throws ResourceNotFoundException
    {
        InputStream results = null;

        if (org.apache.commons.lang.StringUtils.isEmpty(source))
        {
            throw new ResourceNotFoundException("Need to have a resource!");
        }

        String normalizedPath = StringUtils.normalizePath( source );

        if ( normalizedPath == null || normalizedPath.length() == 0 )
        {
            String msg = "JAR resource error : argument " + normalizedPath +
                " contains .. and may be trying to access " +
                "content outside of template root.  Rejected.";

            Logger.error(this, "JarResourceLoader : " + msg );

            throw new ResourceNotFoundException ( msg );
        }

        /*
         *  if a / leads off, then just nip that :)
         */
        if ( normalizedPath.startsWith("/") )
        {
            normalizedPath = normalizedPath.substring(1);
        }

        if ( entryDirectory.containsKey( normalizedPath ) )
        {
            String jarurl  = (String)entryDirectory.get( normalizedPath );

            if ( jarfiles.containsKey( jarurl ) )
            {
                JarHolder holder = (JarHolder)jarfiles.get( jarurl );
                results =  holder.getResource( normalizedPath );
                return results;
            }
        }

        throw new ResourceNotFoundException( "JarResourceLoader Error: cannot find resource " +
          source );

    }


    // TODO: SHOULD BE DELEGATED TO THE JARHOLDER

    /**
     * @see org.apache.velocity.runtime.resource.loader.ResourceLoader#isSourceModified(org.apache.velocity.runtime.resource.Resource)
     */
    public boolean isSourceModified(Resource resource)
    {
        return true;
    }

    /**
     * @see org.apache.velocity.runtime.resource.loader.ResourceLoader#getLastModified(org.apache.velocity.runtime.resource.Resource)
     */
    public long getLastModified(Resource resource)
    {
        return 0;
    }
}
