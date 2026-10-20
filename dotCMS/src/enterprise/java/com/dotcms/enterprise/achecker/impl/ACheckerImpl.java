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

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.achecker.AChecker;
import com.dotcms.enterprise.achecker.ACheckerRequest;
import com.dotcms.enterprise.achecker.ACheckerRequestValidator;
import com.dotcms.enterprise.achecker.ACheckerResponse;
import com.dotcms.enterprise.achecker.AccessibilityResult;
import com.dotcms.enterprise.achecker.dao.GuidelinesDAO;
import com.dotcms.enterprise.achecker.model.GuideLineBean;
import com.dotcms.enterprise.achecker.utility.Constants;
import com.dotcms.enterprise.achecker.validation.AccessibilityValidator;
import com.dotmarketing.business.DotValidationException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;


/*
 * This is the web service interface to check accessibility on a given URI
 * Expected parameters:
 * id: to identify the user. must be given
 * uri: The URL of the document to validate. must be given
 * guide: The guidelines to validate against. 
 *        can be multiple guides, separated by comma (,)
 */

public class ACheckerImpl implements AChecker {
	
	/**
	 * Checks the request content against the requested Accessibility Guidelines.
	 * <p>The request's comma-separated guideline abbreviations are trimmed, blank entries are
	 * skipped, and each one must match an existing guideline (letter case is ignored). When no
	 * abbreviation is given, the default guideline is used. Fragment content is wrapped in a
	 * minimal HTML document before it is checked, and the reported line numbers are adjusted
	 * back.</p>
	 *
	 * @param request the {@link ACheckerRequest} holding the content, guidelines, language and
	 *                fragment flag
	 * @return the {@link ACheckerResponse} with the results and errors of the check
	 * @throws DotValidationException if a guideline abbreviation is not accepted or does not
	 *                                match an existing guideline
	 * @throws Exception if the license level is too low, the default guideline is missing, or the
	 *                   check fails
	 */
	@Override
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
			GuidelinesDAO guidelinesDAO = new GuidelinesDAO();
			for (String abbr : ACheckerRequestValidator.guidelineTokens(request.getGuide())) {
				GuideLineBean guideline = guidelinesDAO.getEnabledGuidelinesByAbbr(abbr, true);
				if ( guideline == null ) {
					throw new DotValidationException(ACheckerRequestValidator.UNSUPPORTED_GUIDELINES_MESSAGE);
				}
				gids.add(guideline.getGuideline_id());
			}

			// set to default guideline if no input guidelines
			if ( gids.isEmpty() ) {
				GuideLineBean defaultGuideline = guidelinesDAO.getEnabledGuidelinesByAbbr(Constants.DEFAULT_GUIDELINE, true);
				if ( defaultGuideline == null ) {
					throw new DotDataException("The default Accessibility Checker guideline is not available");
				}
				gids.add(defaultGuideline.getGuideline_id());
			}

			// validating uri content
			AccessibilityValidator aValidator = new AccessibilityValidator(request.getContent(), gids);

			aValidator.validate();

			List<AccessibilityResult> result = aValidator.getValidationResults();

			List<AccessibilityResult> errors = aValidator.getValidationErrorRpt();
			Logger.debug(this, () -> String.format("Accessibility check finished with %d results and %d errors",
					result == null ? 0 : result.size(), errors == null ? 0 : errors.size()));

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
			
		} catch (final DotValidationException e) {
			throw e;
		} catch (final Exception e) {
			Logger.error(this, "Accessibility check failed: " + e.getClass().getName(), e);
			throw e;
		}

	}
}
