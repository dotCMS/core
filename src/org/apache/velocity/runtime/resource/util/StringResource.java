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
 * Wrapper for Strings containing templates, allowing to add additional meta
 * data like timestamps.
 *
 * @author <a href="mailto:eelco.hillenius@openedge.nl">Eelco Hillenius</a>
 * @author <a href="mailto:henning@apache.org">Henning P. Schmiedehausen</a>
 * @version $Id: StringResource.java 685685 2008-08-13 21:43:27Z nbubna $
 * @since 1.5
 */
public final class StringResource
{
    /** template body */
    private String body;
    
    /** encoding */
    private String encoding;

    /** last modified ts */
    private long lastModified;

    /**
     * convenience constructor; sets body to 'body' and sets lastModified to now
     * @param body
     */
    public StringResource(final String body, final String encoding)
    {
        setBody(body);
        setEncoding(encoding);
    }

    /**
     * Sets the template body.
     * @return String containing the template body.
     */
    public String getBody()
    {
        return body;
    }

    /**
     * Returns the modification date of the template.
     * @return Modification date in milliseconds.
     */
    public long getLastModified()
    {
        return lastModified;
    }

    /**
     * Sets a new  value for the template body.
     * @param body New body value
     */
    public void setBody(final String body)
    {
        this.body = body;
        this.lastModified = System.currentTimeMillis();
    }

    /**
     * Changes the last modified parameter.
     * @param lastModified The modification time in millis.
     */
    public void setLastModified(final long lastModified)
    {
        this.lastModified = lastModified;
    }

    /**
     * Returns the encoding of this String resource.
     * 
     * @return The encoding of this String resource.
     */
    public String getEncoding() {
        return this.encoding;
    }

    /**
     * Sets the encoding of this string resource.
     * 
     * @param encoding The new encoding of this resource.
     */
    public void setEncoding(final String encoding)
    {
        this.encoding = encoding;
    }
}
