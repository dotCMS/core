package com.dotcms.publishing;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.dotmarketing.util.Logger;

public class PublisherAPIImpl implements PublisherAPI {
	
	
	
	@Override
	public PublishStatus publish(PublisherConfig config) throws DotPublishingException {
	
		return publish(config, new PublishStatus());
	}
		
	@Override
	public PublishStatus publish(PublisherConfig config, PublishStatus status) throws DotPublishingException {


		
		
		Logger.info(this.getClass(), "PubAPI: Starting Publishing Task for Bundle: "+ config.getId());
		try {

			List<Publisher> pubs = new ArrayList<Publisher>();
			List<Class> bundlers = new ArrayList<Class>();
			List<IBundler> confBundlers = new ArrayList<IBundler>();
			
			
			
			
			
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
			
			
			if(config.isIncremental() ){
				if(BundlerUtil.bundleExists(config)){
					PublisherConfig p = BundlerUtil.readBundleXml(config);
					if(p.getEndDate() != null){
						config.setStartDate(p.getEndDate());
						config.setEndDate(new Date());
					}
					else{
						config.setStartDate(p.getEndDate());
						config.setEndDate(new Date());	
						
					}
				}
				else{
					config.setStartDate(new Date(0));
					config.setEndDate(new Date());
				}
			}
			
			
			
			
			
			
			
			
			// run bundlers
			
			File bundleRoot = BundlerUtil.getBundleRoot(config);
			BundlerUtil.writeBundleXML(config);
			for (Class<IBundler> c : bundlers) {
				IBundler b = (IBundler) c.newInstance();
				confBundlers.add(b);
				b.setConfig(config);
				BundlerStatus bs = new BundlerStatus();
				status.addToBs(bs);
				Logger.info(this.getClass(), "PubAPI: Running Bundler  : "+b.getName());
				b.generate(bundleRoot, bs);
				Logger.info(this.getClass(), "PubAPI: Bundler Completed: "+b.getName());
			}
			config.setBundlers(confBundlers);

			// run publishers
			for (Publisher p : pubs) {
				Logger.info(this.getClass(), "PubAPI: Running Publisher    : "+p.getClass().getName());
				p.process();
				Logger.info(this.getClass(), "PubAPI: Publisher Completed  : "+p.getClass().getName());
			}
			Logger.info(this.getClass(), "PubAPI: Completed Publishing Task for Bundle: "+ config.getId());
		} catch (Exception e) {
			Logger.error(PublisherAPIImpl.class, e.getMessage());
			throw new DotPublishingException(e.getMessage());
		}

		return status;
	}

}
