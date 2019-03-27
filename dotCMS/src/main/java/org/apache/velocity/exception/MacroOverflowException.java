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
 * Application-level exception thrown when macro calls within macro calls
 * exceeds the maximum allowed depth. The maximum allowable depth is given
 * in the configuration as velocimacro.max.depth.
 * @since 1.6
 */
public class MacroOverflowException extends VelocityException
{
    /**
    * Version Id for serializable
    */
    private static final long serialVersionUID = 7305635093478106342L;

    /**
     * @param exceptionMessage The message to register.
     */
    public MacroOverflowException(final String exceptionMessage)
    {
        super(exceptionMessage);
    }

    /**
     * @param exceptionMessage The message to register.
     * @param wrapped A throwable object that caused the Exception.
     */
    public MacroOverflowException(final String exceptionMessage, final Throwable wrapped)
    {
        super(exceptionMessage, wrapped);
    }

    /**
     * @param wrapped A throwable object that caused the Exception.
     */
    public MacroOverflowException(final Throwable wrapped)
    {
        super(wrapped);
    }
}
