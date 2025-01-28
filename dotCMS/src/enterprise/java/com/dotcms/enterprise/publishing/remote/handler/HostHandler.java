/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.remote.handler;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.receiver.handler.IHandler;
import com.dotcms.publishing.PublisherConfig;

import java.io.File;

/**
 * This handler class is part of the Push Publishing mechanism that deals with Site-related information inside a bundle
 * and saves it in the receiving instance. This class will read and process only the {@link com.dotmarketing.beans.Host}
 * data files. Keep in mind that Sites are objects of type
 * {@link com.dotmarketing.portlets.contentlet.model.Contentlet},
 * so they're handled and saved via the {@link ContentHandler}.
 * <p>
 * dotCMS supports the addition multiple Sites that can all share the same templates, containers, files, and content as
 * needed. You can create as many Sites as your server/network hardware specifications will allow. The Site addresses
 * will, of course, need to be configured/maintained in the DNS records of your organization by a system administrator.
 *
 * @author root
 * @since Mar 7, 2013
 */
public class HostHandler implements IHandler {
	private ContentHandler contentHandler;


	public HostHandler(PublisherConfig config) {
		contentHandler = new ContentHandler(config);
	}

	@Override
	public String getName() {
		return this.getClass().getName();
	}

	@Override
	public void handle(File bundleFolder) throws Exception {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
            throw new RuntimeException("need an enterprise pro license to run this");
        }
		contentHandler.handle(bundleFolder, true);
	}


}
