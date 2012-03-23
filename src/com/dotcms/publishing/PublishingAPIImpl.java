package com.dotcms.publishing;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.util.Logger;

public class PublishingAPIImpl implements PublisherAPI {

	PublisherUtil util = new PublisherUtil();
	public void publish(PublisherConfig config) throws DotPublishingException {
		
		
		
		
		
		
		List<Publisher> pubs = new ArrayList<Publisher>();

		util.initBundle(config);
		
		for (Class<Publisher> c : config.getPublishers()) {

			Publisher p;
			try {
				p = c.newInstance();
			} catch (Exception e) {
				Logger.error(PublishingAPIImpl.class,e.getMessage());
				throw new DotPublishingException(e.getMessage());
			} 

			config = p.init(config);
			pubs.add(p);
		}

		for (Publisher p : pubs) {
			p.process();
		}

	}

}
