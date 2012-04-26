<?php
/************************************************************************/
/* AChecker                                                             */
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
* BasicFunctions.class.php
* Class for basic functions provided to users in writing check functions
*
* @access	public
* @author	Cindy Qi Li
* @package  checker
*/

if (!defined("AC_INCLUDE_PATH")) die("Error: AC_INCLUDE_PATH is not defined.");
include_once(AC_INCLUDE_PATH. 'classes/BasicChecks.class.php');
include_once(AC_INCLUDE_PATH. 'classes/ColorValue.class.php');
include_once(AC_INCLUDE_PATH. 'classes/DAO/LangCodesDAO.class.php');
include_once(AC_INCLUDE_PATH. 'classes/DAO/ChecksDAO.class.php');

class BasicFunctions {
	
	/**
	* check if associated label of $global_e has text
	* return true if has, otherwise, return false
	*/
	public static function associatedLabelHasText()
	{
		global $global_e, $global_content_dom;
		
		// 1. The element $global_e has a "title" attribute
		if (trim($global_e->attr["title"]) <> "") return true;

		// 2. The element $global_e is contained by a "label" element
		if ($global_e->parent()->tag == "label")
		{
			$pattern = "/(.*)". preg_quote($global_e->outertext, '/') ."/";
			preg_match($pattern, $global_e->parent->innertext, $matches);
			if (strlen(trim($matches[1])) > 0) return true;
		}
		
		// 3. The element $global_e has an "id" attribute value that matches the "for" attribute value of a "label" element
		$input_id = $global_e->attr["id"];
		
		if ($input_id == "") return false;  // attribute "id" must exist
		
		foreach ($global_content_dom->find("label") as $e_label)
		{
			if ($e_label->attr["for"] == $input_id)
			{
				// label contains text
				if (trim($e_label->plaintext) <> "") return true;
				
				// label contains an image with alt text
				foreach ($e_label->children as $e_label_child)
					if ($e_label_child->tag == "img" && strlen(trim($e_label_child->attr["alt"])) > 0)
						return true;
			}
		}
		
		return false;
	}

	/**
	* return the length of the trimed value of specified attribute
	*/
	public static function getAttributeTrimedValueLength($attr)
	{
		global $global_e;
		
		return strlen(trim($global_e->attr[$attr]));
	}
		
	/**
	* return the value of the specified attribute
	*/
	public static function getAttributeValue($attr)
	{
		global $global_e;
		
		return trim($global_e->attr[$attr]);
	}

	/**
	* return the value of specified attribute as a number
	*/
	public static function getAttributeValueAsNumber($attr)
	{
		global $global_e;
		
		return intval(trim($global_e->attr[$attr]));
	}
	
	/**
	* return the value of the specified attribute in lower case
	*/
	public static function getAttributeValueInLowerCase($attr)
	{
		global $global_e;
		
		return strtolower(trim($global_e->attr[$attr]));
	}

	/**
	* return the length of the value of specified attribute
	*/
	public static function getAttributeValueLength($attr)
	{
		global $global_e;
		
		return strlen($global_e->attr[$attr]);
	}
		
	/**
	* return html tag of the first child
	*/
	public static function getFirstChildTag()
	{
		global $global_e;
		
		$children = $global_e->children();

		return $children[0]->tag;
	}

	/**
	* return the width of the image. return false if the image is not accessible or at failure
	*/
	public static function getImageWidthAndHeight($attr)
	{
		global $global_e, $base_href, $uri, $global_array_image_sizes;
		
		$file = BasicChecks::getFile($global_e->attr[$attr], $base_href, $uri);
		$file_size_checked = false;
		
		// Check if the image has already been fetched.
		// Since the remote fetching is the bottle neck that slows down the validation,
		// $global_array_image_sizes is to save width/height of all the fetched images. 
		if (is_array($global_array_image_sizes)) {
			foreach ($global_array_image_sizes as $image=>$info) {
				if ($image == $file) {
					$file_size_checked = true;
					if (!$info["is_exist"]) {
						return false;
					} else {
						return array($info["width"], $info["height"]);
					}
				}
			}
		} 

		if (!$file_size_checked) {
			$dimensions = @getimagesize($file);
			
			if (is_array($dimensions)) {
				$global_array_image_sizes[$file] = array("is_exist"=>true, "width"=>$dimensions[0], "height"=>$dimensions[1]);
				return array($dimensions[0], $dimensions[1]);
			} else {
				$global_array_image_sizes[] = array($file=>array("is_exist"=>false, "width"=>NULL, "height"=>NULL));
				return false;
			}
		}
	}

	/**
	* return the trimed value of inner text
	*/
	public static function getInnerText()
	{
		global $global_e;
		
		return trim($global_e->innertext);
	}
		
