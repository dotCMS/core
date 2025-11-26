/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
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
