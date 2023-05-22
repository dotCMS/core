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

package com.dotcms.enterprise.achecker.impl;


import java.util.LinkedList;
import java.util.List;

import com.dotcms.enterprise.license.LicenseLevel;
import org.apache.commons.logging.Log;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.achecker.AChecker;
import com.dotcms.enterprise.achecker.ACheckerRequest;
import com.dotcms.enterprise.achecker.ACheckerResponse;
import com.dotcms.enterprise.achecker.AccessibilityResult;
import com.dotcms.enterprise.achecker.dao.GuidelinesDAO;
import com.dotcms.enterprise.achecker.model.GuideLineBean;
import com.dotcms.enterprise.achecker.utility.Constants;
import com.dotcms.enterprise.achecker.validation.AccessibilityValidator;


/*
 * This is the web service interface to check accessibility on a given URI
 * Expected parameters:
 * id: to identify the user. must be given
 * uri: The URL of the document to validate. must be given
 * guide: The guidelines to validate against. 
 *        can be multiple guides, separated by comma (,)
 */

public class ACheckerImpl implements AChecker {
	
	private static final Log LOG = org.apache.commons.logging.LogFactory.getLog(ACheckerImpl.class );
	
	public ACheckerResponse validate(ACheckerRequest request) throws Exception {
	    if(LicenseUtil.getLevel()< LicenseLevel.STANDARD.level)
            throw new RuntimeException("need enterprise license");
	    
		if ( request.isFragment() ) {
			String pre = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">";
			pre += "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\" xml:lang=\"en\">\r\n";
			String post = "\r\n</html>";
			String newContent = pre + request.getContent() + post;
			request.setContent(newContent);
		}
		
		try{
			// Generate guidelines
			List<Integer> gids = new LinkedList<>();
			String[] guides = request.getGuide().split(",");
			GuidelinesDAO guidelinesDAO = new GuidelinesDAO();
			for (String abbr : guides) {

				if ( abbr.trim().equals("") ){
					continue;
				}
				 GuideLineBean  result = guidelinesDAO.getEnabledGuidelinesByAbbr(abbr, true);

				//for ( GuideLineBean item : result ) {
					Integer id = (Integer) result.getGuideline_id();
					gids.add(id);
				//}
			}

			// set to default guideline if no input guidelines
			if ( gids.isEmpty() ) {

				 GuideLineBean  result = guidelinesDAO.getEnabledGuidelinesByAbbr(Constants.DEFAULT_GUIDELINE, true);

				//for ( GuideLineBean item : result ) {
					Integer id = (Integer) result.getGuideline_id();
					gids.add(id);
				//}
			}
			if( LOG.isDebugEnabled() ){
				LOG.debug("content da validare " + request.getContent() );
			}

			// validating uri content
			AccessibilityValidator aValidator = new AccessibilityValidator(request.getContent(), gids);

			aValidator.validate();

			List<AccessibilityResult> result = aValidator.getValidationResults();

			List<AccessibilityResult> errors = aValidator.getValidationErrorRpt();
			if( LOG.isDebugEnabled() ){
				LOG.debug("result" + result);
			}

			if( LOG.isDebugEnabled() ){
				LOG.debug("errors" + errors);
			}
			
			ACheckerResponse response = new ACheckerResponse(result, errors, request.getLang());

			// Adjust line numbers if was a fragment
			// Adjust language for check beans
			for ( AccessibilityResult r : response.getResults()) {
				if ( request.isFragment() )
					r.setLine_number( r.getLine_number() - 1 );
				if ( r.getCheck() != null )
					r.getCheck().setLang(request.getLang());
			}
			
			return response;
			
		}catch (Exception e) {
			LOG.error(e.getMessage() ,e );
			throw e;
		} 

	}
}
