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
