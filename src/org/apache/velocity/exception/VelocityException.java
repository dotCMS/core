package org.apache.velocity.exception;

import org.apache.velocity.util.ExceptionUtils;

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
*  Base class for Velocity runtime exceptions thrown to the 
 * application layer.    
 *
 * @author <a href="mailto:kdowney@amberarcher.com">Kyle F. Downey</a>
 * @version $Id: VelocityException.java 685685 2008-08-13 21:43:27Z nbubna $
 */
public class VelocityException
        extends RuntimeException
{
    /**
     * Version Id for serializable
     */
    private static final long serialVersionUID = 1251243065134956045L;

    private final Throwable wrapped;

    /**
     * @param exceptionMessage The message to register.
     */
    public VelocityException(final String exceptionMessage)
    {
        super(exceptionMessage);
        wrapped = null;
    }

    /**
     * @param exceptionMessage The message to register.
     * @param wrapped A throwable object that caused the Exception.
     * @since 1.5
     */
    public VelocityException(final String exceptionMessage, final Throwable wrapped)
    {
        super(exceptionMessage);
        this.wrapped = wrapped;
        ExceptionUtils.setCause(this, wrapped);
    }

    /**
     * @param wrapped A throwable object that caused the Exception.
     * @since 1.5
     */
    public VelocityException(final Throwable wrapped)
    {
        super();
        this.wrapped = wrapped;
        ExceptionUtils.setCause(this, wrapped);
    }

    /**
     *  returns the wrapped Throwable that caused this
     *  MethodInvocationException to be thrown
     *
     *  @return Throwable thrown by method invocation
     *  @since 1.5
     */
    public Throwable getWrappedThrowable()
    {
        return wrapped;
    }
}
