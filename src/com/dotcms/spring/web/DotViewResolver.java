package com.dotcms.spring.web;

import java.util.Locale;

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;


public class DotViewResolver implements ViewResolver {

	
	String prefix;
	String suffix;
	
	
	
	
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}


	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}




	public View resolveViewName(String path, Locale locale) throws Exception {


		path = (prefix != null) 
			? prefix + path
					: path;
		
		path = (suffix != null) 
			?  path + suffix
					: path;
		
		return new DotView(path);
	}
	
	
	
	
}