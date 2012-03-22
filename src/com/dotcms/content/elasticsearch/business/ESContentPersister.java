package com.dotcms.content.elasticsearch.business;

import java.util.List;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

public abstract class ESContentPersister extends Thread {


	public abstract void setContentlet(Contentlet con) ;

	public abstract void setContentlets(List<Contentlet> contentlets) ;

	@Override
	public abstract void run();

}