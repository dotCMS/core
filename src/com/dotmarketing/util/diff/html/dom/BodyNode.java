/*
 * Copyright 2007 Guy Van den Broeck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dotmarketing.util.diff.html.dom;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.helpers.AttributesImpl;

/**
 * Represents the root of a HTML document.
 */
public class BodyNode extends TagNode {

    public BodyNode() {
        super(null, "body", new AttributesImpl());
    }

    @Override
    public Node copyTree() {
        BodyNode newThis = new BodyNode();
        for (Node child : this) {
            Node newChild = child.copyTree();
            newChild.setParent(newThis);
            newThis.addChild(newChild);
        }
        return newThis;
    }
    
    @Override
    public List<Node> getMinimalDeletedSet(long id) {
        List<Node> nodes = new ArrayList<Node>();
        for (Node child : this) {
            List<Node> childrenChildren = child.getMinimalDeletedSet(id);
            nodes.addAll(childrenChildren);

        }        
        return nodes;
    }

}
