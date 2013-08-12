package com.dotcms.publishing;

import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PublisherAPIImpl implements PublisherAPI {

	@Override
	public PublishStatus publish(PublisherConfig config) throws DotPublishingException {

		return publish(config, new PublishStatus());
	}

	@Override
	public PublishStatus publish(PublisherConfig config, PublishStatus status) throws DotPublishingException {

		PushPublishLogger.log(this.getClass(), "Started Publishing Task", config.getId());

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

			if(config.isIncremental() && config.getEndDate()==null && config.getStartDate()==null) {
			    // if its incremental and start/end dates aren't se we take it from latest bundle
				if(BundlerUtil.bundleExists(config)){
					PublisherConfig p = BundlerUtil.readBundleXml(config);
					if(p.getEndDate() != null){
						config.setStartDate(p.getEndDate());
						config.setEndDate(new Date());
					}
					else{
					    config.setStartDate(null);
						config.setEndDate(new Date());
					}
				}
				else{
					config.setStartDate(null);
					config.setEndDate(new Date());
				}
			}

            //Before to build the bundle lets make sure it wasn't already created
            File compressedBundle = new File( ConfigUtils.getBundlePath() + File.separator + config.getId() + ".tar.gz" );
            if ( !compressedBundle.exists() ) {

                // Run bundlers
                File bundleRoot = BundlerUtil.getBundleRoot( config );

                BundlerUtil.writeBundleXML( config );
                for ( Class<IBundler> c : bundlers ) {

                    IBundler bundler = c.newInstance();
                    confBundlers.add( bundler );
                    bundler.setConfig( config );
                    BundlerStatus bs = new BundlerStatus( bundler.getClass().getName() );
                    status.addToBs( bs );
                    //Generate the bundler
                    bundler.generate( bundleRoot, bs );
                }
                config.setBundlers( confBundlers );
            }

			// run publishers
			for (Publisher p : pubs) {
				p.process(status);
			}

			PushPublishLogger.log(this.getClass(), "Completed Publishing Task", config.getId());
		} catch (Exception e) {
			Logger.error(PublisherAPIImpl.class, e.getMessage(), e);
			throw new DotPublishingException(e.getMessage(),e);
		}

		return status;
	}

}