	/**
	* return the length of the trimed inner text of specified attribute
	*/
	public static function getInnerTextLength()
	{
		global $global_e;
		
		return strlen(trim($global_e->innertext));
	}
		
	/**
	* return language code that is defined in the given html
	* return language code
	*/
	public static function getLangCode()
	{
		global $global_content_dom;
		
		// get html language
		$e_htmls = $global_content_dom->find("html");

		foreach ($e_htmls as $e_html)
		{
			if (isset($e_html->attr["xml:lang"])) 
			{
				$lang = trim($e_html->attr["xml:lang"]);
				break;
			}
			else if (isset($e_html->attr["lang"]))
			{
				$lang = trim($e_html->attr["lang"]);
				break;
			}
		}
		
		return BasicChecks::cutOutLangCode($lang);
	}
	
	/**
	* return last 4 characters. Usually used to get file extension 
	*/
	public static function getLast4CharsFromAttributeValue($attr)
	{
		global $global_e;
		
		return substr(trim($global_e->attr[$attr]), -4);
	}

	/**
	* scan thru all the children and return the length of attribute value that 
	* the specified html tag appears in the first children 
	*/
	public static function getLengthOfAttributeValueWithGivenTagInChildren($tag, $attr)
	{
		global $global_e;
		
		$len = 0;
		
		foreach ($global_e->children() as $child)
			if ($child->tag == $tag) $len = strlen(trim($child->attr[$attr]));
				
		return $len;
	}

	/**
	* scan thru all the children and return the length of attribute value that 
	* the specified html tag appears in the first children 
	*/
	public static function getLowerCaseAttributeValueWithGivenTagInChildren($tag, $attr)
	{
		global $global_e;
		
		foreach ($global_e->children() as $child)
			if ($child->tag == $tag) $value = strtolower(trim($child->attr[$attr]));
				
		return $value;
	}

	/**
	* scan thru all the children and return the length of plain text that 
	* the specified html tag appears in the first children 
	*/
	public static function getLowerCasePlainTextWithGivenTagInChildren($tag)
	{
		global $global_e;
		
		foreach ($global_e->children() as $child)
			if ($child->tag == $tag) $value = strtolower(trim($child->plaintext));
				
		return $value;
	}

	/**
	Check if the luminosity contrast ratio between $color1 and $color2 is at least 5:1
	Input: color values to compare: $color1 & $color2. Color value can be one of: rgb(x,x,x), #xxxxxx, colorname
	Return: true or false
	*/
	public static function getLuminosityContrastRatio($color1, $color2)
	{
		$color1 = new ColorValue($color1);
		$color2 = new ColorValue($color2);
		
		if (!$color1->isValid() || !$color2->isValid())
			return true;
		
		$linearR1 = $color1->getRed()/255;
		$linearG1 = $color1->getRed()/255;
		$linearB1 = $color1->getRed()/255;

		$lum1 = (pow ($linearR1, 2.2) * 0.2126) +
			(pow ($linearG1, 2.2) * 0.7152) +
			(pow ($linearB1, 2.2) * 0.0722) + .05;
			
		$linearR2 = $color2->getRed()/255;
		$linearG2 = $color2->getRed()/255;
		$linearB2 = $color2->getRed()/255;

		$lum2 = (pow ($linearR2, 2.2) * 0.2126) +
			(pow ($linearG2, 2.2) * 0.7152) +
			(pow ($linearB2, 2.2) * 0.0722) + .05;
			
		$ratio = max ($lum1, $lum2) / min($lum1, $lum2);

		// round the ratio to 2 decimal places
		$factor = pow(10,2);

		// Shift the decimal the correct number of places
		// to the right.
		$val = $ratio * $factor;

		// Round to the nearest integer.
		$tmp = round($val);

		// Shift the decimal the correct number of places back to the left.
		$ratio2 = $tmp / $factor;

		return $ratio2;
	}
	
	/**
	* return the html tag of the next sibling
	*/
	public static function getNextSiblingAttributeValueInLowerCase($attr)
	{
		global $global_e;
		
		return strtolower(trim($global_e->next_sibling()->attr[$attr]));
	}

	/**
	* return the inner text of the next sibling
	* for example: if next sibling is "<a href="rex.html">[d]</a></p>", this function returns "[d]"
	*/
	public static function getNextSiblingInnerText()
	{
		global $global_e;
		
		return $global_e->next_sibling()->innertext;
	}

	/**
	* return the html tag of the next sibling
	*/
	public static function getNextSiblingTag()
	{
		global $global_e;
		
		return trim($global_e->next_sibling()->tag);
	}

