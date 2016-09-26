package org.apache.velocity.app.event.implement;

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

import org.apache.velocity.util.introspection.Info;

/**
 * Convenience class to use when reporting out invalid syntax 
 * with line, column, and template name.
 * 
 * @author <a href="mailto:wglass@forio.com">Will Glass-Husain </a>
 * @version $Id: InvalidReferenceInfo.java 685685 2008-08-13 21:43:27Z nbubna $
 * @since 1.5
 */
public class InvalidReferenceInfo extends Info
{
    private String invalidReference;
    
    public InvalidReferenceInfo(String invalidReference, Info info)
    {
        super(info.getTemplateName(),info.getLine(),info.getColumn());
        this.invalidReference = invalidReference; 
    }

    /**
     * Get the specific invalid reference string.
     * @return the invalid reference string
     */
    public String getInvalidReference()
    {
        return invalidReference;
    }
    
    

    /**
     * Formats a textual representation of this object as <code>SOURCE
     * [line X, column Y]: invalidReference</code>.
     *
     * @return String representing this object.
     */
    public String toString()
    {
        return getTemplateName() + " [line " + getLine() + ", column " +
            getColumn() + "]: " + invalidReference;
    }
}
