package com.dotcms.contenttype.business.uniquefields.extratable;

import com.dotcms.ai.listener.EmbeddingContentListener;
import com.dotcms.config.DotInitializer;
import com.dotmarketing.business.APILocator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

/**
 * Subscribe the {@link UniqueFieldsTableCleaner} to listen by events when the unique_fields extra table need to be
 * cleaning up
 */
@Dependent
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
