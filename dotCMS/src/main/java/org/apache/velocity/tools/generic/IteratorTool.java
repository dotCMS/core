/*
 * Copyright 2003 The Apache Software Foundation.
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

package org.apache.velocity.tools.generic;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.apache.velocity.util.ArrayIterator;
import org.apache.velocity.util.EnumerationIterator;

/**
 * <p>
 * A convenience tool to use with #foreach loops. It wraps a list
 * to let the designer specify a condition to terminate the loop, 
 * and reuse the same list in different loops.
 * </p>
 * <p>
 * Example of use:
 * <pre>  
 *  Java
 *  ----
 *  context.put("mill", new IteratorTool());
 *   
 *  
 *  VTL
 *  ---
 *  
 *  #set ($list = [1, 2, 3, 5, 8, 13])
 *  #set ($numbers = $mill.wrap($list))
 *  
 *  #foreach ($item in $numbers)
 *  #if ($item < 8) $numbers.more()#end
 *  #end
 *  
 *  $numbers.more()
 *  
 *  
 *  Output
 *  ------
 *  
 *   1 2 3 5
 *  8
 *
 * Example toolbox.xml config (if you want to use this with VelocityView):
 * &lt;tool&gt;
 *   &lt;key&gt;mill&lt;/key&gt;
 *   &lt;scope&gt;request&lt;/scope&gt;
 *   &lt;class&gt;org.apache.velocity.tools.generic.IteratorTool&lt;/class&gt;
 * &lt;/tool&gt;
 * </pre>
 * </p>
 * <p>
 * <b>Warning:</b> It is not recommended to use hasNext() with this
 * tool as it is used to control the #foreach. Use hasMore() instead.
 * </p>
 *
 * @author <a href="mailto:jido@respublica.fr">Denis Bredelet</a>
 * @version $Id: IteratorTool.java 233420 2005-08-19 03:50:14Z nbubna $
 */

public class IteratorTool implements Iterator {


    private Object wrapped;
    private Iterator iterator;
    private boolean wantMore;
    private boolean cachedNext;
    protected Object next;


    /**
     * Create a IteratorTool instance to use as tool.
     * When it is created this way, the tool returns a new
     * instance each time wrap() is called. This is
     * useful when you want to allow the designers to create instances.
     */
    public IteratorTool()
    {
        this(null);
    }


    /**
     * Create a IteratorTool instance to use in #foreach.
     *
     * @param wrapped The list to wrap. 
     */
    public IteratorTool(Object wrapped)
    {
        internalWrap(wrapped);
    }


    /**
     * Wraps a list with the tool.
     * <br>The list can be an array, a Collection, a Map, an Iterator
     * or an Enumeration.
     * <br>If the list is a Map, the tool iterates over the values.
     * <br>If the list is an Iterator or an Enumeration, the tool can
     * be used only once.
     *
     * @param list The list to wrap.
     * @return A new wrapper if this object is used as a tool, or
     *         itself if it is a wrapper.
     */
    public IteratorTool wrap(Object list)
    {
        if (this.wrapped == null)
        {
            return new IteratorTool(list);
        }
        else if (list != null)
        {
            internalWrap(list);
            return this;
        }
        else
        {
            throw new IllegalArgumentException("Need a valid list to wrap");
        }
    }


    /**
     * Wraps a list with the tool. This object can therefore
     * be used instead of the list itself in a #foreach.
     * The list can be an array, a Collection, a Map, an
     * Iterator or an Enumeration.
     * <br>- If the list is a Map, the tool iterates over the values.
     * <br>- If the list is an Iterator or an Enumeration, the tool
     * can be used only once.
     * 
     * @param wrapped The list to wrap.
     */
    private void internalWrap(Object wrapped)
    {
        if (wrapped != null)
        {
            /* rip-off from org/apache/velocity/runtime/directive/ForEach.java */
            if (wrapped.getClass().isArray())
            {
                this.iterator = new ArrayIterator((Object[])wrapped);
            }
            else if (wrapped instanceof Collection)
            {
                this.iterator = ((Collection)wrapped).iterator();
            }
            else if (wrapped instanceof Map)
            {
                this.iterator = ((Map)wrapped).values().iterator();
            }
            else if (wrapped instanceof Iterator)
            {
                this.iterator = (Iterator)wrapped;
            }
            else if (wrapped instanceof Enumeration)
            {
                this.iterator = new EnumerationIterator((Enumeration)wrapped);
            }
            else
            {
                /* Don't know what is the object.
                 * Should we put it in a one-item array? */
                throw new IllegalArgumentException("Don't know how to wrap this list");
            }

            this.wrapped = wrapped;
            this.wantMore = true;
            this.cachedNext = false;
        }
        else
        {
            this.iterator = null;
            this.wrapped = null;
            this.wantMore = false;
            this.cachedNext = false;
        }
    }


