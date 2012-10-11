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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.jdom.Element;

/**
 * This class allows you to walk a tree of JDOM Element objects.
 * It first walks the tree itself starting at the Element passed
 * into allElements() and stores each node of the tree
 * in a Vector which allElements() returns as a result of its
 * execution. You can then use a #foreach in Velocity to walk
 * over the Vector and visit each Element node. However, you can
 * achieve the same effect by calling <code>element.selectNodes("//*")</code>.
 *
 * @author <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @author <a href="mailto:szegedia@freemail.hu">Attila Szegedi</a>
 * @version $Id: TreeWalker.java 463298 2006-10-12 16:10:32Z henning $
 */
public class TreeWalker
{
    /**
     * Empty constructor
     */
    public TreeWalker()
    {
        // Left blank
    }

    /**
     * Creates a new Vector and walks the Element tree.
     *
     * @param e the starting Element node
     * @return Vector a vector of Element nodes
     */
    public NodeList allElements(Element e)
    {
        ArrayList theElements = new ArrayList();
        treeWalk (e, theElements);
        return new NodeList(theElements, false);
    }

    /**
     * A recursive method to walk the Element tree.
     * @param Element the current Element
     */
    private final void treeWalk(Element e, Collection theElements )
    {
        for (Iterator i=e.getChildren().iterator(); i.hasNext(); )
        {
            Element child = (Element)i.next();
            theElements.add(child);
            treeWalk(child, theElements);
        }
    }
}
