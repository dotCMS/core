/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.sitesearch;

import com.dotcms.publishing.PublisherConfig;
import com.google.common.collect.ImmutableList;
import java.util.List;


public class SiteSearchConfig extends PublisherConfig {

	public SiteSearchConfig() {
		super();
	}

	private boolean switchIndexWhenDone = false;
	
	boolean switchIndexWhenDone(){
		return switchIndexWhenDone;
	}

	public void setSwitchIndexWhenDone(boolean switchIndexWhenDone) {
		this.switchIndexWhenDone = switchIndexWhenDone;
	}

	private enum MyConfig {
		CRON_EXPRESSION, QUARTZ_JOB_NAME, RUN_NOW, INDEX_NAME, NEW_INDEX_NAME, NEW_INDEX_DEFAULT, INDEX_ALIAS, JOB_ID;
	}
	
	public String getCronExpression(){
		return (String) this.get(MyConfig.CRON_EXPRESSION.toString());
	}
	
	void setCronExpression(String cron){
		this.put(MyConfig.CRON_EXPRESSION.toString(), cron);
	}
	
	public void setJobId(String id) {
	    this.put(MyConfig.JOB_ID.toString(), id);
	}
	
	public String getJobId() {
	    return (String)this.get(MyConfig.JOB_ID.toString());
	}
	
	public String getJobName(){
		return (String) this.get(MyConfig.QUARTZ_JOB_NAME.toString());
	}
	
	public void setJobName(String id){
		this.put(MyConfig.QUARTZ_JOB_NAME.toString(), id);
	}

	public boolean runNow(){
		return this.get(MyConfig.RUN_NOW.toString()) !=null && Boolean
				.parseBoolean((String) this.get(MyConfig.RUN_NOW.toString()));
		
	}
	public void setRunNow(boolean once){
		this.put(MyConfig.RUN_NOW.toString(), once);
	}

	/**
	 * An existing index that needs to be dropped and replaced by the new one
	 * @return
	 */
	public String getIndexName(){
		return (String) this.get(MyConfig.INDEX_NAME.toString());
	}

	/**
	 * An existing index that needs to be dropped and replaced by the new one
	 * @param name
	 */
	public void setIndexName(final String name){
		this.put(MyConfig.INDEX_NAME.toString(), name);
	}

	/**
	 * An brand-new index that needs to be used to replaced by the one marked as current. Or the default one.
	 * @return
	 */
	public String getNewIndexName(){
		return (String) this.get(MyConfig.NEW_INDEX_NAME.toString());
	}

	/**
	 * An brand-new index that needs to be used to replaced by the one marked as current. Or the default one.
	 * @param name
	 */
	public void setNewIndexName(final String name){
		this.put(MyConfig.NEW_INDEX_NAME.toString(), name);
	}

	/**
	 * If there's an Alias we better save it right away. Cuz the api might return null leaving us with no alias to setup.
	 * @return
	 */
	public String getIndexAlias(){
		return (String) this.get(MyConfig.INDEX_ALIAS.toString());
	}

	/**
	 * If there's an Alias we better save it right away. Cuz the api might return null leaving us with no alias to setup.
	 * @param alias
	 */
	public void setIndexAlias(final String alias){
		this.put(MyConfig.INDEX_ALIAS.toString(), alias);
	}

	@Override
	public List<Class> getPublishers(){
		return ImmutableList.of(ESSiteSearchPublisher.class);
	}

	@Override
	public boolean liveOnly() {
	    //We only need live content to feed indices built on non-incremental mode.
	    //if we're building updating our index incrementally we also need to get non-live content.
	    // So we can remove it from our index.
		return !isIncremental();
	}
}
