/*
 * Copyright 2003 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.velocity.tools.view;


/**
 * Interface to simplify and abstract tool handling.
 *
 * Implementations of this class should hold both the context
 * key for the tool and sufficient information to return 
 * an instance of the tool.
 *
 * @author <a href="mailto:nathan@esha.com">Nathan Bubna</a>
 *
 * @version $Id: ToolInfo.java 71982 2004-02-18 20:11:07Z nbubna $
 */
public interface ToolInfo
{


    /**
     * @return the context key for the tool
     */
    String getKey();


    /**
     * @return the fully qualified classname for the tool
     */
    String getClassname();


    /**
     * Returns an instance of the tool.
     *
     * Instances returned may be new on each call, pooled, or 
     * the be same instance every time depending on the
     * implementation.  The object passed to this method may
     * be used to initialize or create the tool that is returned,
     * or it may be null if no such data is required.
     *
     * @param initData an object that may be used to initialize the instance
     * @return an instance of the tool
     */
    Object getInstance(Object initData);


}
