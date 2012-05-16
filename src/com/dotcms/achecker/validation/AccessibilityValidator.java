package com.dotcms.achecker.validation;

import static com.dotcms.achecker.utility.Utility.col;
import static com.dotcms.achecker.utility.Utility.row;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.dotcms.achecker.AccessibilityResult;
import com.dotcms.achecker.CheckBean;
import com.dotcms.achecker.dao.ChecksDAO;
import com.dotcms.achecker.parsing.ExpressionEvaluator;
import com.dotcms.achecker.parsing.XMLParser;
import com.dotcms.achecker.utility.CheckFuncUtility;
import com.dotcms.achecker.utility.Constants;
import com.dotcms.achecker.utility.Globals;
import com.dotcms.achecker.utility.Utility;

/************************************************************************/
/* ACheckerImpl                                                             */
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
 * AccessibilityValidator
 * Class for accessibility validate
 * This class checks the accessibility of the given html based on requested guidelines. 
 * @access	public
 * @author	Cindy Qi Li
 * @package checker
 */

public class AccessibilityValidator {
	
	// Number of success for each check_id
	private Map<Integer, Integer> num_success = new HashMap<Integer, Integer>();

	// number of errors
	private int num_of_errors = 0;              

	// html content to check
	private String validate_content;               

	// array, guidelines to check on
	private List<Integer> guidelines;

	// all check results, including success ones and failed ones
	private List<AccessibilityResult> result = new LinkedList<AccessibilityResult>();

	// all check results, including success ones and failed ones
	private List<AccessibilityResult> errors = new LinkedList<AccessibilityResult>();

	// array of the to-be-checked check_ids 
	private Set<Integer> allChecks = new TreeSet<Integer>();

	// array of the to-be-checked check_ids 
	private Map<String, Set<Integer>> checkIdsForTagName = new HashMap<String, Set<Integer>>();

	// array of prerequisite check_ids of the to-be-checked check_ids 
	private Map<Integer, Set<Integer>> checkPrerequisites = new HashMap<Integer, Set<Integer>>();

	// array of all the check functions 
	private Map<Integer, String> check_func_array = new HashMap<Integer, String>();

	// dom of $validate_content
	private Document content_dom;

	/**
	 * public
	 * $content: string, html content to check
	 * $guidelines: array, guidelines to check on
	 */
	public AccessibilityValidator(String content, List<Integer> guidelines) {
		this.validate_content = content;
		this.guidelines = guidelines;
	}

	/* public
	 * Validation
	 */
	public void validate() throws Exception {

		// dom of the content to be validated
		this.content_dom = XMLParser.readXML(this.validate_content);

		// prepare gobal vars used in BasicFunctions.class.php to fasten the validation
		this.prepare_global_vars();

		// set arrays of check_id, prerequisite check_id, next check_id
		this.prepare_check_arrays(this.guidelines);

		this.validate_element(this.content_dom.getElementsByTagName("html"));

		this.myfinalize();

		// end of validation process
	}

	/** private
	 * set global vars used in Checks.class.php and BasicFunctions.class.php
	 * to fasten the validation process.
	 * return nothing.
	 * @throws Exception 
	 */
	private void prepare_global_vars() throws Exception {

		// find all header tags which are used in BasicFunctions.class.php
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();

		XPathExpression expr1 = xpath.compile("//h1|//h2|//h3|//h4|//h5|//h6|//h7");
		Object result1 = expr1.evaluate(this.content_dom, XPathConstants.NODESET);
		Globals.header_array = (NodeList) result1;

		XPathExpression expr2 = xpath.compile("//base[@href]/@href");
		Object result2 = expr2.evaluate(this.content_dom, XPathConstants.STRING);
		Globals.base_href = (String) result2;

		// set all check functions
		ChecksDAO checksDAO = new ChecksDAO();

		List<CheckBean> rows = checksDAO.getAllOpenChecks();
		if ( rows != null && rows.size() > 0 ) {
			for (CheckBean row : rows ) {
				this.check_func_array.put((Integer) row.getCheck_id(),  CheckFuncUtility.convertCode((String) row.getFunc() ));
			}
		}
	}

