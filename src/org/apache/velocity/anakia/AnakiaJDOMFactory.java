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

import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.DefaultJDOMFactory;

/**
 * A customized JDOMFactory for Anakia that produces {@link AnakiaElement}
 * instances instead of ordinary JDOM {@link Element} instances.
 *
 * @author <a href="mailto:szegedia@freemail.hu">Attila Szegedi</a>
 * @version $Id: AnakiaJDOMFactory.java 463298 2006-10-12 16:10:32Z henning $
 */
public class AnakiaJDOMFactory extends DefaultJDOMFactory
{
    /**
     *
     */
    public AnakiaJDOMFactory()
    {
    }

    /**
     * @see org.jdom.DefaultJDOMFactory#element(java.lang.String, org.jdom.Namespace)
     */
    public Element element(String name, Namespace namespace)
    {
        return new AnakiaElement(name, namespace);
    }

    /**
     * @see org.jdom.DefaultJDOMFactory#element(java.lang.String)
     */
    public Element element(String name)
    {
        return new AnakiaElement(name);
    }

    /**
     * @see org.jdom.DefaultJDOMFactory#element(java.lang.String, java.lang.String)
     */
    public Element element(String name, String uri)
    {
        return new AnakiaElement(name, uri);
    }

    /**
     * @see org.jdom.DefaultJDOMFactory#element(java.lang.String, java.lang.String, java.lang.String)
     */
    public Element element(String name, String prefix, String uri)
    {
        return new AnakiaElement(name, prefix, uri);
    }
}
