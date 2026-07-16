package com.dotmarketing.sitesearch.viewtool;

import com.dotcms.content.index.IndexAPI;
import com.dotcms.content.index.domain.Aggregation;
import com.dotcms.content.index.domain.AggregationBucket;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
            IndexAPI iapi=APILocator.getESIndexAPI();
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
     * Returns the aggregations for a given query
     *
     * @param indexName Name of the index where the search will be made, if null the default index
     * will be use if exist
     * @param query Query to apply
     */
    public Map<String, Aggregation> getAggregations(final String indexName, final String query)
            throws DotDataException {
        return siteSearchAPI.getAggregations(indexName, query);
    }

    /**
     * Returns the Facets for a given query
     *
     * @param indexName Name of the index where the search will be made, if null the default index
     * will be use if exist
     * @param query Query to apply
     * @deprecated use {@link #getAggregations(String, String)} instead
     */
    public Map<String, Facet> getFacets(final String indexName, final String query)
            throws DotDataException, IllegalAccessException, NoSuchFieldException {

        Facet internalFacet;
        //The map we will finally send
        final Map<String, Facet> internalFacets = new HashMap<>();

        //Search with the given query
        final Map<String, Aggregation> aggregations = this.getAggregations(indexName, query);
        for (String key : aggregations.keySet()) {

            final Aggregation aggregation = aggregations.get(key);
            final String type = aggregation.getType();

            if (isHistogram(type)) {
                internalFacet = new InternalWrapperCountDateHistogramFacet(aggregation.getName(),
                        type, aggregation.getBuckets());
            } else if (!aggregation.getBuckets().isEmpty()) {
                internalFacet = new InternalWrapperStringTermsFacet(aggregation.getName(),
                        type, aggregation.getBuckets());
            } else {
                internalFacet = new Facet(aggregation.getName(), type);
            }
            internalFacets.put(key, internalFacet);
        }

        return internalFacets;
    }

    /**
     * A histogram aggregation (date or numeric) reports a vendor type containing
     * {@code "histogram"} (e.g. {@code date_histogram}); its buckets carry numeric keys.
     */
    private static boolean isHistogram(final String type) {
        return type != null && type.contains("histogram");
    }

    /**
     * Internal wrapper class for backwards compatibility with the new Elastic Search in Site
     * Search.
     *
     * @deprecated use the vendor-neutral {@link #getAggregations(String, String)} instead
     */
    public class InternalWrapperCountDateHistogramFacet extends Facet {

        private final List<CountEntry> entries;

        public InternalWrapperCountDateHistogramFacet(final String name, final String type,
                List<AggregationBucket> entries) {
            super(name, type);
            this.entries = new ArrayList<>();
            for (final AggregationBucket entry : entries) {
                final Number key = entry.getKeyAsNumber();
                final long time = key != null ? key.longValue() : 0L;
                this.entries.add(new CountEntry(time, entry.getDocCount()));
            }
        }

        public List<CountEntry> entries() {
            return entries;
        }

        public class CountEntry {

            private final long time;
            private final long count;

            public CountEntry(final long time, final long count) {
                this.time = time;
                this.count = count;
            }

            public long getTime() {
                return time;
            }

            public long getCount() {
                return count;
            }
        }
    }

    /**
     * Internal wrapper class for backwards compatibility with the new Elastic Search in Site
     * Search.
     *
     * @deprecated use the vendor-neutral {@link #getAggregations(String, String)} instead
     */
    public class InternalWrapperStringTermsFacet extends Facet {

        private List<InternalTermEntry> entries;

        public InternalWrapperStringTermsFacet(final String name, final String type, final List<AggregationBucket> entries) {

            super(name, type);

            this.entries = new ArrayList<>();
            for (final AggregationBucket entry : entries) {
                this.entries
                        .add(new InternalTermEntry(entry.getKey(), entry.getDocCount()));
            }
        }

        public java.util.List<InternalTermEntry> entries() {
            return entries;
        }

        public class InternalTermEntry {

            private final String term;
            private final long count;

            public InternalTermEntry(final String term, final long count) {
                this.term = term;
                this.count = count;
            }

            public String getTerm() {
                return term;
            }

            public long getCount() {
                return count;
            }
        }
    }

    /**
     * @deprecated use the vendor-neutral {@link #getAggregations(String, String)} instead
     */
    public class Facet {

        private final String name;
        private final String type;

        public Facet(final String name, final String type) {

            this.name = name;
            this.type = type;
        }


        public String getType() {
            return type;
        }


        public String getName() {
            return name;
        }
    }

}