	/**
	 * private
	 * generate arrays of check ids, prerequisite check ids, next check ids
	 * array structure:
	 check_array
	 (
	 [html_tag] => Array
	 (
	 [0] => check_id 1
	 [1] => check_id 2
	 ...
	 )
	 ...
	 )

	 prerequisite_check_array
	 (
	 [check_id] => Array
	 (
	 [0] => prerequisite_check_id 1
	 [1] => prerequisite_check_id 2
	 ...
	 )
	 ...
	 )

//	 next_check_array
//	 (
//	 [check_id] => Array
//	 (
//	 [0] => next_check_id 1
//	 [1] => next_check_id 2
//	 ...
//	 )
	 ...
	 )
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	private void prepare_check_arrays(List<Integer> guidelines) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {

		// validation process

		ChecksDAO checksDAO = new ChecksDAO();

		// generate array of "all element"
		List<CheckBean> rows = checksDAO.getOpenChecksForAllByGuidelineIDs(guidelines);

		if (rows != null && rows.size() > 0) {
			for ( CheckBean  row : rows ) {
				this.allChecks.add((Integer) row.getCheck_id() );
			}
		}

		// generate array of check_id
		rows = checksDAO.getOpenChecksNotForAllByGuidelineIDs(guidelines);

		if (rows != null && rows.size() > 0) {
			for ( CheckBean row : rows ) {

				if ( this.checkIdsForTagName.get(row.getHtml_tag() ) == null ) {
					this.checkIdsForTagName.put((String) row.getHtml_tag( ), new TreeSet<Integer>());
				}

				this.checkIdsForTagName.get((String) row.getHtml_tag( )).add((Integer) row.getCheck_id());

			}
		}

		// generate array of prerequisite check_ids
		List<Map<String, Object>>  rowspre = checksDAO.getOpenPreChecksByGuidelineIDs(guidelines);

		Map<Integer, Set<Integer>> checkPrerequisites = new HashMap<Integer, Set<Integer>>();

		if (rowspre != null && rowspre.size() > 0) {
			for ( Map<String, Object> row : rowspre ) {
				
				Integer prerequisiteCheckId = (Integer) row.get("prerequisite_check_id");
				Integer checkId = (Integer) row.get("check_id");
				
				Set<Integer> list = checkPrerequisites.get(checkId);
				if ( list == null ) {
					list = new TreeSet<Integer>();
					checkPrerequisites.put(checkId, list);
				}

				list.add(prerequisiteCheckId);
				
			}
		}

		this.checkPrerequisites = checkPrerequisites;

	}

	/**
	 * private
	 * Recursive function to validate html elements
	 */
	private void validate_element(NodeList element_array) {

		Map<String, Set<Integer>> check_array = new HashMap<String, Set<Integer>>();

		for ( int i = 0; i < element_array.getLength(); i ++ ) {

			Node e = element_array.item(i);
			
			if ( e instanceof Text )
				continue;
			
			Set<Integer> list = this.checkIdsForTagName.get(e.getNodeName());
			if ( list != null ) {
				list.addAll(this.allChecks);
				check_array.put(e.getNodeName(), list);
			}
			else
				check_array.put(e.getNodeName(), this.allChecks);

			boolean check_result = false;

			for ( Integer check_id : check_array.get(e.getNodeName())) {

				// System.out.println("Verify check: " + check_id + " for tag '" + e.getNodeName() + "' - " + truncate(Utility.getPlainNodeContent(e).replaceAll("\\n", " "), 100));

				// check prerequisite ids first, if fails, report failure and don't need to proceed with $check_id
				boolean prerequisite_failed = false;

				if ( this.checkPrerequisites.get(check_id) != null ) {

					for ( Integer prerequisite_check_id : this.checkPrerequisites.get(check_id)) {

						// System.out.println("\tVerify prerequisite: " + prerequisite_check_id);
						
						check_result = this.checkedCheck(e, prerequisite_check_id);

						if (check_result == false)
						{
							// System.out.println("\t* FAIL");
							prerequisite_failed = true;
							break;
						}
						else {
							// System.out.println("\t* SUCCESS");
						}
					}
				}

				// if prerequisite check passes, proceed with current check_id
				if (! prerequisite_failed)
				{
					// System.out.println("- Check: " + check_id + " - prerequisite passed");
					check_result = this.checkedCheck(e, check_id);
				}
			}

			this.validate_element(e.getChildNodes());

		}
	}

