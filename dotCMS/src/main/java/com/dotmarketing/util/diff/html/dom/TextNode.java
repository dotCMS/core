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

import com.dotmarketing.util.diff.html.modification.Modification;
import com.dotmarketing.util.diff.html.modification.ModificationType;

/**
 * Represents a piece of text in the HTML file.
 */
public class TextNode extends Node implements Cloneable {

    private String s;

    private Modification modification;

    public TextNode(TagNode parent, String s) {
        super(parent);
        this.modification = new Modification(ModificationType.NONE);
        this.s = s;
    }

    @Override
    public Node copyTree() {
        try {
            Node node = (Node) clone();
            node.setParent(null);
            return node;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    @Override
    public Node getLeftMostChild() {
        return this;
    }

    @Override
    public List<Node> getMinimalDeletedSet(long id) {
        List<Node> nodes = new ArrayList<Node>(1);
        if (getModification().getType() == ModificationType.REMOVED
                && getModification().getID() == id)
            nodes.add(this);

        return nodes;
    }

    public Modification getModification() {
        return this.modification;
    }

    @Override
    public Node getRightMostChild() {
        return this;
    }

    public String getText() {
        return s;
    }

    public boolean isSameText(Object other) {
        if (other == null)
            return false;

        TextNode otherTextNode;
        try {
            otherTextNode = (TextNode) other;
        } catch (ClassCastException e) {
            return false;
        }
        return getText().replace('\n', ' ').equals(
                otherTextNode.getText().replace('\n', ' '));
    }

    public void setModification(Modification m) {
        this.modification = m;
    }

    @Override
    public String toString() {
        return getText();
    }
}
