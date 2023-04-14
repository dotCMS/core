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

package com.dotcms.enterprise.achecker.test;


import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.dotcms.enterprise.achecker.ACheckerRequest;
import com.dotcms.enterprise.achecker.ACheckerResponse;
import com.dotcms.enterprise.achecker.AccessibilityResult;
import com.dotcms.enterprise.achecker.CheckBean;
import com.dotcms.enterprise.achecker.Confidence;
import com.dotcms.enterprise.achecker.dao.ChecksDAO;
import com.dotcms.enterprise.achecker.dao.GuidelinesDAO;
import com.dotcms.enterprise.achecker.dao.test.CheckExamplesDAO;
import com.dotcms.enterprise.achecker.impl.ACheckerImpl;
import com.dotcms.enterprise.achecker.model.GuideLineBean;
import com.dotcms.enterprise.achecker.utility.LanguageUtility;

public class TestExampleAChecker {
	
	class AnalysisResult {
		
		public int success = 0;
		
		public int fail = 0;
		
		public int not_found = 0;
		
	}
	
	private ChecksDAO checksDAO;
	
	private GuidelinesDAO guidelinesDAO;
	
	private CheckExamplesDAO checkExamplesDAO;
	
	private ACheckerImpl checker;
	
	private List<String> exclude;
	
	private enum Verified { FALSE, TRUE, NOT_FOUND } ;
	
	public static void main(String[] args) throws Exception {
		TestExampleAChecker tester = new TestExampleAChecker();
		tester.testAll();
	}
	
	public TestExampleAChecker() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		checksDAO = new ChecksDAO();
		guidelinesDAO = new GuidelinesDAO();
		checkExamplesDAO = new CheckExamplesDAO();
		checker = new ACheckerImpl();
		exclude = Arrays.asList(); // "BITV1", "508", "STANCA", "WCAG1-A", "WCAG1-AA"); 
	}
	
	public void testAll() throws Exception {
		for ( GuideLineBean guideline : guidelinesDAO.getOpenGuidelines()) {
			String title = (String) guideline.getTitle();
			String abbr = (String) guideline.getAbbr();
			if ( exclude.contains(abbr))
				continue;
			System.out.println("Start Analysis: [ " + title + " ]");
			AnalysisResult result = verifyChecksForGuideline(abbr);
			System.out.println("Summary:");
			System.out.println("\tSUCCESS:" + result.success);
			System.out.println("\tFAIL:" + result.fail);
			System.out.println("\tNOT FOUND:" + result.not_found);
			System.out.println("");
		}
	}
	
	public void printLocalized(String subject, String content) {
		System.out.println(subject);
		System.out.println("\t" + LanguageUtility._AC(content));
	}
	
	public AnalysisResult verifyChecksForGuideline(String guideline) throws Exception {
		
		AnalysisResult result = new AnalysisResult();
		boolean active = true;
		int startCheckID = 138;

		Integer guidelineID = (Integer)((GuideLineBean)  guidelinesDAO.getGuidelineByAbbr(guideline)).getGuideline_id();
		
		List<CheckBean> checks = checksDAO.getChecksByGuidelineID(guidelineID);

		for (CheckBean check : checks) {
			
			if ( check.getCheck_id() == startCheckID)
				active = true;
			
			if (!active)
				continue;
			
			if (  	check.getConfidenceEnum() != Confidence.KNOWN &&
					check.getConfidenceEnum() != Confidence.LIKELY )
				continue;

			// List all failed tests
			List<Map<String, Object>> rows = checkExamplesDAO.getByCheckIDAndType(check.getCheck_id(), 0);
			
			for ( Map<String, Object> row : rows ) {

				String content = (String) row.get("content");

				// Validate
				ACheckerRequest request = new ACheckerRequest("eng", content, guideline, false);
				ACheckerResponse response = checker.validate(request);
				Verified success = isCheckVerified(check.getCheck_id(), response, false);
				
				System.out.println("[FALSE] Check: " + check.getCheck_id() + ", TestID: " + row.get("check_example_id") + ", Result: " + success);
				if ( success == Verified.FALSE ) {
					System.out.println(response);
					System.out.println("Tag: " + check.getHtml_tag());
					System.out.println("Content:");
					System.out.println(content);
					System.out.println("Function:");
					System.out.println(check.getFunc());
					result.fail ++;
				}
				else if ( success == Verified.TRUE ) {
					result.success ++;
				}
				else {
					System.out.println(response);
					System.out.println("Tag: " + check.getHtml_tag());
					System.out.println("Content:");
					System.out.println(content);
					System.out.println("Function:");
					System.out.println(check.getFunc());
					result.not_found ++;
					return null;
				}
			}

			// List all failed tests
			rows = checkExamplesDAO.getByCheckIDAndType(check.getCheck_id(), 1);
			
			for ( Map<String, Object> row : rows ) {

				String content = (String) row.get("content");

				// Validate
				ACheckerRequest request = new ACheckerRequest(null, content, guideline, false);
				ACheckerResponse response = checker.validate(request);
				Verified success = isCheckVerified(check.getCheck_id(), response, true);
				
				System.out.println("[TRUE] Check: " + check.getCheck_id() + ", TestID: " + row.get("check_example_id") + ", Result: " + success);
				if ( success == Verified.FALSE ) {
					System.out.println(response);
					System.out.println("Tag: " + check.getHtml_tag());
					System.out.println("Content:");
					System.out.println(content);
					System.out.println("Function:");
					System.out.println(check.getFunc());

					printLocalized("Test Procedure:", check.getTest_procedure());
					printLocalized("Test Expected Result:", check.getTest_expected_result());
					printLocalized("Test Failed Result:", check.getTest_failed_result());
					System.out.println();
					result.fail ++;
				}
				else if ( success == Verified.TRUE ) {
					result.success ++;
				}
				else {
					System.out.println(response);
					System.out.println("Tag: " + check.getHtml_tag());
					System.out.println("Content:");
					System.out.println(content);
					System.out.println("Function:");
					System.out.println(check.getFunc());

					printLocalized("Test Procedure:", check.getTest_procedure());
					printLocalized("Test Expected Result:", check.getTest_expected_result());
					printLocalized("Test Failed Result:", check.getTest_failed_result());
					System.out.println();
					result.not_found ++;
					return null;
				}
			}

		}
		
		return result;

	}

	private Verified isCheckVerified(int checkID, ACheckerResponse response, boolean success) {
		boolean found = false;
		for ( AccessibilityResult result : response.getResults()) {
			if ( result.getCheck().getCheck_id() == checkID) {
				found = true;
				if ( result.isSuccess() == success ) {
					return Verified.TRUE;
				}
			}
		}

		if ( !found ) {
			return Verified.TRUE;
			// return Verified.NOT_FOUND;
		}
		
		return Verified.FALSE;
	}
	
}
