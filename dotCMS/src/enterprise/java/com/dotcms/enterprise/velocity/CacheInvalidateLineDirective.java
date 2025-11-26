/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.velocity;

import java.io.IOException;
import java.io.Writer;

import com.dotcms.enterprise.license.LicenseLevel;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.parser.node.Node;

import com.dotmarketing.business.CacheLocator;

public class CacheInvalidateLineDirective extends DotDirective {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String getName() {
		return "dotcacheinvalidate";
	}

	public int getType() {
		return LINE;
	}
	

	public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException,
			ResourceNotFoundException, ParseErrorException, MethodInvocationException {
		String key = String.valueOf(node.jjtGetChild(0).value(context));
		CacheLocator.getBlockDirectiveCache().remove(key);
		
		
		return true;
	}
	@Override
	protected int[] getAllowedVersions() {
		return new int[]{LicenseLevel.STANDARD.level, LicenseLevel.PROFESSIONAL.level,
				LicenseLevel.PRIME.level, LicenseLevel.PLATFORM.level};
	}
}
