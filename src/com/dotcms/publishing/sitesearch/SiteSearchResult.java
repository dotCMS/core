package com.dotcms.publishing.sitesearch;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SiteSearchResult {
	Map<String, Object> map = new HashMap<String, Object>();
	public Map<String, Object>  getMap(){
		return map;
	}
	public String getContent() {
		return (String) map.get("content");
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
		return (Long) map.get("contentLength");
	}

	public void setContentLength(long contentLength) {
		map.put("contentLength", contentLength);
	}

	public Date getModified() {
		return (Date) map.get("modified");
	}

	public void setModified(Date modified) {
		map.put("modified", modified);
	}

}
