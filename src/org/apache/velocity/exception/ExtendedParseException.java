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
 * All Exceptions that can provide additional information about the place
 * where the error happened (template name, column and line number) can
 * implement this interface and the ParseErrorException will then be able
 * to deal with this information.
 *
 * @author <a href="hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @version $Id: ExtendedParseException.java 685685 2008-08-13 21:43:27Z nbubna $
 * @since 1.5
 */
public interface ExtendedParseException
{
    /**
     * returns the Template name where this exception occured.
     * @return The Template name where this exception occured.
     */
    String getTemplateName();

    /**
     * returns the line number where this exception occured.
     * @return The line number where this exception occured.
     */
    int getLineNumber();

    /**
     * returns the column number where this exception occured.
     * @return The column number where this exception occured.
     */
    int getColumnNumber();
}

