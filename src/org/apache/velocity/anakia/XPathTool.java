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

import java.util.List;

import org.jdom.Document;
import org.jdom.Element;

/**
 * This class adds an entrypoint into XPath functionality,
 * for Anakia.
 * <p>
 * All methods take a string XPath specification, along with
 * a context, and produces a resulting java.util.List.
 * <p>
 * The W3C XPath Specification (http://www.w3.org/TR/xpath) refers
 * to NodeSets repeatedly, but this implementation simply uses
 * java.util.List to hold all Nodes.  A 'Node' is any object in
 * a JDOM object tree, such as an org.jdom.Element, org.jdom.Document,
 * or org.jdom.Attribute.
 * <p>
 * To use it in Velocity, do this:
 * <p>
 * <pre>
 * #set $authors = $xpath.applyTo("document/author", $root)
 * #foreach ($author in $authors)
 *   $author.getValue()
 * #end
 * #set $chapterTitles = $xpath.applyTo("document/chapter/@title", $root)
 * #foreach ($title in $chapterTitles)
 *   $title.getValue()
 * #end
 * </pre>
 * <p>
 * In newer Anakia builds, this class is obsoleted in favor of calling
 * <code>selectNodes()</code> on the element directly:
 * <pre>
 * #set $authors = $root.selectNodes("document/author")
 * #foreach ($author in $authors)
 *   $author.getValue()
 * #end
 * #set $chapterTitles = $root.selectNodes("document/chapter/@title")
 * #foreach ($title in $chapterTitles)
 *   $title.getValue()
 * #end
 * </pre>
 * <p>
 *
 * @author <a href="mailto:bob@werken.com">bob mcwhirter</a>
 * @author <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @author <a href="mailto:szegedia@freemail.hu">Attila Szegedi</a>
 * @version $Id: XPathTool.java 463298 2006-10-12 16:10:32Z henning $
 */
public class XPathTool
{
    /**
     * Constructor does nothing, as this is mostly
     * just objectified static methods
     */
    public XPathTool()
    {
        //        RuntimeSingleton.info("XPathTool::XPathTool()");
        // intentionally left blank
    }

    /**
     * Apply an XPath to a JDOM Document
     *
     * @param xpathSpec The XPath to apply
     * @param doc The Document context
     *
     * @return A list of selected nodes
     */
    public NodeList applyTo(String xpathSpec,
                        Document doc)
    {
        //RuntimeSingleton.info("XPathTool::applyTo(String, Document)");
        return new NodeList(XPathCache.getXPath(xpathSpec).applyTo( doc ), false);
    }

    /**
     * Apply an XPath to a JDOM Element
     *
     * @param xpathSpec The XPath to apply
     * @param elem The Element context
     *
     * @return A list of selected nodes
     */
    public NodeList applyTo(String xpathSpec,
                        Element elem)
    {
        //RuntimeSingleton.info("XPathTool::applyTo(String, Element)");
        return new NodeList(XPathCache.getXPath(xpathSpec).applyTo( elem ), false);
    }

    /**
     * Apply an XPath to a nodeset
     *
     * @param xpathSpec The XPath to apply
     * @param nodeSet The nodeset context
     *
     * @return A list of selected nodes
     */
    public NodeList applyTo(String xpathSpec,
                        List nodeSet)
    {
        //RuntimeSingleton.info("XPathTool::applyTo(String, List)");
        return new NodeList(XPathCache.getXPath(xpathSpec).applyTo( nodeSet ), false);
    }
}



