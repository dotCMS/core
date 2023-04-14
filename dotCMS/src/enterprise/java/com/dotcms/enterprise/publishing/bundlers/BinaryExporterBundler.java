package com.dotcms.enterprise.publishing.bundlers;

import com.dotmarketing.servlets.BinaryExporterServlet;
import com.dotmarketing.util.Config;

public class BinaryExporterBundler extends AbstractServletBundler {

	private final static String BINARY_EXPORT_BUNDLER_PATTERN =
			Config.getStringProperty("BINARY_EXPORT_BUNDLER_PATTERN","\\/contentAsset\\/[\\w-]+\\/[\\w-\\d]+(\\/([^\"\\)'\\s]+)?)?");

	public BinaryExporterBundler() {
		super(BINARY_EXPORT_BUNDLER_PATTERN, new BinaryExporterServlet() );
	}
}