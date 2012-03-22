package com.dotmarketing.viewtools;

import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.tools.view.tools.ViewTool;

public class DotCMSDocumentationAPI implements ViewTool {



	/**
	 * @param  obj  the ViewContext that is automatically passed on view tool initialization, either in the request or the application
	 * @return      
	 * @see         ViewTool, ViewContext
	 */
	public void init(Object obj) {



	}
	
	
	public String getParentTOC(String x){
	    
	    
	    
	    return "Asd";
	}
	
	public String getParentDoc(String x){
	    
	       return "Asd";
	}

	public List<String> getTOC(){
	    
	    return new ArrayList<String>();
	}
	
	
	
	

}