package com.eng.achecker;


public class AccessibilityResult {
	
	private int line_number;

	private int col_number;

	private CheckBean check;
	
	private boolean success;
	
	private String htmlCode;
	
	private String image;
	
	private String imageAlt;
	
	private String cssCode;
	
	public AccessibilityResult() {}
	
	public AccessibilityResult(int line_number, int col_number, CheckBean check, boolean success) {
		super();
		this.line_number = line_number;
		this.col_number = col_number;
		this.check = check;
		this.success = success;
	}
	
	public AccessibilityResult(AccessibilityResult result) {
		this(
				result.getLine_number(), 
				result.getCol_number(),
				result.getCheck(),
				result.isSuccess()
		);
	}
	
	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getImageAlt() {
		return imageAlt;
	}

	public void setImageAlt(String imageAlt) {
		this.imageAlt = imageAlt;
	}

	public String getCssCode() {
		return cssCode;
	}

	public void setCssCode(String cssCode) {
		this.cssCode = cssCode;
	}

	public String getHtmlCode() {
		return htmlCode;
	}

	public void setHtmlCode(String htmlCode) {
		this.htmlCode = htmlCode;
	}

	public int getLine_number() {
		return line_number;
	}

	public void setLine_number(int line_number) {
		this.line_number = line_number;
	}

	public CheckBean getCheck() {
		return check;
	}

	public void setCheck(CheckBean check) {
		this.check = check;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public int getCol_number() {
		return col_number;
	}

	public void setCol_number(int col_number) {
		this.col_number = col_number;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(check.toString() + " ");
		buffer.append(success + " ");
		buffer.append("{ row: " + line_number + ", col: " + col_number + "}");
		buffer.append("[ " + htmlCode + " ]");
		return buffer.toString();
	}
	
}