	/**
	* scan thru all the children and return the number of times that the specified html tag appears in all children 
	*/
	public static function getNumOfTagInChildren($tag)
	{
		global $global_e;
		
		$num = 0;
		
		foreach ($global_e->children() as $child)
		{
			if ($child->tag == $tag) $num++;
		}
		
		return $num;
	}

	/**
	* scan thru all the children, check if the given html tag exists and its inner text has content
	* return true if the given html tag exists and its inner text has content. otherwise, return false
	*/
	public static function getNumOfTagInChildrenWithInnerText($tag)
	{
		global $global_e;
		
		$num = 0;
		
		foreach ($global_e->children() as $child)
		{
			if ($child->tag == $tag && strlen(trim($child->innertext)) > 0)
				$num++;
		}
		return $num;
	}

	/**
	* return the number of times that the specified html tag appears in the content 
	*/
	public static function getNumOfTagInWholeContent($tag)
	{
		global $global_content_dom;
		
		return count($global_content_dom->find($tag));
	}

	/**
	* scan thru recursively of all the children and return the number of times that the specified html tag 
	* appears in all children 
	*/
	public static function getNumOfTagRecursiveInChildren($tag)
	{
		global $global_e;
		
		$num = 0;
		
		foreach($global_e->children() as $child)
			if ($child->tag == $tag) $num++;
			else $num += BasicChecks::getNumOfTagRecursiveInChildren($child, $tag);
				
		return $num;
	}

	/**
	* return the tag of the parent html tag
	*/
	public static function getParentHTMLTag()
	{
		global $global_e;
		
		return $global_e->parent()->tag;
	}
	
	/**
	* return the length of the trimed plain text of specified attribute
	*/
	public static function getPlainTextInLowerCase()
	{
		global $global_e;
		
		return strtolower(trim($global_e->plaintext));
	}
		
	/**
	* return the length of the trimed plain text of specified attribute
	*/
	public static function getPlainTextLength()
	{
		global $global_e;
		
		return strlen(trim($global_e->plaintext));
	}
		
	/**
	* Returns the portion of string  specified by the start  and length  parameters.
	* A wrapper on php function substr
	*/
	public static function getSubstring($string, $start, $length)
	{
		return substr($string, $start, $length);
	}
	
	/**
	* check if current element has associated label
	* return true if has, otherwise, return false
	*/
	public static function hasAssociatedLabel()
	{
		global $global_e, $global_content_dom;
		
		// 1. The element $global_e is contained by a "label" element
		// 2. The element $global_e has a "title" attribute
		if ($global_e->parent()->tag == "label" || isset($global_e->attr["title"])) return true;
		
		// 3. The element $global_e has an "id" attribute value that matches the "for" attribute value of a "label" element
		$input_id = $global_e->attr["id"];
		
		if ($input_id == "") return false;  // attribute "id" must exist
		
		foreach ($global_content_dom->find("label") as $global_e_label)
		  if (strtolower(trim($global_e_label->attr["for"])) == strtolower(trim($global_e->attr["id"])))
			return true;
	  
	  return false;
	}

	/**
	* check if the element has attribute $attr defined
	* return true if has, otherwise, return false
	*/
	public static function hasAttribute($attr)
	{
		global $global_e;
		
		return isset($global_e->attr[$attr]);
	}
	
	/**
	* Check recursively if there are duplicate $attr defined in children of $global_e
	* set global var hasDuplicateAttribute to true if there is, otherwise, set it to false
	*/
	public static function hasDuplicateAttribute($attr)
	{
		global $has_duplicate_attribute, $global_e;

		$has_duplicate_attribute = false;
		$id_array = array();
		
		BasicChecks::hasDuplicateAttribute($global_e, $attr, $id_array);

		return $has_duplicate_attribute;
	}
	
	/**
	 * Check if form has "fieldset" and "legend" to group multiple checkbox buttons.
	 * @return true if has, otherwise, false
	 */
	public static function hasFieldsetOnMultiCheckbox()
	{
		global $global_e;
		
		// find if there are radio buttons with same name
		$children = $global_e->children();
		$num_of_children = count($children);
		
		foreach ($children as $i => $child)
		{
			if (strtolower(trim($child->attr["type"])) == "checkbox")
			{
				$this_name = strtolower(trim($child->attr["name"]));
				
				for($j=$i+1; $j <=$num_of_children; $j++)
					// if there are radio buttons with same name,
					// check if they are contained in "fieldset" and "legend" elements
					if (strtolower(trim($children[$j]->attr["name"])) == $this_name)
						if (BasicChecks::hasParent($global_e, "fieldset"))
							return BasicChecks::hasParent($global_e, "legend");
						else
							return false;
			}
			else
				return BasicChecks::hasFieldsetOnMultiCheckbox($child);
		}
		
		return true;
	}
	
