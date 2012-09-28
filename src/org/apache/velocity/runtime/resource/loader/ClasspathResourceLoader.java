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

import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.util.ClassUtils;
import org.apache.velocity.util.ExceptionUtils;

import com.dotmarketing.util.Logger;

/**
 *  ClasspathResourceLoader is a simple loader that will load
 *  templates from the classpath.
 *  <br>
 *  <br>
 *  Will load templates from  from multiple instances of
 *  and arbitrary combinations of :
 *  <ul>
 *  <li> jar files
 *  <li> zip files
 *  <li> template directories (any directory containing templates)
 *  </ul>
 *  This is a configuration-free loader, in that there are no
 *  parameters to be specified in the configuration properties,
 *  other than specifying this as the loader to use.  For example
 *  the following is all that the loader needs to be functional :
 *  <br>
 *  <br>
 *  resource.loader = class
 *  class.resource.loader.class =
 *    org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader
 *  <br>
 *  <br>
 *  To use, put your template directories, jars
 *  and zip files into the classpath or other mechanisms that make
 *  resources accessable to the classloader.
 *  <br>
 *  <br>
 *  This makes deployment trivial for web applications running in
 *  any Servlet 2.2 compliant servlet runner, such as Tomcat 3.2
 *  and others.
 *  <br>
 *  <br>
 *  For a Servlet Spec v2.2 servlet runner,
 *  just drop the jars of template files into the WEB-INF/lib
 *  directory of your webapp, and you won't have to worry about setting
 *  template paths or altering them with the root of the webapp
 *  before initializing.
 *  <br>
 *  <br>
 *  I have also tried it with a WAR deployment, and that seemed to
 *  work just fine.
 *
 * @author <a href="mailto:mailmur@yahoo.com">Aki Nieminen</a>
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @version $Id: ClasspathResourceLoader.java 471259 2006-11-04 20:26:57Z henning $
 */
public class ClasspathResourceLoader extends ResourceLoader
{

    /**
     *  This is abstract in the base class, so we need it
     * @param configuration
     */
    public void init( ExtendedProperties configuration)
    {
        if (Logger.isDebugEnabled(this.getClass()))
        {
            Logger.debug(this,"ClasspathResourceLoader : initialization complete.");
        }
    }

    /**
     * Get an InputStream so that the Runtime can build a
     * template with it.
     *
     * @param name name of template to get
     * @return InputStream containing the template
     * @throws ResourceNotFoundException if template not found
     *         in  classpath.
     */
    public InputStream getResourceStream( String name )
        throws ResourceNotFoundException
    {
        InputStream result = null;

        if (StringUtils.isEmpty(name))
        {
            throw new ResourceNotFoundException ("No template name provided");
        }

        /**
         * look for resource in thread classloader first (e.g. WEB-INF\lib in
         * a servlet container) then fall back to the system classloader.
         */

        try
        {
            result = ClassUtils.getResourceAsStream( getClass(), name );
        }
        catch( Exception fnfe )
        {
            throw (ResourceNotFoundException) ExceptionUtils.createWithCause(ResourceNotFoundException.class, "problem with template: " + name, fnfe );
        }

        if (result == null)
        {
             String msg = "ClasspathResourceLoader Error: cannot find resource " +
              name;

             throw new ResourceNotFoundException( msg );
        }

        return result;
    }

    /**
     * @see org.apache.velocity.runtime.resource.loader.ResourceLoader#isSourceModified(org.apache.velocity.runtime.resource.Resource)
     */
    public boolean isSourceModified(Resource resource)
    {
        return false;
    }

    /**
     * @see org.apache.velocity.runtime.resource.loader.ResourceLoader#getLastModified(org.apache.velocity.runtime.resource.Resource)
     */
    public long getLastModified(Resource resource)
    {
        return 0;
    }
}

