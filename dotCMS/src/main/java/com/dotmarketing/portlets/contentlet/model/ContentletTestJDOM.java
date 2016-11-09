package com.dotmarketing.portlets.contentlet.model;

import java.util.List;


public class ContentletTestJDOM  {

	List<Contentlet> contentlets;
	List<ContentletTestRelsJDOM> rls;
	
	public ContentletTestJDOM() {}
	

	public List<Contentlet> getContentlets() {
		return contentlets;
	}

	public void setContentlets(List<Contentlet> contentlets) {
		this.contentlets = contentlets;
	}

	public List<ContentletTestRelsJDOM> getRls() {
		return rls;
	}

	public void setRls(List<ContentletTestRelsJDOM> rls) {
		this.rls = rls;
	}
	
}