	/**
	 * Check if the luminosity contrast ratio between $color1 and $color2 is at least 5:1
	 * Input: color values to compare: $color1 & $color2. Color value can be one of: rgb(x,x,x), #xxxxxx, colorname
	 * Return: true or false
	 */
	public static function hasGoodContrastWaiert($color1, $color2)
	{
		include_once (AC_INCLUDE_PATH . "classes/ColorValue.class.php");

		$color1 = new ColorValue($color1);
		$color2 = new ColorValue($color2);
		
		if (!$color1->isValid() || !$color2->isValid())
			return true;
		
		$colorR1 = $color1->getRed();
		$colorG1 = $color1->getGreen();
		$colorB1 = $color1->getBlue();
		
		$colorR2 = $color2->getRed();
		$colorG2 = $color2->getGreen();
		$colorB2 = $color2->getBlue();

		$brightness1 = (($colorR1 * 299) + 
							($colorG1 * 587) + 
							($colorB1 * 114)) / 1000;

		$brightness2 = (($colorR2 * 299) + 
							($colorG2 * 587) + 
							($colorB2 * 114)) / 1000;

		$difference = 0;
		if ($brightness1 > $brightness2)
		{
			$difference = $brightness1 - $brightness2;
		}
		else 
		{
			$difference = $brightness2 - $brightness1;
		}

		if ($difference < 125)
		{
			return false;
		}

		// calculate the color difference
		$difference = 0;
		// red
		if ($colorR1 > $colorR2)
		{
			$difference = $colorR1 - $colorR2;
		}
		else
		{
			$difference = $colorR2 - $colorR1;
		}

		// green
		if ($colorG1 > $colorG2)
		{
			$difference += $colorG1 - $colorG2;
		}
		else
		{
			$difference += $colorG2 - $colorG1;
		}

		// blue
		if ($colorB1 > $colorB2)
		{
			$difference += $colorB1 - $colorB2;
		}
		else
		{
			$difference += $colorB2 - $colorB1;
		}

		return ($difference > 499);
	}

	/**
	 * Check if the table contains more than one row or either row or column headers.
	 * @return true if contains, otherwise, false
	 */
	public static function hasIdHeaders()
	{
		global $global_e;
		
		// check if the table contains both row and column headers
		list($num_of_header_rows, $num_of_header_cols) = BasicChecks::getNumOfHeaderRowCol($global_e);
		
		// if table has more than 1 header rows or has both header row and header column,
		// check if all "th" has "id" attribute defined and all "td" has "headers" defined
		if ($num_of_header_rows > 1 || ($num_of_header_rows > 0 && $num_of_header_cols > 0))
		{
			foreach ($global_e->find("th") as $th)
				if (!isset($th->attr["id"])) return false;

			foreach ($global_e->find("td") as $td)
				if (!isset($td->attr["headers"])) return false;
		}
				
		return true;
	}
	
	/**
	 * Check if the table contains more than one row or either row or column headers.
	 * @return true if contains, otherwise, false
	 */
	public static function hasLinkChildWithText($searchStrArray)
	{
		global $global_e;
		
		foreach ($global_e->children() as $child)
		{
			if ($child->tag == 'a' && BasicChecks::inSearchString($child->attr['href'], $searchStrArray))
			{
				return true;
			}
		}
				
		return false;
	}
	
	/**
	* Check recursively to find if $global_e has a parent with tag $parent_tag
	* return true if found, otherwise, false
	*/
	public static function hasParent($parent_tag)
	{
		global $global_e;
		
		if ($global_e->parent() == NULL) return false;
		
		if ($global_e->parent()->tag == $parent_tag)
			return true;
		else
			return BasicChecks::hasParent($global_e->parent(), $parent_tag);
	}
	
	/**
	 * Check if the table contains both row and column headers.
	 * @return true if contains, otherwise, false
	 */
	public static function hasScope()
	{
		global $global_e;
		
		// check if the table contains both row and column headers
		list($num_of_header_rows, $num_of_header_cols) = BasicChecks::getNumOfHeaderRowCol($global_e);
		
		if ($num_of_header_rows > 0 && $num_of_header_cols > 0)
		{
			foreach ($global_e->find("th") as $th)
				if (!isset($th->attr["scope"])) return false;
		}
		
		return true;
	}

	/**
	* check if the tag plain text contains a line that is separated by more than one tab or vertical line
	* return true if yes, otherwise, false
	*/
	public static function hasTabularInfo()
	{
		global $global_e;

		$text = $global_e->plaintext;
		
		return (preg_match("/.*\t.+\t.*/", $text) || preg_match("/.*\|.+\|.*/", $text));
	}
	
	/**
	* check if there's given tag in children.
	* return true if has, otherwise, false
	*/
	public static function hasTagInChildren($tag)
	{
		global $global_e;

		$tags = $global_e->find($tag);
		
		return (count($tags) > 0);
	}
	
