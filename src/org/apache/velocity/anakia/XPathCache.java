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

import com.werken.xpath.XPath;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Provides a cache for XPath expressions. Used by {@link NodeList} and
 * {@link AnakiaElement} to minimize XPath parsing in their
 * <code>selectNodes()</code> methods.
 *
 * @author <a href="mailto:szegedia@freemail.hu">Attila Szegedi</a>
 * @version $Id: XPathCache.java 463298 2006-10-12 16:10:32Z henning $
 */
class XPathCache
{
    // Cache of already parsed XPath expressions, keyed by String representations
    // of the expression as passed to getXPath().
    private static final Map XPATH_CACHE = new WeakHashMap();

    private XPathCache()
    {
    }

    /**
     * Returns an XPath object representing the requested XPath expression.
     * A cached object is returned if it already exists for the requested expression.
     * @param xpathString the XPath expression to parse
     * @return the XPath object that represents the parsed XPath expression.
     */
    static XPath getXPath(String xpathString)
    {
        XPath xpath = null;
        synchronized(XPATH_CACHE)
        {
            xpath = (XPath)XPATH_CACHE.get(xpathString);
            if(xpath == null)
            {
                xpath = new XPath(xpathString);
                XPATH_CACHE.put(xpathString, xpath);
            }
        }
        return xpath;
    }
}
