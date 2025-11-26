/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.achecker;


import java.util.Hashtable;

import com.dotcms.enterprise.achecker.impl.ACheckerImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	
	private AChecker tm;
	
	public void start(BundleContext context) throws Exception {
		
		try {

			tm = new ACheckerImpl();

			Hashtable<String, String> props = new Hashtable<>();
			context.registerService(AChecker.class.getName(), tm, props);

			// System.out.println("Added ACheckerImpl service");

			String stringa = "<p>Michele</p>";
			String guide = "BITV1,508,STANCA,WCAG1-A,WCAG1-AA,WCAG1-AAA,WCAG2-A,WCAG2-AA,WCAG2-AAA";

			// Create checker
			ACheckerImpl checker = new ACheckerImpl();

			// Check content
			ACheckerRequest request = new ACheckerRequest(null, stringa, guide, true);

			ACheckerResponse response = checker.validate(request);

			// Write response
			int count = 1;
			for ( AccessibilityResult error : response.getErrors() ) {
				System.out.println(count + ") " + dump(error));
				count ++;
			}
			// System.out.println(response.getErrors());
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private String dump(AccessibilityResult error) {
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
	public void stop(BundleContext context) throws Exception {
		
		tm = null;
		
		// System.out.println("Removed ACheckerImpl service");

	}
	
}
