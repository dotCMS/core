package com.dotmarketing.viewtools;

import org.apache.velocity.tools.view.tools.ViewTool;

import com.liferay.util.Xss;



public class XssWebAPI  implements ViewTool {

	  public void init(Object obj) {
	    }
	    

	  /**
	   * This method will take a String and remove any XSS code found in it
	   * @param string
	   * @return
	   */
	public String strip(String string){
		
		return Xss.strip(string);
	 

	}
	/**
	 * This method will HTML escape/sanitize a String for
	 * display to a users browser
	 * @param value
	 * @return
	 */
	public String escapeHTMLAttrib(String value) {
	    return Xss.escapeHTMLAttrib(value);
	}
	
	/**
	 * This method will HTML escape/sanitize a String for
	 * display to a users browser - convieniece method
	 * @param value
	 * @return
	 */
	public String escape(String value) {
	    return escapeHTMLAttrib(value);
	}
	
	
	/**
	 * This method will unescape sanitized HTML escape/sanitize a String for
	 * display to a users browser
	 * @param value
	 * @return
	 */
	public String unEscape(String value) {
	    return Xss.unEscapeHTMLAttrib(value);
	}
	
	/**
	 * This method will test if a sting has 
	 * XSS in it. 
	 * @param value
	 * @return
	 */
	public boolean hasXss(String value) {
	    return Xss.URLHasXSS(value);
	}

}