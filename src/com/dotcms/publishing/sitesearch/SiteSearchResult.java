package com.dotcms.publishing.sitesearch;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotmarketing.util.Logger;

public class SiteSearchResult {
	Map<String, Object> map = new HashMap<String, Object>();
	public Map<String, Object>  getMap(){
		return map;
	}
	public String getContent() {
		return (String) map.get("content");
	}
	public String getFileName() {
		return (String) map.get("fileName");
	}
	
	public void setFileName(String fileName) {
		map.put("fileName", fileName);
	}

	public void setContent(String content) {
		map.put("content", content);
	}

	long contentLength;

	public String getId() {
		return (String) map.get("id");
	}

	public void setId(String id) {
		map.put("id", id);
	}

	public String getHost() {
		return (String) map.get("host");
	}

	public void setHost(String host) {
		map.put("host", host);
	}

	public String getUri() {
		return (String) map.get("uri");
	}

	public void setUri(String uri) {
		map.put("uri", uri);
	}

	public String getUrl() {
		return (String) map.get("url");
	}

	public void setUrl(String url) {
		map.put("url", url);
	}

	public String getMimeType() {
		return (String) map.get("mimeType");
	}

	public void setMimeType(String mimeType) {
		map.put("mimeType", mimeType);
	}

	public String getTitle() {
		return (String) map.get("title");
	}

	public void setTitle(String title) {
		map.put("title", title);
	}

	public String getDescription() {
		return (String) map.get("description");
	}

	public void setDescription(String description) {
		map.put("description", description);
	}

	public String getKeywords() {
		return (String) map.get("keywords");
	}

	public void setKeywords(String keywords) {
		map.put("keywords", keywords);
	}

	public String getAuthor() {
		return (String) map.get("author");
	}

	public void setAuthor(String author) {
		map.put("author", author);
	}

	public long getContentLength() {
		
		if(map.get("contentLength") ==null){
			return 0;
		}
		if(map.get("contentLength") instanceof Integer){
			return new Long((Integer) map.get("contentLength"));
		}
		
		return (Long) map.get("contentLength");
	}

	public void setContentLength(long contentLength) {
		map.put("contentLength", contentLength);
	}

	public Date getModified() {
		
		if(!map.containsKey("modified")){
			return null;
		}
		if(map.get("modified") instanceof Date){
			return (Date) map.get("modified");
		}
		if(map.get("modified") instanceof String){
			
			try {
				return (Date) ESMappingAPIImpl.elasticSearchDateTimeFormat.parseObject((String) map.get("modified") );
			} catch (ParseException e) {
				Logger.error(SiteSearchResult.class,e.getMessage());
			}
	
		}
		return null;
	}

	public void setModified(Date modified) {
		map.put("modified", modified);
	}
	
	public float getScore(){
		return (Float) map.get("score");
		
	}
	public void setScore(float score){
		map.put("score", score);
		
	}
	
	public SiteSearchResult(Map m){
		this.map = m;
		
	}
	
	public SiteSearchResult(){
		this.map = new HashMap<String, Object>();
		
	}
}
