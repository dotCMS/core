package com.dotcms.rest.api.v1.experiment;


import com.dotmarketing.portlets.rules.model.Rule;
import java.util.Collection;

public class ExperimentForm {

    private String name;
    private String key;
    private boolean uniquePerVisitor;
    private int lookBackWindowMinutes;
    private String pageInode;
    private Collection<String> targeting;
    private Collection<ExperimentVariant> variants;
    private Collection<AnalyticEvent> events;

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public boolean isUniquePerVisitor() {
        return uniquePerVisitor;
    }

    public int getLookBackWindowMinutes() {
        return lookBackWindowMinutes;
    }

    public String getPageInode() {
        return pageInode;
    }

    public Collection<String> getTargeting() {
        return targeting;
    }

    public Collection<ExperimentVariant> getVariants() {
        return variants;
    }

    public Collection<AnalyticEvent> getEvents() {
        return events;
    }
}
