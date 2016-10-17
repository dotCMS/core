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
 * <p>Abstract view tool for doing "searching" and robust
 * pagination of search results.  The goal here is to provide a simple
 * and uniform API for "search tools" that can be used in velocity
 * templates (or even a standard Search.vm template).  In particular,
 * this class provides good support for result pagination and some
 * very simple result caching.
 * </p>
 * <p><b>Usage:</b><br>
 * To use this class, you must extend it and implement
 * the setup(HttpServletRequest) and executeQuery(Object)
 * methods.
 * <p>
 * The setup(HttpServletRequest) method ought to extract
 * from the current request the search criteria, the current
 * list index, and optionally, the number of items to display
 * per page of results.  Upon extracting these parameters, they
 * should be set using the provided setCriteria(Object),
 * setIndex(int), and setItemsPerPage(int) methods. A simple 
 * implementation would be:
 * <pre>
 * public void setup(HttpServletRequest req)
 * {
 *     ParameterParser pp = new ParameterParser(req);
 *     setCriteria(pp.getString("find"));
 *     setIndex(pp.getInt("index", 0));
 *     setItemsPerPage(pp.getInt("show", DEFAULT_ITEMS_PER_PAGE));
 * }
 * </pre>
 * <p>
 * The setCriteria(Object) method takes an Object in order to
 * allow the search criteria to meet your needs.  Your criteria
 * may be as simple as a single string, an array of strings, or
 * whatever you like.  The value passed into this method is that
 * which will ultimately be passed into executeQuery(Object) to
 * perform the search and return a list of results.  A simple
 * implementation might be like:
 * <pre>
 * protected List executeQuery(Object crit)
 * {
 *     return MyDbUtils.getFooBarsMatching((String)crit);
 * }
 * </pre>
 * <p>
 * Here's an example of how your subclass would be used in a template:
 * <pre>
 *   &lt;form name="search" method="get" action="$link.setRelative('search.vm')"&gt;
 *     &lt;input type="text"name="find" value="$!search.criteria"&gt;
 *     &lt;input type="submit" value="Find"&gt;
 *   &lt;/form&gt;
 *   #if( $search.hasItems() )
 *   Showing $!search.pageDescription&lt;br&gt;
 *     #set( $i = $search.index )
 *     #foreach( $item in $search.page )
 *       ${i}. $!item &lt;br&gt;
 *       #set( $i = $i + 1 )
 *     #end
 *     &lt;br&gt;
 *     #if ( $search.pagesAvailable &gt; 1 )
 *       #set( $pagelink = $link.setRelative('search.vm').addQueryData("find",$!search.criteria).addQueryData("show",$!search.itemsPerPage) )
 *       #if( $search.prevIndex )
 *           &lt;a href="$pagelink.addQueryData('index',$!search.prevIndex)"&gt;Prev&lt;/a&gt;
 *       #end
 *       #foreach( $index in $search.slip )
 *         #if( $index == $search.index )
 *           &lt;b&gt;$search.pageNumber&lt;/b&gt;
 *         #else
 *           &lt;a href="$pagelink.addQueryData('index',$!index)"&gt;$!search.getPageNumber($index)&lt;/a&gt;
 *         #end
 *       #end
 *       #if( $search.nextIndex )
 *           &lt;a href="$pagelink.addQueryData('index',$!search.nextIndex)"&gt;Next&lt;/a&gt;
 *       #end
 *     #end
 *   #elseif( $search.criteria )
 *   Sorry, no matches were found for "$!search.criteria".
 *   #else
 *   Please enter a search term
 *   #end
 * </pre>
 *
 * The output of this might look like:<br><br>
 *   <form method="get" action="">
 *    <input type="text" value="foo">
 *    <input type="submit" value="Find">
 *   </form>
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
 *   &lt;key&gt;search&lt;/key&gt;
 *   &lt;scope&gt;request&lt;/scope&gt;
 *   &lt;class&gt;com.foo.tools.MySearchTool&lt;/class&gt;
 * &lt;/tool&gt;
 * </pre>
 * </p>
 *
 * @author <a href="mailto:nathan@esha.com">Nathan Bubna</a>
 * @since VelocityTools 1.0
 * @version $Revision: 155305 $ $Date: 2005-02-24 20:40:59 -0800 (Thu, 24 Feb 2005) $
 */
public abstract class AbstractSearchTool extends AbstractPagerTool
{
    /** the key under which StoredResults are kept in session */
    protected static final String STORED_RESULTS_KEY = 
        StoredResults.class.getName();

