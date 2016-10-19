package org.apache.velocity.anakia;

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
 * This class is for escaping CDATA sections. The code was
 * "borrowed" from the JDOM code. Also included is escaping
 * the " -> &amp;quot; character and the conversion of newlines
 * to the platform line separator.
 *
 * @author <a href="mailto:wglass@apache.org">Will Glass-Husain</a>
 * @author <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @version $Id: Escape.java 463298 2006-10-12 16:10:32Z henning $
 */
public class Escape
{
    /**
     *
     */
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /**
     * Empty constructor
     */
    public Escape()
    {
        // left blank on purpose
    }

    /**
     * Do the escaping.
     * @param st
     * @return The escaped text.
     */
    public static final String getText(String st)
    {
        StringBuffer buff = new StringBuffer();
        char[] block = st.toCharArray();
        String stEntity = null;
        int i, last;

        for (i=0, last=0; i < block.length; i++)
        {
            switch(block[i])
            {
                case '<' :
                    stEntity = "&lt;";
                    break;
                case '>' :
                    stEntity = "&gt;";
                    break;
                case '&' :
                    stEntity = "&amp;";
                    break;
                case '"' :
                    stEntity = "&quot;";
                    break;
                case '\n' :
                    stEntity = LINE_SEPARATOR;
                    break;
                default :
                    /* no-op */
                    break;
            }
            if (stEntity != null)
            {
                buff.append(block, last, i - last);
                buff.append(stEntity);
                stEntity = null;
                last = i + 1;
            }
        }
        if(last < block.length)
        {
            buff.append(block, last, i - last);
        }
        return buff.toString();
    }
}
