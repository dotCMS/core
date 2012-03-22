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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.dotmarketing.util.diff.html.ancestor.TextOnlyComparator;
import com.dotmarketing.util.diff.html.dom.helper.AttributesMap;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Node that can contain other nodes. Represents an HTML tag.
 */
public class TagNode extends Node implements Iterable<Node> {

    private List<Node> children = new ArrayList<Node>();

    private String qName;

    private Attributes attributes;

    public TagNode(TagNode parent, String qName, Attributes attributesarg) {
        super(parent);
        this.qName = qName;
        attributes = new AttributesImpl(attributesarg);
    }

    /**
     * appends the provided node to the collection of children if 
     * <code>this</code> node is set as the parameter's parent.
     * This method is used in the <code>Node</code>'s constructor
     * @param node - child to add must have 
     * <code>this</code> node set as its parent.
     * @throws java.lang.IllegalStateException - if the parameter
     * has different parent than <code>this</code> node.
     */
    public void addChild(Node node) {
        if (node.getParent() != this)
            throw new IllegalStateException(
                    "The new child must have this node as a parent.");
        children.add(node);
    }

    /**
     * If the provided parameter is in the same tree with
     * <code>this</code> object then this method fetches 
     * index of the parameter object in the children collection. 
     * If the parameter is from a different tree, then this method
     * attempts to return the index of first semantically equivalent 
     * node to the parameter.
     * @param child - the template of a tag we need an index for.
     * @return the index of first semantically equivalent child 
     * or -1 if couldn't find one
     */
    public int getIndexOf(Node child) {
        return children.indexOf(child);
    }
    
    /**
     * Inserts provided node in the collection of children at the specified index 
     * if <code>this</code> node is set as a parent for the parameter.
     * @param index - desired position among the children
     * @param node - the node to insert as a child. 
     * @throws java.lang.IllegalStateException - if the provided node has
     * different parent from <code>this</code> node.
     */
    public void addChild(int index, Node node) {
        if (node.getParent() != this)
            throw new IllegalStateException(
                    "The new child must have this node as a parent.");
        children.add(index, node);
    }

    public Node getChild(int i) {
        return children.get(i);
    }

    /**
     * @return <code>Iterator&lt;Node></code> over children collection
     * @throws java.lang.NullPointerException - if children collection is null
     */
    public Iterator<Node> iterator() {
        return children.iterator();
    }

    /**
     * @return number of elements in the children collection or 0 if 
     * the collection is <code>null</code>
     */
    public int getNbChildren() {
    	if (children == null){
    		return 0;
    	} else {
    		return children.size();
    	}
    }

