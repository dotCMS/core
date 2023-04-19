/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.publishing.sitesearch;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.util.stream.Collectors;

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

	public void setHighLight(String[] highlight) {
		map.put("highlight", highlight);
	}
	
	public String[] getHighLights() {
		return (String[]) map.get("highlight");
	}
	
	public String[] getHighlights() {
		return (String[]) map.get("highlight");
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
		if(mimeType != null){
			 mimeType = mimeType.replaceAll("\\s", "").toLowerCase();
		 }
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

	public List<String> getKeywords() {
		return (List<String>) map.get("keywords");
	}

    public void setKeywords(String keywords) {
        map.put("keywords",
                UtilMethods.isSet(keywords) ? Arrays.stream(keywords.split(",")).map(String::trim).collect(
                        Collectors.toList()) : Collections.EMPTY_LIST);
    }

	public String getAuthor() {
		return (String) map.get("author");
	}

	public void setAuthor(String author) {
		map.put("author", author);
	}
	
	public long getLanguage() {
		long lang = 0;
		try {
			lang = (Long) map.get("language");
		} catch (ClassCastException c) {
			lang = Long.valueOf((Integer) map.get("language"));
		}

		return lang;
	}

	public void setLanguage(long language) {
		map.put("language", language);
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
				return (Date) new SimpleDateFormat(ESMappingAPIImpl.elasticSearchDateTimeFormatPattern).parse((String) map.get("modified") );
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
	@Override
	public String toString() {
		if(UtilMethods.isSet(this.getTitle())){
			return this.getTitle() + ":"+ this.getUrl();
		}

		
		return this.getUrl();
		
	}
}
