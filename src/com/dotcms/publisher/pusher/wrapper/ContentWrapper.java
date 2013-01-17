package com.dotcms.publisher.pusher.wrapper;

import java.util.List;
import java.util.Map;

import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.tag.model.Tag;

public interface ContentWrapper {
	Contentlet getContent();
	List<Map<String, Object>> getMultiTree();
	List<Map<String, Object>> getTree();
	List<Tag> getTags();
	Operation getOperation();
	ContentletVersionInfo getInfo();
}
