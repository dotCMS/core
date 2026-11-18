package com.dotcms.content.elasticsearch.business;

import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.util.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The removal of destroyed content's binaries, resized-image cache and stored metadata, held until
 * the transaction that destroyed the content commits (#37782).
 * <p>
 * Removing them during the destroy loses them for good when the transaction later rolls back: a
 * folder delete refused on its third subfolder rolls back the rows of the first two, but their
 * files are already gone. So each destroy works out what to remove right away, while the content
 * type and the binary directory still exist, and schedules only the removal itself here.
 * <p>
 * There is one batch per transaction, registered as a synchronous commit listener the first time
 * something is scheduled; every later destroy in the same transaction adds to it, so a site or
 * content type delete does not register a listener per destroyed item. It runs on the committing
 * thread before the commit call returns, which keeps today's ordering for anything written after
 * the delete. A rollback discards it with the transaction's other listeners, and nothing is
 * removed.
 * <p>
 * Each part runs on its own: a failure is logged with what it left on disk and the rest still run,
 * and nothing is thrown, since an exception here would stop the transaction's other after-commit
 * work and reach the caller after the delete had already committed.
 */
final class BinaryRemovalAfterCommit implements Runnable {

    private static final String LISTENER_TAG = BinaryRemovalAfterCommit.class.getName();

    private final List<Part> parts = new ArrayList<>();

    private BinaryRemovalAfterCommit() {
    }

    /**
     * Schedules one part of a removal for after the current transaction commits, or runs it now
     * when no transaction is open, as a destroy outside a transaction always has.
     *
     * @param description what the part removes, logged if it fails, so it can be cleaned up by
     *                    hand
     * @param removal     the removal itself
     */
    static void schedule(final String description, final Runnable removal) {
        final Part part = new Part(description, removal);
        final Optional<BinaryRemovalAfterCommit> batch = HibernateUtil.getOrAddSyncCommitListener(
                LISTENER_TAG, BinaryRemovalAfterCommit::new);
        if (batch.isPresent()) {
            batch.get().parts.add(part);
        } else {
            part.runSafely();
        }
    }

    /**
     * Runs every part scheduled in the committed transaction, in the order they were scheduled.
     */
    @Override
    public void run() {
        this.parts.forEach(Part::runSafely);
    }

    /** One removal and what it removes. */
    private record Part(String description, Runnable removal) {

        /** Runs the removal, logging instead of throwing when it fails. */
        void runSafely() {
            try {
                this.removal.run();
            } catch (final Exception e) {
                Logger.warn(BinaryRemovalAfterCommit.class, String.format(
                        "Unable to remove %s after the delete committed; it is left in place: %s",
                        this.description, e.getMessage()), e);
            }
        }
    }
}
