package com.dotmarketing.util.diff.html.dom.helper;

import java.util.Arrays;
import java.util.HashMap;

import org.xml.sax.Attributes;
/**
 * Map is used to store DOM tag attribute names and values. 
 * This map pays no attention to sequence of attributes.
 * @author karol
 */
public class AttributesMap extends HashMap<String, String> {

	/**
	 * constant for serialization compatibility check
	 */
	private static final long serialVersionUID = -6165499554111988049L;
	
	/**
	 * constant for style attribute name, 
	 * We need to treat this attribute differently to consider 
	 * HTML elements equal if they only differ in style rules order
	 * (in order of style rules inside style attribute value)<br>
	 */
	protected static final String STYLE_ATTR = "style";
	
	/**
	 * constant for class attribute name.
	 * We need to treat this attribute differently to consider 
	 * HTML elements equal if they only differ in order of
	 * multiple CSS classes inside the class attribute value.
	 */
	protected static final String CLASS_ATTR = "class";

	/**
	 * just a constant to use in processing attribute.
	 */
	protected static final String SPACE = " ";
	
	/**
	 * just a constant to use to ignore symbols that
	 * do not affect real attributes value
	 */
	protected static final String NL_TAB_REGEXP = "\\n|\\t";

	public AttributesMap() {
		super();
	}

	/**
	 * This constructor converts all the attribute names to lower case.
	 * @param attributes
	 */
	public AttributesMap(Attributes attributes) {
		super();
		for (int i = 0; i < attributes.getLength(); i++) {
			put(attributes.getQName(i).toLowerCase(), attributes.getValue(i));
		}
	}
	
	/**
	 * this method returns true, 
	 * if two maps have the same set of keys and values assigned to these keys.
	 * Or if the difference is only in the "style" or "class" attributes values,
	 * which are however equivalent. For the "class" attribute that means 
	 * the values consist of the same set of classes, but not necessarily in the 
	 * same order, and for the "style" attribute that means that the values
	 * consist of the same rules (css property : value pairs) but without
	 * order consideration.
	 */
	@Override
	public boolean equals(Object obj) {
		boolean equals = false;
		if (obj instanceof AttributesMap) {
			AttributesMap attributesMap = (AttributesMap) obj;
			
			if(size() == attributesMap.size()){
				equals = true;
				
				for(String attrib:keySet()){
					String localValue = get(attrib);
					String externalValue = attributesMap.get(attrib);
					
					if(externalValue == null || !externalValue.equals(localValue)){
						if (attrib.equals(STYLE_ATTR)){
							if (equivalentStyles(localValue, externalValue)){
								continue;
							}
						} else if (attrib.equals(CLASS_ATTR)){
							if (sameClassSet(localValue, externalValue)){
								continue;
							}
						}
						equals = false;
						break;
					}
				}
			}
		}
		return equals;
	}
	
	@Override
	public int hashCode(){
		int simple = 19;
		int result = 0;
		for (String attr: keySet()){
			result += attr.hashCode()*simple;
			if (attr.equals(STYLE_ATTR)){
				result += normalizeStyleString(get(attr)).hashCode();
			} else if (attr.equals(CLASS_ATTR)){
				result += normalizeClassString(get(attr)).hashCode();
			} else {
				result += get(attr).hashCode();
			}
		}
		return result;
	}
	
