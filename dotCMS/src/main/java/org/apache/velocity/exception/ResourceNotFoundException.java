package org.apache.velocity.exception;

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
 *  Application-level exception thrown when a resource of any type
 *  isn't found by the Velocity engine.
 *  <br>
 *  When this exception is thrown, a best effort will be made to have
 *  useful information in the exception's message.  For complete
 *  information, consult the runtime log.
 *
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @version $Id: ResourceNotFoundException.java 685685 2008-08-13 21:43:27Z nbubna $
 */
public class ResourceNotFoundException extends VelocityException
{
    /**
     * Version Id for serializable
     */
    private static final long serialVersionUID = -4287732191458420347L;

    /**
     * @see VelocityException#VelocityException(String)
     */
    public ResourceNotFoundException(final String exceptionMessage)
    {
        super(exceptionMessage);
    }

    /**
     * @see VelocityException#VelocityException(String, Throwable)
     * @since 1.5
     */
    public ResourceNotFoundException(final String exceptionMessage, final Throwable t)
    {
        super(exceptionMessage, t);
    }

    /**
     * @see VelocityException#VelocityException(Throwable)
     * @since 1.5
     */
    public ResourceNotFoundException(final Throwable t)
    {
        super(t);
    }
}
