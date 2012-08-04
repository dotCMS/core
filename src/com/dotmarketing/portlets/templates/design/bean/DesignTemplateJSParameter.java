package com.dotmarketing.portlets.templates.design.bean;

import java.util.List;

/**
 * Class that represents the javascript parameter for edit the drawed template.
 * 
 * @author	Graziano Aliberti - Engineering Ingegneria Informatica
 * @date	Apr 20, 2012
 */
public class DesignTemplateJSParameter {
	
	public String pageWidth;
	
	public String layout;
	
	public boolean header;
	
	public boolean footer;
	
	public List<SplitBody> bodyRows;

	public String getPageWidth() {
		return pageWidth;
	}

	public void setPageWidth(String pageWidth) {
		this.pageWidth = pageWidth;
	}

	public String getLayout() {
		return layout;
	}

	public void setLayout(String layout) {
		this.layout = layout;
	}

	public boolean isHeader() {
		return header;
	}

	public void setHeader(boolean header) {
		this.header = header;
	}

	public boolean isFooter() {
		return footer;
	}

	public void setFooter(boolean footer) {
		this.footer = footer;
	}

	public List<SplitBody> getBodyRows() {
		return bodyRows;
	}

	public void setBodyRows(List<SplitBody> bodyRows) {
		this.bodyRows = bodyRows;
	}
	
}
