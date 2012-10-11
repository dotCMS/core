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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.jdom.Attribute;
import org.jdom.CDATA;
import org.jdom.Comment;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.EntityRef;
import org.jdom.ProcessingInstruction;
import org.jdom.Text;
import org.jdom.output.XMLOutputter;

/**
 * Provides a class for wrapping a list of JDOM objects primarily for use in template
 * engines and other kinds of text transformation tools.
 * It has a {@link #toString()} method that will output the XML serialized form of the
 * nodes it contains - again focusing on template engine usage, as well as the
 * {@link #selectNodes(String)} method that helps selecting a different set of nodes
 * starting from the nodes in this list. The class also implements the {@link java.util.List}
 * interface by simply delegating calls to the contained list (the {@link #subList(int, int)}
 * method is implemented by delegating to the contained list and wrapping the returned
 * sublist into a <code>NodeList</code>).
 *
 * @author <a href="mailto:szegedia@freemail.hu">Attila Szegedi</a>
 * @version $Id: NodeList.java 463298 2006-10-12 16:10:32Z henning $
 */
public class NodeList implements List, Cloneable
{
    private static final AttributeXMLOutputter DEFAULT_OUTPUTTER =
        new AttributeXMLOutputter();

    /** The contained nodes */
    private List nodes;

    /**
     * Creates an empty node list.
     */
    public NodeList()
    {
        nodes = new ArrayList();
    }

    /**
     * Creates a node list that holds a single {@link Document} node.
     * @param document
     */
    public NodeList(Document document)
    {
        this((Object)document);
    }

    /**
     * Creates a node list that holds a single {@link Element} node.
     * @param element
     */
    public NodeList(Element element)
    {
        this((Object)element);
    }

    private NodeList(Object object)
    {
        if(object == null)
        {
            throw new IllegalArgumentException(
                "Cannot construct NodeList with null.");
        }
        nodes = new ArrayList(1);
        nodes.add(object);
    }

    /**
     * Creates a node list that holds a list of nodes.
     * @param nodes the list of nodes this template should hold. The created
     * template will copy the passed nodes list, so changes to the passed list
     * will not affect the model.
     */
    public NodeList(List nodes)
    {
        this(nodes, true);
    }

    /**
     * Creates a node list that holds a list of nodes.
     * @param nodes the list of nodes this template should hold.
     * @param copy if true, the created template will copy the passed nodes
     * list, so changes to the passed list will not affect the model. If false,
     * the model will reference the passed list and will sense changes in it,
     * altough no operations on the list will be synchronized.
     */
    public NodeList(List nodes, boolean copy)
    {
        if(nodes == null)
        {
            throw new IllegalArgumentException(
                "Cannot initialize NodeList with null list");
        }
        this.nodes = copy ? new ArrayList(nodes) : nodes;
    }

    /**
     * Retrieves the underlying list used to store the nodes. Note however, that
     * you can fully use the underlying list through the <code>List</code> interface
     * of this class itself. You would probably access the underlying list only for
     * synchronization purposes.
     * @return The internal node List.
     */
    public List getList()
    {
        return nodes;
    }

