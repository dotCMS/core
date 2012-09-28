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

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.Log;
import org.apache.velocity.runtime.parser.Parser;
import org.apache.velocity.runtime.parser.Token;

import com.dotmarketing.util.VelocityUtil;

/**
 *
 */
public class SimpleNode implements Node, Serializable
{
    private static final long serialVersionUID = 2212796154857814566L;

    /** */
    protected Node parent;

    /** */
    protected Node[] children;

    /** */
    protected int id;

    /** */
    // TODO - It seems that this field is only valid when parsing, and should not be kept around.    
    protected transient Parser parser;

    /** */
    protected int info; // added

    /** */
    public boolean state;

    /** */
    protected boolean invalid = false;

    /** */
    protected Token first;

    /** */
    protected Token last;
    
    
    protected String templateName;

    
    /**
     * @param i
     */
    public SimpleNode(int i)
    {
        id = i;
    }

    /**
     * @param p
     * @param i
     */
    public SimpleNode(Parser p, int i)
    {
        this(i);
        parser = p;
        templateName = parser.currentTemplateName;
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.Node#jjtOpen()
     */
    public void jjtOpen()
    {
        first = parser.getToken(1); // added
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.Node#jjtClose()
     */
    public void jjtClose()
    {
        last = parser.getToken(0); // added
    }

    /**
     * @param t
     */
    public void setFirstToken(Token t)
    {
        this.first = t;
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.Node#getFirstToken()
     */
    public Token getFirstToken()
    {
        return first;
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.Node#getLastToken()
     */
    public Token getLastToken()
    {
        return last;
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.Node#jjtSetParent(org.apache.velocity.runtime.parser.node.Node)
     */
    public void jjtSetParent(Node n)
    {
        parent = n;
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.Node#jjtGetParent()
     */
    public Node jjtGetParent()
    {
        return parent;
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.Node#jjtAddChild(org.apache.velocity.runtime.parser.node.Node, int)
     */
    public void jjtAddChild(Node n, int i)
    {
        if (children == null)
        {
            children = new Node[i + 1];
        }
        else if (i >= children.length)
        {
            Node c[] = new Node[i + 1];
            System.arraycopy(children, 0, c, 0, children.length);
            children = c;
        }
        children[i] = n;
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.Node#jjtGetChild(int)
     */
    public Node jjtGetChild(int i)
    {
        return children[i];
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.Node#jjtGetNumChildren()
     */
    public int jjtGetNumChildren()
    {
        return (children == null) ? 0 : children.length;
    }


    /**
     * @see org.apache.velocity.runtime.parser.node.Node#jjtAccept(org.apache.velocity.runtime.parser.node.ParserVisitor, java.lang.Object)
     */
    public Object jjtAccept(ParserVisitor visitor, Object data)
    {
        return visitor.visit(this, data);
    }


    /**
     * @see org.apache.velocity.runtime.parser.node.Node#childrenAccept(org.apache.velocity.runtime.parser.node.ParserVisitor, java.lang.Object)
     */
    public Object childrenAccept(ParserVisitor visitor, Object data)
    {
        if (children != null)
        {
            for (int i = 0; i < children.length; ++i)
            {
                children[i].jjtAccept(visitor, data);
            }
        }
        return data;
    }

    /* You can override these two methods in subclasses of SimpleNode to
        customize the way the node appears when the tree is dumped.  If
        your output uses more than one line you should override
        toString(String), otherwise overriding toString() is probably all
        you need to do. */

    //    public String toString()
    // {
    //    return ParserTreeConstants.jjtNodeName[id];
    // }
    /**
     * @param prefix
     * @return String representation of this node.
     */
    public String toString(String prefix)
    {
        return prefix + toString();
    }

    /**
     * Override this method if you want to customize how the node dumps
     * out its children.
     *
     * @param prefix
     */
    public void dump(String prefix)
    {
        System.out.println(toString(prefix));
        if (children != null)
        {
            for (int i = 0; i < children.length; ++i)
            {
                SimpleNode n = (SimpleNode) children[i];
                if (n != null)
                {
                    n.dump(prefix + " ");
                }
            }
        }
    }

    /**
     * Return a string that tells the current location of this node.
     */
    protected String getLocation(InternalContextAdapter context)
    {
        return Log.formatFileString(this);
    }

    // All additional methods

    /**
     * @see org.apache.velocity.runtime.parser.node.Node#literal()
     */
    public String literal()
    {
        // if we have only one string, just return it and avoid
        // buffer allocation. VELOCITY-606
        if (first == last)
        {
            return NodeUtils.tokenLiteral(first);
        }

        Token t = first;
        StrBuilder sb = new StrBuilder(NodeUtils.tokenLiteral(t));
        while (t != last)
        {
            t = t.next;
            sb.append(NodeUtils.tokenLiteral(t));
        }
        return sb.toString();
    }

    /**
     * @throws TemplateInitException 
     * @see org.apache.velocity.runtime.parser.node.Node#init(org.apache.velocity.context.InternalContextAdapter, java.lang.Object)
     */
    public Object init( InternalContextAdapter context, Object data) throws TemplateInitException
    {
        /*
         * hold onto the RuntimeServices
         */

        int i, k = jjtGetNumChildren();

        for (i = 0; i < k; i++)
        {
            jjtGetChild(i).init( context, data);
        }

        return data;
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.Node#evaluate(org.apache.velocity.context.InternalContextAdapter)
     */
    public boolean evaluate( InternalContextAdapter  context)
        throws MethodInvocationException
    {
        return false;
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.Node#value(org.apache.velocity.context.InternalContextAdapter)
     */
    public Object value( InternalContextAdapter context)
        throws MethodInvocationException
    {
        return null;
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.Node#render(org.apache.velocity.context.InternalContextAdapter, java.io.Writer)
     */
    public boolean render( InternalContextAdapter context, Writer writer)
        throws IOException, MethodInvocationException, ParseErrorException, ResourceNotFoundException
    {
        int i, k = jjtGetNumChildren();

        for (i = 0; i < k; i++)
            jjtGetChild(i).render(context, writer);

        return true;
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.Node#execute(java.lang.Object, org.apache.velocity.context.InternalContextAdapter)
     */
    public Object execute(Object o, InternalContextAdapter context)
      throws MethodInvocationException
    {
        return null;
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.Node#getType()
     */
    public int getType()
    {
        return id;
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.Node#setInfo(int)
     */
    public void setInfo(int info)
    {
        this.info = info;
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.Node#getInfo()
     */
    public int getInfo()
    {
        return info;
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.Node#setInvalid()
     */
    public void setInvalid()
    {
        invalid = true;
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.Node#isInvalid()
     */
    public boolean isInvalid()
    {
        return invalid;
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.Node#getLine()
     */
    public int getLine()
    {
        return first.beginLine;
    }

    /**
     * @see org.apache.velocity.runtime.parser.node.Node#getColumn()
     */
    public int getColumn()
    {
        return first.beginColumn;
    }
    
    /**
     * @since 1.5
     */
    public String toString()
    {
        StrBuilder tokens = new StrBuilder();
        
        for (Token t = getFirstToken(); t != null; )
        {
            tokens.append("[").append(t.image).append("]");
            if (t.next != null)
            {
                if (t.equals(getLastToken()))
                {
                    break;
                }
                else
                {
                    tokens.append(", ");
                }
            }
            t = t.next;
        }

        return new ToStringBuilder(this)
            .append("id", getType())
            .append("info", getInfo())
            .append("invalid", isInvalid())
            .append("children", jjtGetNumChildren())
            .append("tokens", tokens)
            .toString();
    }

    public String getTemplateName()
    {
      return templateName;
    }
    
    private void readObject(java.io.ObjectInputStream ois) throws IOException, ClassNotFoundException{ 
        ois.defaultReadObject();
    }
}

