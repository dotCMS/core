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
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;

import com.dotmarketing.util.Logger;

/**
 * This is a simple URL-based loader.
 *
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 * @author <a href="mailto:nbubna@apache.org">Nathan Bubna</a>
 * @version $Id: URLResourceLoader.java 191743 2005-06-21 23:22:20Z dlr $
 * @since 1.5
 */
public class URLResourceLoader extends ResourceLoader
{
    private String[] roots = null;
    protected HashMap templateRoots = null;
    private int timeout = -1;
    private Method[] timeoutMethods;

    /**
     * @see org.apache.velocity.runtime.resource.loader.ResourceLoader#init(org.apache.commons.collections.ExtendedProperties)
     */
    public void init(ExtendedProperties configuration)
    {
        Logger.debug(this,"URLResourceLoader : initialization starting.");

        roots = configuration.getStringArray("root");
        if (Logger.isDebugEnabled(this.getClass()))
        {
            for (int i=0; i < roots.length; i++)
            {
                Logger.debug(this,"URLResourceLoader : adding root '" + roots[i] + "'");
            }
        }

        timeout = configuration.getInt("timeout", -1);
        if (timeout > 0)
        {
            try
            {
                Class[] types = new Class[] { Integer.TYPE };
                Method conn = URLConnection.class.getMethod("setConnectTimeout", types);
                Method read = URLConnection.class.getMethod("setReadTimeout", types);
                timeoutMethods = new Method[] { conn, read };
                Logger.debug(this,"URLResourceLoader : timeout set to "+timeout);
            }
            catch (NoSuchMethodException nsme)
            {
                Logger.debug(this,"URLResourceLoader : Java 1.5+ is required to customize timeout!", nsme);
                timeout = -1;
            }
        }

        // init the template paths map
        templateRoots = new HashMap();

        Logger.debug(this,"URLResourceLoader : initialization complete.");
    }

    /**
     * Get an InputStream so that the Runtime can build a
     * template with it.
     *
     * @param name name of template to fetch bytestream of
     * @return InputStream containing the template
     * @throws ResourceNotFoundException if template not found
     *         in the file template path.
     */
    public synchronized InputStream getResourceStream(String name)
        throws ResourceNotFoundException
    {
        if (StringUtils.isEmpty(name))
        {
            throw new ResourceNotFoundException("URLResourceLoader : No template name provided");
        }

        InputStream inputStream = null;
        Exception exception = null;
        for(int i=0; i < roots.length; i++)
        {
            try
            {
                URL u = new URL(roots[i] + name);
                URLConnection conn = u.openConnection();
                tryToSetTimeout(conn);
                inputStream = conn.getInputStream();

                if (inputStream != null)
                {
                    if (Logger.isDebugEnabled(this.getClass())) Logger.debug(this,"URLResourceLoader: Found '"+name+"' at '"+roots[i]+"'");

                    // save this root for later re-use
                    templateRoots.put(name, roots[i]);
                    break;
                }
            }
            catch(IOException ioe)
            {
                if (Logger.isDebugEnabled(this.getClass())) Logger.debug(this,"URLResourceLoader: Exception when looking for '"+name+"' at '"+roots[i]+"'", ioe);

                // only save the first one for later throwing
                if (exception == null)
                {
                    exception = ioe;
                }
            }
        }

        // if we never found the template
        if (inputStream == null)
        {
            String msg;
            if (exception == null)
            {
                msg = "URLResourceLoader : Resource '" + name + "' not found.";
            }
            else
            {
                msg = exception.getMessage();
            }
            // convert to a general Velocity ResourceNotFoundException
            throw new ResourceNotFoundException(msg);
        }

        return inputStream;
    }

    /**
     * Checks to see if a resource has been deleted, moved or modified.
     *
     * @param resource Resource  The resource to check for modification
     * @return boolean  True if the resource has been modified, moved, or unreachable
     */
    public boolean isSourceModified(Resource resource)
    {
        long fileLastModified = getLastModified(resource);
        // if the file is unreachable or otherwise changed
        if (fileLastModified == 0 ||
            fileLastModified != resource.getLastModified())
        {
            return true;
        }
        return false;
    }

    /**
     * Checks to see when a resource was last modified
     *
     * @param resource Resource the resource to check
     * @return long The time when the resource was last modified or 0 if the file can't be reached
     */
    public long getLastModified(Resource resource)
    {
        // get the previously used root
        String name = resource.getName();
        String root = (String)templateRoots.get(name);

        try
        {
            // get a connection to the URL
            URL u = new URL(root + name);
            URLConnection conn = u.openConnection();
            tryToSetTimeout(conn);
            return conn.getLastModified();
        }
        catch (IOException ioe)
        {
            // the file is not reachable at its previous address
            String msg = "URLResourceLoader: '"+name+"' is no longer reachable at '"+root+"'";
            Logger.error(this,msg, ioe);
            throw new ResourceNotFoundException(msg, ioe);
        }
    }

    /**
     * Returns the current, custom timeout setting. If negative, there is no custom timeout.
     * @since 1.6
     */
    public int getTimeout()
    {
        return timeout;
    }

    private void tryToSetTimeout(URLConnection conn)
    {
        if (timeout > 0)
        {
            Object[] arg = new Object[] { new Integer(timeout) };
            try
            {
                timeoutMethods[0].invoke(conn, arg);
                timeoutMethods[1].invoke(conn, arg);
            }
            catch (Exception e)
            {
                String msg = "Unexpected exception while setting connection timeout for "+conn;
                Logger.error(this,msg, e);
                throw new VelocityException(msg, e);
            }
        }
    }

}