    /**
     * This method returns the string resulting from concatenation of string
     * representations of its nodes. Each node is rendered using its XML
     * serialization format. This greatly simplifies creating XML-transformation
     * templates, as to output a node contained in variable x as XML fragment,
     * you simply write ${x} in the template (or whatever your template engine
     * uses as its expression syntax).
     * @return The Nodelist as printable object.
     */
    public String toString()
    {
        if(nodes.isEmpty())
        {
            return "";
        }

        StringWriter sw = new StringWriter(nodes.size() * 128);
        try
        {
            for(Iterator i = nodes.iterator(); i.hasNext();)
            {
                Object node = i.next();
                if(node instanceof Element)
                {
                    DEFAULT_OUTPUTTER.output((Element)node, sw);
                }
                else if(node instanceof Attribute)
                {
                    DEFAULT_OUTPUTTER.output((Attribute)node, sw);
                }
                else if(node instanceof Text)
                {
                    DEFAULT_OUTPUTTER.output((Text)node, sw);
                }
                else if(node instanceof Document)
                {
                    DEFAULT_OUTPUTTER.output((Document)node, sw);
                }
                else if(node instanceof ProcessingInstruction)
                {
                    DEFAULT_OUTPUTTER.output((ProcessingInstruction)node, sw);
                }
                else if(node instanceof Comment)
                {
                    DEFAULT_OUTPUTTER.output((Comment)node, sw);
                }
                else if(node instanceof CDATA)
                {
                    DEFAULT_OUTPUTTER.output((CDATA)node, sw);
                }
                else if(node instanceof DocType)
                {
                    DEFAULT_OUTPUTTER.output((DocType)node, sw);
                }
                else if(node instanceof EntityRef)
                {
                    DEFAULT_OUTPUTTER.output((EntityRef)node, sw);
                }
                else
                {
                    throw new IllegalArgumentException(
                        "Cannot process a " +
                        (node == null
                         ? "null node"
                         : "node of class " + node.getClass().getName()));
                }
            }
        }
        catch(IOException e)
        {
            // Cannot happen as we work with a StringWriter in memory
            throw new Error();
        }
        return sw.toString();
    }

    /**
     * Returns a NodeList that contains the same nodes as this node list.
     * @return A clone of this list.
     * @throws CloneNotSupportedException if the contained list's class does
     * not have an accessible no-arg constructor.
     */
    public Object clone()
        throws CloneNotSupportedException
    {
        NodeList clonedList = (NodeList)super.clone();
        clonedList.cloneNodes();
        return clonedList;
    }

    private void cloneNodes()
        throws CloneNotSupportedException
    {
        Class listClass = nodes.getClass();
        try
        {
            List clonedNodes = (List)listClass.newInstance();
            clonedNodes.addAll(nodes);
            nodes = clonedNodes;
        }
        catch(IllegalAccessException e)
        {
            throw new CloneNotSupportedException("Cannot clone NodeList since"
            + " there is no accessible no-arg constructor on class "
            + listClass.getName());
        }
        catch(InstantiationException e)
        {
            // Cannot happen as listClass represents a concrete, non-primitive,
            // non-array, non-void class - there's an instance of it in "nodes"
            // which proves these assumptions.
            throw new Error();
        }
    }

    /**
     * Returns the hash code of the contained list.
     * @return The hashcode of the list.
     */
    public int hashCode()
    {
        return nodes.hashCode();
    }

    /**
     * Tests for equality with another object.
     * @param o the object to test for equality
     * @return true if the other object is also a NodeList and their contained
     * {@link List} objects evaluate as equals.
     */
    public boolean equals(Object o)
    {
        return o instanceof NodeList
            ? ((NodeList)o).nodes.equals(nodes)
            : false;
    }

    /**
     * Applies an XPath expression to the node list and returns the resulting
     * node list. In order for this method to work, your application must have
     * access to <a href="http://code.werken.com">werken.xpath</a> library
     * classes. The implementation does cache the parsed format of XPath
     * expressions in a weak hash map, keyed by the string representation of
     * the XPath expression. As the string object passed as the argument is
     * usually kept in the parsed template, this ensures that each XPath
     * expression is parsed only once during the lifetime of the template that
     * first invoked it.
     * @param xpathString the XPath expression you wish to apply
     * @return a NodeList representing the nodes that are the result of
     * application of the XPath to the current node list. It can be empty.
     */
    public NodeList selectNodes(String xpathString)
    {
        return new NodeList(XPathCache.getXPath(xpathString).applyTo(nodes), false);
    }

// List methods implemented hereafter

    /**
     * @see java.util.List#add(java.lang.Object)
     */
    public boolean add(Object o)
    {
        return nodes.add(o);
    }

