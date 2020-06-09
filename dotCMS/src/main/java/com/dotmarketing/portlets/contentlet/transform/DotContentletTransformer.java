package com.dotmarketing.portlets.contentlet.transform;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.List;

/**
 * This interface also introduces the hydration behavior
 */
public interface DotContentletTransformer extends DotMapViewTransformer {

    List<Contentlet> hydrate();

}
