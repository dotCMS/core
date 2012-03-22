package com.dotmarketing.viewtools;

import org.apache.velocity.tools.view.tools.ViewTool;

import com.liferay.util.Xss;



public class XssWebAPI  implements ViewTool {

	  public void init(Object obj) {
	    }
	    

	/**
	 * Update fronend language
	 * @param langId
	 */
	
	public String strip(String string){
		
	 String strip =Xss.strip(string);
	 
	 return strip;
	}
	



}