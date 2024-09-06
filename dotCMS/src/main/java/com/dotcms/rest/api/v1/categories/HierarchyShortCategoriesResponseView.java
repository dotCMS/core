package com.dotcms.rest.api.v1.categories;

import com.dotcms.experiments.model.Experiment;
import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.categories.model.HierarchyShortCategory;

import java.util.List;

public class HierarchyShortCategoriesResponseView extends ResponseEntityView<List<HierarchyShortCategory>> {
    public HierarchyShortCategoriesResponseView(final List<HierarchyShortCategory> categories) {
        super(categories);
    }
}