package org.apache.velocity.runtime.resource;

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

import org.apache.velocity.Template;

/**
 * Class responsible for instantiating <code>Resource</code> objects,
 * given name and type.
 *
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @version $Id: ResourceFactory.java 463298 2006-10-12 16:10:32Z henning $
 */
public class ResourceFactory
{
    /**
     * @param resourceName
     * @param resourceType
     * @return The resource described by name and type.
     */
    public static Resource getResource(String resourceName, int resourceType)
    {
        Resource resource = null;

        switch (resourceType)
        {
            case ResourceManager.RESOURCE_TEMPLATE:
                resource = new Template();
                break;

            case ResourceManager.RESOURCE_CONTENT:
                resource = new ContentResource();
                break;
        }

        return resource;
    }
}