	/**
	* Check if there's text in between <a> elements
	* return true if there is, otherwise, false
	*/
	public static function hasTextInBtw()
	{
		global $global_e;
		
		$next_sibling = $global_e->next_sibling();
		
		if ($next_sibling->tag <> "a") return true;
		
		// check if there's other text in between $global_e and its next sibling
		$pattern = "/". preg_quote($global_e->outertext, '/')."(.*)". preg_quote($next_sibling->outertext, '/') ."/";
		preg_match($pattern, $global_e->parent->innertext, $matches);

		return (strlen(trim($matches[1])) > 0);
	}
	
	/**
	* check if there's child with tag named $childTag, in which the value of attribute $childAttribute equals one of the 
	* values in given $valueArray
	* return true if has, otherwise, false
	*/
	public static function hasTextInChild($childTag, $childAttribute, $valueArray)
	{
		global $global_e;
		
		// if no <link> element is defined or "rel" in all <link> elements are not "alternate" or href is not defined, return false
		foreach ($global_e->children() as $child)
		{
			if ($child->tag == $childTag)
			{
				$rel_val = strtolower(trim($child->attr[$childAttribute]));
				
				if (in_array($rel_val, $valueArray))
					return true;
			}
		}
		
		return false;
	}
	
	/**
	* This function for now is solely used for attribute "usemap", check id 13
	*/
	public static function hasTextLinkEquivalents($attr)
	{
		global $global_e, $global_content_dom;
		
		$map_name = substr($global_e->attr[$attr], 1);  // remove heading #
			
		// find definition of <map> with $map_name
		$map_found = false;
		foreach($global_content_dom->find("map") as $map)
		{
			if ($map->attr["name"] == $map_name)
			{
				$map_found = true;
				$area_hrefs = array();

				foreach ($map->children() as $map_child)
				{
					if ($map_child->tag == "area")
						array_push($area_hrefs, array("href"=>trim($map_child->attr["href"]), "found" => false));
				}
				
				break;  // stop at finding <map> with $map_name
			}
		}
		
		// return false <map> with $map_name is not defined
		if (!$map_found) return false; 
		
		foreach($global_content_dom->find("a") as $a)
		{
			foreach ($area_hrefs as $i => $area_href)
				if ($a->attr["href"] == $area_href["href"])
				{
					$area_hrefs[$i]["found"] = true;
					break;
				}
		}

		$all_href_found = true;
		foreach ($area_hrefs as $area_href)
			if (!$area_href["found"])
			{
				$all_href_found = false;
				break;
			}
		
		// return false when not all area href are defined
		if (!$all_href_found) return false;
		
		return true;
	} 
	
	/**
	* check if window.onload is contained in tag "script".
	* return true if has, otherwise, false
	*/
	public static function hasWindowOpenInScript()
	{
		global $global_content_dom;

		$tags = $global_content_dom->find('script');
		if (is_array($tags))
		{
			foreach ($tags as $tag)
			{
				if (stristr($tag->innertext, 'window.onload')) return true;
			}
		}
		return false;
	}
	
	/**
	 * Check if the html document is validated 
	 * return true if validated, otherwise, false
	 */
	public static function htmlValidated()
	{
		global $htmlValidator;

		if (!isset($htmlValidator)) return false;
		
		return ($htmlValidator->getNumOfValidateError() == 0);
	}

	/**
	* check if the inner text is in one of the search string defined in checks.search_str
	* return true if in, otherwise, return false 
	*/
	public static function isAttributeValueInSearchString($attr)
	{
		global $global_e, $global_check_id;
		
		return BasicChecks::isTextInSearchString(trim($global_e->attr[$attr]), $global_check_id, $global_e);
	}

	/**
	* Makes a guess about the table type.
	* Returns true if this should be a data table, false if layout table.
	*/
	public static function isDataTable()
	{
		global $is_data_table, $global_e;
		
		$is_data_table = false;
		BasicChecks::isDataTable($global_e);
		
		return $is_data_table;
	}
	
	/**
	* check if the inner text is in one of the search string defined in checks.search_str
	* return true if in, otherwise, return false 
	*/
	public static function isInnerTextInSearchString()
	{
		global $global_e, $global_check_id;
		
		return BasicChecks::isTextInSearchString($global_e->innertext, $global_check_id, $global_e);
	}

