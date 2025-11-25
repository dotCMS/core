package com.dotcms.ai.api;


/**
 * This class is in charge of providing the {@link ImageAPI}.
 * @author jsanca
 */
public interface ImageAPIProvider {

    ImageAPI getImageAPI(Object... initArguments);
}
