package com.dotmarketing.portlets.categories.business;

import com.liferay.portal.model.User;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Outcome of {@link CategoryAPI#deleteCategoryAndChildren(List, User, boolean)}.
 * <p>
 * The delete cascades over the whole subtree, so the number of categories actually removed
 * is almost always larger than the number of inodes the caller asked for. Callers that only
 * look at the requested inodes cannot tell how much was deleted, which is why the cascade
 * total travels with the failures instead of being logged and discarded.
 *
 * @author dotCMS
 */
public class CategoryDeleteResult {

    private final int deletedCount;
    private final Map<String, String> failures;

    public CategoryDeleteResult(final int deletedCount, final Map<String, String> failures) {
        this.deletedCount = deletedCount;
        this.failures = null == failures
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(failures);
    }

    /**
     * Total number of categories physically removed, counting every descendant at every depth.
     * A category is counted once even when the batch contained both it and one of its
     * ancestors, since the cascade had already removed it by the time its own turn came.
     *
     * @return the number of deleted categories; {@code 0} when nothing was removed
     */
    public int getDeletedCount() {
        return deletedCount;
    }

    /**
     * The requested categories that were skipped, keyed by inode. The value is the reason,
     * one of the {@code DELETE_FAIL_REASON_*} constants on {@link CategoryAPI}. A skipped
     * category is skipped whole: nothing in its subtree was deleted.
     *
     * @return failures by inode; empty when everything requested was deleted
     */
    public Map<String, String> getFailures() {
        return failures;
    }

    /**
     * @return {@code true} when at least one requested category could not be deleted
     */
    public boolean hasFailures() {
        return !failures.isEmpty();
    }
}
