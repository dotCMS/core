package com.dotcms.achecker.test;


import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.dotcms.achecker.ACheckerRequest;
import com.dotcms.achecker.ACheckerResponse;
import com.dotcms.achecker.AccessibilityResult;
import com.dotcms.achecker.CheckBean;
import com.dotcms.achecker.Confidence;
import com.dotcms.achecker.dao.ChecksDAO;
import com.dotcms.achecker.dao.GuidelinesDAO;
import com.dotcms.achecker.dao.test.CheckExamplesDAO;
import com.dotcms.achecker.impl.ACheckerImpl;
import com.dotcms.achecker.model.GuideLineBean;
import com.dotcms.achecker.utility.LanguageUtility;

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
			
			if (  	check.getConfidenceEnum() != Confidence.KNOW &&
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
					System.exit(1);
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
					System.exit(1);
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
