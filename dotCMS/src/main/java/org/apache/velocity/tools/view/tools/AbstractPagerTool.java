/*
 * Copyright 2003-2004 The Apache Software Foundation.
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

package org.apache.velocity.tools.view.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

/**
 * <p>Abstract view tool for doing request-based pagination of 
 * items in an a list.
 * </p>
 * <p><b>Usage:</b><br>
 * To use this class, you must extend it and implement
 * the setup(HttpServletRequest) method.
 * <p>
 * The setup(HttpServletRequest) method ought to extract
 * from the current request the current list index and,
 * optionally, the number of items to display per page.
 * Upon extracting these parameters, they should be set using
 * the provided setIndex(int) and setItemsPerPage(int) methods.
 * A simple implementation would be:
 * <pre>
 * public void setup(HttpServletRequest req)
 * {
 *     ParameterParser pp = new ParameterParser(req);
 *     setIndex(pp.getInt("index", 0));
 *     setItemsPerPage(pp.getInt("show", DEFAULT_ITEMS_PER_PAGE));
 * }
 * </pre>
 * You can also set the list of items to be paged at this point
 * using the setItems(List) method, or you can always set the
 * item list at another point (even from within the template).
 * </p>
 * <p>
 * Here's an example of how your subclass would be used in a template:
 * <pre>
 *   #if( $pager.hasItems() )
 *   Showing $!pager.pageDescription&lt;br&gt;
 *     #set( $i = $pager.index )
 *     #foreach( $item in $pager.page )
 *       ${i}. $!item &lt;br&gt;
 *       #set( $i = $i + 1 )
 *     #end
 *     &lt;br&gt;
 *     #if ( $pager.pagesAvailable &gt; 1 )
 *       #set( $pagelink = $link.setRelative('pager.vm').addQueryData("find",$!pager.criteria).addQueryData("show",$!pager.itemsPerPage) )
 *       #if( $pager.prevIndex )
 *           &lt;a href="$pagelink.addQueryData('index',$!pager.prevIndex)"&gt;Prev&lt;/a&gt;
 *       #end
 *       #foreach( $index in $pager.slip )
 *         #if( $index == $pager.index )
 *           &lt;b&gt;$pager.pageNumber&lt;/b&gt;
 *         #else
 *           &lt;a href="$pagelink.addQueryData('index',$!index)"&gt;$!pager.getPageNumber($index)&lt;/a&gt;
 *         #end
 *       #end
 *       #if( $pager.nextIndex )
 *           &lt;a href="$pagelink.addQueryData('index',$!pager.nextIndex)"&gt;Next&lt;/a&gt;
 *       #end
 *     #end
 *   #else
 *   No items in list.
 *   #end
 * </pre>
 *
 * The output of this might look like:<br><br>
 *   Showing 1-5 of 8<br>
 *   1. foo<br>
 *   2. bar<br>
 *   3. blah<br>
 *   4. woogie<br>
 *   5. baz<br><br>
 *   <b>1</b> <a href="">2</a> <a href="">Next</a>
 * </p>
 * <p>
 * <b>Example toolbox.xml configuration:</b>
 * <pre>&lt;tool&gt;
 *   &lt;key&gt;pager&lt;/key&gt;
 *   &lt;scope&gt;request&lt;/scope&gt;
 *   &lt;class&gt;com.foo.tools.MyPagerTool&lt;/class&gt;
 * &lt;/tool&gt;
 * </pre>
 * </p>
 *
 * @author <a href="mailto:nathan@esha.com">Nathan Bubna</a>
 * @since VelocityTools 1.2
 * @version $Revision: 72099 $ $Date: 2004-11-10 19:45:19 -0800 (Wed, 10 Nov 2004) $
 */
public abstract class AbstractPagerTool implements ViewTool
{

    /** the default number of items shown per page */
    public static final int DEFAULT_ITEMS_PER_PAGE = 10;

    /** the default max number of page indices to list */
    public static final int DEFAULT_SLIP_SIZE = 20;

    /** the key under which items are stored in session */
    protected static final String STORED_ITEMS_KEY = 
        AbstractPagerTool.class.getName();
    
    private List items;
    private int index = 0;
    private int slipSize = DEFAULT_SLIP_SIZE;
    private int itemsPerPage = DEFAULT_ITEMS_PER_PAGE;    
    protected HttpSession session;
    
    /**
     * Initializes this instance by grabbing the request
     * and session objects from the current ViewContext.
     *
     * @param obj the current ViewContext
     * @throws ClassCastException if the param is not a ViewContext
     */
    public void init(Object obj)
    {
        ViewContext context = (ViewContext)obj;
        HttpServletRequest request = context.getRequest();
        session = request.getSession(false);
        setup(request);        
    }
    
