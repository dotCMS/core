package com.dotmarketing.sitesearch.viewtool;

import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchResults;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.elasticsearch.search.facet.datehistogram.InternalCountDateHistogramFacet;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.elasticsearch.search.facet.terms.strings.InternalStringTermsFacet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

public class SiteSearchWebAPI implements ViewTool {

	
	private static HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();
	private static UserAPI userAPI = APILocator.getUserAPI();
	private static SiteSearchAPI siteSearchAPI = APILocator.getSiteSearchAPI();
	private HttpServletRequest request;
	private HttpServletResponse response;

	public void init(Object initData) {
		ViewContext context = (ViewContext) initData;
		this.request = context.getRequest();
		this.response = context.getResponse();
	}
	
	/**
	 * Performs a search on the default site search index using the current host in the request
	 * Sample usage from velocity:
	 * <pre>
     * {@code
	 * #set($searchresults = $sitesearch.search("dotcms",0,10))
     * #set($hitsdetail = $searchresults.getDetails())
     * #set($summaries = $searchresults.getSummaries())
     * #foreach ($i in [0..$math.sub($searchresults.getEnd(),1)])
     *    $hitsdetail.get($i).getValue("title")
     *    $hitsdetail.get($i).getValue("url")
     *    $summaries.get($i).toHtml(true)
     * #end
     * }
     * </pre>
	 * @param query String to search for
	 * @param start Start row
	 * @param rows  Number of rows to return (10 by default)
	 * @return DotSearchResults
	 * @throws IOException
	 */

	public SiteSearchResults search(String query, int start, int rows) throws IOException {
		return search(null, query, start, rows);
	}
	
	/**
	 * Performs a search on the site search index using the current host in the request
     * Sample usage from velocity:
     * <pre>
     * {@code
     * #set($searchresults = $sitesearch.search("indexAlias","dotcms",0,10))
     * #set($hitsdetail = $searchresults.getDetails())
     * #set($summaries = $searchresults.getSummaries())
     * #foreach ($i in [0..$math.sub($searchresults.getEnd(),1)])
     *    $hitsdetail.get($i).getValue("title")
     *    $hitsdetail.get($i).getValue("url")
     *    $summaries.get($i).toHtml(true)
     * #end
     * }
     * </pre>
	 * @param indexAlias
	 * @param query
	 * @param start
	 * @param rows
	 * @return
	 */
	public SiteSearchResults search(String indexAlias, String query, int start, int rows) {
	    SiteSearchResults results= new SiteSearchResults();
        if(query ==null){
            results.setError("No query passed in");
            return results;
            
        }
        Host host = null;        
        
        if(!StringUtils.isJson(query)){
            try {
                host = hostWebAPI.getCurrentHost(request);
            } catch (Exception e) {
                Logger.error(this, e.getMessage(), e);
                try {
                    Logger.warn(this, "Error getting host from request, trying default host");
                    host = hostWebAPI.findDefaultHost(userAPI.getSystemUser(), false);
                } catch (Exception e1) {
                    Logger.error(this, e1.getMessage(), e1);
                    results.setError("no host:" + e.getMessage());
                    return results;
                }
            
            }
        }
        
        String indexName=null;
        if(indexAlias!=null) {
            ESIndexAPI iapi=new ESIndexAPI();
            indexName=iapi.getAliasToIndexMap(siteSearchAPI.listIndices()).get(indexAlias);
            if(indexName==null) {
                results.setError("Index Alias not found: "+indexAlias);
                return results;
    	    }
        }
        
        return siteSearchAPI.search(indexName, query, start, rows);
	}

    /**
	 * This method will return a list of the site search index names
	 * @return
	 */
	public List<String> listSearchIndicies(){
	    return siteSearchAPI.listIndices();
	}
	
	/**
	 * typo but still here for compatibility
	 * @return
	 */
	public List<String> listSearchIncidies(){
		return listSearchIndicies();
	}

