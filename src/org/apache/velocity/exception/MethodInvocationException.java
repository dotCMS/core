package org.apache.velocity.exception;

import org.apache.velocity.runtime.log.Log;

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
 *  Application-level exception thrown when a reference method is
 *  invoked and an exception is thrown.
 *  <br>
 *  When this exception is thrown, a best effort will be made to have
 *  useful information in the exception's message.  For complete
 *  information, consult the runtime log.
 *
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @version $Id: MethodInvocationException.java 898032 2010-01-11 19:51:03Z nbubna $
 */
public class MethodInvocationException extends VelocityException implements ExtendedParseException
{
    /**
     * Version Id for serializable
     */
    private static final long serialVersionUID = 7305685093478106342L;

    private String referenceName = "";

    private final String methodName;
    
    private final int lineNumber;
    private final int columnNumber;
    private final String templateName;

    /**
     *  CTOR - wraps the passed in exception for
     *  examination later
     *
     *  @param message
     *  @param e Throwable that we are wrapping
     *  @param methodName name of method that threw the exception
     *  @param templateName The name of the template where the exception occured.
     */
    public MethodInvocationException(final String message, final Throwable e, final String methodName, final String templateName, final int lineNumber, final int columnNumber)
    {
        super(message, e);

        this.methodName = methodName;
        this.templateName = templateName;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    /**
     *  Returns the name of the method that threw the
     *  exception.
     *
     *  @return String name of method
     */
    public String getMethodName()
    {
        return methodName;
    }

    /**
     *  Sets the reference name that threw this exception.
     *
     *  @param ref name of reference
     */
    public void setReferenceName(String ref)
    {
        referenceName = ref;
    }

    /**
     *  Retrieves the name of the reference that caused the
     *  exception.
     *
     *  @return name of reference.
     */
    public String getReferenceName()
    {
        return referenceName;
    }

    /**
     * @see ExtendedParseException#getColumnNumber()
     * @since 1.5
     */
    public int getColumnNumber()
    {
	    return columnNumber;
    }

    /**
     * @see ExtendedParseException#getLineNumber()
     * @since 1.5
     */
    public int getLineNumber()
    {
	    return lineNumber;
    }

    /**
     * @see ExtendedParseException#getTemplateName()
     * @since 1.5
     */
    public String getTemplateName()
    {
	    return templateName;
    }

    /**
     * @see Exception#getMessage()
     * @since 1.5
     */
    public String getMessage()
    {
        StringBuffer message = new StringBuffer();
        message.append(super.getMessage());
        message.append(" at ");
        message.append(Log.formatFileString(templateName, lineNumber, columnNumber));
        return message.toString();
    }
}