    /**
     * Abstract method to make it as obvious as possible just
     * where implementing classes should be retrieving and configuring
     * display parameters. 
     * <p>A simple implementation would be:
     * <pre>
     * public void setup(HttpServletRequest req)
     * {
     *     ParameterParser pp = new ParameterParser(req);
     *     setIndex(pp.getInt("index", 0));
     *     setItemsPerPage(pp.getInt("show", DEFAULT_ITEMS_PER_PAGE));
     * }
     * </pre>
     *
     * @param request the current HttpServletRequest
     */
    public abstract void setup(HttpServletRequest request);
    
    /*  ---------------------- mutators ----------------------------- */


    /**
     * Sets the item list to null, page index to zero, and
     * items per page to the default.
     */
    public void reset()
    {
        items = null;
        index = 0;
        itemsPerPage = DEFAULT_ITEMS_PER_PAGE;
    }
    
    /**
     * Sets the List to page through.
     *
     * @param items - the  {@link List} of items to be paged through
     */
    public void setItems(List items)
    {
        this.items = items;
        setStoredItems(items);
    }    
    
    /**
     * Sets the index of the first result in the current page
     *
     * @param index the result index to start the current page with
     */
    public void setIndex(int index)
    {
        if (index < 0)
        {
            /* quietly override to a reasonable value */
            index = 0;
        }
        this.index = index;
    }    
    
    /**
     * Sets the number of items returned in a page of items
     *
     * @param itemsPerPage the number of items to be returned per page
     */
    public void setItemsPerPage(int itemsPerPage)
    {
        if (itemsPerPage < 1)
        {
            /* quietly override to a reasonable value */
            itemsPerPage = DEFAULT_ITEMS_PER_PAGE;
        }
        this.itemsPerPage = itemsPerPage;
    }
    
    /**
     * Sets the number of result page indices for {@link #getSlip} to list.
     * (for google-ish result page links).
     *
     * @see #getSlip
     * @param slipSize - the number of result page indices to list
     */
    public void setSlipSize(int slipSize)
    {
        if (slipSize < 2)
        {
            /* quietly override to a reasonable value */
            slipSize = DEFAULT_SLIP_SIZE;
        }
        this.slipSize = slipSize;
    }
    
    /*  ---------------------- accessors ----------------------------- */

    /**
     * Returns the set number of items to be displayed per page of items
     *
     * @return current number of items shown per page
     */
    public int getItemsPerPage()
    {
        return itemsPerPage;
    }

    /**
     * Returns the number of result page indices {@link #getSlip} 
     * will return per request (if available).
     *
     * @return the number of result page indices {@link #getSlip} 
     *         will try to return
     */
    public int getSlipSize()
    {
        return slipSize;
    }


    /**
     * Returns the current search result index.
     *
     * @return the index for the beginning of the current page
     */
    public int getIndex()
    {
        return index;
    }


    /**
     * Checks whether or not the result list is empty.
     *
     * @return <code>true</code> if the result list is not empty.
     */
    public boolean hasItems()
    {
        return !getItems().isEmpty();
    }

    /**
     * Returns the item list. This is guaranteed
     * to never return <code>null</code>.
     *
     * @return {@link List} of all the items
     */
    public List getItems()
    {
        if (items == null)
        {
            items = getStoredItems();
        }
        
        return (items != null) ? items : Collections.EMPTY_LIST;
    }    
    
    /**
     * Returns the index for the next page of items
     * (as determined by the current index, items per page, and 
     * the number of items).  If no "next page" exists, then null is
     * returned.
     *
     * @return index for the next page or <code>null</code> if none exists
     */
    public Integer getNextIndex()
    {
        int next = index + itemsPerPage;
        if (next < getItems().size())
        {
            return new Integer(next);
        }
        return null;
    }
    

    /**
     * Return the index for the previous page of items
     * (as determined by the current index, items per page, and 
     * the number of items).  If no "next page" exists, then null is
     * returned.
     *
     * @return index for the previous page or <code>null</code> if none exists
     */
    public Integer getPrevIndex()
    {
        int prev = Math.min(index, getItems().size()) - itemsPerPage;
        if (index > 0)
        {
            return new Integer(Math.max(0, prev));
        }
        return null;
    }
    
    /**
     * Returns the number of pages that can be made from this list
     * given the set number of items per page.
     */
    public int getPagesAvailable()
    {
        return (int)Math.ceil(getItems().size() / (double)itemsPerPage);
    }


