package com.dotcms.achecker.validation;

import static com.dotcms.achecker.utility.Utility.attr;
import static com.dotcms.achecker.utility.Utility.children;
import static com.dotcms.achecker.utility.Utility.col;
import static com.dotcms.achecker.utility.Utility.element;
import static com.dotcms.achecker.utility.Utility.intval;
import static com.dotcms.achecker.utility.Utility.nodelist;
import static com.dotcms.achecker.utility.Utility.row;
import static com.dotcms.achecker.utility.Utility.string;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.dotcms.achecker.model.LanguageCodeBean;
import com.dotcms.achecker.parsing.ACheckerDocument;
import com.dotcms.achecker.parsing.DoctypeBean;
import com.dotcms.achecker.utility.Globals;
import com.dotcms.achecker.utility.Utility;
import com.dotcms.achecker.validation.BasicChecks;
import com.dotcms.achecker.validation.ColorValue;
import com.dotcms.achecker.validation.FunctionRepository;

/************************************************************************/
/* ACheckerImplImpl                                                             */
/************************************************************************/
/* Copyright (c) 2008 - 2011                                            */
/* Inclusive Design Institute                                           */
/*                                                                      */
/* This program is free software. You can redistribute it and/or        */
/* modify it under the terms of the GNU General Public License          */
/* as published by the Free Software Foundation.                        */
/************************************************************************/
// $Id$

/**
 * BasicFunctions.class.php Class for basic functions provided to users in
 * writing check functions
 * 
 * @access public
 * @author Cindy Qi Li
 * @package checker
 */

public class BasicFunctions implements FunctionRepository {

	private BasicChecks basicChecks;

	protected Element global_e;

	private Document global_content_dom;

	private int global_check_id;

	public BasicFunctions() {
		basicChecks = new BasicChecks();
	}

	public void setGlobalVariable(String name, Object value) {
		if (name.equals("global_e")) {
			global_e = (Element) value;
		} else if (name.equals("global_check_id")) {
			global_check_id = (Integer) value;
		}
		else if (name.equals("global_content_dom")) {
			global_content_dom = (Document) value;
		}
	}

	/**
	 * check if associated label of $global_e has text return true if has,
	 * otherwise, return false
	 */
	public boolean associatedLabelHasText() {
		// 1. The element $global_e has a "title" attribute
		if (!global_e.getAttribute("title").trim().equals(""))
			return true;

		// 2. The element $global_e is contained by a "label" element
		if (global_e.getParentNode().getNodeName().equals("label")) {
			String innerText = Utility.getPlainNodeContent(global_e
					.getParentNode().getChildNodes());
			String outerText = Utility.getPlainNodeContent(global_e);

			int start = innerText.indexOf(outerText);
			String content = innerText.substring(0, start).trim();
			if (content.length() > 0)
				return true;

		}

		// 3. The element $global_e has an "id" attribute value that matches the
		// "for" attribute value of a "label" element
		String input_id = attr(global_e, "id");

		if (input_id.equals(""))
			return false; // attribute "id" must exist

		for (Node e_label : nodelist(global_content_dom
				.getElementsByTagName("label"))) {

			if (attr(e_label, "for").equals(input_id)) {

				// label contains text
				if (!e_label.getTextContent().trim().equals(""))
					return true;

				// label contains an image with alt text
				for (Node e_label_child : nodelist(e_label.getChildNodes())) {

					if (e_label_child.getNodeName().equals("img")
							&& attr(e_label_child, "alt").trim().length() > 0)
						return true;

				}

			}
		}

		return false;
	}

	/**
	 * return the length of the trimed value of specified attribute
	 */
	public int getAttributeTrimedValueLength(String attr) {
		return attr(global_e, attr).trim().length();
	}

	/**
	 * return the value of the specified attribute
	 */
	public String getAttributeValue(String attr) {
		return global_e.getAttribute(attr).trim();
	}

	/**
	 * return the value of specified attribute as a number
	 */
	public int getAttributeValueAsNumber(String attr) {
		return intval(global_e.getAttribute(attr));
	}

	/**
	 * return the value of the specified attribute in lower case
	 */
	public String getAttributeValueInLowerCase(String attr) {
		return string(global_e.getAttribute(attr)).trim().toLowerCase();
	}

	/**
	 * return the length of the value of specified attribute
	 */
	public int getAttributeValueLength(String attr) {
		return string(global_e.getAttribute(attr)).length();
	}

	/**
	 * return html tag of the first child
	 */
	public String getFirstChildTag() {
		Node node = global_e.getFirstChild();
		if (node == null)
			return null;
		return node.getNodeName();
	}

	// /**
	// * return the width of the image. return false if the image is not
	// accessible or at failure
	// */
	// public static function getImageWidthAndHeight(String attr)
	// {
	// global $global_e, $base_href, $uri, $global_array_image_sizes;
	//
	// $file = BasicChecks::getFile($global_e->attr[$attr], $base_href, $uri);
	// $file_size_checked = false;
	//
	// // Check if the image has already been fetched.
	// // Since the remote fetching is the bottle neck that slows down the
	// validation,
	// // $global_array_image_sizes is to save width/height of all the fetched
	// images.
	// if (is_array($global_array_image_sizes)) {
	// foreach ($global_array_image_sizes as $image=>$info) {
	// if ($image == $file) {
	// $file_size_checked = true;
	// if (!$info["is_exist"]) {
	// return false;
	// } else {
	// return array($info["width"], $info["height"]);
	// }
	// }
	// }
	// }
	//
	// if (!$file_size_checked) {
	// $dimensions = @getimagesize($file);
	//
	// if (is_array($dimensions)) {
	// $global_array_image_sizes[$file] = array("is_exist"=>true,
	// "width"=>$dimensions[0], "height"=>$dimensions[1]);
	// return array($dimensions[0], $dimensions[1]);
	// } else {
	// $global_array_image_sizes[] = array($file=>array("is_exist"=>false,
	// "width"=>NULL, "height"=>NULL));
	// return false;
	// }
	// }
	// }

