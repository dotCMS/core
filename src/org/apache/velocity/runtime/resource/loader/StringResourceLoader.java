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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.util.StringResource;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.apache.velocity.runtime.resource.util.StringResourceRepositoryImpl;
import org.apache.velocity.util.ClassUtils;

import com.dotmarketing.util.Logger;

/**
 * Resource loader that works with Strings. Users should manually add
 * resources to the repository that is used by the resource loader instance.
 *
 * Below is an example configuration for this loader.
 * Note that 'repository.class' is not necessary;
 * if not provided, the factory will fall back on using 
 * {@link StringResourceRepositoryImpl} as the default.
 * <pre>
 * resource.loader = string
 * string.resource.loader.description = Velocity StringResource loader
 * string.resource.loader.class = org.apache.velocity.runtime.resource.loader.StringResourceLoader
 * string.resource.loader.repository.class = org.apache.velocity.runtime.resource.util.StringResourceRepositoryImpl
 * </pre>
 * Resources can be added to the repository like this:
 * <pre><code>
 *   StringResourceRepository repo = StringResourceLoader.getRepository();
 *
 *   String myTemplateName = "/some/imaginary/path/hello.vm";
 *   String myTemplate = "Hi, ${username}... this is some template!";
 *   repo.putStringResource(myTemplateName, myTemplate);
 * </code></pre>
 *
 * After this, the templates can be retrieved as usual.
 * <br>
 * <p>If there will be multiple StringResourceLoaders used in an application,
 * you should consider specifying a 'string.resource.loader.repository.name = foo'
 * property in order to keep you string resources in a non-default repository.
 * This can help to avoid conflicts between different frameworks or components
 * that are using StringResourceLoader.
 * You can then retrieve your named repository like this:
 * <pre><code>
 *   StringResourceRepository repo = StringResourceLoader.getRepository("foo");
 * </code></pre>
 * and add string resources to the repo just as in the previous example.
 * </p>
 * <p>If you have concerns about memory leaks or for whatever reason do not wish
 * to have your string repository stored statically as a class member, then you
 * should set 'string.resource.loader.repository.static = false' in your properties.
 * This will tell the resource loader that the string repository should be stored
 * in the Velocity application attributes.  To retrieve the repository, do:
 * <pre><code>
 *   StringResourceRepository repo = velocityEngine.getApplicationAttribute("foo");
 * </code></pre>
 * If you did not specify a name for the repository, then it will be stored under the
 * class name of the repository implementation class (for which the default is 
 * 'org.apache.velocity.runtime.resource.util.StringResourceRepositoryImpl'). 
 * Incidentally, this is also true for the default statically stored repository.
 * </p>
 * <p>Whether your repository is stored statically or in Velocity's application
 * attributes, you can also manually create and set it prior to Velocity
 * initialization.  For a static repository, you can do something like this:
 * <pre><code>
 *   StringResourceRepository repo = new MyStringResourceRepository();
 *   repo.magicallyAddSomeStringResources();
 *   StringResourceLoader.setRepository("foo", repo);
 * </code></pre>
 * Or for a non-static repository:
 * <pre><code>
 *   StringResourceRepository repo = new MyStringResourceRepository();
 *   repo.magicallyAddSomeStringResources();
 *   velocityEngine.setApplicationAttribute("foo", repo);
 * </code></pre>
 * Then, assuming the 'string.resource.loader.repository.name' property is
 * set to 'some.name', the StringResourceLoader will use that already created
 * repository, rather than creating a new one.
 * </p>
 *
 * @author <a href="mailto:eelco.hillenius@openedge.nl">Eelco Hillenius</a>
 * @author <a href="mailto:henning@apache.org">Henning P. Schmiedehausen</a>
 * @author Nathan Bubna
 * @version $Id: StringResourceLoader.java 825302 2009-10-14 21:51:39Z nbubna $
 * @since 1.5
 */
public class StringResourceLoader extends ResourceLoader
{
    /**
     * Key to determine whether the repository should be set as the static one or not.
     * @since 1.6
     */
    public static final String REPOSITORY_STATIC = "repository.static";

