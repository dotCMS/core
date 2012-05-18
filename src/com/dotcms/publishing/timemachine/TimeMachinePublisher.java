package com.dotcms.publishing.timemachine;

import java.util.ArrayList;
import java.util.List;

import com.dotcms.publishing.DotPublishingException;
import com.dotcms.publishing.PublishStatus;
import com.dotcms.publishing.Publisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.bundlers.DirectoryMirrorBundler;
import com.dotcms.publishing.bundlers.FileAssetBundler;
import com.dotcms.publishing.bundlers.StaticHTMLPageBundler;
import com.dotcms.publishing.bundlers.URLMapBundler;

public class TimeMachinePublisher extends Publisher {

	@Override
	public PublisherConfig init(PublisherConfig config) throws DotPublishingException {
		this.config = super.init(config);
		return this.config;
	}

	@Override
	public PublisherConfig process(final PublishStatus status) throws DotPublishingException {
		return config;
	}

	@Override
	public List<Class> getBundlers() {
		List<Class> list = new ArrayList<Class>();
		list.add(FileAssetBundler.class);
		list.add(StaticHTMLPageBundler.class);
		list.add(URLMapBundler.class);
		list.add(DirectoryMirrorBundler.class);
		return list;
	}

}
