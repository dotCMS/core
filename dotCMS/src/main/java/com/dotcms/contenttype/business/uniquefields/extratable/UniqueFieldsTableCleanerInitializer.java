package com.dotcms.contenttype.business.uniquefields.extratable;

import com.dotcms.ai.listener.EmbeddingContentListener;
import com.dotcms.config.DotInitializer;
import com.dotmarketing.business.APILocator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@ApplicationScoped
public class UniqueFieldsTableCleanerInitializer implements DotInitializer {

    private final UniqueFieldsTableCleaner cleaner;

    @Inject
    public UniqueFieldsTableCleanerInitializer(final UniqueFieldsTableCleaner cleaner){
        this.cleaner = cleaner;
    }

    @Override
    public void init() {
        APILocator.getLocalSystemEventsAPI().subscribe(cleaner);
    }

}
