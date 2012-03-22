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


package org.apache.velocity.tools.view.tools;


/**
 * Generic view tool interface to assist in tool management.
 * This interface provides the {@link #init(Object initData)} method 
 * as a hook for ToolboxManager implementations to pass data in to
 * tools to initialize them.  See 
 * {@link org.apache.velocity.tools.view.ViewToolInfo} for more on this.
 *
 * @author <a href="mailto:nathan@esha.com">Nathan Bubna</a>
 *
 * @version $Id: ViewTool.java 71982 2004-02-18 20:11:07Z nbubna $
 */
public interface ViewTool
{

    /**
     * Initializes this instance using the given data
     *
     * @param initData the initialization data 
     */
    public void init(Object initData);


}
