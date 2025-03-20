/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.bundlers;

import com.dotcms.csspreproc.CSSPreProcessServlet;
import com.dotmarketing.util.Config;

public class CSSExporterBundler extends AbstractServletBundler {

	private final static String CSS_EXPORT_BUNDLER_PATTERN =
			Config.getStringProperty("CSS_EXPORT_BUNDLER_PATTERN","(\\/(DOTLESS|DOTSASS)\\/[\\w-]+\\/[\\w-\\d]+(\\/([^\"\\)'\\s]+)?)?|\\/[\\w-]+\\/[\\w-\\d]+(\\/([^\"\\)'\\s]+)?)?\\.dotsass)");

	public CSSExporterBundler() {
		super(CSS_EXPORT_BUNDLER_PATTERN, new CSSPreProcessServlet() );
	}
}
