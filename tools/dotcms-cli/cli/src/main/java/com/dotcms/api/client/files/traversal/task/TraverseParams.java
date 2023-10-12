package com.dotcms.api.client.files.traversal.task;


import com.dotcms.api.client.files.traversal.data.Retriever;
import com.dotcms.cli.common.OutputOptionMixin;
import java.io.File;
import java.io.Serializable;
import org.jboss.logging.Logger;


public class TraverseParams implements Serializable {

    private final OutputOptionMixin output;
    private final Logger logger;
    private final  Retriever retriever;
    private final boolean siteExists;
    private final String sourcePath;
    private final File workspace;
    private final boolean removeAssets;
    private final boolean removeFolders;
    private final boolean ignoreEmptyFolders;
    private final boolean failFast;

    private TraverseParams(Builder builder) {
        this.output = builder.output;
        this.logger = builder.logger;
        this.retriever = builder.retriever;
        this.siteExists = builder.siteExists;
        this.sourcePath = builder.sourcePath;
        this.workspace = builder.workspace;
        this.removeAssets = builder.removeAssets;
        this.removeFolders = builder.removeFolders;
        this.ignoreEmptyFolders = builder.ignoreEmptyFolders;
        this.failFast = builder.failFast;
    }

    public OutputOptionMixin getOutput() {
        return output;
    }

    public Logger getLogger() {
        return logger;
    }

    public Retriever getRetriever() {
        return retriever;
    }

    public boolean isSiteExists() {
        return siteExists;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public File getWorkspace() {
        return workspace;
    }

    public boolean isRemoveAssets() {
        return removeAssets;
    }

    public boolean isRemoveFolders() {
        return removeFolders;
    }

    public boolean isIgnoreEmptyFolders() {
        return ignoreEmptyFolders;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private OutputOptionMixin output;
        private Logger logger;
        private Retriever retriever;
        private boolean siteExists;
        private String sourcePath;
        private File workspace;
        private boolean removeAssets;
        private boolean removeFolders;
        private boolean ignoreEmptyFolders;
        private boolean failFast;

        public Builder withOutput(OutputOptionMixin output) {
            this.output = output;
            return this;
        }

        public Builder withLogger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public Builder withRetriever(Retriever retriever) {
            this.retriever = retriever;
            return this;
        }

        public Builder withSiteExists(boolean siteExists) {
            this.siteExists = siteExists;
            return this;
        }

        public Builder withSourcePath(String sourcePath) {
            this.sourcePath = sourcePath;
            return this;
        }

        public Builder withWorkspace(File workspace) {
            this.workspace = workspace;
            return this;
        }

        public Builder withRemoveAssets(boolean removeAssets) {
            this.removeAssets = removeAssets;
            return this;
        }

        public Builder withRemoveFolders(boolean removeFolders) {
            this.removeFolders = removeFolders;
            return this;
        }

        public Builder withIgnoreEmptyFolders(boolean ignoreEmptyFolders) {
            this.ignoreEmptyFolders = ignoreEmptyFolders;
            return this;
        }

        public Builder withFailFast(boolean failFast) {
            this.failFast = failFast;
            return this;
        }

        public Builder from(TraverseParams traverseParams) {
            this.output = traverseParams.output;
            this.logger = traverseParams.logger;
            this.retriever = traverseParams.retriever;
            this.siteExists = traverseParams.siteExists;
            this.sourcePath = traverseParams.sourcePath;
            this.workspace = traverseParams.workspace;
            this.removeAssets = traverseParams.removeAssets;
            this.removeFolders = traverseParams.removeFolders;
            this.ignoreEmptyFolders = traverseParams.ignoreEmptyFolders;
            this.failFast = traverseParams.failFast;
            return this;
        }

        public TraverseParams build() {
            return new TraverseParams(this);
        }
    }
}
