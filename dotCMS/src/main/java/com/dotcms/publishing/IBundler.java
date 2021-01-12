package com.dotcms.publishing;

import com.dotcms.enterprise.publishing.sitesearch.SiteSearchConfig;
import com.dotcms.enterprise.publishing.timemachine.TimeMachineConfig;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.output.BundleOutput;
import com.dotmarketing.util.Constants;

import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
     *
	 * @param output
	 * @param status     Object to keep track of the generation process inside this Bundler
	 * @throws DotBundleException If there is an exception while this Bundles is generating the Bundle content
     */
    void generate (BundleOutput output, BundlerStatus status ) throws DotBundleException;

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

	default List<Long> getSortedConfigLanguages( PublisherConfig config, final long defaultLanguageId ) {
		List<Long> languages = new ArrayList<>();
		if (config.isStatic()){
			languages.addAll(config.getLanguages().stream().map(Long::valueOf).collect(Collectors.toList()));
		} else {
			languages.add(config.getLanguage());
		}

		// To sort assets so the default-language ones come first
		Collections.sort(languages, new Comparator<Long>() {

			final Long defaultLangId = defaultLanguageId;

			public int compare(Long o1, Long o2) {
				if (o1.equals(defaultLangId) && o2.equals(defaultLangId)) {
					return 0;
				} else if (o1.equals(defaultLangId)) {
					return -1;
				} else if (o2.equals(defaultLangId)) {
					return 1;
				} else {
					return 0;
				}
			}
		});
		return languages;
	}

}