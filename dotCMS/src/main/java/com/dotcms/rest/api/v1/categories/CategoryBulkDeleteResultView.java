package com.dotcms.rest.api.v1.categories;

import com.dotcms.rest.api.BulkResultView;
import com.dotcms.rest.api.FailedResultView;
import java.util.List;

/**
 * Bulk delete result for categories.
 * <p>
 * Extends the standard bulk shape rather than redefining it: {@code successCount} keeps its
 * usual meaning across every bulk endpoint — how many of the <em>requested</em> items
 * succeeded — and {@code deletedCount} adds what is specific to categories, namely how many
 * rows the cascade actually removed once descendants are included. Deleting one parent of a
 * 15-category subtree therefore reports {@code successCount: 1, deletedCount: 15}.
 *
 * @author dotCMS
 */
public class CategoryBulkDeleteResultView extends BulkResultView {

    private final Long deletedCount;

    public CategoryBulkDeleteResultView(final Long successCount, final Long skippedCount,
                                        final List<FailedResultView> fails,
                                        final Long deletedCount) {
        super(successCount, skippedCount, fails);
        this.deletedCount = deletedCount;
    }

    /**
     * @return total categories removed, descendants at every depth included
     */
    public Long getDeletedCount() {
        return deletedCount;
    }
}
