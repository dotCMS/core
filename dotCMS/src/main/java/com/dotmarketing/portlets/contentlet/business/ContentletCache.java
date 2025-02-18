package com.dotmarketing.portlets.contentlet.business;

import com.dotcms.content.elasticsearch.business.ESContentFactoryImpl.TranslatedQuery;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.Date;
import java.util.Optional;

//This interface should have default package access
public abstract class ContentletCache implements Cachable {

	public abstract Contentlet add(String inode,Contentlet content);
    public abstract Contentlet add(Contentlet content);
	public abstract Contentlet get(String inode);

	public abstract void clearCache();

	public abstract void remove(String key);

    public abstract void addTranslatedQuery(String key, TranslatedQuery translatedQuery);

    public abstract TranslatedQuery getTranslatedQuery(String key);

    public abstract void remove(Contentlet contentlet);

    public abstract void addTimeMachine(Date timeMachineDate, String identifier, Contentlet content);

    public abstract Optional<Contentlet> getTimeMachine(Date timeMachineDate, String identifier, String variant);

    public abstract void invalidateTimeMachine(Contentlet content);

    public abstract void invalidateTimeMachine();
}
