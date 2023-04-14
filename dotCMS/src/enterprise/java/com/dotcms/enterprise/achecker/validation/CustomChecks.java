/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.achecker.validation;

import static com.dotcms.enterprise.achecker.utility.Utility.attr;

import com.dotcms.enterprise.achecker.validation.BasicFunctions;

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
