package com.dotmarketing.sitesearch.business;

import java.net.MalformedURLException;

import org.apache.solr.client.solrj.SolrServerException;

import com.dotmarketing.beans.Host;

public class DotSearchResults {

	Host host;
	String lang;


	public Host getHost() {
		return host;
	}


	public void setHost(Host host) {
		this.host = host;
	}


	public String getLang() {
		return lang;
	}


	public void setLang(String lang) {
		this.lang = lang;
	}


	public String getMisspellings() throws MalformedURLException, SolrServerException {
//		String ret = this.getQuery();
//		try{
//			List<InvalidWord> invalidWords = WordsUtil.checkSpelling(ret);
//
//			for(InvalidWord invalidWord :  invalidWords){
//				List suggestions = invalidWord.getSuggestions();
//				if(suggestions.size() > 0)
//					ret = ret.replaceAll(invalidWord.getInvalidWord(), (String) suggestions.get(0));
//			}
//			
//			// if we have nothing, return null;
//			if(this.getQuery().equals(ret)){
//				return null;
//			}
//			if(host ==null || host.getIdentifier() ==null ||  lang ==null){
//				return null;
//			}
//			
//			// make sure there are results before we suggest.
//			DotSearchResults drs = APILocator.getSiteSearchAPI().search(ret, null, 0, 10, lang, host.getIdentifier());
//			if(drs.getTotalHits() > 0){
//				return ret;
//			}
//			
//		}
//		catch(Exception e){
//			Logger.debug(this.getClass(), "Unable to find a misspelled word in " + this.getQuery());
//			return null;
//		}
		
		return null;
		
		
		
	}
}