    /**
     * By default, repositories are stored statically (shared across the VM).
     * @since 1.6
     */
    public static final boolean REPOSITORY_STATIC_DEFAULT = true;

    /** Key to look up the repository implementation class. */
    public static final String REPOSITORY_CLASS = "repository.class";

    /** The default implementation class. */
    public static final String REPOSITORY_CLASS_DEFAULT =
        StringResourceRepositoryImpl.class.getName();

    /**
     * Key to look up the name for the repository to be used.
     * @since 1.6
     */
    public static final String REPOSITORY_NAME = "repository.name";

    /** The default name for string resource repositories
     * ('org.apache.velocity.runtime.resource.util.StringResourceRepository').
     * @since 1.6
     */
    public static final String REPOSITORY_NAME_DEFAULT =
        StringResourceRepository.class.getName();

    /** Key to look up the repository char encoding. */
    public static final String REPOSITORY_ENCODING = "repository.encoding";

    /** The default repository encoding. */
    public static final String REPOSITORY_ENCODING_DEFAULT = "UTF-8";


    protected static final Map STATIC_REPOSITORIES =
        Collections.synchronizedMap(new HashMap());

    /**
     * Returns a reference to the default static repository.
     */
    public static StringResourceRepository getRepository()
    {
        return getRepository(REPOSITORY_NAME_DEFAULT);
    }

    /**
     * Returns a reference to the repository stored statically under the
     * specified name.
     * @since 1.6
     */
    public static StringResourceRepository getRepository(String name)
    {
        return (StringResourceRepository)STATIC_REPOSITORIES.get(name);
    }

    /**
     * Sets the specified {@link StringResourceRepository} in static storage
     * under the specified name.
     * @since 1.6
     */
    public static void setRepository(String name, StringResourceRepository repo)
    {
        STATIC_REPOSITORIES.put(name, repo);
    }

    /**
     * Removes the {@link StringResourceRepository} stored under the specified
     * name.
     * @since 1.6
     */
    public static StringResourceRepository removeRepository(String name)
    {
        return (StringResourceRepository)STATIC_REPOSITORIES.remove(name);
    }

    /**
     * Removes all statically stored {@link StringResourceRepository}s.
     * @since 1.6
     */
    public static void clearRepositories()
    {
        STATIC_REPOSITORIES.clear();
    }


    // the repository used internally by this resource loader
    protected StringResourceRepository repository;


    /**
     * @see org.apache.velocity.runtime.resource.loader.ResourceLoader#init(org.apache.commons.collections.ExtendedProperties)
     */
    public void init(final ExtendedProperties configuration)
    {
        Logger.debug(this,"StringResourceLoader : initialization starting.");

        // get the repository configuration info
        String repoClass = configuration.getString(REPOSITORY_CLASS, REPOSITORY_CLASS_DEFAULT);
        String repoName = configuration.getString(REPOSITORY_NAME, REPOSITORY_NAME_DEFAULT);
        boolean isStatic = configuration.getBoolean(REPOSITORY_STATIC, REPOSITORY_STATIC_DEFAULT);
        String encoding = configuration.getString(REPOSITORY_ENCODING);

        // look for an existing repository of that name and isStatic setting
        if (isStatic)
        {
            this.repository = getRepository(repoName);
            if (repository != null && Logger.isDebugEnabled(this.getClass()))
            {
                Logger.debug(this,"Loaded repository '"+repoName+"' from static repo store");
            }
        }
        else
        {
            this.repository = (StringResourceRepository)rsvc.getApplicationAttribute(repoName);
            if (repository != null && Logger.isDebugEnabled(this.getClass()))
            {
                Logger.debug(this,"Loaded repository '"+repoName+"' from application attributes");
            }
        }

        if (this.repository == null)
        {
            // since there's no repository under the repo name, create a new one
            this.repository = createRepository(repoClass, encoding);

            // and store it according to the isStatic setting
            if (isStatic)
            {
                setRepository(repoName, this.repository);
            }
            else
            {
                rsvc.setApplicationAttribute(repoName, this.repository);
            }
        }
        else
        {
            // ok, we already have a repo
            // warn them if they are trying to change the class of the repository
            if (!this.repository.getClass().getName().equals(repoClass))
            {
                Logger.debug(this,"Cannot change class of string repository '"+repoName+
                          "' from "+this.repository.getClass().getName()+" to "+repoClass+
                          ". The change will be ignored.");
            }

            // allow them to change the default encoding of the repo
            if (encoding != null &&
                !this.repository.getEncoding().equals(encoding))
            {
                if (Logger.isDebugEnabled(this.getClass()))
                {
                    Logger.debug(this,"Changing the default encoding of string repository '"+repoName+
                              "' from "+this.repository.getEncoding()+" to "+encoding);
                }
                this.repository.setEncoding(encoding);
            }
        }

        Logger.debug(this,"StringResourceLoader : initialization complete.");
    }