    /**
     * Returns the Facets for a given query
     *
     * @param indexName Name of the index where the search will be made, if null the default index will be use if exist
     * @param query     Query to apply
     * @return
     * @throws DotDataException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     * @see <a href="http://www.elasticsearch.org/guide/reference/api/search/facets/">http://www.elasticsearch.org/guide/reference/api/search/facets/</a>
     */
    public Map<String, Facet> getFacets ( String indexName, String query ) throws DotDataException, IllegalAccessException, NoSuchFieldException {

        //The map we will finally send
        Map<String, Facet> internalFacets = new HashMap<String, Facet>();

        //Search with the given query
        Map<String, Facet> facets = siteSearchAPI.getFacets( indexName, query );
        for ( String key : facets.keySet() ) {

            Facet internalFacet = facets.get( key );

            //Verify the type of the facet in order to be able to create a proper wrapper for it
            if ( internalFacet instanceof InternalStringTermsFacet ) {

                InternalStringTermsFacet facet = (InternalStringTermsFacet) internalFacet;

                //Getting private fields values required for the proper build of the wrapper
                Integer requiredSize = (Integer) getFieldValue( InternalStringTermsFacet.class, "requiredSize", facet );
                TermsFacet.ComparatorType comparatorType = (TermsFacet.ComparatorType) getFieldValue( InternalStringTermsFacet.class, "comparatorType", facet );
                //New Instance of the wrapper for the String Facet
                internalFacet = new InternalWrapperStringTermsFacet( facet.getName(), comparatorType, requiredSize, facet.getEntries(), facet.getMissingCount(), facet.getTotalCount() );
            } else if ( internalFacet instanceof InternalCountDateHistogramFacet ) {

                InternalCountDateHistogramFacet facet = (InternalCountDateHistogramFacet) internalFacet;

                //Getting private fields values required for the proper build of the wrapper
                DateHistogramFacet.ComparatorType comparatorType = (DateHistogramFacet.ComparatorType) getFieldValue( InternalCountDateHistogramFacet.class, "comparatorType", facet );
                //New Instance of the wrapper for the Date Facet
                internalFacet = new InternalWrapperCountDateHistogramFacet( facet.getName(), comparatorType, facet.getEntries() );
            }

            internalFacets.put( key, internalFacet );
        }

        return internalFacets;
    }

    /**
     * Utility method to get the value of a private field using reflection
     *
     * @param clazz
     * @param fieldName
     * @param getFrom
     * @return
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private Object getFieldValue ( Class clazz, String fieldName, Object getFrom ) throws NoSuchFieldException, IllegalAccessException {

        Field field = clazz.getDeclaredField( fieldName );
        field.setAccessible( true );
        Object value = field.get( getFrom );
        field.setAccessible( false );

        return value;
    }

    /**
     * Internal wrapper class for backwards compatibility with the new Elastic Search in
     * Site Search.
     */
    public class InternalWrapperCountDateHistogramFacet extends InternalCountDateHistogramFacet {

        public InternalWrapperCountDateHistogramFacet ( String name, ComparatorType comparatorType, List<InternalCountDateHistogramFacet.CountEntry> entries ) {
            super( name, comparatorType, entries.toArray( new InternalCountDateHistogramFacet.CountEntry[entries.size()] ) );
        }

        public java.util.List<CountEntry> entries () {
            return getEntries();
        }
    }

    /**
     * Internal wrapper class for backwards compatibility with the new Elastic Search in
     * Site Search.
     */
    public class InternalWrapperStringTermsFacet extends InternalStringTermsFacet {

        private List<InternalTermEntry> entries;

        public InternalWrapperStringTermsFacet ( String name, ComparatorType comparatorType, int requiredSize, Collection<TermEntry> entries, long missing, long total ) {

            super( name, comparatorType, requiredSize, entries, missing, total );

            this.entries = new ArrayList<InternalTermEntry>();
            for ( TermEntry entry : getEntries() ) {
                this.entries.add( new InternalTermEntry( entry.getTerm().toString(), entry.getCount() ) );
            }
        }

        public java.util.List<InternalTermEntry> entries () {
            return entries;
        }

        public class InternalTermEntry {

            public String term;
            public int count;

            public InternalTermEntry ( String term, int count ) {
                this.term = term;
                this.count = count;
            }

            public String getTerm () {
                return term;
            }

            public int getCount () {
                return count;
            }
        }
    }

}