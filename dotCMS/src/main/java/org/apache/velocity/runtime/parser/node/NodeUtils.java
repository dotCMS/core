package org.apache.velocity.runtime.parser.node;

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

import org.apache.commons.lang.text.StrBuilder;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.runtime.parser.ParserConstants;
import org.apache.velocity.runtime.parser.Token;

/**
 * Utilities for dealing with the AST node structure.
 *
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @version $Id: NodeUtils.java 687386 2008-08-20 16:57:07Z nbubna $
 */
public class NodeUtils
{
    /**
     * @deprecated use getSpecialText(Token t)
     */
    public static String specialText(Token t)
    {
        if (t.specialToken == null || t.specialToken.image.startsWith("##") )
        {
            return "";
        }
        return getSpecialText(t).toString();
    }

    /**
     * Collect all the <SPECIAL_TOKEN>s that
     * are carried along with a token. Special
     * tokens do not participate in parsing but
     * can still trigger certain lexical actions.
     * In some cases you may want to retrieve these
     * special tokens, this is simply a way to
     * extract them.
     * @param t the Token
     * @return StrBuilder with the special tokens.
     */
    public static StrBuilder getSpecialText(Token t)
    {
        StrBuilder sb = new StrBuilder();

        Token tmp_t = t.specialToken;

        while (tmp_t.specialToken != null)
        {
            tmp_t = tmp_t.specialToken;
        }

        while (tmp_t != null)
        {
            String st = tmp_t.image;

            for(int i = 0, is = st.length(); i < is; i++)
            {
                char c = st.charAt(i);

                if ( c == '#' || c == '$' )
                {
                    sb.append( c );
                }

                /*
                 *  more dreaded MORE hack :)
                 *
                 *  looking for ("\\")*"$" sequences
                 */

                if ( c == '\\')
                {
                    boolean ok = true;
                    boolean term = false;

                    int j = i;
                    for( ok = true; ok && j < is; j++)
                    {
                        char cc = st.charAt( j );

                        if (cc == '\\')
                        {
                            /*
                             *  if we see a \, keep going
                             */
                            continue;
                        }
                        else if( cc == '$' )
                        {
                            /*
                             *  a $ ends it correctly
                             */
                            term = true;
                            ok = false;
                        }
                        else
                        {
                            /*
                             *  nah...
                             */
                            ok = false;
                        }
                    }

                    if (term)
                    {
                        String foo =  st.substring( i, j );
                        sb.append( foo );
                        i = j;
                    }
                }
            }

            tmp_t = tmp_t.next;
        }
        return sb;
    }

    /**
     *  complete node literal
     * @param t
     * @return A node literal.
     */
    public static String tokenLiteral( Token t )
    {
        // Look at kind of token and return "" when it's a multiline comment
        if (t.kind == ParserConstants.MULTI_LINE_COMMENT) 
        {
            return "";
        } 
        else if (t.specialToken == null || t.specialToken.image.startsWith("##"))
        {
            return t.image;
        }
        else 
        {
            StrBuilder special = getSpecialText(t);
            if (special.length() > 0)
            {
                return special.append(t.image).toString();
            }
            return t.image;
        }
    } 
    
    /**
     * Utility method to interpolate context variables
     * into string literals. So that the following will
     * work:
     *
     * #set $name = "candy"
     * $image.getURI("${name}.jpg")
     *
     * And the string literal argument will
     * be transformed into "candy.jpg" before
     * the method is executed.
     * 
     * @deprecated this method isn't called by any class
     * 
     * @param argStr
     * @param vars
     * @return Interpoliation result.
     * @throws MethodInvocationException
     */
    public static String interpolate(String argStr, Context vars) throws MethodInvocationException
    {
        // if there's nothing to replace, skip this (saves buffer allocation)
        if( argStr.indexOf('$') == -1 )
            return argStr;
        
        StrBuilder argBuf = new StrBuilder();

        for (int cIdx = 0, is = argStr.length(); cIdx < is;)
        {
            char ch = argStr.charAt(cIdx);
            
            if( ch == '$' )
            {
                StrBuilder nameBuf = new StrBuilder();
                for (++cIdx ; cIdx < is; ++cIdx)
                {
                    ch = argStr.charAt(cIdx);
                    if (ch == '_' || ch == '-'
                        || Character.isLetterOrDigit(ch))
                        nameBuf.append(ch);
                    else if (ch == '{' || ch == '}')
                        continue;
                    else
                        break;
                }

                if (nameBuf.length() > 0)
                {
                    Object value = vars.get(nameBuf.toString());

                    if (value == null)
                        argBuf.append("$").append(nameBuf.toString());
                    else
                        argBuf.append(value.toString());
                }
                
            }
            else
            {
                argBuf.append(ch);
                ++cIdx;
            }
        }

        return argBuf.toString();
    }
}