    public String getQName() {
        return qName;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    /**
     * checks tags for being semantically equivalent if it's from 
     * a different tree and for being the same object if it's 
     * from the same tree as <code>this</code> tag.
     * @param other - tag to compare to
     */
    public boolean isSameTag(TagNode other) {
        if (other == null)
            return false;
        return equals(other);
    }

    /**
     * Considers tags from different trees equal 
     * if they have same name and equivalent attributes.
     * No attention paid to the content (children) of the tag.
     * Considers tags from the same tree equal if it is 
     * the same object.
     * @param obj - object to compare to.
     */
    @Override
    public boolean equals(Object obj) {
    	if (obj == null){
    		return false;
    	}
    	boolean result = false;
    	
    	if (obj instanceof TagNode) {
			TagNode tagNode = (TagNode) obj;
			
			if (tagNode == this){
				result = true;
			} else {
				boolean differentTrees = true;
				//attempt to avoid recursive calls
				//to get root. comparing parents,
				//as most often equals method is used
				//when operating with children collection
				if (tagNode.getParent() != null && 
					tagNode.getParent() == this.getParent()){
					differentTrees = false;
				} else {
					TagNode myRoot = this.getRoot();
					TagNode otherRoot = tagNode.getRoot();
					differentTrees = !(myRoot == otherRoot);
				}
				if (differentTrees){ //still a chance for being equal
					//if we are in the different tree
					//we should use semantic equivalence instead
					if(this.getQName().equalsIgnoreCase(tagNode.getQName())){
						AttributesMap localAttributesMap = new AttributesMap(getAttributes());
						AttributesMap externalAttributesMap = new AttributesMap(tagNode.getAttributes());
						result = localAttributesMap.equals(externalAttributesMap);
					}
				} 
			}
		}
    	return result;
    }
    
    /**
     * Since we only consider so much information of the TagNode in
     * <code>equals</code> method, we need to re-write 
     * <code>hashCode</code> method to correspond. Otherwise 
     * <code>HashTable</code>s and <code>HashMaps</code> might
     * behave unexpectedly.
     */
    @Override
    public int hashCode(){
    	final int simple = 29;
    	int result = this.getQName().hashCode();
    	AttributesMap attrs = new AttributesMap(getAttributes());
    	result = result*simple + attrs.hashCode();
    	return result;
    }
    
    /**
     * Produces <code>String</code> for the opening HTML tag for this node.
     * Includes the attributes. This probably doesn't work for image tag.
     * @return the <code>String</code> representation of the corresponding
     * opening HTML tag.
     */
    public String getOpeningTag() {
        String s = "<" + getQName();
        Attributes localAttributes = getAttributes();
        for (int i = 0; i < localAttributes.getLength(); i++) {
            s += " " + localAttributes.getQName(i) + "=\""
                    + localAttributes.getValue(i) + "\"";
        }
        return s += ">";
    }

    /**
     * @return <code>String</code> representation of the closing HTML tag that
     * corresponds to the current node. Probably doesn't work for image tag.
     */
    public String getEndTag() {
        return "</" + getQName() + ">";
    }

    /**
     * <p> This recursive method considers a descendant deleted if all its 
     * children had <code>TextNode</code>s that now are marked as removed 
     * with the provided id. If all children of a descendant is considered 
     * deleted, only that descendant is kept in the collection of the 
     * deleted nodes, and its children are removed from the collection 
     * of the deleted nodes.<br>
     * The HTML tag nodes that never had any text content are never considered 
     * removed </p>
     * <p>It actually might have nothing to do with being really deleted, because
     * the element might be kept after its text content was deleted.<br>
     * Example:<br>
     * table cells can be kept after its text content was deleted</br>
     * horizontal rule has never had text content, but can be deleted</p>
     */
    @Override
    public List<Node> getMinimalDeletedSet(long id) {

        List<Node> nodes = new ArrayList<Node>();

        //no-content tags are never included in the set
        if (children.size() == 0)
            return nodes;

        //by default we think that all kids are in the deleted set
        //until we prove otherwise
        boolean hasNotDeletedDescendant = false;

        for (Node child : this) {//check if kids are in the deleted set
            List<Node> childrenChildren = child.getMinimalDeletedSet(id);
            nodes.addAll(childrenChildren);
            if (!hasNotDeletedDescendant
                    && !(childrenChildren.size() == 1 && childrenChildren
                            .contains(child))) {
                // This child is not entirely deleted
                hasNotDeletedDescendant = true;
            }
        }
        //if all kids are in the deleted set - remove them and put this instead
        if (!hasNotDeletedDescendant) {
            nodes.clear();
            nodes.add(this);
        }
        return nodes;
    }

    @Override
    public String toString() {
        return getOpeningTag();
    }

    /**
     * Attempts to create 2 <code>TagNode</code>s with 
     * the same name and attributes as the original <code>this</code> node.
     * All children preceding split parameter are placed into the left part,
     * all children following the split parameter are placed into 
     * the right part. Placement of the split node is determined by 
     * includeLeft flag parameter. The newly created nodes are only added to
     * the parent of <code>this</code> node if they have some children.
     * The original <code>this</code> node is removed afterwards. The process
     * proceeds recursively hiking up the tree until the "parent" node is
     * reached. "Parent" node will not be touched.
     * This method is used when the parent tags of a deleted 
     * <code>TextNode</code> can no longer be found in the new doc. (means
     * they either has been deleted or changed arguments). The "parent" 
     * parameter in that case is the deepest common parent between the
     * deleted node and its surrounding remaining siblings.
     * @param parent - the node that should not participate in split operation
     * (where the split operation stops)
     * @param split - the node-divider to divide children among splitted parts
     * @param includeLeft - if <code>true</code> the "split" node will be 
     * included in the left part.
     * @return <code>true</code> if single <code>this</code> node 
     * was substituted with 2 new similar nodes with original children
     * divided among them. 
     */
    public boolean splitUntill(TagNode parent, Node split, boolean includeLeft) {
        boolean splitOccured = false;
        if (parent != this) {
            TagNode part1 = new TagNode(null, getQName(), getAttributes());
            TagNode part2 = new TagNode(null, getQName(), getAttributes());
            part1.setParent(getParent());
            part2.setParent(getParent());

            int i = 0;
            while (i < children.size() && children.get(i) != split) {
                children.get(i).setParent(part1);
                part1.addChild(children.get(i));
                i++;
            }
            if (i < children.size()) {//means we've found "split" node
                if (includeLeft) {
                    children.get(i).setParent(part1);
                    part1.addChild(children.get(i));
                } else {
                    children.get(i).setParent(part2);
                    part2.addChild(children.get(i));
                }
                i++;
            }
            while (i < children.size()) {
                children.get(i).setParent(part2);
                part2.addChild(children.get(i));
                i++;
            }
            if (part1.getNbChildren() > 0)
                getParent().addChild(getParent().getIndexOf(this), part1);

            if (part2.getNbChildren() > 0)
                getParent().addChild(getParent().getIndexOf(this), part2);

            if (part1.getNbChildren() > 0 && part2.getNbChildren() > 0) {
                splitOccured = true;
            }
            
            //since split isn't meant for no-children tags,
            //we won't have a case where we removed this and did not
            //substitute it with anything
            getParent().removeChild(this);

            if (includeLeft)
                getParent().splitUntill(parent, part1, includeLeft);
            else
                getParent().splitUntill(parent, part2, includeLeft);
        }
        return splitOccured;

    }

    private void removeChild(Node node) {
        children.remove(node);
    }

    //block tags
    private static Set<String> blocks = new HashSet<String>();
    static {
        blocks.add("html");
        blocks.add("body");
        blocks.add("p");
        blocks.add("blockquote");
        blocks.add("h1");
        blocks.add("h2");
        blocks.add("h3");
        blocks.add("h4");
        blocks.add("h5");
        blocks.add("pre");
        blocks.add("div");
        blocks.add("ul");
        blocks.add("ol");
        blocks.add("li");
        blocks.add("table");
        blocks.add("tbody");
        blocks.add("tr");
        blocks.add("td");
        blocks.add("th");
        blocks.add("br");
        blocks.add("thead");
        blocks.add("tfoot");
    }

    public static boolean isBlockLevel(String qName) {
        return blocks.contains(qName.toLowerCase());
    }

    public static boolean isBlockLevel(Node node) {
        try {
            TagNode tagnode = (TagNode) node;
            return isBlockLevel(tagnode.getQName());
        } catch (ClassCastException e) {
            return false;
        }
    }

    public boolean isBlockLevel() {
        return isBlockLevel(this);
    }

    public static boolean isInline(String qName) {
        return !isBlockLevel(qName);
    }

    public static boolean isInline(Node node) {
        return !isBlockLevel(node);
    }

    public boolean isInline() {
        return isInline(this);
    }

    @Override
    public Node copyTree() {
        TagNode newThis = new TagNode(null, getQName(), new AttributesImpl(
                getAttributes()));
        newThis.setWhiteBefore(isWhiteBefore());
        newThis.setWhiteAfter(isWhiteAfter());
        for (Node child : this) {
            Node newChild = child.copyTree();
            newChild.setParent(newThis);
            newThis.addChild(newChild);
        }
        return newThis;
    }

    public double getMatchRatio(TagNode other) {
        TextOnlyComparator txtComp = new TextOnlyComparator(other);
        return txtComp.getMatchRatio(new TextOnlyComparator(this));
    }

    public void expandWhiteSpace() {

        int shift = 0;
        boolean spaceAdded = false;

        int nbOriginalChildren = getNbChildren();
        for (int i = 0; i < nbOriginalChildren; i++) {
            Node child = getChild(i + shift);
            try {
                TagNode tagChild = (TagNode) child;

                if (!tagChild.isPre()) {
                    tagChild.expandWhiteSpace();
                }
            } catch (ClassCastException e) {
            }

            if (!spaceAdded && child.isWhiteBefore()) {
                WhiteSpaceNode ws = new WhiteSpaceNode(null, " ", child
                        .getLeftMostChild());
                ws.setParent(this);
                addChild(i + (shift++), ws);
            }
            if (child.isWhiteAfter()) {
                WhiteSpaceNode ws = new WhiteSpaceNode(null, " ", child
                        .getRightMostChild());
                ws.setParent(this);
                addChild(i + 1 + (shift++), ws);
                spaceAdded = true;
            } else {
                spaceAdded = false;
            }

        }
    }

    @Override
    public Node getLeftMostChild() {
        if (getNbChildren() < 1)
            return this;
        Node child = getChild(0);
        return child.getLeftMostChild();

    }

    @Override
    public Node getRightMostChild() {
        if (getNbChildren() < 1)
            return this;
        Node child = getChild(getNbChildren() - 1);
        return child.getRightMostChild();
    }

    public boolean isPre() {
        return getQName().equalsIgnoreCase("pre");
    }

}
