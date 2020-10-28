package com.dotmarketing.portlets.contentlet.transform;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.List;

/**
 * This interface also introduces the hydration behavior
 */
public interface DotContentletTransformer extends DotMapViewTransformer {

    /**
     * A hydrate method typically will add all the extra params but still return a contentlet.
     * @return list of contentlets
     */
    List<Contentlet> hydrate();

}
