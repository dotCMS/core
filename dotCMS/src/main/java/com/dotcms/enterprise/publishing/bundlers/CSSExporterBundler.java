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