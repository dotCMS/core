package com.dotcms.achecker.test;

import com.dotcms.achecker.ACheckerRequest;
import com.dotcms.achecker.ACheckerResponse;
import com.dotcms.achecker.AccessibilityResult;
import com.dotcms.achecker.impl.ACheckerImpl;
import com.dotcms.achecker.utility.ContentDownloader;
import com.dotcms.achecker.utility.URLConnectionInputStream;
import com.dotcms.achecker.utility.Utility;

public class TestAChecker {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		String uri = "http://www.apple.com";
		// uri = "http://www.futouring.com";
		
		if (!Utility.getValidURI(uri)) {
			System.out.println("Invalid uri: '" + uri + "'");
			return;
		}
		
		// Download content to validate
		URLConnectionInputStream urlConnection = new URLConnectionInputStream(uri);
		urlConnection.setupProxy("proxy.eng.it", "3128", "micmastr", "apriti80");
		String validate_content = ContentDownloader.getContent(urlConnection);
		
		// PrettyDump.dump(validate_content, System.out);

		// Guideline to check
		String guide = "WCAG1-A";

		// Create checker
		ACheckerImpl checker = new ACheckerImpl();

		// Check content
		ACheckerRequest request = new ACheckerRequest(null, validate_content, guide, false);
		ACheckerResponse response = checker.validate(request);
		
		// Write response
		int count = 1;
		for ( AccessibilityResult error : response.getErrors() ) {
			System.out.println(count + ") " + dump(error));
			count ++;
		}
		// System.out.println(response.getErrors());
		
	}

	public static String dump(AccessibilityResult error) {
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("Line: " + error.getLine_number());
		buffer.append(", Column: " + error.getCol_number());
		buffer.append(", ID: " + error.getCheck().getCheck_id());
		buffer.append(", Confidence: " + error.getCheck().getConfidenceEnum());
		buffer.append(", Description: " + error.getCheck().getDescription());
		buffer.append(", HowRepair: " + error.getCheck().getHow_to_repair());
		buffer.append(", HTML: " + error.getCheck().getHtml_tag());
		
		return buffer.toString();
	}
	
	public static ACheckerResponse checkStringa(String stringa ) throws Exception{
		
		String guide = "BITV1,508,STANCA,WCAG1-A,WCAG1-AA,WCAG1-AAA,WCAG2-A,WCAG2-AA,WCAG2-AAA";
			
		// Create checker
		ACheckerImpl checker = new ACheckerImpl();

		// Check content
		ACheckerRequest request = new ACheckerRequest(null, stringa, guide, true);
		ACheckerResponse response = checker.validate(request);
				
		return response;
	}


}
