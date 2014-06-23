package com.dotcms.publisher.integrity;

import javax.servlet.ServletContext;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.rest.IntegrityResource.ProcessStatus;
import com.dotcms.rest.IntegrityUtil;
import com.dotmarketing.util.Logger;

public class IntegrityDataGeneratorThread extends Thread {

	private PublishingEndPoint requesterEndPoint;
	public ServletContext servletContext;

	public IntegrityDataGeneratorThread(PublishingEndPoint mySelf, ServletContext servletContext) {
		this.requesterEndPoint = mySelf;
		this.servletContext = servletContext;
	}

	public void run() {

		try {

			if(requesterEndPoint==null)
				throw new Exception("Not valid endpoint provided");

			servletContext.setAttribute("integrityDataGenerationStatus", ProcessStatus.PROCESSING);

			IntegrityUtil integrityUtil = new IntegrityUtil();
			integrityUtil.generateDataToCheckZip(requesterEndPoint.getId());


		} catch (Exception e) {

			Logger.error(IntegrityDataGeneratorThread.class, "Error generating data to check", e);
			servletContext.setAttribute("integrityDataGenerationStatus", ProcessStatus.ERROR);
			servletContext.setAttribute("integrityDataGenerationError", e.getMessage());


		} finally {

        	servletContext.setAttribute("integrityDataGenerationStatus", ProcessStatus.FINISHED);
        }
	}

}
