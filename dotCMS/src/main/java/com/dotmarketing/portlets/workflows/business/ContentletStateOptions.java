package com.dotmarketing.portlets.workflows.business;

public class ContentletStateOptions {

    private final boolean isNew;
    private final boolean isPublish;
    private final boolean isArchived;
    private final boolean canLock;
    private final boolean isLocked;
    private final WorkflowAPI.RenderMode renderMode;

    public ContentletStateOptions(final boolean isNew,
                                  final boolean isPublish,
                                  final boolean isArchived,
                                  final boolean canLock,
                                  final boolean isLocked,
                                  final WorkflowAPI.RenderMode renderMode) {

        this.isNew = isNew;
        this.isPublish = isPublish;
        this.isArchived = isArchived;
        this.canLock = canLock;
        this.isLocked = isLocked;
        this.renderMode = renderMode;
    }

    public boolean isNew() {
        return isNew;
    }

    public boolean isPublish() {
        return isPublish;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public boolean isCanLock() {
        return canLock;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public WorkflowAPI.RenderMode getRenderMode() {
        return renderMode;
    }
}
