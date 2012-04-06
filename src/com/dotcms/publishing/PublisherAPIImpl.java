package com.dotcms.publishing;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.dotcms.publishing.bundlers.BundlerUtil;
import com.dotmarketing.util.Logger;

public class PublisherAPIImpl implements PublisherAPI {
	
	@Override
	public void publish(PublisherConfig config) throws DotPublishingException {

		try {

			List<Publisher> pubs = new ArrayList<Publisher>();
			List<Class> bundlers = new ArrayList<Class>();
			
			
			
			
			
			
			// init publishers
			for (Class<Publisher> c : config.getPublishers()) {
				Publisher p = c.newInstance();
				config = p.init(config);
				pubs.add(p);
				// get bundlers
				for (Class clazz : p.getBundlers()) {
					if (!bundlers.contains(clazz)) {
						bundlers.add(clazz);
					}
				}
			}
			
			
			
			// run bundlers
			config.put("bundlers", bundlers);
			File bundleRoot = BundlerUtil.getBundleRoot(config);
			for (Class<IBundler> c : bundlers) {
				IBundler b = (IBundler) c.newInstance();
				b.setConfig(config);
				BundlerStatus bs = new BundlerStatus();
				b.generate(bundleRoot, bs);
			}


			// run publishers
			for (Publisher p : pubs) {
				p.process();
			}
		} catch (Exception e) {
			Logger.error(PublisherAPIImpl.class, e.getMessage());
			throw new DotPublishingException(e.getMessage());
		}

	}

}
