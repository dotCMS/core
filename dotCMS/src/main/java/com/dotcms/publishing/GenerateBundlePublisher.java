package com.dotcms.publishing;

import com.dotcms.publishing.output.BundleOutput;
import com.dotcms.publishing.output.DirectoryBundleOutput;
import com.dotcms.publishing.output.TarGzipBundleOutput;
import java.io.File;
import com.dotcms.publisher.pusher.PushPublisher;
import com.dotmarketing.util.Logger;
import java.io.IOException;

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
    public PublisherConfig process(final PublishStatus status) throws DotPublishingException {
        final File bundleRoot = BundlerUtil.getBundleRoot(this.config.getName(), false);
        Logger.info(this.getClass(), "Bundling Complete: " + bundleRoot);
        return this.config;
    }

    public BundleOutput createBundleOutput() throws IOException {
        return new TarGzipBundleOutput(config);
    }
    
}
