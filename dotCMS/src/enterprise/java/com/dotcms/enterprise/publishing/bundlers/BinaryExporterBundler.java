/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

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
