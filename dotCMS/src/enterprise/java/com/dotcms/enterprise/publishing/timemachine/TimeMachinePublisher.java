/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.timemachine;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.enterprise.publishing.bundlers.*;
import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig;

import java.util.ArrayList;
import java.util.List;

public class TimeMachinePublisher extends Publisher {

	@Override
	public PublisherConfig init(PublisherConfig config) throws DotPublishingException {
	    if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            throw new RuntimeException("need an enterprise license to run this");
	    
		this.config = super.init(config);
		return this.config;
	}

	@Override
	public PublisherConfig process(final PublishStatus status) throws DotPublishingException {
	    if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level)
            throw new RuntimeException("need an enterprise license to run this");
	    
		return config;
	}

	@Override
	public List<Class> getBundlers() {
		List<Class> list = new ArrayList<>();
		list.add(FileAssetBundler.class);
		list.add(HTMLPageAsContentBundler.class);
		list.add(URLMapBundler.class);
		list.add(BinaryExporterBundler.class);
		list.add(TimeMachineBundler.class);
		list.add(CSSExporterBundler.class);
		list.add(ShortyBundler.class);
		return list;
	}

}
