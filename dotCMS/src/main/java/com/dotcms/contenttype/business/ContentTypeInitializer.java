package com.dotcms.contenttype.business;

import com.dotcms.config.DotInitializer;
import com.dotcms.contenttype.business.init.DotAnnouncementsInitializer;
import com.dotcms.contenttype.business.init.DotFavoritePageInitializer;

import java.util.Arrays;
import java.util.List;

/**
 * Initialiaze content types
 * @author jsanca
 */
public class ContentTypeInitializer implements DotInitializer {

    List<DotInitializer> contentTypesInitializers = Arrays.asList(
            new DotFavoritePageInitializer(),
            new DotAnnouncementsInitializer()
    );

    @Override
    public void init() {
        this.contentTypesInitializers.forEach(DotInitializer::init);
    }

}
