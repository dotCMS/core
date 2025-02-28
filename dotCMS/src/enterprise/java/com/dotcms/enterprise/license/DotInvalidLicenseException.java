/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license;

import java.io.IOException;

public class DotInvalidLicenseException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	String message;

	@Override
	public String getMessage() {
		return message;
	}

	public DotInvalidLicenseException(final String string) {
		this.message = string;
	}

	public DotInvalidLicenseException(String string, IOException e) {
		this.message = string;
		initCause(e);
	}

}
