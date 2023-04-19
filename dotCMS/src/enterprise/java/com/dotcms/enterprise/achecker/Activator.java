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

			Hashtable<String, String> props = new Hashtable<String, String>();
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
