/*
 * Copyright 2007-2009 Guy Van den Broeck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dotmarketing.util.diff.html.modification;

/**
 * This class holds the removal or addition of HTML tags around text. It contains
 * the same information that is presented in the tooltips of default Daisy Diff HTML output.
 * 
 * This class is not used internally by DaisyDiff. It does not take any part in the diff process.
 * It is simply provided for applications that use the DaisyDiff library and need more information
 * on the results.
 * 
 * @author kapelonk
 *
 */
public class HtmlLayoutChange {

	/**
	 * Either an HTML was introduced in the new output, or it was deleted 
	 * (but the text in-between the opening and closing tag is the same).
	 */
	public enum Type {
		TAG_ADDED, TAG_REMOVED
	}
	
	/**
	 * Only two enumeration values possible
	 */
	private Type type = null;
	
	/**
	 * Full text of the opening tag. (e.g <td width="50%").
	 */
	private String openingTag = null;
	
	/**
	 * Full text of the closing tag (e.g. </td>)
	 */
	private String endingTag = null;
	
	/**
	 * Default contructor that justs inserts sane values.
	 */
	public HtmlLayoutChange()
	{
		openingTag = "";
		endingTag = "";
	}

	/**
	 * Getter for the type.
	 * 
	 * @return the type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Setter for the type.
	 * 
	 * @param type the type to set
	 */
	public void setType(Type type) {
		this.type = type;
	}

	/**
	 * Getter for the opening tag.
	 * 
	 * @return the openingTag
	 */
	public String getOpeningTag() {
		return openingTag;
	}

	/**
	 * Setter for the opening tag.
	 * 
	 * @param openingTag the openingTag to set
	 */
	public void setOpeningTag(String openingTag) {
		this.openingTag = openingTag;
	}

	/**
	 * Getter for the ending tag.
	 * 
	 * @return the endingTag
	 */
	public String getEndingTag() {
		return endingTag;
	}

	/**
	 * Setter for the ending tag.
	 * 
	 * @param endingTag the endingTag to set
	 */
	public void setEndingTag(String endingTag) {
		this.endingTag = endingTag;
	}
	
}
