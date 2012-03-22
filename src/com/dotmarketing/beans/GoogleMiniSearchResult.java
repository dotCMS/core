package com.dotmarketing.beans;

import java.util.HashMap;

public class GoogleMiniSearchResult {
	private int resultIndex;
	private int identationLevel;
	private String mimeType;
	private String resultURL;
	private String resultURLEnconded;
	private String title;
	private int generalRatingRelevance;
	private HashMap<String, String> additionalSearchDetails;
	private HashMap<String, String[]> metaTagsFields;
	private String snippet;
	private String specialQueryTerm;
	private String moreResultsDirectory;
	private String language;
	private String label;
	private String documentCacheSize;
	private String documentCacheId;
	
	public HashMap<String, String> getAdditionalSearchDetails() {
		return additionalSearchDetails;
	}
	
	public void setAdditionalSearchDetails(HashMap<String, String> additionalSearchDetails) {
		this.additionalSearchDetails = additionalSearchDetails;
	}
	
	public int getGeneralRatingRelevance() {
		return generalRatingRelevance;
	}
	
	public void setGeneralRatingRelevance(int generalRatingRelevance) {
		this.generalRatingRelevance = generalRatingRelevance;
	}
	
	public int getIdentationLevel() {
		return identationLevel;
	}
	
	public void setIdentationLevel(int identationLevel) {
		this.identationLevel = identationLevel;
	}
	
	public String getMimeType() {
		return mimeType;
	}
	
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	
	public String getMoreResultsDirectory() {
		return moreResultsDirectory;
	}
	
	public void setMoreResultsDirectory(String moreResultsDirectory) {
		this.moreResultsDirectory = moreResultsDirectory;
	}
	
	public int getResultIndex() {
		return resultIndex;
	}
	
	public void setResultIndex(int resultIndex) {
		this.resultIndex = resultIndex;
	}
	
	public String getResultURL() {
		return resultURL;
	}
	
	public void setResultURL(String resultURL) {
		this.resultURL = resultURL;
	}
	
	public String getResultURLEnconded() {
		return resultURLEnconded;
	}
	
	public void setResultURLEnconded(String resultURLEnconded) {
		this.resultURLEnconded = resultURLEnconded;
	}
	
	public String getSnippet() {
		return snippet;
	}
	
	public void setSnippet(String snippet) {
		this.snippet = snippet;
	}
	
	public String getSpecialQueryTerm() {
		return specialQueryTerm;
	}
	
	public void setSpecialQueryTerm(String specialQueryTerm) {
		this.specialQueryTerm = specialQueryTerm;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public void setMetaTagsFields(HashMap<String, String[]> metaTagsFields) {
		this.metaTagsFields = metaTagsFields;
	}
	
	public HashMap<String, String[]> getMetaTagsFields() {
		return metaTagsFields;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("GoogleMiniSearchResult [\n");
		sb.append("resultIndex = " + resultIndex);
		sb.append("identationLevel = " + identationLevel);
		sb.append("mimeType = " + mimeType);
		sb.append("resultURL = " + resultURL);
		sb.append("resultURLEnconded = " + resultURLEnconded);
		sb.append("title = " + title);
		sb.append("generalRatingRelevance = " + generalRatingRelevance);
		sb.append("snippet = " + snippet);
		sb.append("specialQueryTerm = " + specialQueryTerm);
		sb.append("moreResultsDirectory = " + moreResultsDirectory);
		sb.append("additionalSearchDetails = " + additionalSearchDetails);
		sb.append("metaTagsFields = " + metaTagsFields);
		sb.append("]\n");
		return sb.toString();
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getDocumentCacheSize() {
		return documentCacheSize;
	}

	public void setDocumentCacheSize(String documentCacheSize) {
		this.documentCacheSize = documentCacheSize;
	}

	public String getDocumentCacheId() {
		return documentCacheId;
	}

	public void setDocumentCacheId(String documentCacheId) {
		this.documentCacheId = documentCacheId;
	}
}