    /**
     * Returns the current "page" of search items.
     *
     * @return a {@link List} of items for the "current page"
     */
    public List getPage()
    {
        /* return null if we have no items */
        if (!hasItems())
        {
            return null;
        }
        /* quietly keep the page indices to legal values for robustness' sake */
        int start = Math.min(getItems().size() - 1, index);
        int end = Math.min(getItems().size(), index + itemsPerPage);
        return getItems().subList(start, end);
    }
    
    /**
     * Returns the "page number" for the specified index.  Because the page
     * number is used for the user interface, the page numbers are 1-based.
     *
     * @param i the index that you want the page number for
     * @return the approximate "page number" for the specified index or 
     *         <code>null</code> if there are no items
     */
    public Integer getPageNumber(int i)
    {
        if (!hasItems())
        {
            return null;
        }
        return new Integer(1 + i / itemsPerPage);
    }


    /**
     * Returns the "page number" for the current index.  Because the page
     * number is used for the user interface, the page numbers are 1-based.
     *
     * @return the approximate "page number" for the current index or 
     *         <code>null</code> if there are no items
     */
    public Integer getPageNumber()
    {
        return getPageNumber(index);
    }

    /**
     * <p>Returns a description of the current page.  This implementation
     * displays a 1-based range of result indices and the total number 
     * of items.  (e.g. "1 - 10 of 42" or "7 of 7")</p>
     *
     * <p>Sub-classes may override this to provide a customized 
     * description (such as one in another language).</p>
     *
     * @return a description of the current page
     */
    public String getPageDescription()
    {
        StringBuffer out = new StringBuffer();
        int first = index + 1;
        int total = getItems().size();
        if (first >= total)
        {
            out.append(total);
            out.append(" of ");
            out.append(total);
        }
        else
        {
            int last = Math.min(index + itemsPerPage, total);
            out.append(first);
            out.append(" - ");
            out.append(last);
            out.append(" of ");
            out.append(total);
        }
        return out.toString();
    }
    
    /**
     * Returns a <b>S</b>liding <b>L</b>ist of <b>I</b>ndices for <b>P</b>ages
     * of items.
     *
     * <p>Essentially, this returns a list of item indices that correspond
     * to available pages of items (as based on the set items-per-page). 
     * This makes it relativly easy to do a google-ish set of links to 
     * available pages.</p>
     *
     * <p>Note that this list of Integers is 0-based to correspond with the
     * underlying result indices and not the displayed page numbers (see
     * {@link #getPageNumber}).</p>
     *
     * @return {@link List} of Integers representing the indices of result 
     *         pages or empty list if there's one or less pages available
     */
    public List getSlip()
    {
        /* return an empty list if there's no pages to list */
        int totalPgs = getPagesAvailable();
        if (totalPgs <= 1)
        {
            return Collections.EMPTY_LIST;
        }

        /* page number is 1-based so decrement it */
        int curPg = getPageNumber().intValue() - 1;

        /* start at zero or just under half of max slip size 
         * this keeps "forward" and "back" pages about even
         * but gives preference to "forward" pages */
        int slipStart = Math.max(0, (curPg - (slipSize / 2)));

        /* push slip end as far as possible */
        int slipEnd = Math.min(totalPgs, (slipStart + slipSize));

        /* if we're out of "forward" pages, then push the 
         * slip start toward zero to maintain slip size */
        if (slipEnd - slipStart < slipSize)
        {
            slipStart = Math.max(0, slipEnd - slipSize);
        }

        /* convert 0-based page numbers to indices and create list */
        List slip = new ArrayList(slipEnd - slipStart);
        for (int i=slipStart; i < slipEnd; i++)
        {
            slip.add(new Integer(i * itemsPerPage));
        }
        return slip;
    }

    /*  ---------------------- protected methods ------------------------  */

    /**
     * Retrieves stored search items (if any) from the user's
     * session attributes.
     *
     * @return the {@link List} retrieved from memory
     */
    protected List getStoredItems()
    {
        if (session != null)
        {
            return (List)session.getAttribute(STORED_ITEMS_KEY);
        }
        return null;
    }


    /**
     * Stores current search items in the user's session attributes
     * (if one currently exists) in order to do efficient result pagination.
     *
     * <p>Override this to store search items somewhere besides the
     * HttpSession or to prevent storage of items across requests. In
     * the former situation, you must also override getStoredItems().</p>
     *
     * @param items the {@link List} to be stored
     */
    protected void setStoredItems(List items)
    {
        if (session != null)
        {
            session.setAttribute(STORED_ITEMS_KEY, items);
        }
    }

}
