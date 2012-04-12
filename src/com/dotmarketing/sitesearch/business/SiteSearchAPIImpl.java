package com.dotmarketing.sitesearch.business;

import java.util.concurrent.ConcurrentMap;

import com.google.common.base.Function;
import com.google.common.collect.MapMaker;


/**
 * 
 * @author Roger
 *
 */
public class SiteSearchAPIImpl implements SiteSearchAPI{
	
	private static final int defaultNumRows = 10;
	private static final String defaultDedupField = "site";
	private static final String defaultRespType = "xml";
	private static final ConcurrentMap<String,NutchBeanProxy> nutchBeans = new MapMaker().makeComputingMap(
	  new Function<String,NutchBeanProxy>() {
          public NutchBeanProxy apply(String hostId) {
            return new NutchBeanProxy(hostId);
          }
      });


	public DotSearchResults search(String query, String sort, int start, int rows, String lang, String hostId) {
		
//        DotHits hits = null;
//		try {
//			hits = getHits(query,sort,start,rows,lang,hostId);
//		} catch (Exception e) {
//			Logger.error(this, e.getMessage(), e);
//			return null;
//		}
		DotSearchResults results = new DotSearchResults();
//		results.setResponseType(defaultRespType);
//		results.setQuery(query);
//		results.setLang(lang);
//		results.setSort(sort);
//		results.setReverse(false);
//		results.setStart(start);
//		results.setRows(rows);
//		results.setEnd(hits.getLength());
//		results.setTotalHits(hits.getTotalHits());
//		results.setHits(hits.getHits());
//		results.setDetails(hits.getDetails());
//		results.setSummaries(hits.getSummary());
//		results.setWithSummary(true);
		return results;
	}
	
	/**
	 * 
	 * @param query
	 * @param sort
	 * @param start
	 * @param rows
	 * @param lang
	 * @param hostId
	 * @return
	 * @throws Exception
	 */
	private DotHits getHits(String query, String sort, int start, int rows, String lang, String hostId) throws Exception{

		DotHits dotHits = new DotHits();
//		rows = rows == 0 ? defaultNumRows : rows;
//		NutchBeanProxy nutchBeanProxy = nutchBeans.get(hostId);
//		NutchBean nutchBean = nutchBeanProxy.getNutchBean();
//		if(nutchBean!=null){
//			Query queryObj = Query.parse(query,lang,nutchBeanProxy.getConf());
//			try{
//				Hits hits;
//				try {
//					queryObj.getParams().initFrom(start + rows, rows,
//							defaultDedupField, sort, false);
//					hits = nutchBean.search(queryObj);
//				} catch (IOException e) {
//					hits = new Hits(0, new Hit[0]);
//				}
//				int end = (int) Math.min(hits.getLength(), start + rows);
//				int length = end - start;
//				int realEnd = (int) Math.min(hits.getLength(), start + rows);
//				Hit[] show = hits.getHits(start, realEnd - start);
//				HitDetails[] details = nutchBean.getDetails(show);
//				Summary[] summaries = nutchBean.getSummary(details, queryObj);	
//				dotHits.setTotalHits(hits.getTotal());
//				dotHits.setLength(length);
//				dotHits.setSummary(summaries);
//				dotHits.setDetails(details);
//				dotHits.setHits(show);
//			}finally{
//				nutchBeanProxy.refresh();
//			}
//		}else{
//			nutchBeans.remove(hostId);
//		}
		return dotHits;
	}
}