    /**
     * <p>
     * Resets the wrapper so that it starts over at the beginning of the list.
     * </p>
     * <p>
     * <b>Note to programmers:</b> This method has no effect if the wrapped
     * object is an enumeration or an iterator.
     */
    public void reset()
    {
        if (this.wrapped != null)
        {
            internalWrap(this.wrapped);
        }
    }


    /**
     * <p>
     * Gets the next object in the list. This method is called
     * by #foreach to define $item in:
     * <pre>
     * #foreach( $item in $list )
     * </pre>
     * </p>
     * <p>
     * This method is not intended for template designers, but they can use
     * them if they want to read the value of the next item without doing
     * more().
     * </p>
     *
     * @return The next item in the list.
     * @throws NoSuchElementException if there are no more 
     *         elements in the list.
     */
    public Object next()
    {
        if (this.wrapped == null)
        {
            throw new IllegalStateException("Use wrap() before calling next()");
        }
        
        if (!this.cachedNext)
        {
            this.cachedNext = true;
            this.next = this.iterator.next();
            return this.next;
        }
        else
        {
            return this.next;
        }
    }

    /**
     * Returns true if there are more elements in the
     * list and more() was called.
     * <br>This code always return false:
     * <pre>
     * tool.hasNext()? tool.hasNext(): false;
     * </pre>
     * 
     * @return true if there are more elements, and either more()
     *         or hasNext() was called since last call.
     */
    public boolean hasNext()
    {
        if (this.wantMore)
        {
            /* don't want more unless more is called */
            this.wantMore = false;
            return hasMore();
        }
        else
        {
            /* prepare for next #foreach */
            this.wantMore = true;
            return false;
        }
    }

    /**
     * Removes the current element from the list.
     * The current element is defined as the last element that was read
     * from the list, either with next() or with more().
     *
     * @throws UnsupportedOperationException if the wrapped list
     *  iterator doesn't support this operation.
     */
    public void remove() throws UnsupportedOperationException
    {
        if (this.wrapped == null)
        {
            throw new IllegalStateException("Use wrap() before calling remove()");
        }

        /* Let the iterator decide whether to implement this or not */
        this.iterator.remove();
    }


    /**
     * <p>
     * Asks for the next element in the list. This method is to be used
     * by the template designer in #foreach loops.
     * </p>
     * <p>
     * If this method is called in the body of #foreach, the loop
     * continues as long as there are elements in the list.
     * <br>If this method is not called the loop terminates after the
     * current iteration.
     * </p>
     *
     * @return The next element in the list, or null if there are no
     *         more elements.
     */
    public Object more()
    {
        this.wantMore = true;
        if (hasMore())
        {
            Object next = next();
            this.cachedNext = false;
            return next;
        }
        else
        {
            return null;
        }
    }

    /**
     * Returns true if there are more elements in the wrapped list.
     * <br>If this object doesn't wrap a list, the method always returns false.
     *
     * @return true if there are more elements in the list.
     */
    public boolean hasMore()
    {
        if (this.wrapped == null)
        {
            return false;
        }
        return cachedNext || this.iterator.hasNext();
    }

    
    /**
     * Puts a condition to break out of the loop.
     * The #foreach loop will terminate after this iteration, unless more()
     * is called after stop().
     */
    public void stop()
    {
        this.wantMore = false;
    }


    /**
     * Returns this object as a String.
     * <br>If this object is used as a tool, it just gives the class name.
     * <br>Otherwise it appends the wrapped list to the class name.
     *
     * @return A string representation of this object.
     */
    public String toString()
    {
        StringBuffer out = new StringBuffer(this.getClass().getName());
        if (this.wrapped != null)
        {
            out.append('(');
            out.append(this.wrapped);
            out.append(')');
        }
        return out.toString();
    }


}
