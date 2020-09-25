package com.dotcms.publishing;

import java.io.File;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotmarketing.util.Logger;

/**
 * This GenerateBundlePublisher uses the same config/bundlers as the PushPublisher - the only
 * difference is that once this publisher has built the bundle it does not do anything with it. The
 * GenerateBundlePublisher is meant to be used to build the bundle when a user selects to download a
 * bundle
 * 
 * @author will
 *
 */
public class GenerateBundlePublisher extends PushPublisher {

    @Override
    public PublisherConfig process(PublishStatus status) throws DotPublishingException {
        File bundleRoot = BundlerUtil.getBundleRoot(this.config);
        Logger.info(this.getClass(), "Bundling Complete:" + bundleRoot);
        return this.config;
        
        
    }
    
    
    
}
