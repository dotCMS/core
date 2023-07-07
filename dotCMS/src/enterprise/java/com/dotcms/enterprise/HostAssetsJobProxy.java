/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
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
