package org.apache.velocity.exception;

import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.util.ExceptionUtils;
import org.apache.velocity.util.introspection.Info;

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
    
    /**
     * Creates a string that formats the template filename with line number
     * and column of the given Directive. We use this routine to provide a cosistent format for displaying 
     * file errors.
     */
    public static final String formatFileString(Directive directive)
    {
      return formatFileString(directive.getTemplateName(), directive.getLine(), directive.getColumn());      
    }

    /**
     * Creates a string that formats the template filename with line number
     * and column of the given Node. We use this routine to provide a cosistent format for displaying 
     * file errors.
     */
    public static final String formatFileString(Node node)
    {
      return formatFileString(node.getTemplateName(), node.getLine(), node.getColumn());      
    }
    
    /**
     * Simply creates a string that formats the template filename with line number
     * and column. We use this routine to provide a cosistent format for displaying 
     * file errors.
     */
    public static final String formatFileString(Info info)
    {
        return formatFileString(info.getTemplateName(), info.getLine(), info.getColumn());
    }
    
    /**
     * Simply creates a string that formats the template filename with line number
     * and column. We use this routine to provide a cosistent format for displaying 
     * file errors.
     * @param template File name of template, can be null
     * @param linenum Line number within the file
     * @param colnum Column number withing the file at linenum
     */
    public static final String formatFileString(String template, int linenum, int colnum)
    {
        if (template == null || template.equals(""))
        {
            template = "<unknown template>";
        }
        return template + "[line " + linenum + ", column " + colnum + "]";
    }
}
