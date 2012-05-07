package com.dotcms.achecker.validation;

import static com.dotcms.achecker.utility.Utility.attr;

import com.dotcms.achecker.validation.BasicFunctions;

public class CustomChecks extends BasicFunctions {
	
	public boolean check60() {

		if (!attr(global_e, "type").equals("image")) return true;

		String lang_code = getLangCode();

		if (lang_code.equals("ger") || lang_code.equals("de"))
		   return (getAttributeTrimedValueLength("alt") <= 115);
		else if (lang_code.equals("kor") || lang_code.equals("ko"))
		   return (getAttributeTrimedValueLength("alt") <= 90);
		else
		   return (getAttributeTrimedValueLength("alt") <= 100);
		
	}
	
	public boolean check61() {

		if (!getAttributeValue("type").equals("image")) 
			return true;
		
		String src = getAttributeValue("src");
		String alt = getAttributeValue("alt");
				
		if (!src.equals("") && !alt.equals(""))
		   return (!src.equals(alt));
		else
		   return true;
		
	}

	public boolean check115() {
		
		if (isDataTable()) return true;
		
		if (getFirstChildTag().equals("caption")) 
		   return false;
		else 
		   return true;		

	}
	
	public boolean check163() {
		if (getNextSiblingTag().equals("noembed")) return true;

		if (hasTagInChildren("noembed")) 
		   return true;
		else
		   return false;		
	}
	
	public boolean check188() {
		if ( attr(global_e, "type").equals("submit")) 
			return hasAttribute("value");
		else if (attr(global_e, "type").equals("hidden") || attr(global_e, "type").equals("button"))
			return true;
		else
			return associatedLabelHasText();		
	}
	
	public boolean check243() {
		
		if (attr(global_e, "summary").equals("")) return true;

		String caption = getLowerCasePlainTextWithGivenTagInChildren("caption");

		return !getAttributeValueInLowerCase("summary").equals(caption);
		
	}

	public boolean check252() {

		int count_colors = 0;

		if (hasAttribute("text")) count_colors ++;
		if (hasAttribute("link")) count_colors ++;
		if (hasAttribute("alink")) count_colors ++;
		if (hasAttribute("vlink")) count_colors ++;
		if (hasAttribute("bgcolor")) count_colors ++;

		return (count_colors == 0 || count_colors == 5);
	}
	
}
