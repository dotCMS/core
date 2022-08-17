package com.dotcms.rest.api.v1.experiments;

import com.dotcms.experiments.model.Experiment.Status;

public class ExperimentForm {
    private String name;
    private String description;
    private Status status;
    private String pageId;

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }
}
