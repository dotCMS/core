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

import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.util.ClassUtils;

/**
 * Factory to grab a template loader.
 *
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @version $Id: ResourceLoaderFactory.java 898032 2010-01-11 19:51:03Z nbubna $
 */
public class ResourceLoaderFactory
{
    /**
     * Gets the loader specified in the configuration file.
     * @param rs
     * @param loaderClassName
     * @return TemplateLoader
     */
    public static ResourceLoader getLoader(RuntimeServices rs, String loaderClassName)
    {
        ResourceLoader loader = null;

        try
        {
            loader = (ResourceLoader) ClassUtils.getNewInstance( loaderClassName );

            rs.getLog().debug("ResourceLoader instantiated: "
                              + loader.getClass().getName());

            return loader;
        }
        // The ugly three strike again: ClassNotFoundException,IllegalAccessException,InstantiationException
        catch(Exception e)
        {
            String msg = "Problem instantiating the template loader: "+loaderClassName+".\n" +
                         "Look at your properties file and make sure the\n" +
                         "name of the template loader is correct.";
            rs.getLog().error(msg, e);
            throw new VelocityException(msg, e);
        }
    }
}
