package com.eng.achecker.model;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class LanguageCodeBean extends ReflectionBean {

	private String code_3letters;
	private String direction;
	private String code_2letters;
	private String description;

	
	public LanguageCodeBean(Map<String, Object> init)
			throws IntrospectionException, IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		super(init);
	}

	public boolean isEqualTo(String code) {
		if ( code == null )
			return false;
		if ( code.length() == 2 )
			return code.equals(code_2letters);
		if ( code.length() == 3 )
			return code.equals(code_3letters);
		return false;
	}

	public String getCode_3letters() {
		return code_3letters;
	}


	public void setCode_3letters(String code_3letters) {
		this.code_3letters = code_3letters;
	}


	public String getDirection() {
		return direction;
	}


	public void setDirection(String direction) {
		this.direction = direction;
	}


	public String getCode_2letters() {
		return code_2letters;
	}


	public void setCode_2letters(String code_2letters) {
		this.code_2letters = code_2letters;
	}


	public String getDescription() {
		return description;
	}


	public void setDescription(String description) {
		this.description = description;
	}

}
