package com.dotmarketing.viewtools;

import java.io.StringWriter;

import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.util.StringUtils;

public class StringsWebApi implements ViewTool{
	public void init(Object obj) {
	}
	
	public String formatPhoneNumber(String phoneNumber){
		return StringUtils.formatPhoneNumber(phoneNumber);
	}
	
	public StringWriter getEmptyStringWriter(){
		return new StringWriter();
	}
	
	public Boolean MatchCommaSeparated(String list, String string) {
		Boolean result = false;
		if (list.contains(",")) {
			String[] elements = list.split(",");
			for (String element : elements) {
				if (element.trim().equals(string.trim())) {
					result = true;
				}
			}
		}
		else {
			if(list.trim().equals(string.trim())){
				return true;
			}
		}
		return result;
	}
	
	
}