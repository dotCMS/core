package com.dotcms.api.contenttype;

import com.dotcms.model.contenttype.ContentType;
import java.util.List;

/**
 * Context that handles pull and push operations over content types
 */
public interface ContentTypeService {

    /**
     * Obtains a list of all content types
     * @return
     */
    List<ContentType> getContentTypes() ;

}
