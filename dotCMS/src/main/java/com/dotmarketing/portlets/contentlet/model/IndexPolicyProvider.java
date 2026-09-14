package com.dotmarketing.portlets.contentlet.model;

import com.dotmarketing.util.Config;

/**
 * This class provides the {@link IndexPolicy} for a single content
 * @author jsanca
 */
public class IndexPolicyProvider {

    private volatile IndexPolicy singleContentIndexPolicy = null;
    private volatile IndexPolicy dependenciesIndexPolicy = null;
    private static class SingletonHolder {
        private static final IndexPolicyProvider INSTANCE = new IndexPolicyProvider();
    }


    /**
     * Get the instance.
     * @return IndexPolicyProvider
     */
    public static IndexPolicyProvider getInstance() {

        return IndexPolicyProvider.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Give the index policy for single content. Defaults to {@link IndexPolicy#DEFER},
     * overridable with {@code INDEX_POLICY_SINGLE_CONTENT}.
     *
     * <p>DEFER means the index write is handed to a post-commit listener rather than applied
     * inline, which is why content deletion needs a durable journal record to survive a failed
     * or lost listener (see #37276).</p>
     *
     * @return  IndexPolicy
     */
    public IndexPolicy forSingleContent () {

        if (null == this.singleContentIndexPolicy) {
            this.singleContentIndexPolicy =
                    IndexPolicy.parseIndexPolicy(Config.getStringProperty("INDEX_POLICY_SINGLE_CONTENT", "DEFER"));
        }

        return this.singleContentIndexPolicy;
    }
    /**
     * Give the index policy for a content's dependencies. Defaults to
     * {@link IndexPolicy#DEFER}, overridable with {@code INDEX_POLICY_DEPENDENCIES}.
     *
     * @return  IndexPolicy
     */
    public IndexPolicy forContentDependencies () {

        if (null == this.dependenciesIndexPolicy) {
            this.dependenciesIndexPolicy =
                    IndexPolicy.parseIndexPolicy(Config.getStringProperty("INDEX_POLICY_DEPENDENCIES", "DEFER"));
        }

        return this.dependenciesIndexPolicy;
    }
}
