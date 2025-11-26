/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise;

import com.dotcms.enterprise.priv.HostAssetsJobImpl;
import com.dotcms.rest.PushPublisherJob;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.quartz.DotStatefulJob;
import com.dotmarketing.quartz.job.HostCopyOptions;
import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.Map;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;

public class HostAssetsJobProxy extends DotStatefulJob {

	public static final String DESTINATION_HOST_ID = "destinationHostId";
	public static final String SOURCE_HOST_ID 	   = "sourceHostId";
	public static final String COPY_OPTIONS 	   = "copyOptions";
	public static final String USER_ID 			   = "userId";

	public HostAssetsJobProxy () {

	}

	public void run(JobExecutionContext jobContext) throws JobExecutionException {

		HostAssetsJobImpl jobImpl = new HostAssetsJobImpl(this);
		jobImpl.run(jobContext);
	}

	/**
	 * Gets the job parameters
	 * @param trigger {@link Trigger}
	 * @return Map
	 */
	public final Map<String, Serializable> getExecutionData(final Trigger trigger) {

		final Map<String, Serializable> executionData = getExecutionData(trigger, HostAssetsJobProxy.class);
		return executionData;
	}

	@Override
	public void updateProgress(int currentProgress) {
		super.updateProgress(currentProgress);
	}
	
	@Override
	public void addMessage(String newMessage) {
		super.addMessage(newMessage);
	}

	/**
	 * Fires the job to copy a new site from existing site.
	 * @param destinationHostId
	 * @param sourceHostId
	 * @param hostCopyOptions
	 * @param userId
	 */
	public static void fireJob(
			final String destinationHostId,
			final String sourceHostId,
			final HostCopyOptions hostCopyOptions,
			final String userId) {
		final ImmutableMap<String, Serializable> nextExecutionData = ImmutableMap
				.of(DESTINATION_HOST_ID, destinationHostId, SOURCE_HOST_ID, sourceHostId,
						COPY_OPTIONS, hostCopyOptions, USER_ID, userId);
		try {
			DotStatefulJob.enqueueTrigger(nextExecutionData, HostAssetsJobProxy.class);
		} catch (Exception e) {
			Logger.error(HostAssetsJobProxy.class, "Error scheduling  copy site job", e);
			throw new DotRuntimeException("Error scheduling copy site job", e);
		}
	}

}