    private Object criteria;


    /*  ---------------------- mutators -----------------------------  */

    /**
     * Sets the criteria and results to null, page index to zero, and
     * items per page to the default.
     */
    public void reset()
    {
        super.reset();
        criteria = null;
    }


    /**
     * Sets the criteria for this search.
     *
     * @param criteria - the criteria used for this search
     */
    public void setCriteria(Object criteria)
    {
        this.criteria = criteria;
    }


    /*  ---------------------- accessors -----------------------------  */

    /**
     * Return the criteria object for this request.
     * (for a simple search mechanism, this will typically be
     *  just a java.lang.String)
     *
     * @return criteria object
     */
    public Object getCriteria()
    {
        return criteria;
    }


    /**
     * @deprecated Use {@link AbstractPagerTool#hasItems()}
     */
    public boolean hasResults()
    {
        return hasItems();
    }


    /**
     * @deprecated Use {@link AbstractPagerTool#getItems()}.
     */
    public List getResults()
    {
        return getItems();
    }


    /**
     * Gets the results for the given criteria either in memory
     * or by performing a new query for them.  If the criteria
     * is null, an empty list will be returned.
     *
     * @return {@link List} of all items for the criteria
     */
    public List getItems()
    {
        /* return empty list if we have no criteria */
        if (criteria == null)
        {
            return Collections.EMPTY_LIST;
        }

        /* get the current list */
        List list = super.getItems();

        /* if empty, execute a query for the criteria */
        if (list.isEmpty())
        {
            /* perform a new query */
            list = executeQuery(criteria);

            /* because we can't trust executeQuery() not to return null
               and getItems() must _never_ return null... */
            if (list == null)
            {
                list = Collections.EMPTY_LIST;
            }

            /* save the new results */
            setItems(list);
        }
        return list;
    }


    /*  ---------------------- protected methods -----------------------------  */

    protected List getStoredItems()
    {
        StoredResults sr = getStoredResults();

        /* if the criteria equals that of the stored results, 
         * then return the stored result list */
        if (sr != null && criteria.equals(sr.getCriteria()))
        {
            return sr.getList();
        }
        return null;
    }


    protected void setStoredItems(List items)
    {
        setStoredResults(new StoredResults(criteria, items));
    }


    /**
     * Executes a query for the specified criteria.
     * 
     * <p>This method must be implemented! A simple
     * implementation might be something like:
     * <pre>
     * protected List executeQuery(Object crit)
     * {
     *     return MyDbUtils.getFooBarsMatching((String)crit);
     * }
     * </pre>
     * 
     * @return a {@link List} of results for this query
     */
    protected abstract List executeQuery(Object criteria);


    /**
     * Retrieves stored search results (if any) from the user's
     * session attributes.
     *
     * @return the {@link StoredResults} retrieved from memory
     */
    protected StoredResults getStoredResults()
    {
        if (session != null)
        {
            return (StoredResults)session.getAttribute(STORED_RESULTS_KEY);
        }
        return null;
    }


    /**
     * Stores current search results in the user's session attributes
     * (if one currently exists) in order to do efficient result pagination.
     *
     * <p>Override this to store search results somewhere besides the
     * HttpSession or to prevent storage of results across requests. In
     * the former situation, you must also override getStoredResults().</p>
     *
     * @param results the {@link StoredResults} to be stored
     */
    protected void setStoredResults(StoredResults results)
    {
        if (session != null)
        {
            session.setAttribute(STORED_RESULTS_KEY, results);
        }
    }


    /*  ---------------------- utility class -----------------------------  */

    /**
     * Simple utility class to hold a criterion and its result list.
     * <p>
     * This class is by default stored in a user's session,
     * so it implements Serializable, but its members are
     * transient. So functionally, it is not serialized and
     * the last results/criteria will not be persisted if
     * the session is serialized.
     * </p>
     */
    public class StoredResults implements java.io.Serializable
    {

        private transient Object crit;
        private transient List list;

        /**
         * Creates a new instance.
         *
         * @param crit the criteria for these results
         * @param list the {@link List} of results to store
         */
        public StoredResults(Object crit, List list)
        {
            this.crit = crit;
            this.list = list;
        }

        /**
         * @return the stored criteria object
         */
        public Object getCriteria()
        {
            return crit;
        }

        /**
         * @return the stored {@link List} of results
         */
        public List getList()
        {
            return list;
        }

    }


}