	/**
	 * return the trimed value of inner text
	 */
	public String getInnerText() {
		return Utility.getPlainNodeContent(global_e).trim();
	}

	/**
	 * return the length of the trimed inner text of specified attribute
	 */
	public int getInnerTextLength() {
		return string(global_e.getTextContent()).length();
	}

	/**
	 * return language code that is defined in the given html return language
	 * code
	 */
	public String getLangCode() {
		String lang = null;

		// get html language
		for (Node e_html : nodelist(global_content_dom
				.getElementsByTagName("html"))) {
			Element html = (Element) e_html;
			if (html.getAttribute("xml:lang") != null) {
				lang = html.getAttribute("xml:lang").trim();
				break;
			} else if (html.getAttribute("lang") != null) {
				lang = html.getAttribute("lang").trim();
				break;
			}
		}

		return basicChecks.cutOutLangCode(lang);
	}

	/**
	 * return last 4 characters. Usually used to get file extension
	 */
	public String getLast4CharsFromAttributeValue(String attr) {
		String value = global_e.getAttribute(attr).trim();
		return value.substring(Math.max(value.length() - 4, 0), value.length());
	}

	/**
	 * scan thru all the children and return the length of attribute value that
	 * the specified html tag appears in the first children
	 */
	public int getLengthOfAttributeValueWithGivenTagInChildren(String tag,
			String attr) {
		int len = 0;

		for (Node child : children(global_e))
			if (child.getNodeName().equals(tag))
				len = element(child).getAttribute(attr).trim().length();

		return len;
	}

