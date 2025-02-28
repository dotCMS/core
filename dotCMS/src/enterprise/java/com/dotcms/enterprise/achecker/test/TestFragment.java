/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.achecker.test;

import com.dotcms.enterprise.achecker.ACheckerRequest;
import com.dotcms.enterprise.achecker.ACheckerResponse;
import com.dotcms.enterprise.achecker.AccessibilityResult;
import com.dotcms.enterprise.achecker.impl.ACheckerImpl;

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
