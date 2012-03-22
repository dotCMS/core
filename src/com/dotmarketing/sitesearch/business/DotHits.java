package com.dotmarketing.sitesearch.business;


import org.apache.nutch.searcher.Hit;
import org.apache.nutch.searcher.HitDetails;
import org.apache.nutch.searcher.Summary;

public class DotHits {
	
	private Hit[] hits;
	
	private HitDetails[] details;
	
	private Summary[] summary;
	
	private int length;
	
    private long totalHits;

    
	public Hit[] getHits() {
		return hits;
	}

	public void setHits(Hit[] hits) {
		this.hits = hits;
	}

	public HitDetails[] getDetails() {
		return details;
	}

	public void setDetails(HitDetails[] details) {
		this.details = details;
	}

	public Summary[] getSummary() {
		return summary;
	}

	public void setSummary(Summary[] summary) {
		this.summary = summary;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public long getTotalHits() {
		return totalHits;
	}

	public void setTotalHits(long totalHits) {
		this.totalHits = totalHits;
	}

   
	
	

}
