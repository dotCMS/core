package com.dotcms.publishing;

import com.dotcms.enterprise.publishing.sitesearch.SiteSearchConfig;
import com.dotcms.enterprise.publishing.timemachine.TimeMachineConfig;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotmarketing.util.Constants;

import java.io.File;
import java.io.FileFilter;

/**
 * The Purpose of the IGenerators is to provide a way to say how to write out the different parts and objects of the bundle
 *
 * @author jasontesser
 */
public interface IBundler {

    String getName ();

    void setConfig ( PublisherConfig pc );

    void setPublisher ( IPublisher publisher );

    /**
     * Generates depending of the type of content this Bundler handles parts and objects that will be add it later
     * to a Bundle.
     *
     * @param bundleRoot Where the Bundle we are creating will live.
     * @param status     Object to keep track of the generation process inside this Bundler
     * @throws DotBundleException If there is an exception while this Bundles is generating the Bundle content
     */
    void generate ( File bundleRoot, BundlerStatus status ) throws DotBundleException;

    FileFilter getFileFilter ();

    /**
     * Returs the Agent Browser depending on the instance of the Push Publisher.
     *
     * @param publisherConfig
     * @return
     */
    default String getUserAgent(PublisherConfig publisherConfig){
        if (publisherConfig instanceof SiteSearchConfig){
            return Constants.USER_AGENT_DOTCMS_SITESEARCH;
        }
        if (publisherConfig instanceof TimeMachineConfig){
            return Constants.USER_AGENT_DOTCMS_TIMEMACHINE;
        }
        if (publisherConfig instanceof PushPublisherConfig){
            return Constants.USER_AGENT_DOTCMS_PUSH_PUBLISH;
        }
        return Constants.USER_AGENT_DOTCMS_BROWSER;
    }

}