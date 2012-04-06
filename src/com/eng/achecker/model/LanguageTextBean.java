package com.eng.achecker.model;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

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