	/**
	 * scan thru all the children and return the length of attribute value that
	 * the specified html tag appears in the first children
	 */
	public String getLowerCaseAttributeValueWithGivenTagInChildren(String tag,
			String attr) {
		NodeList children = global_e.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				String value = ((Element) child).getAttribute(attr);
				if (value != null)
					return value.trim().toLowerCase();
			}
		}
		return "";
	}

	/**
	 * scan thru all the children and return the length of plain text that the
	 * specified html tag appears in the first children
	 */
	public String getLowerCasePlainTextWithGivenTagInChildren(String tag) {
		String value = null;

		for (Node child : children(global_e))
			if (child.getNodeName().equals(tag))
				value = child.getTextContent().trim().toLowerCase();

		return value;
	}

	/**
	 * Check if the luminosity contrast ratio between $color1 and $color2 is at
	 * least 5:1 Input: color values to compare: $color1 & $color2. Color value
	 * can be one of: rgb(x,x,x), #xxxxxx, colorname Return: true or false
	 */
	public double getLuminosityContrastRatio(String color1_, String color2_) {
		ColorValue color1 = new ColorValue(color1_);
		ColorValue color2 = new ColorValue(color2_);

		if (!color1.isValid() || !color2.isValid())
			return Double.NaN;

		double linearR1 = (double) color1.getRed() / 255.0;
		double linearG1 = (double) color1.getRed() / 255.0;
		double linearB1 = (double) color1.getRed() / 255.0;

		double lum1 = (Math.pow(linearR1, 2.2) * 0.2126)
				+ (Math.pow(linearG1, 2.2) * 0.7152)
				+ (Math.pow(linearB1, 2.2) * 0.0722) + .05;

		double linearR2 = (double) color2.getRed() / 255.0;
		double linearG2 = (double) color2.getRed() / 255.0;
		double linearB2 = (double) color2.getRed() / 255.0;

		double lum2 = (Math.pow(linearR2, 2.2) * 0.2126)
				+ (Math.pow(linearG2, 2.2) * 0.7152)
				+ (Math.pow(linearB2, 2.2) * 0.0722) + .05;

		double ratio = Math.max(lum1, lum2) / Math.min(lum1, lum2);

		// round the ratio to 2 decimal places
		double factor = Math.pow(10, 2);

		// Shift the decimal the correct number of places
		// to the right.
		double val = ratio * factor;

		// Round to the nearest integer.
		double tmp = Math.round(val);

		// Shift the decimal the correct number of places back to the left.
		double ratio2 = tmp / factor;

		return ratio2;
	}

	/**
	 * return the html tag of the next sibling
	 */
	public String getNextSiblingAttributeValueInLowerCase(String attr) {
		Node sibling = global_e.getNextSibling();
		if (sibling == null)
			return null;
		if (!(sibling instanceof Element))
			return null;
		return ((Element) sibling).getAttribute(attr).trim().toLowerCase();
	}

	/**
	 * return the inner text of the next sibling for example: if next sibling is
	 * "<a href="rex.html">[d]</a></p>", this function returns "[d]"
	 */
	public String getNextSiblingInnerText() {
		Node sibling = global_e.getNextSibling();
		if (sibling == null)
			return null;
		return Utility.getPlainNodeContent(sibling);
	}

	/**
	 * return the html tag of the next sibling
	 */
	public String getNextSiblingTag() {
		Node next_sibling = null;
		for ( next_sibling = global_e.getNextSibling(); next_sibling != null; next_sibling = next_sibling.getNextSibling()) {
			if ( next_sibling instanceof Element ) {
				break;
			}
		}
		if ( next_sibling == null )
			return "";
		return next_sibling.getNodeName();
	}

	/**
	 * scan thru all the children and return the number of times that the
	 * specified html tag appears in all children
	 */
	public int getNumOfTagInChildren(String tag) {
		int num = 0;
		for (Node child : children(global_e)) {
			if (child.getNodeName().equals(tag))
				num++;
		}
		return num;
	}

	/**
	 * scan thru all the children, check if the given html tag exists and its
	 * inner text has content return true if the given html tag exists and its
	 * inner text has content. otherwise, return false
	 */
	public int getNumOfTagInChildrenWithInnerText(String tag) {
		int num = 0;

		for (Node child : children(global_e))
			if (child.getNodeName().equals(tag)
					&& Utility.getPlainNodeContent(child).trim().length() > 0)
				num++;

		return num;
	}

	/**
	 * return the number of times that the specified html tag appears in the
	 * content
	 */
	public int getNumOfTagInWholeContent(String tag) {
		if (tag.trim().equalsIgnoreCase("doctype")) {
			if ( global_content_dom instanceof ACheckerDocument ) {
				return ((ACheckerDocument) global_content_dom).getDoctypeBean() != null ? 1 : 0;
			}
			else {
				return global_content_dom.getDoctype() != null ? 1 : 0;
			}
		}
		return global_content_dom.getElementsByTagName(tag).getLength();
	}

	public int getNumOfTagRecursiveInChildren(String tag) {
		return getNumOfTagRecursiveInChildren(global_e, tag);
	}

	/**
	 * scan thru recursively of all the children and return the number of times
	 * that the specified html tag appears in all children
	 */
	public int getNumOfTagRecursiveInChildren(Node node, String tag) {
		int num = 0;

		for (Node child : children(node)) {
			if (child.getNodeName().equals(tag))
				num++;
			else
				num += getNumOfTagRecursiveInChildren(child, tag);
		}

		return num;
	}

	/**
	 * return the tag of the parent html tag
	 */
	public String getParentHTMLTag() {
		return global_e.getParentNode().getNodeName();
	}

	/**
	 * return the length of the trimed plain text of specified attribute
	 */
	public String getPlainTextInLowerCase() {
		return global_e.getTextContent().trim().toLowerCase();
	}

	/**
	 * return the length of the trimed plain text of specified attribute
	 */
	public int getPlainTextLength() {
		String text = global_e.getTextContent().trim();
		// String text = Utility.getPlainNodeContent(global_e.getChildNodes()).trim();
		return text.length();
	}

	/**
	 * Returns the portion of string specified by the start and length
	 * parameters. A wrapper on php function substr
	 */
	public String getSubstring(String string, double start, double length) {
		return string.substring((int) start, (int) start + (int) length);
	}

	/**
	 * check if current element has associated label return true if has,
	 * otherwise, return false
	 */
	public boolean hasAssociatedLabel() {
		// 1. The element $global_e is contained by a "label" element
		// 2. The element $global_e has a "title" attribute
		if (global_e.getParentNode().getNodeName().equals("label")
				|| !attr(global_e, "title").equals(""))
			return true;

		// 3. The element $global_e has an "id" attribute value that matches the
		// "for" attribute value of a "label" element
		String input_id = attr(global_e, "id").trim();

		if (input_id.equals(""))
			return false; // attribute "id" must exist

		for (Node label : nodelist(global_content_dom.getElementsByTagName("label"))) {
			if (attr(label, "for").trim().toLowerCase().equals(input_id))
				return true;
		}

		return false;
	}

	/**
	 * check if the element has attribute $attr defined return true if has,
	 * otherwise, return false
	 */
	public boolean hasAttribute(String attr) {
		for ( Node attribute : nodelist(global_e.getAttributes())) {
			if ( attribute.getNodeName().equals(attr))
				return true;
		}
		return false;
	}

	/**
	 * Check recursively if there are duplicate $attr defined in children of
	 * $global_e set global var hasDuplicateAttribute to true if there is,
	 * otherwise, set it to false
	 */
	public boolean hasDuplicateAttribute(String attr) {
		Set<String> id_array = new TreeSet<String>();
		return basicChecks.has_duplicate_attribute(global_e, attr, id_array);
	}

	/**
	 * Check if form has "fieldset" and "legend" to group multiple checkbox
	 * buttons.
	 * 
	 * @return true if has, otherwise, false
	 */
	public boolean hasFieldsetOnMultiCheckbox() {
		
		return basicChecks.hasFieldsetOnMultiCheckbox(global_e);
		
//		// find if there are radio buttons with same name
//		NodeList children = global_e.getChildNodes();
//
//		int num_of_children = children.getLength();
//
//		for (int i = 0; i < children.getLength(); i++) {
//
//			Node child = children.item(i);
//
//			if (attr(child, "type").trim().toLowerCase().equals("checkbox")) {
//
//				String this_name = attr(child, "name").trim().toLowerCase();
//
//				for (int j = i + 1; j < num_of_children; j++) {
//
//					// if there are radio buttons with same name,
//					// check if they are contained in "fieldset" and "legend"
//					// elements
//					if (attr(children.item(i), "name").trim().toLowerCase().equals(this_name)) {
//
//						if (basicChecks.hasParent(child, "fieldset"))
//							return basicChecks.hasParent(child, "legend");
//						else
//							return false;
//
//					}
//				}
//				
//			} else
//				return basicChecks.hasFieldsetOnMultiCheckbox(child);
//		}
//
//		return true;
	}

	/**
	 * Check if the luminosity contrast ratio between $color1 and $color2 is at
	 * least 5:1 Input: color values to compare: $color1 & $color2. Color value
	 * can be one of: rgb(x,x,x), #xxxxxx, colorname Return: true or false
	 */
	public boolean hasGoodContrastWaiert(String color1_, String color2_) {
		ColorValue color1 = new ColorValue(color1_);
		ColorValue color2 = new ColorValue(color2_);

		if (!color1.isValid() || !color2.isValid())
			return true;

		int colorR1 = color1.getRed();
		int colorG1 = color1.getGreen();
		int colorB1 = color1.getBlue();

		int colorR2 = color2.getRed();
		int colorG2 = color2.getGreen();
		int colorB2 = color2.getBlue();

		double brightness1 = (double) ((colorR1 * 299) + (colorG1 * 587) + (colorB1 * 114)) / 1000.0;

		double brightness2 = (double) ((colorR2 * 299) + (colorG2 * 587) + (colorB2 * 114)) / 1000.0;

		double difference = 0;
		if (brightness1 > brightness2) {
			difference = brightness1 - brightness2;
		} else {
			difference = brightness2 - brightness1;
		}

		if (difference < 125) {
			return false;
		}

		// calculate the color difference
		difference = 0;
		// red
		if (colorR1 > colorR2) {
			difference = colorR1 - colorR2;
		} else {
			difference = colorR2 - colorR1;
		}

		// green
		if (colorG1 > colorG2) {
			difference += colorG1 - colorG2;
		} else {
			difference += colorG2 - colorG1;
		}

		// blue
		if (colorB1 > colorB2) {
			difference += colorB1 - colorB2;
		} else {
			difference += colorB2 - colorB1;
		}

		return (difference > 499);
	}

	/**
	 * Check if the table contains more than one row or either row or column
	 * headers.
	 * 
	 * @return true if contains, otherwise, false
	 */
	public boolean hasIdHeaders() {
		
		// check if the table contains both row and column headers
		int[] list = basicChecks.getNumOfHeaderRowCol(global_e);

		int num_of_header_rows = list[0];
		int num_of_header_cols = list[1];

		// if table has more than 1 header rows or has both header row and
		// header column,
		// check if all "th" has "id" attribute defined and all "td" has
		// "headers" defined
		if (num_of_header_rows > 1
				|| (num_of_header_rows > 0 && num_of_header_cols > 0)) {

			for (Node th : nodelist(global_e.getElementsByTagName("th")))
				if (attr(th, "id").equals(""))
					return false;

			for (Node td : nodelist(global_e.getElementsByTagName("td")))
				if (attr(td, "headers").equals(""))
					return false;

		}

		return true;
	}

	/**
	 * Check if the table contains more than one row or either row or column
	 * headers.
	 * 
	 * @return true if contains, otherwise, false
	 */
	public boolean hasLinkChildWithText(List<String> searchStrArray) {
		boolean found = false;
		NodeList children = global_e.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeName().equals("a")) {
				found = true;
				Element a = (Element) child;
				if (inSearchString(a.getAttribute("href"), searchStrArray))
					return true;
			}
		}
		
		if (!found)
			return true;

		return false;
	}

	// TODO remove and take the BasicCheck
	private boolean inSearchString(String attribute, List<String> searchStrArray) {
		return true;
	}

	public boolean hasParent(String parent_tag) {
		return hasParent(global_e, parent_tag);
	}

	/**
	 * Check recursively to find if $global_e has a parent with tag $parent_tag
	 * return true if found, otherwise, false
	 */
	public boolean hasParent(Node node, String parent_tag) {
		if (node.getParentNode() == null)
			return false;

		if (node.getParentNode().getNodeName().equals(parent_tag))
			return true;

		return hasParent(node.getParentNode(), parent_tag);
	}

	/**
	 * Check if the table contains both row and column headers.
	 * 
	 * @return true if contains, otherwise, false
	 */
	public boolean hasScope() {
		
		// check if the table contains both row and column headers
		int[] list = basicChecks.getNumOfHeaderRowCol(global_e);

		int num_of_header_rows = list[0];
		int num_of_header_cols = list[1];

		if (num_of_header_rows > 0 && num_of_header_cols > 0) {

			for (Node th : nodelist(global_e.getElementsByTagName("th")))
				if (attr(th, "scope").equals(""))
					return false;

		}

		return true;
	}

	/**
	 * check if the tag plain text contains a line that is separated by more
	 * than one tab or vertical line return true if yes, otherwise, false
	 */
	public boolean hasTabularInfo() {
		String text = global_e.getTextContent();

		String regexp1 = "/.*\\t.+\\t.*/";
		String regexp2 = "/.*\\|.+\\|.*/";

		return (Pattern.matches(regexp1, text) || Pattern
				.matches(regexp2, text));
	}

	/**
	 * check if there's given tag in children. return true if has, otherwise,
	 * false
	 */
	public boolean hasTagInChildren(String tag) {
		return global_e.getElementsByTagName(tag).getLength() > 0;
	}

	/**
	 * Check if there's text in between <a> elements return true if there is,
	 * otherwise, false
	 */
	public boolean hasTextInBtw() {

		Node next_sibling = null;
		
		// Look for next element sibling
		for ( next_sibling = global_e.getNextSibling(); next_sibling != null; next_sibling = next_sibling.getNextSibling()) {
			if ( next_sibling instanceof Element ) {
				break;
			}
		}
		
		if (next_sibling == null)
			return false;

		if (!next_sibling.getNodeName().equals("a"))
			return false;

		// check if there's other text in between $global_e and its next sibling
		String outer1 = Utility.getPlainNodeContent(global_e);
		String outer2 = Utility.getPlainNodeContent(next_sibling);
		String parent = Utility.getPlainNodeContent(global_e.getParentNode());

		int start1 = parent.indexOf(outer1);
		int start2 = parent.indexOf(outer2);
		
		if ( start1 != -1 && start2 != 1 ) {
			String middle = parent.substring(start1 + outer1.length(), start2);
			return middle.trim().length() > 0;
		}
		
		return false;
	}

	/**
	 * check if there's child with tag named $childTag, in which the value of
	 * attribute $childAttribute equals one of the values in given $valueArray
	 * return true if has, otherwise, false
	 */
	public boolean hasTextInChild(String childTag, String childAttribute,
			List<String> valueArray) {
		// if no <link> element is defined or "rel" in all <link> elements are
		// not "alternate" or href is not defined, return false
		for (Node child : nodelist(global_e.getChildNodes())) {

			if (child.getNodeName().equals(childTag)) {

				Element childE = (Element) child;
				String attribute = childE.getAttribute(childAttribute);

				if (attribute != null) {

					String ret_val = attribute.trim().toLowerCase();

					if (valueArray.contains(ret_val))
						return true;

				}
			}
		}

		return false;
	}

	/**
	 * This function for now is solely used for attribute "usemap", check id 13
	 */
	public boolean hasTextLinkEquivalents(String attr) {

		String map_name = global_e.getAttribute(attr).substring(1); // remove
																	// heading #

		// find definition of <map> with $map_name
		boolean map_found = false;

		List<Map<String, String>> area_hrefs = new ArrayList<Map<String, String>>();

		for (Node nmap : nodelist(global_content_dom
				.getElementsByTagName("map"))) {

			Element map = (Element) nmap;

			if (map.getAttribute("name").equals(map_name)) {

				map_found = true;

				for (Node map_child : nodelist(map.getChildNodes())) {

					if (map_child.getNodeName().equals("area")) {
						Map<String, String> item = new HashMap<String, String>();
						item.put(
								"href",
								string(
										((Element) map_child)
												.getAttribute("href")).trim());
						item.put("found", "false");
						area_hrefs.add(item);
					}

				}

				break; // stop at finding <map> with $map_name
			}
		}

		// return false <map> with $map_name is not defined
		if (!map_found)
			return false;

		for (Node a : nodelist(global_content_dom.getElementsByTagName("a"))) {

			for (Map<String, String> area_href : area_hrefs) {

				if (element(a).getAttribute("href").equals(
						area_href.get("href"))) {
					area_href.put("found", "true");
					break;
				}

			}
		}

		boolean all_href_found = true;

		for (Map<String, String> area_href : area_hrefs) {
			if (!area_href.get("found").equals("true")) {
				all_href_found = false;
				break;
			}
		}

		// return false when not all area href are defined
		if (!all_href_found)
			return false;

		return true;
	}

	/**
	 * check if window.onload is contained in tag "script". return true if has,
	 * otherwise, false
	 */
	public boolean hasWindowOpenInScript() {
		NodeList tags = global_content_dom.getElementsByTagName("script");

		for (Node child : nodelist(tags)) {
			if (Utility.getPlainNodeContent(child.getChildNodes()).contains(
					"window.onload"))
				return true;
		}

		return false;
	}

	/**
	 * Check if the html document is validated return true if validated,
	 * otherwise, false
	 */
	public boolean htmlValidated() {
		// SKIPPED HTML Validation
		return true;
		//
		// global $htmlValidator;
		//
		// if (!isset($htmlValidator)) return false;
		//
		// return ($htmlValidator->getNumOfValidateError() == 0);
	}

	/**
	 * check if the inner text is in one of the search string defined in
	 * checks.search_str return true if in, otherwise, return false
	 */
	public boolean isAttributeValueInSearchString(String attr) {
		return basicChecks.isTextInSearchString(attr(global_e, attr).trim(),
				global_check_id, global_e);
	}

	/**
	 * Makes a guess about the table type. Returns true if this should be a data
	 * table, false if layout table.
	 */
	public boolean isDataTable() {
		return basicChecks.isDataTable(global_e);
	}

	/**
	 * check if the inner text is in one of the search string defined in
	 * checks.search_str return true if in, otherwise, return false
	 */
	public boolean isInnerTextInSearchString() {
		String innerText = Utility
				.getPlainNodeContent(global_e.getChildNodes());
		return basicChecks.isTextInSearchString(innerText, global_check_id,
				global_e);
	}

	/**
	 * check if the next tag, is not in given array $notInArray return true if
	 * not in, otherwise, return false
	 */
	public boolean isNextTagNotIn(List<String> notInArray) {
		if (Globals.header_array == null)
			return true;

		Node next_header = null;

		// find the next header after $global_e->linenumber,
		// $global_e->colnumber
		for (Node e : nodelist(Globals.header_array)) {

			if (row(e) > row(global_e)
					|| (row(e) == row(global_e) && col(e) > col(global_e))) {
				if (next_header == null)
					next_header = e;
				else if (row(e) < row(next_header)
						|| (row(e) == row(next_header) && col(e) > col(next_header)))
					next_header = e;
			}
		}

		if (next_header != null
				&& !notInArray.contains(next_header.getNodeName()))
			return false;
		else
			return true;
	}

	/**
	 * check if the plain text is in one of the search string defined in
	 * checks.search_str return true if in, otherwise, return false
	 */
	public boolean isPlainTextInSearchString() {
		String plainText = global_e.getTextContent();
		return basicChecks.isTextInSearchString(plainText, global_check_id,
				global_e);
	}

	/**
	 * Check radio button groups are marked using "fieldset" and "legend"
	 * elements Return: use global variable $is_radio_buttons_grouped to return
	 * true (grouped properly) or false (not grouped)
	 */
	public boolean isRadioButtonsGrouped() {
		List<Node> radio_buttons = new ArrayList<Node>();

		for (Node e_input : nodelist(global_e.getElementsByTagName("input"))) {

			if (attr(e_input, "type").trim().toLowerCase().equals("radio"))
				radio_buttons.add(e_input);

		}

		for (int i = 0; i < radio_buttons.size(); i++) {

			for (int j = 0; j < radio_buttons.size(); j++) {

				if (i != j
						&& attr(radio_buttons.get(i), "name")
								.trim()
								.toLowerCase()
								.equals(attr(radio_buttons.get(j), "name")
										.trim().toLowerCase())
						&& !basicChecks.hasParent(radio_buttons.get(i),
								"fieldset")
						&& !basicChecks.hasParent(radio_buttons.get(i),
								"legend")) {

					return false;

				}

			}
		}

		return true;
	}

	/**
	 * check if the labels for all the submit buttons on the form are different
	 * return true if all different, otherwise, return false
	 */
	public boolean isSubmitLabelDifferent() {
		List<String> submit_labels = new ArrayList<String>();

		for (Node form : nodelist(global_e.getElementsByTagName("form"))) {

			for (Node button : nodelist(global_e.getElementsByTagName("input"))) {

				String button_type = attr(button, "type").trim().toLowerCase();

				if (button_type.equals("submit") || button_type.equals("image")) {

					String button_value = null;

					if (button_type.equals("submit"))
						button_value = attr(button, "value").trim()
								.toLowerCase();

					if (button_type.equals("image"))
						button_value = attr(button, "alt").trim().toLowerCase();

					if (submit_labels.contains(button_value))
						return false;
					else
						submit_labels.add(button_value);

				}
			}
		}

		return true;
	}

	/**
	 * check if the element content is marked with the html tags given in
	 * $htmlTagArray return true if valid, otherwise, return false
	 */
	public boolean isTextMarked(List<String> htmlTagArray) {
		NodeList children = global_e.getChildNodes();

		if (children.getLength() == 1) {

			Node child = children.item(0);

			String tag = child.getNodeName();

			String plainText = child.getTextContent();

			if (htmlTagArray.contains(tag)
					&& plainText.equals(global_e.getTextContent()))
				return false;

		}
		return true;
	}

	/**
	 * check if value in the given attribute is a valid language code return
	 * true if valid, otherwise, return false
	 */
	public boolean isValidLangCode() {
		
		boolean is_text_content = false;
		boolean is_application_content = false;

		for (Node nMeta : nodelist(global_content_dom.getElementsByTagName("meta"))) {
			Element meta = (Element) nMeta;
			if (meta.getAttribute("content").toLowerCase().contains("text/html"))
				is_text_content = true;
			if (meta.getAttribute("content").toLowerCase().contains("application/xhtml+xml"))
				is_application_content = true;
		}

		if ( global_content_dom instanceof ACheckerDocument ) {
			
			DoctypeBean doctypeBean = ((ACheckerDocument)global_content_dom).getDoctypeBean();
			if ( doctypeBean == null )
				return false;

			String doctype_content = doctypeBean.getPublicId();

			// If the content is HTML, check the value of the html element's
			// lang attribute
			if (doctype_content.toLowerCase().contains("html") && !doctype_content.toLowerCase().contains("xhtml")) {
				return basicChecks.isValidLangCode(attr(global_e, "lang").trim());
			}

			// If the content is XHTML 1.0, or any version of XHTML served
			// as "text/html",
			// check the values of both the html element's lang attribute
			// and xml:lang attribute.
			// Note: both lang attributes must be set to the same value.
			if (doctype_content.toLowerCase().contains("xhtml 1.0") || (doctype_content.toLowerCase().contains(" xhtml ") && is_text_content)) {
				
				return (basicChecks.isValidLangCode(attr(global_e, "lang").trim()) &&
						basicChecks.isValidLangCode(attr(global_e, "xml:lang").trim()) &&
						attr(global_e, "lang").trim().equals(attr(global_e, "xml:lang").trim()));
				
			} else if (doctype_content.toLowerCase().contains(" xhtml ") && is_application_content) {
				return basicChecks.isValidLangCode(attr(global_e, "xml:lang").trim());
			}
			
		}
		else {

			NodeList doctypes = global_content_dom.getElementsByTagName("doctype");
			if (doctypes.getLength() == 0)
				return false;

			for (Node doctype : nodelist(doctypes)) {

				for (Node attr : nodelist(doctype.getAttributes())) {

					String doctype_content = attr.getNodeName();
					// String garbage = attr.getNodeValue();

					// If the content is HTML, check the value of the html element's
					// lang attribute
					if (doctype_content.toLowerCase().contains("html")
							&& !doctype_content.toLowerCase().contains("xhtml")) {
						return basicChecks.isValidLangCode(global_e.getAttribute(
						"lang").trim());
					}

					// If the content is XHTML 1.0, or any version of XHTML served
					// as "text/html",
					// check the values of both the html element's lang attribute
					// and xml:lang attribute.
					// Note: both lang attributes must be set to the same value.
					if (doctype_content.toLowerCase().contains("xhtml 1.0")
							|| (doctype_content.toLowerCase().contains(" xhtml ") && is_text_content)) {
						return basicChecks.isValidLangCode(global_e.getAttribute(
						"lang").trim())
						&& basicChecks.isValidLangCode(global_e
								.getAttribute("xml:lang").trim())
								&& global_e
								.getAttribute("lang")
								.trim()
								.equals(global_e.getAttribute("xml:lang")
										.trim());
					} else if (doctype_content.toLowerCase().contains(" xhtml ")
							&& is_application_content) {
						return basicChecks.isValidLangCode(global_e.getAttribute(
						"xml:lang").trim());
					}

				}
			}
		}
		
		return true;
	}

	/*
	 * Validate if the <code>dir</code> attribute's value is "rtl" for languages
	 * that are read left-to-right or "ltr" for languages that are read
	 * right-to-left. return true if it's valid, otherwise, false
	 */
	public boolean isValidRTL() {
		
		String lang_code = null;
		
		if (!attr(global_e, "lang").trim().equals(""))
			lang_code = attr(global_e, "lang").trim();
		else
			lang_code = attr(global_e, "xml:lang").trim();

		// return no error if language code is not specified
		if (!basicChecks.isValidLangCode(lang_code))
			return true;

		List<LanguageCodeBean> rtl_lang_codes = basicChecks.getRtlLangCodes();

		for ( LanguageCodeBean bean : rtl_lang_codes ) {
			
			if ( bean.isEqualTo(lang_code)) {

				// When these 2 languages, "dir" attribute must be set and set to
				// "rtl"
				return attr(global_e, "dir").trim().toLowerCase().equals("rtl");

			}
		}
		
		if (attr(global_e, "dir").trim().equals(""))
			return true;

		return attr(global_e, "dir").trim().toLowerCase().equals("ltr");
	}

	/**
	 * This function validates html "doctype" return true if doctype is valid,
	 * otherwise, false
	 */
	public boolean validateDoctype() {
		
		if ( global_content_dom instanceof ACheckerDocument ) {
			DoctypeBean doctype = ((ACheckerDocument) global_content_dom).getDoctypeBean();
			if ( doctype != null ) {
				if ( doctype.getPublicId().contains("-//W3C//DTD HTML 4.01//EN"))
					return true;
				if ( doctype.getSystemId().contains("-//W3C//DTD HTML 4.01//EN"))
					return true;
				if ( doctype.getPublicId().contains("-//W3C//DTD HTML 4.0//EN"))
					return true;
				if ( doctype.getSystemId().contains("-//W3C//DTD HTML 4.0//EN"))
					return true;
				if ( doctype.getPublicId().contains("-//W3C//DTD XHTML 1.0 Strict//EN"))
					return true;
				if ( doctype.getSystemId().contains("-//W3C//DTD XHTML 1.0 Strict//EN"))
					return true;
			}
		}
		else {
			NodeList doctypes = global_content_dom.getElementsByTagName("doctype");
			if ( doctypes.getLength() == 0 ) return false;

			for (Node doctype : nodelist(doctypes)) {
				if ( doctype instanceof DocumentType ) {
					DocumentType dType = (DocumentType) doctype;
					if ( dType.getPublicId().contains("-//W3C//DTD HTML 4.01//EN"))
						return true;
					if ( dType.getSystemId().contains("-//W3C//DTD HTML 4.01//EN"))
						return true;
					if ( dType.getPublicId().contains("-//W3C//DTD HTML 4.0//EN"))
						return true;
					if ( dType.getSystemId().contains("-//W3C//DTD HTML 4.0//EN"))
						return true;
					if ( dType.getPublicId().contains("-//W3C//DTD XHTML 1.0 Strict//EN"))
						return true;
					if ( dType.getSystemId().contains("-//W3C//DTD XHTML 1.0 Strict//EN"))
						return true;
				}
			}
		}

		return false;
	}

	// //MB
	// //Color Contrast Functions (checks 301 - 310)
	// public static function checkColorContrastForGeneralElementWCAG2AA() {
	// //WCAG2.0 Contrast check
	// global $background, $foreground;
	// global $global_e, $global_content_dom;
	// global $stringa_testo_prova;
	// $e = $global_e;
	// $content_dom = $global_content_dom;
	//
	// BasicChecks::setCssSelectors ( $content_dom );
	//
	// $background = '';
	// $foreground = '';
	// //elementi testuali
	// if (($e->tag == "div" || $e->tag == "p" || $e->tag == "span" || $e->tag
	// == "strong" || $e->tag == "em" || $e->tag == "q" || $e->tag == "cite" ||
	// $e->tag == "blockquote" || $e->tag == "li" || $e->tag == "dd" || $e->tag
	// == "dt" || $e->tag == "td" || $e->tag == "th" || $e->tag == "h1" ||
	// $e->tag == "h2" || $e->tag == "h3" || $e->tag == "h4" || $e->tag == "h5"
	// || $e->tag == "h6" || $e->tag == "label" || $e->tag == "acronym" ||
	// $e->tag == "abbr" || $e->tag == "code" || $e->tag == "pre") &&
	// BasicChecks::isElementVisible ( $e )) {
	// //l'elemento non contiene testo "visibile": non eseguo il controllo del
	// contrasto
	// if (trim ( BasicChecks::remove_children ( $e ) ) == "" || trim (
	// BasicChecks::remove_children ( $e ) ) == "&nbsp;"){
	// return true;
	// }
	//
	// $background = BasicChecks::getBackground ( $e );
	// $foreground = BasicChecks::getForeground ( $e );
	//
	// if ($foreground == "" || $foreground == null || $background ==
	// "undetermined") {
	// return true;
	// }
	//
	// if ($background == "" || $background == null || $background == "-1" ||
	// $background == "undetermined") {
	// return true;
	// }
	//
	// $background = BasicChecks::convert_color_to_hex ( $background );
	// $foreground = BasicChecks::convert_color_to_hex ( $foreground );
	//
	// $ris = BasicChecks::ContrastRatio ( strtolower ( $background ),
	// strtolower ( $foreground ) );
	// //echo "tag->"; echo $e->tag; echo " bg->"; echo $background; echo
	// " fr->"; echo $foreground; echo " ris="; echo $ris; echo "<br>";
	//
	// $size = BasicChecks::fontSizeToPt ( $e );
	// $bold = BasicChecks::get_p_css ( $e, "font-weight" );
	// if ($e->tag == "h1" || $e->tag == "h2" || $e->tag == "h3" || $e->tag ==
	// "h4" || $e->tag == "h5" || $e->tag == "h6")
	// $bold = "bold";
	//
	// if ($size < 0) //formato non supportato
	// return true;
	// elseif ($size >= 18 || ($bold == "bold" && $size >= 14))
	// $threashold = 3;
	// else
	// $threashold = 4.5;
	// $stringa_testo_prova = '';
	//
	// $stringa_testo_prova = "<p>ris: " . $ris . " threashold: " . $threashold
	// . "</p>";
	//
	// if ($ris < $threashold) {
	// return false;
	//
	// } else {
	// return true;
	// }
	//
	// }
	//
	// return true;
	//
	// }
	//
	// //visited links
	// public static function checkColorContrastForVisitedLinkWCAG2AA() {
	//
	// return (BasicChecks::checkLinkContrastWcag2AA ( "visited", "vlink" ));
	// }
	//
	// //active links
	// public static function checkColorContrastForActiveLinkWCAG2AA() {
	//
	// return (BasicChecks::checkLinkContrastWcag2AA ( "active", "alink" ));
	// }
	//
	// //hover links
	// public static function checkColorContrastForHoverLinkWCAG2AA() {
	//
	// return (BasicChecks::checkLinkContrastWcag2AA ( "hover", null ));
	// }
	//
	// //not visited links
	// public static function checkColorContrastForNotVisitedLinkWCAG2AA() {
	//
	// return (BasicChecks::checkLinkContrastWcag2AA ( "link", "link" ));
	// }
	//
	// public static function checkColorContrastForGeneralElementWCAG2AAA() {
	// //WCAG2.0 Contrast check
	// global $background, $foreground;
	// global $global_e, $global_content_dom;
	// global $stringa_testo_prova;
	// $e = $global_e;
	// $content_dom = $global_content_dom;
	//
	// BasicChecks::setCssSelectors ( $content_dom );
	//
	// $background = '';
	// $foreground = '';
	// //elementi testuali
	// if (($e->tag == "div" || $e->tag == "p" || $e->tag == "span" || $e->tag
	// == "strong" || $e->tag == "em" || $e->tag == "q" || $e->tag == "cite" ||
	// $e->tag == "blockquote" || $e->tag == "li" || $e->tag == "dd" || $e->tag
	// == "dt" || $e->tag == "td" || $e->tag == "th" || $e->tag == "h1" ||
	// $e->tag == "h2" || $e->tag == "h3" || $e->tag == "h4" || $e->tag == "h5"
	// || $e->tag == "h6" || $e->tag == "label" || $e->tag == "acronym" ||
	// $e->tag == "abbr" || $e->tag == "code" || $e->tag == "pre") &&
	// BasicChecks::isElementVisible ( $e )) {
	//
	// if (trim ( BasicChecks::remove_children ( $e ) ) == "" || trim (
	// BasicChecks::remove_children ( $e ) ) == "&nbsp;") //l'elemento non
	// contiene testo "visibile": non eseguo il controllo del contrasto
	// return true;
	//
	// $background = BasicChecks::getBackground ( $e );
	// $foreground = BasicChecks::getForeground ( $e );
	//
	// if ($foreground == "" || $foreground == null || $background ==
	// "undetermined")
	// return true;
	//
	// if ($background == "" || $background == null || $background == "-1" ||
	// $background == "undetermined")
	// return true;
	//
	// $background = BasicChecks::convert_color_to_hex ( $background );
	// $foreground = BasicChecks::convert_color_to_hex ( $foreground );
	//
	// $ris = '';
	// $ris = BasicChecks::ContrastRatio ( strtolower ( $background ),
	// strtolower ( $foreground ) );
	// //echo "tag->"; echo $e->tag; echo " bg->"; echo $background; echo
	// " fr->"; echo $foreground; echo " ris="; echo $ris; echo "<br>";
	//
	//
	// $size = BasicChecks::fontSizeToPt ( $e );
	// $bold = BasicChecks::get_p_css ( $e, "font-weight" );
	// if ($e->tag == "h1" || $e->tag == "h2" || $e->tag == "h3" || $e->tag ==
	// "h4" || $e->tag == "h5" || $e->tag == "h6")
	// $bold = "bold";
	//
	//
	// if ($size < 0) //formato non supportato
	// return true;
	// elseif ($size >= 18 || ($bold == "bold" && $size >= 14))
	// $threashold = 4.5;
	// else
	// $threashold = 7;
	//
	// $stringa_testo_prova = '';
	//
	// $stringa_testo_prova = "<p>ris: " . $ris . " threashold: " . $threashold
	// . "</p>";
	//
	// if ($ris < $threashold) {
	//
	// return false;
	// } else {
	//
	// return true;
	// }
	//
	// }
	//
	// return true;
	//
	// }
	//
	// //visited links
	// public static function checkColorContrastForVisitedLinkWCAG2AAA() {
	//
	// return (BasicChecks::checkLinkContrastWcag2AAA ( "visited", "vlink" ));
	// }
	//
	// //active links
	// public static function checkColorContrastForActiveLinkWCAG2AAA() {
	//
	// return (BasicChecks::checkLinkContrastWcag2AAA ( "active", "alink" ));
	// }
	//
	// //hover links
	// public static function checkColorContrastForHoverLinkWCAG2AAA() {
	//
	// return (BasicChecks::checkLinkContrastWcag2AAA ( "hover", null ));
	// }
	//
	// //not visited links
	// public static function checkColorContrastForNotVisitedLinkWCAG2AAA() {
	//
	// return (BasicChecks::checkLinkContrastWcag2AAA ( "link", "link" ));
	// }

}
