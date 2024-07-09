package com.dotcms.concurrent.monitor;

import java.io.Serializable;

/**
 * Class to manifest the progress of a bean
 * @author jsanca
 */
public class ProgressBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private final int progress;
    private final String message;
    private final boolean completed;
    private final boolean failed;
    private final Object processId;
    private final Object subProcessId;

    public ProgressBean(final Builder builder) {
        super();
        this.progress = builder.progress;
        this.message = builder.message;
        this.completed = builder.completed;
        this.failed = builder.failed;
        this.processId = builder.processId;
        this.subProcessId = builder.subProcessId;
    }

    public int getProgress() {
        return progress;
    }

    public String getMessage() {
        return message;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isFailed() {
        return failed;
    }

    public Object getProcessId() {
        return processId;
    }

    public Object getSubProcessId() {
        return subProcessId;
    }

    public static class Builder {

        private int progress;
        private String message;
        private boolean completed;
        private boolean failed;
        private Object processId;
        private Object subProcessId;

        public Builder progress(final int progress) {
            this.progress = progress;
            return this;
        }

        public Builder message(final String message) {
            this.message = message;
            return this;
        }

        public Builder completed(final boolean completed) {
            this.completed = completed;
            return this;
        }

        public Builder failed(final boolean failed) {
            this.failed = failed;
            return this;
        }

        public Builder processId(final Object processId) {
            this.processId = processId;
            return this;
        }

        public Builder subProcessId(final Object subProcessId) {
            this.subProcessId = subProcessId;
            return this;
        }

        public ProgressBean build() {
            return new ProgressBean(this);
        }
    }

    @Override
    public String toString() {
        return "ProgressBean [progress=" + progress + ", message=" + message + ", completed=" + completed + ", failed=" + failed + ", processId=" + processId + ", subProcessId=" + subProcessId + "]";
    }
}