	/**
	* check if the next tag, is not in given array $notInArray
	* return true if not in, otherwise, return false
	*/
	public static function isNextTagNotIn($notInArray)
	{
		global $header_array, $global_e;
		
		if (!is_array($header_array)) return true;
		
		// find the next header after $global_e->linenumber, $global_e->colnumber
		foreach ($header_array as $e)
		{
			if ($e->linenumber > $global_e->linenumber || ($e->linenumber == $global_e->linenumber && $e->colnumber > $global_e->colnumber))
			{
				if (!isset($next_header)) 
					$next_header = $e;
				else if ($e->linenumber < $next_header->line_number || ($e->linenumber == $next_header->line_number && $e->colnumber > $next_header->col_number))
					$next_header = $e;
			}
		}

		if (isset($next_header) && !in_array($next_header->tag, $notInArray))
			return false;
		else
			return true;
	}
	
	/**
	* check if the plain text is in one of the search string defined in checks.search_str
	* return true if in, otherwise, return false 
	*/
	public static function isPlainTextInSearchString()
	{
		global $global_e, $global_check_id;
		
		return BasicChecks::isTextInSearchString($global_e->plaintext, $global_check_id, $global_e);
	}

	/**
	* Check radio button groups are marked using "fieldset" and "legend" elements
	* Return: use global variable $is_radio_buttons_grouped to return true (grouped properly) or false (not grouped)
	*/
	public static function isRadioButtonsGrouped()
	{
		global $global_e;
		
		$radio_buttons = array();
		
		foreach ($global_e->find("input") as $e_input)
		{
			if (strtolower(trim($e_input->attr["type"])) == "radio")
				array_push($radio_buttons, $e_input);
		}

		for ($i=0; $i < count($radio_buttons); $i++)
		{
			for ($j=0; $j < count($radio_buttons); $j++)
			{
				if ($i <> $j && strtolower(trim($radio_buttons[$i]->attr["name"])) == strtolower(trim($radio_buttons[$j]->attr["name"]))
				    && !BasicChecks::hasParent($radio_buttons[$i], "fieldset") && !BasicChecks::hasParent($radio_buttons[$i], "legend")) {
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
	public static function isSubmitLabelDifferent()
	{
		global $global_e;
		
		$submit_labels = array();
		
		foreach ($global_e->find("form") as $form)
		{
			foreach ($form->find("input") as $button)
			{
				$button_type = strtolower(trim($button->attr["type"]));

				if ($button_type == "submit" || $button_type == "image")
				{
					if ($button_type == "submit")
						$button_value = strtolower(trim($button->attr["value"]));
					
					if ($button_type == "image")
						$button_value = strtolower(trim($button->attr["alt"]));
					
					if (in_array($button_value, $submit_labels)) return false;
					else array_push($submit_labels, $button_value);
				}
			}
		}

		return true;
	}
	
	/**
	* check if the element content is marked with the html tags given in $htmlTagArray
	* return true if valid, otherwise, return false
	*/
	public static function isTextMarked($htmlTagArray)
	{
		global $global_e;
		
		$children = $global_e->children();
		
		if (count($children) == 1)
		{
			$child = $children[0];
			
			$tag = $child->tag;

			if (in_array($tag, $htmlTagArray) && $child->plaintext == $global_e->plaintext)
				return false;
		}
		return true;
	}
	
	/**
	* check if value in the given attribute is a valid language code
	* return true if valid, otherwise, return false
	*/
	public static function isValidLangCode()
	{
		global $global_e, $global_content_dom;
		
		$is_text_content = false;
		$is_application_content = false;
		
		$metas = $global_content_dom->find("meta");
		if (is_array($metas))
		{
			foreach ($metas as $meta)
			{
				if (stristr($meta->attr['content'], 'text/html')) $is_text_content = true;
				if (stristr($meta->attr['content'], 'application/xhtml+xml')) $is_application_content = true;
			}
		}
		$doctypes = $global_content_dom->find("doctype");
		
		if (count($doctypes) == 0) return false;
		
		foreach ($doctypes as $doctype)
		{
			foreach ($doctype->attr as $doctype_content => $garbage)
			{
				// If the content is HTML, check the value of the html element's lang attribute
				if (stristr($doctype_content, "HTML") && !stristr($doctype_content, "XHTML")) {
					return BasicChecks::isValidLangCode(trim($global_e->attr['lang']));
				}
				
				// If the content is XHTML 1.0, or any version of XHTML served as "text/html", 
				// check the values of both the html element's lang attribute and xml:lang attribute.
				// Note: both lang attributes must be set to the same value.
				if (stristr($doctype_content, "XHTML 1.0") || (stristr($doctype_content, " XHTML ") && $is_text_content))
				{
					return (BasicChecks::isValidLangCode(trim($global_e->attr['lang'])) && 
					        BasicChecks::isValidLangCode(trim($global_e->attr['xml:lang'])) &&
					        trim($global_e->attr['lang']) == trim($global_e->attr['xml:lang']));
				}
				else if (stristr($doctype_content, " XHTML ") && $is_application_content)
				{
					return BasicChecks::isValidLangCode(trim($global_e->attr['xml:lang']));
				}
			}
		}
		return true;
	}

	/*
	 * Validate if the <code>dir</code> attribute's value is "rtl" for languages 
	 * that are read left-to-right or "ltr" for languages that are read right-to-left.
	 * return true if it's valid, otherwise, false
	 */
	public static function isValidRTL()
	{
		global $global_e;
		
		if (isset($global_e->attr["lang"]))
			$lang_code = trim($global_e->attr["lang"]);
		else
			$lang_code = trim($global_e->attr["xml:lang"]);

		// return no error if language code is not specified
		if (!BasicChecks::isValidLangCode($lang_code)) return true;
		
		$rtl_lang_codes = BasicChecks::getRtlLangCodes();

		if (in_array($lang_code, $rtl_lang_codes))
			// When these 2 languages, "dir" attribute must be set and set to "rtl"
			return (strtolower(trim($global_e->attr["dir"])) == "rtl");
		else
			return (!isset($global_e->attr["dir"]) || strtolower(trim($global_e->attr["dir"])) == "ltr");
	}
	
	/**
	* This function validates html "doctype"
	* return true if doctype is valid, otherwise, false
	*/
	public static function validateDoctype()
	{
		global $global_content_dom;
		
		$doctypes = $global_content_dom->find("doctype");

		if (count($doctypes) == 0) return false;
		
		foreach ($doctypes as $doctype)
		{
			foreach ($doctype->attr as $doctype_content => $garbage)
				if (stristr($doctype_content, "-//W3C//DTD HTML 4.01//EN") ||
						stristr($doctype_content, "-//W3C//DTD HTML 4.0//EN") ||
						stristr($doctype_content, "-//W3C//DTD XHTML 1.0 Strict//EN"))
					return true;
		}
		return false;
	}
		
	//MB
	//Color Contrast Functions (checks 301 - 310)
	public static function checkColorContrastForGeneralElementWCAG2AA() {
		//WCAG2.0 Contrast check
		global $background, $foreground;
		global $global_e, $global_content_dom;
		global $stringa_testo_prova;
		$e = $global_e;
		$content_dom = $global_content_dom;
		
		BasicChecks::setCssSelectors ( $content_dom );
		
		$background = '';
		$foreground = '';
		//elementi testuali
		if (($e->tag == "div" || $e->tag == "p" || $e->tag == "span" || $e->tag == "strong" || $e->tag == "em" || $e->tag == "q" || $e->tag == "cite" || $e->tag == "blockquote" || $e->tag == "li" || $e->tag == "dd" || $e->tag == "dt" || $e->tag == "td" || $e->tag == "th" || $e->tag == "h1" || $e->tag == "h2" || $e->tag == "h3" || $e->tag == "h4" || $e->tag == "h5" || $e->tag == "h6" || $e->tag == "label" || $e->tag == "acronym" || $e->tag == "abbr" || $e->tag == "code" || $e->tag == "pre") && BasicChecks::isElementVisible ( $e )) {
			 //l'elemento non contiene testo "visibile": non eseguo il controllo del contrasto
			if (trim ( BasicChecks::remove_children ( $e ) ) == "" || trim ( BasicChecks::remove_children ( $e ) ) == "&nbsp;"){
				return true;
			}
			
			$background = BasicChecks::getBackground ( $e );
			$foreground = BasicChecks::getForeground ( $e );
			
			if ($foreground == "" || $foreground == null || $background == "undetermined") {
				return true;
			}
			
			if ($background == "" || $background == null || $background == "-1" || $background == "undetermined") {
				return true;
			}
			
			$background = BasicChecks::convert_color_to_hex ( $background );
			$foreground = BasicChecks::convert_color_to_hex ( $foreground );
			
			$ris = BasicChecks::ContrastRatio ( strtolower ( $background ), strtolower ( $foreground ) );
			//echo "tag->"; echo $e->tag; echo " bg->"; echo $background; echo " fr->"; echo $foreground; echo " ris="; echo $ris; echo "<br>";	
			
			$size = BasicChecks::fontSizeToPt ( $e );
			$bold = BasicChecks::get_p_css ( $e, "font-weight" );
			if ($e->tag == "h1" || $e->tag == "h2" || $e->tag == "h3" || $e->tag == "h4" || $e->tag == "h5" || $e->tag == "h6")
				$bold = "bold";

			if ($size < 0) //formato non supportato
				return true;
			elseif ($size >= 18 || ($bold == "bold" && $size >= 14))
				$threashold = 3;
			else
				$threashold = 4.5;
			$stringa_testo_prova = '';
			
			$stringa_testo_prova = "<p>ris: " . $ris . " threashold: " . $threashold . "</p>";
			
			if ($ris < $threashold) {
				return false;
			
			} else {
				return true;
			}
		
		}
		
		return true;
	
	}
	
	//visited links
	public static function checkColorContrastForVisitedLinkWCAG2AA() {
		
		return (BasicChecks::checkLinkContrastWcag2AA ( "visited", "vlink" ));
	}
	
	//active links
	public static function checkColorContrastForActiveLinkWCAG2AA() {

		return (BasicChecks::checkLinkContrastWcag2AA ( "active", "alink" ));
	}
	
	//hover links
	public static function checkColorContrastForHoverLinkWCAG2AA() {
		
		return (BasicChecks::checkLinkContrastWcag2AA ( "hover", null ));
	}
	
	//not visited links
	public static function checkColorContrastForNotVisitedLinkWCAG2AA() {
		
		return (BasicChecks::checkLinkContrastWcag2AA ( "link", "link" ));
	}
	
	public static function checkColorContrastForGeneralElementWCAG2AAA() {
		//WCAG2.0 Contrast check
		global $background, $foreground;
		global $global_e, $global_content_dom;
		global $stringa_testo_prova;
		$e = $global_e;
		$content_dom = $global_content_dom;
		
		BasicChecks::setCssSelectors ( $content_dom );
		
		$background = '';
		$foreground = '';
		//elementi testuali
		if (($e->tag == "div" || $e->tag == "p" || $e->tag == "span" || $e->tag == "strong" || $e->tag == "em" || $e->tag == "q" || $e->tag == "cite" || $e->tag == "blockquote" || $e->tag == "li" || $e->tag == "dd" || $e->tag == "dt" || $e->tag == "td" || $e->tag == "th" || $e->tag == "h1" || $e->tag == "h2" || $e->tag == "h3" || $e->tag == "h4" || $e->tag == "h5" || $e->tag == "h6" || $e->tag == "label" || $e->tag == "acronym" || $e->tag == "abbr" || $e->tag == "code" || $e->tag == "pre") && BasicChecks::isElementVisible ( $e )) {
			
			if (trim ( BasicChecks::remove_children ( $e ) ) == "" || trim ( BasicChecks::remove_children ( $e ) ) == "&nbsp;") //l'elemento non contiene testo "visibile": non eseguo il controllo del contrasto
				return true;
			
			$background = BasicChecks::getBackground ( $e );
			$foreground = BasicChecks::getForeground ( $e );
			
			if ($foreground == "" || $foreground == null || $background == "undetermined")
				return true;
			
			if ($background == "" || $background == null || $background == "-1" || $background == "undetermined")
				return true;
			
			$background = BasicChecks::convert_color_to_hex ( $background );
			$foreground = BasicChecks::convert_color_to_hex ( $foreground );
			
			$ris = '';
			$ris = BasicChecks::ContrastRatio ( strtolower ( $background ), strtolower ( $foreground ) );
			//echo "tag->"; echo $e->tag; echo " bg->"; echo $background; echo " fr->"; echo $foreground; echo " ris="; echo $ris; echo "<br>";	
			

			$size = BasicChecks::fontSizeToPt ( $e );
			$bold = BasicChecks::get_p_css ( $e, "font-weight" );
			if ($e->tag == "h1" || $e->tag == "h2" || $e->tag == "h3" || $e->tag == "h4" || $e->tag == "h5" || $e->tag == "h6")
				$bold = "bold";
			

			if ($size < 0) //formato non supportato
				return true;
			elseif ($size >= 18 || ($bold == "bold" && $size >= 14))
				$threashold = 4.5;
			else
				$threashold = 7;
			
			$stringa_testo_prova = '';
			
			$stringa_testo_prova = "<p>ris: " . $ris . " threashold: " . $threashold . "</p>";
			
			if ($ris < $threashold) {
				
				return false;
			} else {
				
				return true;
			}
		
		}
		
		return true;
	
	}
	
	//visited links
	public static function checkColorContrastForVisitedLinkWCAG2AAA() {
		
		return (BasicChecks::checkLinkContrastWcag2AAA ( "visited", "vlink" ));
	}
	
	//active links
	public static function checkColorContrastForActiveLinkWCAG2AAA() {

		return (BasicChecks::checkLinkContrastWcag2AAA ( "active", "alink" ));
	}
	
	//hover links
	public static function checkColorContrastForHoverLinkWCAG2AAA() {
		
		return (BasicChecks::checkLinkContrastWcag2AAA ( "hover", null ));
	}
	
	//not visited links
	public static function checkColorContrastForNotVisitedLinkWCAG2AAA() {
		
		return (BasicChecks::checkLinkContrastWcag2AAA ( "link", "link" ));
	}	

}
?>