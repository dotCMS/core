package com.dotcms.publishing;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.dotcms.publishing.bundlers.BundlerUtil;
import com.dotmarketing.util.Logger;

public class PublishingAPIImpl implements PublisherAPI {
	
	@Override
	public void publish(PublisherConfig config) throws DotPublishingException {

		try {

			List<Publisher> pubs = new ArrayList<Publisher>();

			List<Class> bundlers = new ArrayList<Class>();
			for (Class<Publisher> c : config.getPublishers()) {
				Publisher p = c.newInstance();

				for (Class clazz : p.getBundlers()) {
					if (!bundlers.contains(clazz)) {
						bundlers.add(clazz);
					}
				}
			}
			config.put("bundlers", bundlers);
			File bundleRoot = BundlerUtil.initBundle(config);

			for (Class<IBundler> c : bundlers) {
				IBundler b = (IBundler) c.newInstance();
				b.setConfig(config);
				BundlerStatus bs = new BundlerStatus();
				b.generate(bundleRoot, bs);
			}

			for (Class<Publisher> c : config.getPublishers()) {

				Publisher p = c.newInstance();

				config = p.init(config);
				pubs.add(p);
			}

			for (Publisher p : pubs) {
				p.process();
			}
		} catch (Exception e) {
			Logger.error(PublishingAPIImpl.class, e.getMessage());
			throw new DotPublishingException(e.getMessage());
		}

	}

}
