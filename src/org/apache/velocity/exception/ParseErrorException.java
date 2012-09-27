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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.velocity.runtime.log.Log;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.util.introspection.Info;

/**
 *  Application-level exception thrown when a resource of any type
 *  has a syntax or other error which prevents it from being parsed.
 *  <br>
 *  When this resource is thrown, a best effort will be made to have
 *  useful information in the exception's message.  For complete
 *  information, consult the runtime log.
 *
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @author <a href="hps@intermeta.de">Henning P. Schmiedehausen</a>
 * @version $Id: ParseErrorException.java 736638 2009-01-22 13:42:52Z byron $
 */
public class ParseErrorException extends VelocityException
{
    /**
     * Version Id for serializable
     */
    private static final long serialVersionUID = -6665197935086306472L;

    /**
     * The column number of the parsing error, or -1 if not defined.
     */
    private int columnNumber = -1;

    /**
     * The line number of the parsing error, or -1 if not defined.
     */
    private int lineNumber = -1;

    /**
     * The name of the template containing the error, or null if not defined.
     */
    private String templateName = "*unset*";

    /**
     * If applicable, contains the invalid syntax or reference that triggered this exception
     */
    private String invalidSyntax;
    
    /**
     * If we modify the message, then we set this
     */
    private String msg = null;

    /**
     * Create a ParseErrorException with the given message.
     *
     * @param exceptionMessage the error exception message
     */
    public ParseErrorException(String exceptionMessage)
    {
          super(exceptionMessage);
    }

    private static final Pattern lexError = Pattern.compile("Lexical error.*TokenMgrError.*line (\\d+),.*column (\\d+)\\.(.*)");
    
    /**
     * Create a ParseErrorException with the given ParseException.
     *
     * @param pex the parsing exception
     * @since 1.5
     */
    public ParseErrorException(ParseException pex, String templName)
    {
        super(pex.getMessage());
        
        if (templName != null) templateName = templName;

        // Don't use a second C'tor, TemplateParseException is a subclass of
        // ParseException...
        if (pex instanceof ExtendedParseException)
        {
            ExtendedParseException xpex = (ExtendedParseException) pex;

            columnNumber = xpex.getColumnNumber();
            lineNumber = xpex.getLineNumber();
            templateName = xpex.getTemplateName();
        }
        else
        { 
            // We get here if the the Parser has thrown an exception. Unfortunately,
            // the error message created is hard coded by javacc, so here we alter
            // the error message, so that it is in our standard format.          
            Matcher match =  lexError.matcher(pex.getMessage());
            if (match.matches())
            {
               lineNumber = Integer.parseInt(match.group(1));
               columnNumber = Integer.parseInt(match.group(2));
               String restOfMsg = match.group(3);
               msg = "Lexical error, " + restOfMsg + " at " 
                 + Log.formatFileString(templateName, lineNumber, columnNumber);
            }
          
            //  ugly, ugly, ugly...

            if (pex.currentToken != null && pex.currentToken.next != null)
            {
                columnNumber = pex.currentToken.next.beginColumn;
                lineNumber = pex.currentToken.next.beginLine;
            }
        }
    }

    /**
     * Create a ParseErrorException with the given ParseException.
     *
     * @param pex the parsing exception
     * @since 1.5
     */
    public ParseErrorException(VelocityException pex, String templName)
    {
        super(pex.getMessage());
        
        if (templName != null) templateName = templName;

        // Don't use a second C'tor, TemplateParseException is a subclass of
        // ParseException...
        if (pex instanceof ExtendedParseException)
        {
            ExtendedParseException xpex = (ExtendedParseException) pex;

            columnNumber = xpex.getColumnNumber();
            lineNumber = xpex.getLineNumber();
            templateName = xpex.getTemplateName();
        }
        else if (pex.getWrappedThrowable() instanceof ParseException)
        {
            ParseException pex2 = (ParseException) pex.getWrappedThrowable();

            if (pex2.currentToken != null && pex2.currentToken.next != null)
            {
                columnNumber = pex2.currentToken.next.beginColumn;
                lineNumber = pex2.currentToken.next.beginLine;
            }
        }
    }


    /**
     * Create a ParseErrorRuntimeException with the given message and info
     * 
     * @param exceptionMessage the error exception message
     * @param info an Info object with the current template info
     * @since 1.5
     */
    public ParseErrorException(String exceptionMessage, Info info)
    {
        super(exceptionMessage);
        columnNumber = info.getColumn();
        lineNumber = info.getLine();
        templateName = info.getTemplateName();        
    }    

    /**
     * Create a ParseErrorRuntimeException with the given message and info
     * 
     * @param exceptionMessage the error exception message
     * @param info an Info object with the current template info
     * @param invalidSyntax the invalid syntax or reference triggering this exception
     * @since 1.5
     */
    public ParseErrorException(String exceptionMessage, 
            Info info, String invalidSyntax)
    {
        super(exceptionMessage);
        columnNumber = info.getColumn();
        lineNumber = info.getLine();
        templateName = info.getTemplateName();  
        this.invalidSyntax = invalidSyntax;       
    }    


    /**
     * Return the column number of the parsing error, or -1 if not defined.
     *
     * @return column number of the parsing error, or -1 if not defined
     * @since 1.5
     */
    public int getColumnNumber()
    {
        return columnNumber;
    }

    /**
     * Return the line number of the parsing error, or -1 if not defined.
     *
     * @return line number of the parsing error, or -1 if not defined
     * @since 1.5
     */
    public int getLineNumber()
    {
        return lineNumber;
    }

    /**
     * Return the name of the template containing the error, or null if not
     * defined.
     *
     * @return the name of the template containing the parsing error, or null
     *      if not defined
     * @since 1.5
     */
    public String getTemplateName()
    {
        return templateName;
    }

    /**
     * Return the invalid syntax or reference that triggered this error, or null
     * if not defined.
     * 
     * @return Return the invalid syntax or reference that triggered this error, or null
     * if not defined
     * @since 1.5
     */
    public String getInvalidSyntax()
    {
        return invalidSyntax;
    }

    /**
     * Return our custum message if we have one, else return the default message
     */
    public String getMessage()
    {
      if (msg != null) return msg;
      return super.getMessage();
    }
}
