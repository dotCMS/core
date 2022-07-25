package com.dotcms.rest.api.v1.experiment;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.rules.model.Rule;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class Experiment {

    private String name;
    private String key;
    private ExperimentStatus status;
    private Collection<Rule> targeting;
    private boolean uniquePerVisitor;
    private int lookBackWindowMinutes;

    private Collection<AnalyticEvent> events;
    private VariantsCollection variants;


    public Experiment(final String key, final String name, final ExperimentStatus status, final boolean uniquePerVisitor, final int lookBackWindowMinutes) {
        this.name = name;
        this.key = key;
        this.uniquePerVisitor = uniquePerVisitor;
        this.lookBackWindowMinutes = lookBackWindowMinutes;
        this.status = status;
    }

    public void setEvents(Collection<AnalyticEvent> events) {
        this.events = events;
    }

    public String getName() {
        return name;
    }

    public Collection<Rule> getTargeting() {
        return targeting;
    }

    public double getTraffic() {
        return 0;
    }

    public boolean getUniquePerVisitor() {
        return uniquePerVisitor;
    }

    public int getLookBackWindowMinutes() {
        return lookBackWindowMinutes;
    }

    public Collection<AnalyticEvent> getEvents() {
        return events;
    }

    public VariantsCollection getVariants() {
        return variants;
    }

    public String getVariantURL(final ExperimentVariant experimentVariant) {
        try {
            final String uri = this.variants.getPage().getURI();
            return uri + "?variant=" + experimentVariant.getVariant().getKey();
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    public String getURL() {
        try {
            return this.getVariants().getPage().getURI();
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

    public void setRules(final Collection<Rule> rules) {
        this.targeting = rules;
    }

    public String getKey() {
        return key;
    }

    public void setVariantsCollection(final VariantsCollection variantsCollection) {
        this.variants = variantsCollection;
    }

    public enum ExperimentStatus {
        START,
        STOP
    }
}