	/**
	 * Checks if 2 values for "style" attribute of an HTML tag
	 * are equivalent (contain same CSS property : value pairs,
	 * but in different (or the same) order.
	 * Pairs are separated by semicolons and any amount of space,
	 * and names from values are separated by colon and any amount of space
	 * @param style1
	 * @param style2
	 * @return
	 */
	public static boolean equivalentStyles(String style1, String style2){
		if (style1 == null){
			if (style2 == null){
				return true; //both are nulls
			} else {
				return false; //one(#2) isn't null, while other is
			}
		} else if (style2 == null){
			return false; //one(#1) isn't null, while other is
		}
		//no nulls at this point
		//get rid of the new line symbols and tabulation
		//substituting them with spaces to not "jam" separate tokens together 
		style1 = style1.replaceAll(NL_TAB_REGEXP, SPACE);
		style2 = style2.replaceAll(NL_TAB_REGEXP, SPACE);
		//get rid of consecutive spaces
		style1 = style1.replaceAll(SPACE + "++", SPACE);
		style2 = style2.replaceAll(SPACE + "++", SPACE);
		//get rid of leading/trailing spaces
		style1 = style1.trim();
		style2 = style2.trim();
		//check if they were actually the same
		if (style1.equals(style2)){
			//literally equal except maybe for leading/trailing spaces
			//and text positioning with tabs/new lines
			return true;
		}
		//style rules in the style attribute value are
		//separated by semicolon with any amount of space on either side
		final char SEMICOLON = ';';
		//notice, that this delimiter will "eat up" all the empty styles
		//like in this case: "prop1:val1  ;  ;  ;;;; prop2 : val2"
		//you will only get 2 tokens and this: "  ;  ;  ;;;; " will be 
		//considered as a single delimiter.
		//atomic group and possessive quantifier used to speed up regexp
		final String DELIM = SPACE + "*+(?>" + SEMICOLON + SPACE + "*+)++";
		//split those to CSS property name : value pairs
		String[] styleRules1 = style1.split(DELIM);
		String[] styleRules2 = style2.split(DELIM);
		//should be the same amount of properties, or it's not equivalent
		if (styleRules1.length != styleRules2.length){
			return false;
		}
		//sort by CSS property name
		Arrays.sort(styleRules1);
		Arrays.sort(styleRules2);
		//remove the spaces between property name,
		//the colon and the value 
		final String COLON_W_SPACES = 
			SPACE + "*+:" + SPACE + "*+";
		final String COLON = ":";
		for (int i = 0; i < styleRules1.length; i++){
			styleRules1[i] = 
				styleRules1[i].replaceFirst(COLON_W_SPACES, COLON);
			styleRules2[i] = 
				styleRules2[i].replaceFirst(COLON_W_SPACES, COLON);
			if (!styleRules1[i].equals(styleRules2[i])){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Checks if 2 values for "class" attribute of an HTML tag
	 * are equivalent (contain same CSS class names, but in different
	 * (or same order) Ignores new line symbols and tabulation<br>
	 * Example:<br>
	 * <code>&lt;p class="styleName1 styleName2"&gt;</code><br>
	 * is equivalent to <br>
	 * <code>&lt;p class="  styleName1    styleName2  "&gt;</code><br>
	 * and to <br>
	 * <code>&lt;p class="styleName2 styleName1"&gt;</code>
	 * @param classSet1 
	 * @param classSet2
	 * @return true if the values are equivalent (including null values)
	 */
	public static boolean sameClassSet(String classSet1, String classSet2){
		if (classSet1 == null){
			if (classSet2 == null){
				return true; //both are nulls
			} else {
				return false; //one(#2) isn't null, while other is
			}
		} else if (classSet2 == null){
			return false; //one(#1) isn't null, while other is
		}
		//no nulls at this point
		//get rid of new line and tabulation symbols
		classSet1 = classSet1.replaceAll(NL_TAB_REGEXP, SPACE);
		classSet2 = classSet2.replaceAll(NL_TAB_REGEXP, SPACE);
		//trim leading/trailing spaces
		classSet1 = classSet1.trim();
		classSet2 = classSet2.trim();
		if (classSet1.equals(classSet2)){
			//literally equal except maybe for leading/trailing spaces
			//or for white space symbols (tab and new line)
			//notice that style names are case-sensitive.
			return true; 
		}
		//multiple class names in the class attributes
		//are separated by spaces - split into array of single classes
		final String DELIM = SPACE + "++";//"++" is possessive quantifier
		//splitting by any amount of spaces in between
		String[] set1 = classSet1.split(DELIM);
		String[] set2 = classSet2.split(DELIM);
		//should be the same amount of classes, or it's not equivalent
		if (set1.length != set2.length){
			return false;
		}
		//checking classes
		Arrays.sort(set1);
		Arrays.sort(set2);
		return Arrays.equals(set1, set2);
	}

	/**
	 * The <code>hashCode()</code> method should correspond to
	 * <code>equals</code> method, so we need a way to get the
	 * styles attribute value in the same representation we use
	 * when we're comparing. We could use this method in comparison
	 * method, however the way comparison method is written now
	 * is much faster, because it can fail or succeed long before 
	 * normalization is finished.
	 * @param styleVal - value of "style" attribute of an HTML tag.
	 * @return normalized representation of the provided value
	 */
	public static String normalizeStyleString(String styleVal){
		if (styleVal == null || styleVal.length() == 0){
			return styleVal; //nothing to Normalize
		} 
		//no nulls at this point
		//get rid of the new line symbols and tabulation
		//substituting them with spaces to not "jam" separate tokens together 
		styleVal = styleVal.replaceAll(NL_TAB_REGEXP, SPACE);
		//get rid of consecutive spaces
		styleVal = styleVal.replaceAll(SPACE + "++", SPACE);
		//get rid of leading/trailing spaces
		styleVal = styleVal.trim();
		//check if they there's anything left
		if (styleVal.length() == 0){
			return styleVal;
		}
		//style rules in the style attribute value are
		//separated by semicolon with any amount of space on either side
		final char SEMICOLON = ';';
		//notice, that this delimiter will "eat up" all the empty styles
		//like in this case: "prop1:val1  ;  ;  ;;;; prop2 : val2"
		//you will only get 2 tokens and this: "  ;  ;  ;;;; " will be 
		//considered as a single delimiter.
		//atomic group and possessive quantifier used to speed up regexp
		final String DELIM = SPACE + "*+(?>" + SEMICOLON + SPACE + "*+)++";
		//split those to CSS property name : value pairs
		String[] styleRules = styleVal.split(DELIM);
		//sort by CSS property name
		Arrays.sort(styleRules);
		//remove the spaces between property name,
		//the colon and the value 
		final String COLON_W_SPACES = 
			SPACE + "*+:" + SPACE + "*+";
		final String COLON = ":";
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < styleRules.length; i++){
			result.append(styleRules[i].replaceFirst(COLON_W_SPACES, COLON))
				  .append("; ");
		}
		//take away last trailing "; "
		result.setLength(result.length() - 2);
		return result.toString();
	}

	/**
	 * The <code>hashCode()</code> method should correspond to
	 * <code>equals</code> method, so we need a way to get the
	 * class attribute value in the same representation we use
	 * when we're comparing. We could use this method in comparison
	 * method, however the way comparison method is written now
	 * is much faster, because it can fail or succeed long before 
	 * normalization is finished.
	 * @param classVal - value of "class" attribute of an HTML tag.
	 * @return normalized representation of the provided value
	 */
	public static String normalizeClassString(String classVal){
		if (classVal == null || classVal.length() == 0){
			return classVal; //nothing to normalize
		}
		//no nulls at this point
		//get rid of new line and tabulation symbols
		classVal = classVal.replaceAll(NL_TAB_REGEXP, SPACE);
		//trim leading/trailing spaces
		classVal = classVal.trim();
		//multiple class names in the class attributes
		//are separated by spaces - split into array of single classes
		final String DELIM = SPACE + "++";//"++" is possessive quantifier
		//splitting by any amount of spaces in between
		String[] classNames = classVal.split(DELIM);
		//sorting classes
		Arrays.sort(classNames);
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < classNames.length; i++){
			result.append(classNames[i]).append(SPACE);
		}
		//take away last space
		result.setLength(result.length() - 1);
		return result.toString();
	}
	
	//just for a quick test
	public static void main(String[] args){
		String s1 = "margin-left:50px;font-size:16pt;";
		String s2 = "    font-size  :  16pt    ;  ;   ;  ; margin-left  : 50px   ";
		System.out.println("equal? -" + equivalentStyles(s1, s2));
	}
	
}
