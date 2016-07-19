package com.dotcms.web.websocket;

import com.dotcms.web.websocket.delegate.bean.JobDelegateDataBean;

/**
 * 
 * @author Jose Castro
 * @version 1.0
 * @since Jul 13, 2016
 *
 */
public interface JobDelegate {

	/**
	 * 
	 * @param data
	 */
	public void execute(JobDelegateDataBean data);

}
