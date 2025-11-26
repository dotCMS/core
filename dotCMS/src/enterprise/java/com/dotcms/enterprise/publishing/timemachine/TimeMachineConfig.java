/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.timemachine;

import java.util.ArrayList;
import java.util.List;

import com.dotcms.publishing.PublisherConfig;

public class TimeMachineConfig extends PublisherConfig {

	public TimeMachineConfig() {
		super();
	}
	
	@Override
	public List<Class> getPublishers() {
		List<Class> clazz = new ArrayList<>();
		clazz.add(TimeMachinePublisher.class);
		return clazz;
	}
	

	@Override
	public boolean liveOnly() {
		return true;
	}

}