    /**
     * @since 1.6
     */
    public StringResourceRepository createRepository(final String className,
                                                     final String encoding)
    {
        if (Logger.isDebugEnabled(this.getClass()))
        {
            Logger.debug(this,"Creating string repository using class "+className+"...");
        }

        StringResourceRepository repo;
        try
        {
            repo = (StringResourceRepository) ClassUtils.getNewInstance(className);
        }
        catch (ClassNotFoundException cnfe)
        {
            throw new VelocityException("Could not find '" + className + "'", cnfe);
        }
        catch (IllegalAccessException iae)
        {
            throw new VelocityException("Could not access '" + className + "'", iae);
        }
        catch (InstantiationException ie)
        {
            throw new VelocityException("Could not instantiate '" + className + "'", ie);
        }

        if (encoding != null)
        {
            repo.setEncoding(encoding);
        }
        else
        {
            repo.setEncoding(REPOSITORY_ENCODING_DEFAULT);
        }

        if (Logger.isDebugEnabled(this.getClass()))
        {
            Logger.debug(this,"Default repository encoding is " + repo.getEncoding());
        }
        return repo;
    }

    /**
     * Overrides superclass for better performance.
     * @since 1.6
     */
    public boolean resourceExists(final String name)
    {
        if (name == null)
        {
            return false;
        }
        return (this.repository.getStringResource(name) != null);
    }

    /**
     * Get an InputStream so that the Runtime can build a
     * template with it.
     *
     * @param name name of template to get.
     * @return InputStream containing the template.
     * @throws ResourceNotFoundException Ff template not found
     *         in the RepositoryFactory.
     */
    public InputStream getResourceStream(final String name)
            throws ResourceNotFoundException
    {
        if (StringUtils.isEmpty(name))
        {
            throw new ResourceNotFoundException("No template name provided");
        }

        StringResource resource = this.repository.getStringResource(name);
        
        if(resource == null)
        {
            throw new ResourceNotFoundException("Could not locate resource '" + name + "'");
        }
        
        byte [] byteArray = null;
    	
        try
        {
            byteArray = resource.getBody().getBytes(resource.getEncoding());
            return new ByteArrayInputStream(byteArray);
        }
        catch(UnsupportedEncodingException ue)
        {
            throw new VelocityException("Could not convert String using encoding " + resource.getEncoding(), ue);
        }
    }

    /**
     * @see org.apache.velocity.runtime.resource.loader.ResourceLoader#isSourceModified(org.apache.velocity.runtime.resource.Resource)
     */
    public boolean isSourceModified(final Resource resource)
    {
        StringResource original = null;
        boolean result = true;

        original = this.repository.getStringResource(resource.getName());

        if (original != null)
        {
            result =  original.getLastModified() != resource.getLastModified();
        }

        return result;
    }

    /**
     * @see org.apache.velocity.runtime.resource.loader.ResourceLoader#getLastModified(org.apache.velocity.runtime.resource.Resource)
     */
    public long getLastModified(final Resource resource)
    {
        StringResource original = null;

        original = this.repository.getStringResource(resource.getName());

        return (original != null)
                ? original.getLastModified()
                : 0;
    }

}

