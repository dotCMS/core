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

package com.dotcms.enterprise.achecker.model;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.dotcms.enterprise.achecker.model.ReflectionBean;

public class LanguageTextBean extends ReflectionBean {
	
	private String language_code;
	private String variable;
	private String term;
	private String text;
	private String  context;
	private String textString;
 

	public LanguageTextBean(Map<String, Object> init)
			throws IntrospectionException, IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		super(init);
		
	}


	public String getLanguage_code() {
		return language_code;
	}


	public void setLanguage_code(String languageCode) {
		language_code = languageCode;
	}


	public String getVariable() {
		return variable;
	}


	public void setVariable(String variable) {
		this.variable = variable;
	}


	public String getTerm() {
		return term;
	}


	public void setTerm(String term) {
		this.term = term;
	}


	public String getText() {
		return text;
	}


	public void setText(String text) {
		this.text = text;
	}

	public String getTextString() {
		if( text!= null ){
			textString = new String( text );
		}
		return textString;
	}
	

	public String getContext() {
		return context;
	}


	public void setContext(String context) {
		this.context = context;
	}


	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("language_code : " + language_code );
		sb.append("textString : " + getTextString() );
		sb.append("term : " + term );
		sb.append("variable : " + variable );
		return sb.toString();
	}

}
