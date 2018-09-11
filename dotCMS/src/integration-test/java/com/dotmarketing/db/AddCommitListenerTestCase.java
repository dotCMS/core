package com.dotmarketing.db;

class AddCommitListenerTestCase {
    private boolean asyncReindexCommitListeners;
    private boolean asyncCommitListeners;
    private Runnable dotRunnable;
    private int expectedAsyncListeners;
    private int expectedSyncListeners;

    public boolean isAsyncReindexCommitListeners() {
        return asyncReindexCommitListeners;
    }

    public boolean isAsyncCommitListeners() {
        return asyncCommitListeners;
    }

    public Runnable getDotRunnable() {
        return dotRunnable;
    }

    public int getExpectedAsyncListeners() {
        return expectedAsyncListeners;
    }

    public int getExpectedSyncListeners() {
        return expectedSyncListeners;
    }

    private AddCommitListenerTestCase(final boolean asyncReindexCommitListeners, final boolean asyncCommitListeners,
                                      final Runnable dotRunnable, final int expectedAsyncListeners,
                                      final int expectedSyncListeners) {
        this.asyncReindexCommitListeners = asyncReindexCommitListeners;
        this.asyncCommitListeners = asyncCommitListeners;
        this.dotRunnable = dotRunnable;
        this.expectedAsyncListeners = expectedAsyncListeners;
        this.expectedSyncListeners = expectedSyncListeners;
    }

    static class Builder {
        private boolean asyncReindexCommitListeners;
        private boolean asyncCommitListeners;
        private Runnable dotRunnable;
        private int expectedAsyncListeners;
        private int expectedSyncListeners;

        public Builder asyncReindexCommitListeners(boolean asyncReindexCommitListeners) {
            this.asyncReindexCommitListeners = asyncReindexCommitListeners;
            return this;
        }

        public Builder asyncCommitListeners(boolean asyncCommitListeners) {
            this.asyncCommitListeners = asyncCommitListeners;
            return this;
        }

        public Builder dotRunnable(Runnable dotRunnable) {
            this.dotRunnable = dotRunnable;
            return this;
        }

        public Builder expectedAsyncListeners(final int expectedAsyncListeners) {
            this.expectedAsyncListeners = expectedAsyncListeners;
            return this;
        }

        public Builder expectedSyncListeners(final int expectedSyncListeners) {
            this.expectedSyncListeners = expectedSyncListeners;
            return this;
        }

        public AddCommitListenerTestCase createReindexCommitListenerTestCase() {
            return new AddCommitListenerTestCase(asyncReindexCommitListeners, asyncCommitListeners,
                    dotRunnable, expectedAsyncListeners, expectedSyncListeners);
        }
    }
}
