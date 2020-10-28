package com.dotmarketing.util;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import com.liferay.util.Time;

public class LuceneHits implements Serializable {

	private long _start;
	private float _searchTime;
	private List<LuceneHit> luceneHits = new ArrayList<>();
	private long _length;
	private long _total;
	private String _luceneQuery = "";
	private int _offset = 0;
	private String _sortBy = "";
	
	private class LuceneHit {
		float score = 0;
		Document doc = null;
		
		public LuceneHit (float score, Document doc) {
			this.score = score;
			this.doc = doc;
		}

		public float getScore() {
			return score;
		}

		public void setScore(float score) {
			this.score = score;
		}

		public Document getDoc() {
			return doc;
		}

		public void setDoc(Document doc) {
			this.doc = doc;
		}
	}
	
	private static final long serialVersionUID = 1L;
	
	public LuceneHits() {
		_start = System.currentTimeMillis();
	}

	public void recordHits(TopDocs hits) throws IOException {
		recordHits (hits, -1, -1, null, null);
	}

	public void recordHits(TopDocs hits, int offset, int limit, String sortBy, IndexSearcher searcher)
		throws IOException {

		_total = hits.totalHits.value;
		long upperIndex;
		if (offset > -1 && limit > 0) {
			if (offset + limit >  hits.totalHits.value)
				upperIndex = hits.totalHits.value;
			else 
				upperIndex = offset + limit;
		} else {
			upperIndex = hits.totalHits.value;
			offset = 0;
		}
		
		if(upperIndex > offset){
			_length = upperIndex - offset;
		}else{
			return;
		}
		
        ScoreDoc[] hitsList = hits.scoreDocs;
       
   		luceneHits = new ArrayList(Math.toIntExact(_length));
        
        for (int i = offset; i < upperIndex; i++) {
        	int docId = hitsList[i].doc;
   			Document d = searcher.doc(docId);
   			LuceneHit hit = new LuceneHit (docId, d);
            luceneHits.add(hit);
        }
        
        if(UtilMethods.isSet(sortBy) && sortBy.trim().equalsIgnoreCase("random")){
        	Collections.shuffle(luceneHits);
        }
        
		_searchTime =
			(float)(System.currentTimeMillis() - _start) / Time.SECOND;
        
        this._offset = offset;
	}

	public Document doc(int n) {
		return luceneHits.get(n).getDoc();
	}

	public long length() {
		return _length;
	}

	public float score(int n) {
		return luceneHits.get(n).getScore();
	}

	public float searchTime() {
		return _searchTime;
	}

	public long getTotal() {
		return _total;
	}

	public void setTotal(int _total) {
		this._total = _total;
	}
    
    public String getLuceneQuery() {
        return _luceneQuery;
    }

    public void setLuceneQuery(String query) {
        _luceneQuery = query;
    }

    public int getOffset() {
        return _offset;
    }

    public void setOffset(int offset) {
        this._offset = offset;
    }
    
    public String getSortBy() {
        return _sortBy;
    }

    public void setSortBy(String sortBy) {
        this._sortBy = sortBy;
    }

}