    /**
     * @see java.util.List#add(int, java.lang.Object)
     */
    public void add(int index, Object o)
    {
        nodes.add(index, o);
    }

    /**
     * @see java.util.List#addAll(java.util.Collection)
     */
    public boolean addAll(Collection c)
    {
        return nodes.addAll(c);
    }

    /**
     * @see java.util.List#addAll(int, java.util.Collection)
     */
    public boolean addAll(int index, Collection c)
    {
        return nodes.addAll(index, c);
    }

    /**
     * @see java.util.List#clear()
     */
    public void clear()
    {
        nodes.clear();
    }

    /**
     * @see java.util.List#contains(java.lang.Object)
     */
    public boolean contains(Object o)
    {
        return nodes.contains(o);
    }

    /**
     * @see java.util.List#containsAll(java.util.Collection)
     */
    public boolean containsAll(Collection c)
    {
        return nodes.containsAll(c);
    }

    /**
     * @see java.util.List#get(int)
     */
    public Object get(int index)
    {
        return nodes.get(index);
    }

    /**
     * @see java.util.List#indexOf(java.lang.Object)
     */
    public int indexOf(Object o)
    {
        return nodes.indexOf(o);
    }

    /**
     * @see java.util.List#isEmpty()
     */
    public boolean isEmpty()
    {
        return nodes.isEmpty();
    }

    /**
     * @see java.util.List#iterator()
     */
    public Iterator iterator()
    {
        return nodes.iterator();
    }

    /**
     * @see java.util.List#lastIndexOf(java.lang.Object)
     */
    public int lastIndexOf(Object o)
    {
        return nodes.lastIndexOf(o);
    }

    /**
     * @see java.util.List#listIterator()
     */
    public ListIterator listIterator()
    {
        return nodes.listIterator();
    }

    /**
     * @see java.util.List#listIterator(int)
     */
    public ListIterator listIterator(int index)
    {
        return nodes.listIterator(index);
    }

    /**
     * @see java.util.List#remove(int)
     */
    public Object remove(int index)
    {
        return nodes.remove(index);
    }

    /**
     * @see java.util.List#remove(java.lang.Object)
     */
    public boolean remove(Object o)
    {
        return nodes.remove(o);
    }

    /**
     * @see java.util.List#removeAll(java.util.Collection)
     */
    public boolean removeAll(Collection c)
    {
        return nodes.removeAll(c);
    }

    /**
     * @see java.util.List#retainAll(java.util.Collection)
     */
    public boolean retainAll(Collection c)
    {
        return nodes.retainAll(c);
    }

    /**
     * @see java.util.List#set(int, java.lang.Object)
     */
    public Object set(int index, Object o)
    {
        return nodes.set(index, o);
    }

    /**
     * @see java.util.List#size()
     */
    public int size()
    {
        return nodes.size();
    }

    /**
     * @see java.util.List#subList(int, int)
     */
    public List subList(int fromIndex, int toIndex)
    {
        return new NodeList(nodes.subList(fromIndex, toIndex));
    }

    /**
     * @see java.util.List#toArray()
     */
    public Object[] toArray()
    {
        return nodes.toArray();
    }

    /**
     * @see java.util.List#toArray(java.lang.Object[])
     */
    public Object[] toArray(Object[] a)
    {
        return nodes.toArray(a);
    }

    /**
     * A special subclass of XMLOutputter that will be used to output
     * Attribute nodes. As a subclass of XMLOutputter it can use its protected
     * method escapeAttributeEntities() to serialize the attribute
     * appropriately.
     */
    private static final class AttributeXMLOutputter extends XMLOutputter
    {
        /**
         * @param attribute
         * @param out
         * @throws IOException
         */
        public void output(Attribute attribute, Writer out)
            throws IOException
        {
            out.write(" ");
            out.write(attribute.getQualifiedName());
            out.write("=");

            out.write("\"");
            out.write(escapeAttributeEntities(attribute.getValue()));
            out.write("\"");
        }
    }
}
