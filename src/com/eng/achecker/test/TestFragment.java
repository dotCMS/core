package com.eng.achecker.test;

import com.eng.achecker.ACheckerRequest;
import com.eng.achecker.ACheckerResponse;
import com.eng.achecker.AccessibilityResult;
import com.eng.achecker.impl.ACheckerImpl;

public class TestFragment {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		String content = "<p>rrr</p>";
		
		ACheckerRequest request = new ACheckerRequest(null, content, "WCAG1-AAA", true);

		ACheckerImpl checker = new ACheckerImpl();
		
		ACheckerResponse response = checker.validate(request);
		
		for ( AccessibilityResult result : response.getErrors()) {
			System.out.println(result);
			System.out.println(result.getCheck().dump());
		}

	}

}
