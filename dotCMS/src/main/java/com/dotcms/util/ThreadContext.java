package com.dotcms.util;

/**
 * Encapsulates Thread Local context information
 *
 * <p>What is left here is deliberately narrow: a single mutable flag that travels <b>callee to
 * caller</b>. Code deep in the stack records that the deferred reindex must include dependencies
 * (see {@code ThreadContextUtil.ifReindex}), and code higher up reads it afterwards to decide how
 * to index (see {@code WorkflowAPIImpl.fireWorkflowPostCheckin}).</p>
 *
 * <p>That direction of travel is why this is still a thread local rather than a
 * {@link ScopedValue}: a scoped binding is immutable, so a callee cannot use one to hand a value
 * back to its caller. The reindex flag, which does descend from caller to callee, is a scoped value
 * in {@link ThreadContextUtil}.</p>
 *
 * @author jsanca
 */
public class ThreadContext {

    // when the reindex happens later, the api call can set this to true in order to tell at the end of the thread process to do reindex including dependencies
    private boolean includeDependencies = false;

    public boolean isIncludeDependencies() {
        return includeDependencies;
    }

    public void setIncludeDependencies(boolean includeDependencies) {
        this.includeDependencies = includeDependencies;
    }
}
