package org.apache.velocity.runtime.resource.util;

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

/**
 * A StringResourceRepository functions as a central repository for Velocity templates
 * stored in Strings.
 *
 * @author <a href="mailto:eelco.hillenius@openedge.nl">Eelco Hillenius</a>
 * @author <a href="mailto:henning@apache.org">Henning P. Schmiedehausen</a>
 * @version $Id: StringResourceRepository.java 685724 2008-08-13 23:12:12Z nbubna $
 * @since 1.5
 */
public interface StringResourceRepository
{
    /**
     * get the string resource that is stored with given key
     * @param name String name to retrieve from the repository.
     * @return A StringResource containing the template.
     */
    StringResource getStringResource(String name);

    /**
     * add a string resource with given key.
     * @param name The String name to store the template under.
     * @param body A String containing a template.
     */
    void putStringResource(String name, String body);

    /**
     * add a string resource with given key.
     * @param name The String name to store the template under.
     * @param body A String containing a template.
     * @param encoding The encoding of this string template
     * @since 1.6
     */
    void putStringResource(String name, String body, String encoding);

    /**
     * delete a string resource with given key.
     * @param name The string name to remove from the repository.
     */
    void removeStringResource(String name);
    
    /**
     * Sets the default encoding of the repository. Encodings can also be stored per
     * template string. The default implementation does this correctly.
     * 
     * @param encoding The encoding to use.
     */
    void setEncoding(String encoding);
    
    /**
     * Returns the current encoding of this repository.
     * 
     * @return The current encoding of this repository.
     */
    String getEncoding();
}