	private boolean checkedCheck(Node e, int check_id) {
		try {
			return check(e, check_id);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return false;
	}

	/**
	 * private
	 * check given html dom node for given check_id, save result into $this->result
	 * parameters:
	 * $e: simple html dom node
	 * $check_id: check id
	 *
	 * return "success" or "fail"
	 * @throws Exception 
	 */
	private boolean check(Node e, int check_id) throws Exception {

		int col_number = col(e);
		int line_number = row(e);

		String css_code = null;

		AccessibilityResult result = get_check_result(line_number, col_number, check_id);
		
		// This check is already verified
		if ( result != null )
			return true;
			
		// Get expression to check
		String expression = check_func_array.get(check_id);
		
		// expression = Utility.phpToJavaExpression(expression);

		// System.out.println("\t\tExp: '" + expression + "'");

		// Evaluate expression
		ExpressionEvaluator evaluator = new ExpressionEvaluator();
		evaluator.setVariable("global_e", e);
		evaluator.setVariable("global_content_dom", this.content_dom);
		evaluator.setVariable("global_check_id", check_id);
		
		// System.err.println("Check: " + check_id);
		Object check_result = evaluator.evaluate(expression);

		// Adjust result to boolean
		Boolean checked_result = (Boolean) check_result;

		// System.out.println("\t\tresult: " + checked_result);

		ChecksDAO checksDAO = new ChecksDAO();

		CheckBean row = checksDAO.getCheckByID(check_id);

		if (checked_result == null)
		{
			System.err.println("Warning in check (forced to true): " + e.getNodeName() + " - " + expression);

			// System.err.println("Result was: " + check_result.getClass() + " - " + check_result);
			
			// when check_result is not true/false, must be something 
			// wrong with the check function.
			// show warning message and skip this check

			// $msg->addError(array('CHECK_FUNC', $row['html_tag'].': '._AC($row['name'])));

			// skip this check
			checked_result = Boolean.TRUE;

		}

		String html_code = Utility.getNodeContent(e);
		html_code = Utility.truncateTo(html_code, Constants.DISPLAY_PREVIEW_HTML_LENGTH);
		String image = null;
		String image_alt = null;

		if ( checked_result ) {
			if ( this.num_success.get(check_id) == null ) {
				this.num_success.put(check_id, 1);
			}
			else {
				this.num_success.put(check_id, this.num_success.get(check_id) + 1);
			}
		}
		else {
			
			// find out preview images for validation on <img>
			if ( ((String) row.getHtml_tag()).equalsIgnoreCase("img")) 
			{
				// find out image alt text for preview image
				Element img = (Element) e;
				if ( img.getAttribute("alt") == null )
					image_alt = "NOT DEFINED";
				else if ( img.getAttribute("alt").equals(""))
					image_alt = "EMPTY";
				else
					image_alt = img.getAttribute("alt");
			}

		}

		result = this.save_result(line_number, col_number, html_code, row, checked_result, image, image_alt, css_code);
		return result.isSuccess();

	}

	//MB
	/**
	 * public 
	 * get number of success errors
	 */
	public Map<Integer, Integer> get_num_success() {
		return this.num_success;
	}

	/**
	 * private
	 * get check result from $result. Return null if the result is not found.
	 * Parameters:
	 * $line_number: line number in the content for this check
	 * $check_id: check id
	 */
	private AccessibilityResult get_check_result(int line_number, int col_number, int check_id) {
		for ( AccessibilityResult oneResult : this.result ) {
			if ( oneResult.getCheck().getCheck_id() != check_id )
				continue;
			if ( oneResult.getLine_number() != line_number )
				continue;
			if ( oneResult.getCol_number() != col_number )
				continue;
			return oneResult;
		}
		return null;
	}

	/**
	 * private
	 * save each check result
	 * Parameters:
	 * $line_number: line number in the content for this check
	 * $check_id: check id
	 * $result: result to save
	 */
	private AccessibilityResult save_result(
			int line_number, 
			int col_number, 
			String htmlCode, 
			CheckBean check, 
			boolean result, 
			String image, 
			String imageAlt, 
			String cssCode) {

		AccessibilityResult r = new AccessibilityResult(line_number, col_number, check, result);
		r.setHtmlCode(htmlCode);
		r.setImage(image);
		r.setImageAlt(imageAlt);
		r.setCssCode(cssCode);
		this.result.add(r);
		
		if (!result)
			this.errors.add(r);
		
		return r;
	}

	/**
	 * private 
	 * generate class value: array of error results, number of errors
	 */
	private void myfinalize() {
		this.num_of_errors = this.result.size();
	}

	/**
	 * public 
	 * return array of all checks that have been done, including successful and failed ones
	 */
	public List<AccessibilityResult> getValidationResults() {
		return this.result;
	}

	/**
	 * public 
	 * return array of all checks that have been done, including successful and failed ones
	 */
	public List<AccessibilityResult> getValidationErrorRpt() {
		return this.errors;
	}

	/**
	 * public 
	 * return number of errors
	 */
	public int getNumOfValidateError() {
		return this.num_of_errors;
	}

	/**
	 * public 
	 * return array of all checks that have been done by check id, including successful and failed ones
	 */
	public List<AccessibilityResult> getResultsByCheckID(int check_id) {
		List<AccessibilityResult> rtn = new LinkedList<AccessibilityResult>();
		for ( AccessibilityResult oneResult : this.result ) {
			if ( oneResult.getCheck().getCheck_id() == check_id ) {
				rtn.add(new AccessibilityResult(oneResult));
			}
		}
		return rtn;
	}

	/**
	 * public 
	 * return array of all checks that have been done by line number, including successful and failed ones
	 */
	public List<AccessibilityResult> getResultsByLine(int line_number) {
		List<AccessibilityResult> rtn = new LinkedList<AccessibilityResult>();
		for ( AccessibilityResult oneResult : this.result ) {
			if ( oneResult.getLine_number() == line_number ) {
				rtn.add(new AccessibilityResult(oneResult));
			}
		}
		return rtn;
	}
}

