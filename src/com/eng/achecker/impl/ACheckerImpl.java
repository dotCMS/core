package com.eng.achecker.impl;


import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;

import com.eng.achecker.AChecker;
import com.eng.achecker.ACheckerRequest;
import com.eng.achecker.ACheckerResponse;
import com.eng.achecker.AccessibilityResult;
import com.eng.achecker.dao.GuidelinesDAO;
import com.eng.achecker.model.GuideLineBean;
import com.eng.achecker.utility.Constants;
import com.eng.achecker.validation.AccessibilityValidator;

/************************************************************************/
/* ACheckerImpl                                                             */
/************************************************************************/
/* Copyright (c) 2008 - 2011                                            */
/* Inclusive Design Institute                                           */
/*                                                                      */
/* This program is free software. You can redistribute it and/or        */
/* modify it under the terms of the GNU General Public License          */
/* as published by the Free Software Foundation.                        */
/************************************************************************/
// $Id$

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
		
		if ( request.isFragment() ) {
			String pre = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">";
			pre += "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\" xml:lang=\"en\">\r\n";
			String post = "\r\n</html>";
			String newContent = pre + request.getContent() + post;
			request.setContent(newContent);
		}
		
		try{
			// Generate guidelines
			List<Integer> gids = new LinkedList<Integer>();
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
