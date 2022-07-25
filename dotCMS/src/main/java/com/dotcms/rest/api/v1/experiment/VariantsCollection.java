package com.dotcms.rest.api.v1.experiment;

import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import java.util.Collection;

public class VariantsCollection {
    private Collection<ExperimentVariant> variants;
    private IHTMLPage page;

    public VariantsCollection(
            Collection<ExperimentVariant> variants,
            IHTMLPage page) {
        this.variants = variants;
        this.page = page;
    }

    public Collection<ExperimentVariant> getAll() {
        return variants;
    }

    public IHTMLPage getPage() {
        return page;
    }
}
